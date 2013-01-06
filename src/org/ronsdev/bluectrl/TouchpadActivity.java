/*
 * Copyright (C) 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ronsdev.bluectrl;

import org.ronsdev.bluectrl.daemon.DaemonActivity;
import org.ronsdev.bluectrl.daemon.DaemonService;
import org.ronsdev.bluectrl.widget.ComposeTextLayout;
import org.ronsdev.bluectrl.widget.KeyboardInputView;
import org.ronsdev.bluectrl.widget.OnKeyboardComposingTextListener;
import org.ronsdev.bluectrl.widget.OnSendComposeTextListener;
import org.ronsdev.bluectrl.widget.TouchpadView;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.ClipboardManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

/**
 * This Activity is a Touchpad for the Mouse input and also allows Keyboard input.
 */
public class TouchpadActivity extends DaemonActivity implements OnMouseButtonClickListener {

    /**
     * Used as a Parcelable BluetoothDevice extra field in start Activity intents to get the
     * current Bluetooth device.
     */
    public static final String EXTRA_DEVICE =
            "org.ronsdev.bluectrl.touchpad.extra.DEVICE";

    /**
     * Used as a boolean extra field in start Activity intents to determine if the device was
     * just added.
     */
    public static final String EXTRA_IS_NEW_DEVICE =
            "org.ronsdev.bluectrl.touchpad.extra.IS_NEW_DEVICE";


    private static final String TAG = "TouchpadActivity";


    private static final int DIALOG_SEND_TEXT_PROGRESS = 2;


    private static final int TOUCHPAD_AREA_ICON_BUTTON_PADDING_DP = 48;

    private static final int DIM_SCREEN_ON_IDLE_TIMEOUT = 30 * 1000;

    private static final int SEND_TEXT_PROGRESS_MIN_SIZE = 300;
    private static final int SEND_TEXT_CHUNK_SIZE = 30;


    private static final String SAVED_STATE_IS_AUTO_CONNECT = "IsAutoConnect";
    private static final String SAVED_STATE_IS_PAIRING_CONNECT = "IsPairingConnect";
    private static final String SAVED_STATE_IS_SCREEN_DIMMED = "IsScreenDimmed";
    private static final String SAVED_STATE_DIM_SCREEN_ON_IDLE = "DimScreenOnIdle";


    private ImageButton mButtonKeyboard;
    private ViewFlipper mViewFlipper;
    private View mViewConnected;
    private KeyboardInputView mKeyboardInputView;
    private TouchpadView mTouchpadView;
    private ComposeTextLayout mViewComposeText;
    private LinearLayout mAndroidControls;
    private ViewGroup mPs3Controls;
    private View mViewDisconnected;
    private ImageView mInfoImage;
    private TextView mInfoTitle;
    private TextView mInfoText;
    private TextView mInfoReconnect;
    private View mViewConnecting;
    private ProgressDialog mSendTextProgressDlg;

    private int mTouchpadAreaIconButtonPadding;

    private BluetoothDevice mBtDevice;
    private ClipboardManager mClipboard;
    private DeviceSettings mDeviceSettings;
    private HidKeyboard mHidKeyboard;
    private HidMouse mHidMouse;
    private Handler mIdleHandler = new Handler();

    private boolean mIsAutoConnect = true;
    private boolean mIsPairingConnect = false;
    private boolean mKeepConnected = false;
    private boolean mIsScreenDimmed = false;
    private boolean mDimScreenOnIdle = false;

    private CharSequence mSendTextValue = "";
    private SendTextThread mSendTextThread;


    private final Runnable mDimScreenRunnable = new Runnable() {
        @Override
        public void run() {
            dimScreen(true);
        }
    };

