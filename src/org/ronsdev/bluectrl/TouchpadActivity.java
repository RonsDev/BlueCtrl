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
import org.ronsdev.bluectrl.widget.KeyboardInputView;
import org.ronsdev.bluectrl.widget.TouchpadBackgroundView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * This Activity is a Touchpad for the Mouse input and also allows Keyboard input.
 */
public class TouchpadActivity extends DaemonActivity
        implements OnMouseModeChangedListener, OnMouseGestureListener, OnMouseButtonClickListener {

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


    private static final int DIALOG_HELP = 1;
    private static final int DIALOG_SEND_TEXT_PROGRESS = 2;


    private static final int SEND_TEXT_PROGRESS_MIN_SIZE = 300;
    private static final int SEND_TEXT_CHUNK_SIZE = 30;


    private static final String SAVED_STATE_IS_AUTO_CONNECT = "IsAutoConnect";
    private static final String SAVED_STATE_IS_PAIRING_CONNECT = "IsPairingConnect";
    private static final String SAVED_STATE_IS_FULLSCREEN = "IsFullscreen";
    private static final String SAVED_STATE_SHOW_KEYBOARD_ON_CONNECT = "ShowKeyboardOnConnect";


    private ImageButton mActionBarHome;
    private TextView mActionBarTitle;
    private ImageButton mButtonKeyboard;
    private KeyboardInputView mKeyboardInputView;
    private TouchpadBackgroundView mTouchBackground;
    private ProgressBar mInfoWait;
    private ImageView mInfoImage;
    private TextView mInfoTitle;
    private TextView mInfoText;
    private TextView mInfoReconnect;
    private ProgressDialog mSendTextProgressDlg;

    private BluetoothDevice mBtDevice;
    private InputMethodManager mInputMethodManager;
    private ClipboardManager mClipboard;
    private DeviceSettings mDeviceSettings;
    private HidKeyboard mKeyboard;
    private HidMouse mMouse;
    private KeyboardInputHandler mKeyInputHandler;
    private MouseInputHandler mMouseInputHandler;

    private boolean mIsAutoConnect = true;
    private boolean mIsPairingConnect = false;
    private boolean mIsFullscreen = false;
    private boolean mShowKeyboardOnConnect = false;
    private boolean mWasKeyboardToggled = false;

    private CharSequence mSendTextValue = "";
    private SendTextThread mSendTextThread;


    private OnClickListener mActionBarHomeClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            TouchpadActivity.this.finish();
        }
    };

    private OnClickListener mToggleKeyboardClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            toggleKeyboard();
        }
    };


    private final Handler mSendTextProgressHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (mSendTextProgressDlg.isShowing()) {
                int total = msg.arg1;
                if (total >= 0) {
                    mSendTextProgressDlg.setProgress(total);
                } else {
                    mSendTextProgressDlg.dismiss();
                }
            }
        }
    };

    private class SendTextThread extends Thread {
        Handler mHandler;
        CharSequence mText;


        SendTextThread(Handler handler, CharSequence text) {
            mHandler = handler;
            mText = text;
        }

        public void run() {
            int position = 0;
            while ((position < mText.length()) && !interrupted()) {
                // The KeyboardInputHandler can become temporarily inactive if the screen is
                // rotated. In this case wait until it is active again.
                if (!mKeyInputHandler.isActive()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }

                int endPosition = position + SEND_TEXT_CHUNK_SIZE;
                if (endPosition > mText.length()) {
                    endPosition = mText.length();
                }
                CharSequence chunk = mText.subSequence(position, endPosition);
                mKeyboardInputView.pasteText(chunk.toString());
                position = endPosition;

                Message msg = mHandler.obtainMessage();
                msg.arg1 = position;
                mHandler.sendMessage(msg);
            }

            Message msg = mHandler.obtainMessage();
            msg.arg1 = -1;
            mHandler.sendMessage(msg);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        mBtDevice = extras.getParcelable(EXTRA_DEVICE);

        if (savedInstanceState == null) {
            mIsPairingConnect = extras.getBoolean(EXTRA_IS_NEW_DEVICE);
        } else {
            mIsAutoConnect = savedInstanceState.getBoolean(SAVED_STATE_IS_AUTO_CONNECT);
            mIsPairingConnect = savedInstanceState.getBoolean(SAVED_STATE_IS_PAIRING_CONNECT);
            setWindowFullscreen(savedInstanceState.getBoolean(SAVED_STATE_IS_FULLSCREEN));
            mShowKeyboardOnConnect = savedInstanceState.getBoolean(
                    SAVED_STATE_SHOW_KEYBOARD_ON_CONNECT);
        }

        mInputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        mClipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);

        mDeviceSettings = DeviceSettings.get(this, mBtDevice);

        loadLayout();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopSendTextTask();

        if (isDaemonAvailable()) {
            final DaemonService daemon = getDaemon();
            daemon.disconnectHid();
        }

        if (mWasKeyboardToggled) {
            hideKeyboard();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(SAVED_STATE_IS_AUTO_CONNECT, mIsAutoConnect);
        outState.putBoolean(SAVED_STATE_IS_PAIRING_CONNECT, mIsPairingConnect);
        outState.putBoolean(SAVED_STATE_IS_FULLSCREEN, mIsFullscreen);
        outState.putBoolean(SAVED_STATE_SHOW_KEYBOARD_ON_CONNECT, mShowKeyboardOnConnect);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        loadLayout();
    }

    @Override
    protected void onDaemonAvailable() {
        final DaemonService daemon = getDaemon();

        mKeyboard = new HidKeyboard(daemon);

        mMouse = new HidMouse(daemon);
        mMouse.setOnMouseButtonClickListener(this);

        initInputHandler();

        onHidStateChanged(daemon.getHidState(),
                daemon.getConnectedDevice(),
                daemon.getHidErrorCode());
    }

    @Override
    protected void onDaemonUnavailable(int errorCode) {
        stopSendTextTask();

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

        refreshViewInfo();
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
        case DIALOG_HELP:
            return createHelpDialog();
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
        final boolean keyboardInputActive = ((mKeyInputHandler != null) &&
                mKeyInputHandler.isActive());

        MenuItem pasteItem = menu.findItem(R.id.menu_paste);
        pasteItem.setEnabled(keyboardInputActive && mClipboard.hasText());

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_paste:
            pasteClipboard();
            return true;
        case R.id.menu_preferences:
            Intent prefIntent = new Intent(this, DevicePreferenceActivity.class);
            prefIntent.putExtra(DevicePreferenceActivity.EXTRA_DEVICE, mBtDevice);
            startActivity(prefIntent);
            return true;
        case R.id.menu_help:
            showDialog(DIALOG_HELP);
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
        setContentView(R.layout.touchpad);

        mActionBarHome = (ImageButton)findViewById(R.id.action_bar_home);
        mActionBarHome.setOnClickListener(mActionBarHomeClickListener);

        mActionBarTitle = (TextView)findViewById(R.id.action_bar_title);
        if (mBtDevice != null) {
            mActionBarTitle.setText(mBtDevice.getName());
        }

        mButtonKeyboard = (ImageButton)findViewById(R.id.button_keyboard);
        mButtonKeyboard.setOnClickListener(mToggleKeyboardClickListener);

        mKeyboardInputView = (KeyboardInputView)findViewById(R.id.keyboard_input);
        mKeyboardInputView.requestFocus();

        mTouchBackground = (TouchpadBackgroundView)findViewById(R.id.touch_background);

        mInfoWait = (ProgressBar)findViewById(R.id.info_wait);
        mInfoImage = (ImageView)findViewById(R.id.info_image);
        mInfoTitle = (TextView)findViewById(R.id.info_title);
        mInfoText = (TextView)findViewById(R.id.info_text);
        mInfoReconnect = (TextView)findViewById(R.id.info_reconnect);

        refreshViewInfo();
        initInputHandler();
    }

    public Dialog createHelpDialog() {
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.help_dialog,
                (ViewGroup)findViewById(R.id.view_help));

        return new AlertDialog.Builder(this)
            .setView(layout)
            .setTitle(R.string.menu_help)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            })
            .create();
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

        mSendTextThread = new SendTextThread(mSendTextProgressHandler, mSendTextValue);
        mSendTextThread.start();

        mSendTextValue = "";
    }

    private void initInputHandler() {
        if (mKeyboard != null) {
            mKeyInputHandler = new KeyboardInputHandler(mKeyboardInputView,
                    mKeyboard,
                    mDeviceSettings);
        }

        if (mMouse != null) {
            mMouseInputHandler = new MouseInputHandler(mTouchBackground, mMouse, mDeviceSettings);
            mMouseInputHandler.setOnMouseModeChangedListener(this);
            mMouseInputHandler.setOnMouseGestureListener(this);
        }
    }

    /** Checks if the given Bluetooth device is from another HID host */
    private boolean isForeignHostDevice(BluetoothDevice btDevice) {
        return ((btDevice != null) && !btDevice.equals(mBtDevice));
    }

    private void connectHid(DaemonService daemon) {
        mIsAutoConnect = false;
        daemon.connectHid(mBtDevice.getAddress());
    }

    public void onMouseModeChanged(int newMode, int oldMode) {
        switch (oldMode) {
        case MouseInputHandler.MODE_SCROLL:
            mTouchBackground.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            break;
        }

        switch (newMode) {
        case MouseInputHandler.MODE_SCROLL:
            mTouchBackground.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            showViewInfoTouchpad(R.drawable.scroll);
            break;
        default:
            showViewInfoTouchpad();
            break;
        }
    }

    public boolean onMouseGesture(int gesture, int direction) {
        switch (gesture) {
        case MouseInputHandler.GESTURE_EDGE_RIGHT:
            switch (direction) {
            case MouseInputHandler.GESTURE_DIRECTION_UP:
            case MouseInputHandler.GESTURE_DIRECTION_DOWN:
                mMouseInputHandler.activateScrollMode();
                return true;
            }
            break;
        case MouseInputHandler.GESTURE_2FINGER:
            switch (direction) {
            case MouseInputHandler.GESTURE_DIRECTION_UP:
            case MouseInputHandler.GESTURE_DIRECTION_DOWN:
                mMouseInputHandler.activateScrollMode();
                return true;
            case MouseInputHandler.GESTURE_DIRECTION_LEFT:
                mMouse.clickButton(HidMouse.BUTTON_4);
                return true;
            case MouseInputHandler.GESTURE_DIRECTION_RIGHT:
                mMouse.clickButton(HidMouse.BUTTON_5);
                return true;
            }
            break;
        }
        return false;
    }

    public void onMouseButtonClick(int clickType) {
        switch (clickType) {
        case HidMouse.CLICK_TYPE_DOWN:
        case HidMouse.CLICK_TYPE_UP:
            mTouchBackground.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            mTouchBackground.playSoundEffect(SoundEffectConstants.CLICK);
            break;
        case HidMouse.CLICK_TYPE_CLICK:
            mTouchBackground.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            mTouchBackground.playSoundEffect(SoundEffectConstants.CLICK);
            break;
        }
    }

    private void setWindowFullscreen(boolean fullscreen) {
        if (mIsFullscreen != fullscreen) {
            mIsFullscreen = fullscreen;

            final Window wnd = getWindow();
            if (fullscreen) {
                wnd.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                wnd.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            } else {
                wnd.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                wnd.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
    }

    private void showKeyboard() {
        mKeyboardInputView.requestFocus();
        mInputMethodManager.showSoftInput(mKeyboardInputView, InputMethodManager.SHOW_FORCED);
        mWasKeyboardToggled = true;
    }

    private void hideKeyboard() {
        mWasKeyboardToggled = false;
        mInputMethodManager.hideSoftInputFromWindow(
                mKeyboardInputView.getWindowToken(),
                0,
                new ResultReceiver(null) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        if (resultCode == InputMethodManager.RESULT_HIDDEN) {
                            mShowKeyboardOnConnect = true;
                        }
                    }
                });
    }

    private void toggleKeyboard() {
        mKeyboardInputView.requestFocus();

        /*
         * If the SoftInput is shown with the SHOW_FORCED option it will stay even when the
         * Activity stops, so we will hide it manually in onStop. This wouldn't be necessary with
         * the preferred SHOW_IMPLICIT option which can't be used because it strangely doesn't
         * work in landscape mode.
         */
        mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        mWasKeyboardToggled = true;
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
    }

    private void pasteClipboard() {
        final CharSequence pasteText = mClipboard.getText();
        if ((pasteText != null) && (pasteText.length() > 0)) {
            if (pasteText.length() < SEND_TEXT_PROGRESS_MIN_SIZE) {
                mKeyboardInputView.pasteText(pasteText.toString());
            } else {
                startSendTextTask(pasteText);
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

    private void showViewInfoTouchpad() {
        mTouchBackground.setVisibility(View.VISIBLE);
        mInfoWait.setVisibility(View.GONE);
        mInfoImage.setVisibility(View.GONE);
        setViewInfoText("", "", false);
    }

    private void showViewInfoTouchpad(int resid) {
        mTouchBackground.setVisibility(View.VISIBLE);
        mInfoWait.setVisibility(View.GONE);
        mInfoImage.setImageResource(resid);
        mInfoImage.setVisibility(View.VISIBLE);
        setViewInfoText("", "", false);
    }

    private void showViewInfoWait(String title, String text) {
        mTouchBackground.setVisibility(View.GONE);
        mInfoWait.setVisibility(View.VISIBLE);
        mInfoImage.setVisibility(View.GONE);
        setViewInfoText(title, text, false);
    }

    private void showViewInfoImage(int resid, String title, String text, boolean showReconnect) {
        mTouchBackground.setVisibility(View.GONE);
        mInfoWait.setVisibility(View.GONE);
        mInfoImage.setImageResource(resid);
        mInfoImage.setVisibility(View.VISIBLE);
        setViewInfoText(title, text, showReconnect);
    }

    private void refreshViewInfo() {
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
        setWindowFullscreen(isConnected);
        mButtonKeyboard.setVisibility(isConnected ? View.VISIBLE : View.GONE);

        switch (hidState) {
        case DaemonService.HID_STATE_CONNECTING:
            showViewInfoWait(getString(R.string.info_title_connecting), "");
            break;
        case DaemonService.HID_STATE_DISCONNECTING:
        case DaemonService.HID_STATE_DISCONNECTED:
            if (mWasKeyboardToggled) {
                hideKeyboard();
            }

            // No need for update when the Activity is closing
            if (!isFinishing()) {
                switch (errorCode) {
                case 0:
                    showViewInfoImage(R.drawable.disconnected,
                            getString(R.string.info_title_disconnected), "", true);
                    break;
                case DaemonService.ERROR_ACCES:
                    showViewInfoImage(R.drawable.problem,
                            getString(R.string.info_title_permission_denied),
                            getString(R.string.info_text_pair_again),
                            false);
                    break;
                case DaemonService.ERROR_HOSTDOWN:
                    showViewInfoImage(R.drawable.problem,
                            getString(R.string.info_title_host_unavailable),
                            getString(R.string.info_text_host_unavailable),
                            true);
                    break;
                case DaemonService.ERROR_CONNREFUSED:
                    showViewInfoImage(R.drawable.problem,
                            getString(R.string.info_title_connection_refused),
                            getString(R.string.info_text_connection_refused),
                            true);
                    break;
                case DaemonService.ERROR_BADE:
                    showViewInfoImage(R.drawable.problem,
                            getString(R.string.info_title_authorization_error),
                            getString(R.string.info_text_pair_again),
                            false);
                    break;
                case DaemonService.ERROR_TIMEDOUT:
                    showViewInfoImage(R.drawable.problem,
                            getString(R.string.info_title_connection_timeout),
                            getString(R.string.info_text_host_unavailable),
                            true);
                    break;
                case DaemonService.ERROR_ALREADY:
                    showViewInfoWait(getString(R.string.info_title_connecting), "");
                    break;
                default:
                    showViewInfoImage(R.drawable.problem,
                            getString(R.string.info_title_connection_problem),
                            getString(R.string.info_text_host_unavailable),
                            true);
                    break;
                }
            }
            break;
        case DaemonService.HID_STATE_CONNECTED:
            showViewInfoTouchpad();

            if (mShowKeyboardOnConnect) {
                mShowKeyboardOnConnect = false;
                showKeyboard();
            }
            break;
        }
    }
}