    private OnClickListener mActionBarHomeClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            TouchpadActivity.this.finish();
        }
    };

    private OnClickListener mToggleKeyboardClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if ((mViewComposeText != null) && mViewComposeText.isShown()) {
                mViewComposeText.hide();
                if (mKeyboardInputView != null) {
                    mKeyboardInputView.hideToggledKeyboard();
                }
            } else {
                if (mKeyboardInputView != null) {
                    mKeyboardInputView.toggleKeyboard();
                }
            }
        }
    };

    private OnLongClickListener mToggleKeyboardLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            InputMethodManager imm = getInputManager();
            if (imm.getEnabledInputMethodList().size() > 1) {
                imm.showInputMethodPicker();

                if ((mKeyboardInputView != null) &&
                        ((mViewComposeText == null) || !mViewComposeText.isShown())) {
                    mKeyboardInputView.showKeyboard();
                }
                return true;
            }

            return false;
        }
    };

    private OnKeyboardComposingTextListener mKeyboardComposingTextListener =
            new OnKeyboardComposingTextListener() {
                @Override
                public void OnKeyboardComposingText(CharSequence composingText) {
                    if (mViewComposeText != null) {
                        mViewComposeText.show(composingText);
                    }
                }
            };

    private OnSendComposeTextListener mSendComposeTextListener = new OnSendComposeTextListener() {
        @Override
        public void OnSendComposeText(CharSequence text) {
            sendText(text);
        }
    };


    private class SendTextThread extends Thread {
        CharSequence mText;


        SendTextThread(CharSequence text) {
            mText = text;
        }

        public void run() {
            int position = 0;
            while ((position < mText.length()) && !interrupted() && mHidKeyboard.isConnected()) {
                int endPosition = position + SEND_TEXT_CHUNK_SIZE;
                if (endPosition > mText.length()) {
                    endPosition = mText.length();
                }
                CharSequence chunk = mText.subSequence(position, endPosition);
                mHidKeyboard.typeText(chunk.toString());
                position = endPosition;

                final int progress = position;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSendTextProgressDlg.setProgress(progress);
                    }
                });
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSendTextProgressDlg.dismiss();
                }
            });
        }
    }


    public static void startActivity(Activity curActivity, BluetoothDevice device,
            Boolean isNewDevice) {
        Intent intent = new Intent(curActivity, TouchpadActivity.class);
        intent.putExtra(EXTRA_DEVICE, device);
        intent.putExtra(EXTRA_IS_NEW_DEVICE, isNewDevice);
        curActivity.startActivity(intent);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final float displayDensity = getResources().getDisplayMetrics().density;

        mTouchpadAreaIconButtonPadding =
                (int)(TOUCHPAD_AREA_ICON_BUTTON_PADDING_DP * displayDensity + 0.5f);

        Bundle extras = getIntent().getExtras();
        mBtDevice = extras.getParcelable(EXTRA_DEVICE);

        mClipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);

        mDeviceSettings = DeviceSettings.get(this, mBtDevice);

        if (savedInstanceState == null) {
            mIsPairingConnect = extras.getBoolean(EXTRA_IS_NEW_DEVICE);
        } else {
            mIsAutoConnect = savedInstanceState.getBoolean(SAVED_STATE_IS_AUTO_CONNECT);
            mIsPairingConnect = savedInstanceState.getBoolean(SAVED_STATE_IS_PAIRING_CONNECT);
            mIsScreenDimmed = savedInstanceState.getBoolean(SAVED_STATE_IS_SCREEN_DIMMED);
            mDimScreenOnIdle = savedInstanceState.getBoolean(SAVED_STATE_DIM_SCREEN_ON_IDLE);
        }

        loadLayout();

        if ((savedInstanceState == null) && (mKeyboardInputView != null) &&
                (mDeviceSettings.getOperatingSystem().equals(DeviceSettings.OS_IOS))) {
            // iOS devices don't support mouse control so directly show the keyboard
            mKeyboardInputView.showKeyboard();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mKeepConnected = false;

        if (mHidKeyboard != null) {
            mHidKeyboard.setKeyMap(this, mDeviceSettings.getKeyMap());
        }

        updateViewSettings();

        resetDimScreenOnIdleTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopDimScreenOnIdleTimer();
        stopSendTextTask();

        if (!mKeepConnected && isDaemonAvailable()) {
            final DaemonService daemon = getDaemon();
            daemon.disconnectHid();
        }

        if (mKeyboardInputView != null) {
            mKeyboardInputView.onActivityPause();
        }
        if (mViewComposeText != null) {
            mViewComposeText.onActivityPause();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(SAVED_STATE_IS_AUTO_CONNECT, mIsAutoConnect);
        outState.putBoolean(SAVED_STATE_IS_PAIRING_CONNECT, mIsPairingConnect);
        outState.putBoolean(SAVED_STATE_IS_SCREEN_DIMMED, mIsScreenDimmed);
        outState.putBoolean(SAVED_STATE_DIM_SCREEN_ON_IDLE, mDimScreenOnIdle);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        loadLayout();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        resetDimScreenOnIdleTimer();
    }

    @Override
    protected void onDaemonAvailable() {
        final DaemonService daemon = getDaemon();

        mHidKeyboard = new HidKeyboard(daemon);
        mHidKeyboard.setKeyMap(this, mDeviceSettings.getKeyMap());

        if (mKeyboardInputView != null) {
            mKeyboardInputView.setHidKeyboard(mHidKeyboard);
        }

        mHidMouse = new HidMouse(daemon);
        mHidMouse.setOnMouseButtonClickListener(this);

        if (mTouchpadView != null) {
            mTouchpadView.setHidMouse(mHidMouse);
            mTouchpadView.setHidKeyboard(mHidKeyboard);
        }

        onHidStateChanged(daemon.getHidState(),
                daemon.getConnectedDevice(),
                daemon.getHidErrorCode());
    }

    @Override
    protected void onDaemonUnavailable(int errorCode) {
        // This Activity is useless without the daemon
        this.finish();
    }

    @Override
    protected void onHidStateChanged(int hidState, BluetoothDevice btDevice, int errorCode) {
        if (isDaemonAvailable()) {
            final DaemonService daemon = getDaemon();

            if (isForeignHostDevice(btDevice)) {
                switch (hidState) {
                case DaemonService.HID_STATE_CONNECTED:
                    daemon.disconnectHid();
                    break;
                }
            } else {
                switch (hidState) {
                case DaemonService.HID_STATE_CONNECTED:
                    mIsAutoConnect = false;

                    if (mIsPairingConnect) {
                        mIsPairingConnect = false;

                        /*
                         * UGLY HACK:
                         * Once the host has enabled the Smooth Scrolling feature, it is
                         * automatically enabled on every new connection. This is against the
                         * recommended way where the feature is disabled by default and the host
                         * must explicitly enable it on every connection. It is necessary because
                         * Microsoft Windows (tested in version 7) has problems to detect when the
                         * device got disconnected and as a result won't enable the feature on the
                         * next connection. It is also important to reset this setting if the
                         * device was paired again and the host hasn't enabled the feature because
                         * the same Bluetooth adapter can be used with different OS which might
                         * not support this feature.
                         */
                        final boolean hasHostActivatedSmoothScroll =
                                (daemon.isSmoothScrollXOn() && daemon.isSmoothScrollYOn());
                        mDeviceSettings.setForceSmoothScroll(hasHostActivatedSmoothScroll);
                        mDeviceSettings.saveToPreferences();
                    } else {
                        if (mDeviceSettings.getForceSmoothScroll()) {
                            daemon.setSmoothScroll(true, true);
                        }
                    }
                    break;
                case DaemonService.HID_STATE_DISCONNECTED:
                    if (mIsAutoConnect) {
                        connectHid(daemon);
                    }
                    break;
                }
            }
        }

        updateWindowFlagFullscreen();
        updateWindowFlagKeepScreenOn();
        updateViews();
    }

    @Override
    protected void onHidMouseFeatureReceived() {
        if (isDaemonAvailable()) {
            final DaemonService daemon = getDaemon();

            /*
             * Once the host has enabled the Smooth Scroll feature, it is
             * automatically enabled on every new connection. For more information
             * read the UGLY HACK comment inside the 'onHidStateChanged' method.
             */
            if (daemon.isSmoothScrollYOn() && daemon.isSmoothScrollXOn() &&
                    !mDeviceSettings.getForceSmoothScroll()) {
                mDeviceSettings.setForceSmoothScroll(true);
                mDeviceSettings.saveToPreferences();
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch(id) {
        case DIALOG_SEND_TEXT_PROGRESS:
            return createSendTextProgressDialog();
        default:
            return null;
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        switch(id) {
        case DIALOG_SEND_TEXT_PROGRESS:
            prepareSendTextProgressDialog();
            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.touchpad, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final boolean isConnected = (isDaemonAvailable() &&
                (getDaemon().getHidState() == DaemonService.HID_STATE_CONNECTED));

        MenuItem composeTextItem = menu.findItem(R.id.menu_compose_text);
        composeTextItem.setEnabled(isConnected);

        MenuItem pasteItem = menu.findItem(R.id.menu_paste);
        pasteItem.setEnabled(isConnected && mClipboard.hasText());

        MenuItem tutorialItem = menu.findItem(R.id.menu_tutorial);
        tutorialItem.setEnabled(isConnected);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_compose_text:
            if (mViewComposeText != null) {
                mViewComposeText.toggleVisibility();
            }
            return true;
        case R.id.menu_paste:
            sendText(mClipboard.getText());
            return true;
        case R.id.menu_preferences:
            DevicePreferenceActivity.startActivity(this, mBtDevice);
            return true;
        case R.id.menu_tutorial:
            mKeepConnected = true;
            TouchpadTutorialActivity.startActivity(this, mBtDevice);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isDaemonAvailable()) {
            if (mInfoReconnect.isShown()) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    connectHid(getDaemon());
                    return true;
                }
            }
        }

        return super.onTouchEvent(event);
    }


    private void loadLayout() {
        // Save some control states before the setContentView method will reset them.
        SparseArray<Parcelable> stateContainer = new SparseArray<Parcelable>();
        if (mKeyboardInputView != null) {
            mKeyboardInputView.saveHierarchyState(stateContainer);
        }
        if (mViewComposeText != null) {
            mViewComposeText.saveHierarchyState(stateContainer);
        }


        setContentView(R.layout.touchpad);


        ImageButton actionBarHome = (ImageButton)findViewById(R.id.action_bar_home);
        actionBarHome.setOnClickListener(mActionBarHomeClickListener);

        TextView actionBarTitle = (TextView)findViewById(R.id.action_bar_title);
        if (mBtDevice != null) {
            actionBarTitle.setText(DeviceManager.getDeviceName(this, mBtDevice));
        }

        mButtonKeyboard = (ImageButton)findViewById(R.id.button_keyboard);
        if (getInputManager().getEnabledInputMethodList().isEmpty()) {
            mButtonKeyboard.setVisibility(View.GONE);
            mButtonKeyboard = null;
        } else {
            mButtonKeyboard.setOnClickListener(mToggleKeyboardClickListener);
            mButtonKeyboard.setOnLongClickListener(mToggleKeyboardLongClickListener);
        }


        mViewFlipper = (ViewFlipper)findViewById(R.id.flipper);


        // View Connected
        mViewConnected = (View)findViewById(R.id.view_connected);

        mKeyboardInputView = (KeyboardInputView)findViewById(R.id.keyboard_input);
        mKeyboardInputView.restoreHierarchyState(stateContainer);
        mKeyboardInputView.setHidKeyboard(mHidKeyboard);
        mKeyboardInputView.setOnKeyboardComposingTextListener(mKeyboardComposingTextListener);

        mTouchpadView = (TouchpadView)findViewById(R.id.touchpad);
        mTouchpadView.setHidMouse(mHidMouse);
        mTouchpadView.setHidKeyboard(mHidKeyboard);

        mViewComposeText = (ComposeTextLayout)findViewById(R.id.view_compose_text);
        mViewComposeText.restoreHierarchyState(stateContainer);
        mViewComposeText.setOnSendComposeTextListener(mSendComposeTextListener);


        mAndroidControls = (LinearLayout)findViewById(R.id.touchpad_android_controls);

        ImageButton btnAndroidBack = (ImageButton)findViewById(R.id.btn_android_back);
        initKeyboardAppCtrlIconButton(btnAndroidBack, HidKeyboard.AC_KEY_BACK);

        ImageButton btnAndroidHome = (ImageButton)findViewById(R.id.btn_android_home);
        initKeyboardAppCtrlIconButton(btnAndroidHome, HidKeyboard.AC_KEY_HOME);

        ImageButton btnAndroidMenu = (ImageButton)findViewById(R.id.btn_android_menu);
        initKeyboardIconButton(btnAndroidMenu, HidKeyboard.KEYCODE_APPLICATION);

        /*
         * Reverse the order of the buttons in landscape mode so that they are on the same
         * position as in the portrait mode.
         */
        if (mAndroidControls.getOrientation() == LinearLayout.VERTICAL) {
            mAndroidControls.removeAllViews();
            mAndroidControls.addView(btnAndroidMenu);
            mAndroidControls.addView(btnAndroidHome);
            mAndroidControls.addView(btnAndroidBack);
        }


        mPs3Controls = (ViewGroup)findViewById(R.id.touchpad_ps3_controls);

        ImageButton btnPs3Triangle = (ImageButton)findViewById(R.id.btn_ps3_triangle);
        initKeyboardIconButton(btnPs3Triangle, HidKeyboard.KEYCODE_F1);

        ImageButton btnPs3Circle = (ImageButton)findViewById(R.id.btn_ps3_circle);
        initKeyboardIconButton(btnPs3Circle, HidKeyboard.KEYCODE_ESCAPE);

        ImageButton btnPs3Square = (ImageButton)findViewById(R.id.btn_ps3_square);
        initKeyboardIconButton(btnPs3Square, HidKeyboard.KEYCODE_F2);

        ImageButton btnPs3X = (ImageButton)findViewById(R.id.btn_ps3_x);
        initKeyboardIconButton(btnPs3X, HidKeyboard.KEYCODE_ENTER);

        ImageButton btnPs3Select = (ImageButton)findViewById(R.id.btn_ps3_select);
        initKeyboardIconButton(btnPs3Select, HidKeyboard.KEYCODE_F3);

        ImageButton btnPs3Ps = (ImageButton)findViewById(R.id.btn_ps3_ps);
        initKeyboardModifierIconButton(btnPs3Ps, HidKeyboard.MODIFIER_LEFT_GUI);

        ImageButton btnPs3Start = (ImageButton)findViewById(R.id.btn_ps3_start);
        initKeyboardIconButton(btnPs3Start, HidKeyboard.KEYCODE_F4);


        // View Disconnected
        mViewDisconnected = (View)findViewById(R.id.view_disconnected);

        mInfoImage = (ImageView)findViewById(R.id.info_image);
        mInfoTitle = (TextView)findViewById(R.id.info_title);
        mInfoText = (TextView)findViewById(R.id.info_text);
        mInfoReconnect = (TextView)findViewById(R.id.info_reconnect);


        // View Connecting
        mViewConnecting = (View)findViewById(R.id.view_connecting);


        updateWindowFlagFullscreen();
        updateWindowFlagKeepScreenOn();
        updateViewSettings();
        updateViews();
    }

    private InputMethodManager getInputManager() {
        return (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    private void initKeyboardIconButton(ImageButton button, final int hidKeyCode) {
        button.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    onKeyboardIconButtonDown(v);
                    mHidKeyboard.pressKey(hidKeyCode);
                    break;
                case MotionEvent.ACTION_UP:
                    mHidKeyboard.releaseKey(hidKeyCode);
                    break;
                }
                return false;
            }
        });
    }

    private void initKeyboardModifierIconButton(ImageButton button, final int hidModifier) {
        button.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    onKeyboardIconButtonDown(v);
                    mHidKeyboard.pressModifierKey(hidModifier);
                    break;
                case MotionEvent.ACTION_UP:
                    mHidKeyboard.releaseModifierKey(hidModifier);
                    break;
                }
                return false;
            }
        });
    }

    private void initKeyboardAppCtrlIconButton(ImageButton button, final int key) {
        button.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    onKeyboardIconButtonDown(v);
                    mHidKeyboard.pressAppCtrlKey(key);
                    break;
                case MotionEvent.ACTION_UP:
                    mHidKeyboard.releaseAppCtrlKey(key);
                    break;
                }
                return false;
            }
        });
    }

    private void onKeyboardIconButtonDown(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private void updateWindowFlagFullscreen() {
        final Window wnd = getWindow();
        final boolean isConnected = (isDaemonAvailable() &&
                (getDaemon().getHidState() == DaemonService.HID_STATE_CONNECTED));

        if (isConnected && isScreenHeightSmall()) {
            wnd.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            wnd.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        } else {
            wnd.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            wnd.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void updateWindowFlagKeepScreenOn() {
        final Window wnd = getWindow();
        final boolean isConnected = (isDaemonAvailable() &&
                (getDaemon().getHidState() == DaemonService.HID_STATE_CONNECTED));
        final boolean isActivityBusy = (mSendTextThread != null);

        if (isConnected && (mDeviceSettings.getStayAwake() || isActivityBusy)) {
            wnd.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            dimScreenOnIdle(!isActivityBusy);
        } else {
            wnd.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            dimScreenOnIdle(false);
        }
    }

    private void updateViewSettings() {
        int touchpadButtonBarHeight = 0;
        int touchpadAreaPadding = 0;

        if (mTouchpadView != null) {
            mTouchpadView.setGestureMode(mDeviceSettings.getTouchpadGestureMode());
            mTouchpadView.setShowButtons(getShowTouchpadButtons());
            mTouchpadView.setMouseSensitivity(mDeviceSettings.getMouseSensitivity());
            mTouchpadView.setScrollSensitivity(mDeviceSettings.getScrollSensitivity());
            mTouchpadView.setPinchZoomSensitivity(mDeviceSettings.getPinchZoomSensitivity());
            mTouchpadView.setInvertScroll(mDeviceSettings.getInvertScroll());
            mTouchpadView.setFlingScroll(mDeviceSettings.getFlingScroll());

            touchpadButtonBarHeight = mTouchpadView.getVisibleButtonBarHeight();
        }

        if (mAndroidControls != null) {
            if (mDeviceSettings.getOperatingSystem().equals(DeviceSettings.OS_ANDROID)) {
                mAndroidControls.setVisibility(View.VISIBLE);
                mAndroidControls.setPadding(0, 0, 0, touchpadButtonBarHeight);
                touchpadAreaPadding = mTouchpadAreaIconButtonPadding;
            } else {
                mAndroidControls.setVisibility(View.GONE);
            }
        }

        if (mPs3Controls != null) {
            if (mDeviceSettings.getOperatingSystem().equals(DeviceSettings.OS_PLAYSTATION3)) {
                mPs3Controls.setVisibility(View.VISIBLE);
                mPs3Controls.setPadding(0, 0, 0, touchpadButtonBarHeight);
                touchpadAreaPadding = mTouchpadAreaIconButtonPadding;
            } else {
                mPs3Controls.setVisibility(View.GONE);
            }
        }

        if (mTouchpadView != null) {
            if (touchpadAreaPadding > 0) {
                mTouchpadView.setTouchpadAreaPadding(touchpadAreaPadding);
            } else {
                mTouchpadView.resetTouchpadAreaPadding();
            }
        }
    }

    private boolean getShowTouchpadButtons() {
        final String prefValue = mDeviceSettings.getTouchpadButtons();

        if (prefValue.equals(DeviceSettings.TOUCHPAD_BUTTONS_SHOW)) {
            return true;
        } else if (prefValue.equals(DeviceSettings.TOUCHPAD_BUTTONS_SHOW_PORTRAIT)) {
            final Configuration config = getResources().getConfiguration();
            return (config.orientation == Configuration.ORIENTATION_PORTRAIT);
        } else {
            return false;
        }
    }

    private Dialog createSendTextProgressDialog() {
        mSendTextProgressDlg = new ProgressDialog(this);

        mSendTextProgressDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mSendTextProgressDlg.setMessage(getString(R.string.info_title_sending_text));

        mSendTextProgressDlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                stopSendTextTask();
            }
        });

        return mSendTextProgressDlg;
    }

    private void prepareSendTextProgressDialog() {
        mSendTextProgressDlg.setProgress(0);
        mSendTextProgressDlg.setMax(mSendTextValue.length());

        mSendTextThread = new SendTextThread(mSendTextValue);
        mSendTextThread.start();

        mSendTextValue = "";

        updateWindowFlagKeepScreenOn();
    }

    /** Checks if the given Bluetooth device is from another HID host */
    private boolean isForeignHostDevice(BluetoothDevice btDevice) {
        return ((btDevice != null) && !btDevice.equals(mBtDevice));
    }

    private void connectHid(DaemonService daemon) {
        mIsAutoConnect = false;
        daemon.connectHid(mBtDevice.getAddress());
    }

    public void onMouseButtonClick(int clickType, int button) {
        if (mTouchpadView != null) {
            mTouchpadView.onMouseButtonClick(clickType, button);
        }
    }

    private boolean isScreenHeightSmall() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return ((displayMetrics.heightPixels / displayMetrics.density) < 400);
    }

    private void dimScreen(boolean dim) {
        if (dim != mIsScreenDimmed) {
            mIsScreenDimmed = dim;

            final Window wnd = getWindow();
            WindowManager.LayoutParams wndLayoutParams = wnd.getAttributes();
            wndLayoutParams.screenBrightness = dim ? 0.01f : -1f;
            wnd.setAttributes(wndLayoutParams);
        }
    }

    private void dimScreenOnIdle(boolean dimOnIdle) {
        mDimScreenOnIdle = dimOnIdle;
        resetDimScreenOnIdleTimer();
    }

    private void resetDimScreenOnIdleTimer() {
        stopDimScreenOnIdleTimer();
        if (mDimScreenOnIdle) {
            mIdleHandler.postDelayed(mDimScreenRunnable, DIM_SCREEN_ON_IDLE_TIMEOUT);
        }
    }

    private void stopDimScreenOnIdleTimer() {
        mIdleHandler.removeCallbacks(mDimScreenRunnable);
        dimScreen(false);
    }

    private void startSendTextTask(CharSequence text) {
        mSendTextValue = text;
        showDialog(DIALOG_SEND_TEXT_PROGRESS);
    }

    private void stopSendTextTask() {
        if (mSendTextThread != null) {
            mSendTextThread.interrupt();
            try {
                mSendTextThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "SendTextThread join failed", e);
            }
            mSendTextThread = null;
        }

        if ((mSendTextProgressDlg != null) && mSendTextProgressDlg.isShowing()) {
            mSendTextProgressDlg.dismiss();
        }

        updateWindowFlagKeepScreenOn();
    }

    private void sendText(CharSequence text) {
        if ((text != null) && (text.length() > 0) && (mHidKeyboard != null) &&
                mHidKeyboard.isConnected()) {
            if (text.length() < SEND_TEXT_PROGRESS_MIN_SIZE) {
                mHidKeyboard.typeText(text.toString());
            } else {
                startSendTextTask(text);
            }
        }
    }

    private void setViewInfoText(String title, String text, boolean showReconnect) {
        if (title.isEmpty()) {
            mInfoTitle.setVisibility(View.GONE);
        } else {
            mInfoTitle.setVisibility(View.VISIBLE);
            mInfoTitle.setText(title);
        }
        if (text.isEmpty()) {
            mInfoText.setVisibility(View.GONE);
        } else {
            mInfoText.setVisibility(View.VISIBLE);
            mInfoText.setText(text);
        }
        mInfoReconnect.setVisibility(showReconnect ? View.VISIBLE : View.GONE);
    }

    private void changeFlipperView(View child) {
        final int childIndex = mViewFlipper.indexOfChild(child);
        if ((childIndex > -1) && (childIndex != mViewFlipper.getDisplayedChild())) {
            mViewFlipper.setDisplayedChild(childIndex);
        }
    }

    private void showViewConnected() {
        changeFlipperView(mViewConnected);

        if (!mViewComposeText.isShown()) {
            mKeyboardInputView.requestFocus();
        }
    }

    private void showViewDisconnected(int errorCode) {
        switch (errorCode) {
        case 0:
            mInfoImage.setImageResource(R.drawable.disconnected);
            setViewInfoText(getString(R.string.info_title_disconnected), "", true);
            break;
        case DaemonService.ERROR_ACCES:
            mInfoImage.setImageResource(R.drawable.problem);
            setViewInfoText(getString(R.string.info_title_permission_denied),
                    getString(R.string.info_text_pair_again),
                    false);
            break;
        case DaemonService.ERROR_HOSTDOWN:
            mInfoImage.setImageResource(R.drawable.problem);
            setViewInfoText(getString(R.string.info_title_host_unavailable),
                    getString(R.string.info_text_host_unavailable),
                    true);
            break;
        case DaemonService.ERROR_CONNREFUSED:
            if (mIsPairingConnect &&
                    (mDeviceSettings.getOperatingSystem().equals(DeviceSettings.OS_IOS))) {
                mInfoImage.setImageResource(R.drawable.problem);
                setViewInfoText(getString(R.string.info_title_connection_refused),
                        getString(R.string.info_text_ios_bt_off_on),
                        true);
            } else {
                mInfoImage.setImageResource(R.drawable.problem);
                setViewInfoText(getString(R.string.info_title_connection_refused),
                        getString(R.string.info_text_connection_refused),
                        true);
            }
            break;
        case DaemonService.ERROR_BADE:
            mInfoImage.setImageResource(R.drawable.problem);
            setViewInfoText(getString(R.string.info_title_authorization_error),
                    getString(R.string.info_text_pair_again),
                    false);
            break;
        case DaemonService.ERROR_TIMEDOUT:
            mInfoImage.setImageResource(R.drawable.problem);
            setViewInfoText(getString(R.string.info_title_connection_timeout),
                    getString(R.string.info_text_host_unavailable),
                    true);
            break;
        default:
            mInfoImage.setImageResource(R.drawable.problem);
            setViewInfoText(getString(R.string.info_title_connection_problem),
                    getString(R.string.info_text_host_unavailable),
                    true);
            break;
        }

        changeFlipperView(mViewDisconnected);
    }

    private void showViewConnecting() {
        changeFlipperView(mViewConnecting);
    }

    private void updateViews() {
        // No need for update when the Activity is closing
        if (isFinishing()) {
            return;
        }

        final DaemonService daemon = getDaemon();
        int hidState;
        int errorCode = 0;

        // If the DaemonService isn't connected yet or a connection with another device is
        // still pending
        if ((!isDaemonAvailable()) || isForeignHostDevice(daemon.getConnectedDevice())) {
            hidState = DaemonService.HID_STATE_CONNECTING;
        } else {
            hidState = daemon.getHidState();
            errorCode = daemon.getHidErrorCode();
        }

        final boolean isConnected = (hidState == DaemonService.HID_STATE_CONNECTED);
        if (mButtonKeyboard != null) {
            mButtonKeyboard.setVisibility(isConnected ? View.VISIBLE : View.GONE);
        }

        switch (hidState) {
        case DaemonService.HID_STATE_CONNECTING:
            showViewConnecting();
            break;
        case DaemonService.HID_STATE_DISCONNECTING:
        case DaemonService.HID_STATE_DISCONNECTED:
            switch (errorCode) {
            case DaemonService.ERROR_ALREADY:
                showViewConnecting();
                break;
            default:
                showViewDisconnected(errorCode);
                break;
            }
            break;
        case DaemonService.HID_STATE_CONNECTED:
            showViewConnected();
            break;
        }
    }
}
