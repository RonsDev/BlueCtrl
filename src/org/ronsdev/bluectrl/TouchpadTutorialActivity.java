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
import org.ronsdev.bluectrl.widget.TouchpadView;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ViewFlipper;

import java.util.ArrayList;

/**
 * Tutorial Activity for touchpad gestures.
 */
public class TouchpadTutorialActivity extends DaemonActivity
        implements OnMouseButtonClickListener {

    /**
     * Used as a Parcelable BluetoothDevice extra field in start Activity intents to get the
     * current Bluetooth device.
     */
    public static final String EXTRA_DEVICE =
            "org.ronsdev.bluectrl.touchpadtutorial.extra.DEVICE";


    private Animation mAnimFlipperInPrevious;
    private Animation mAnimFlipperOutPrevious;
    private Animation mAnimFlipperInNext;
    private Animation mAnimFlipperOutNext;

    private ImageButton mButtonPrevious;
    private ImageButton mButtonNext;
    private TouchpadView mTouchpadView;
    private ViewFlipper mViewFlipper;

    private ArrayList<View> mPageList;
    private int mCurrentPage = 0;

    private BluetoothDevice mBtDevice;
    private DeviceSettings mDeviceSettings;
    private HidKeyboard mHidKeyboard;
    private HidMouse mHidMouse;


    private OnClickListener mActionBarHomeClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            TouchpadTutorialActivity.this.finish();
        }
    };

    private OnClickListener mPreviousClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            changePage(-1);
        }
    };

    private OnClickListener mNextClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            changePage(1);
        }
    };


    public static void startActivity(Activity curActivity, BluetoothDevice device) {
        Intent intent = new Intent(curActivity, TouchpadTutorialActivity.class);
        intent.putExtra(EXTRA_DEVICE, device);
        curActivity.startActivity(intent);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        mBtDevice = extras.getParcelable(EXTRA_DEVICE);

        mDeviceSettings = DeviceSettings.get(this, mBtDevice);

        loadLayout();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        loadLayout();
    }

    @Override
    protected void onDaemonAvailable() {
        final DaemonService daemon = getDaemon();

        mHidKeyboard = new HidKeyboard(daemon);
        mHidKeyboard.setKeyMap(this, mDeviceSettings.getKeyMap());

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
        if (hidState != DaemonService.HID_STATE_CONNECTED) {
            this.finish();
        }
    }


    private void loadLayout() {
        setContentView(R.layout.touchpad_tutorial);


        mAnimFlipperInPrevious = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        mAnimFlipperOutPrevious = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);
        mAnimFlipperInNext = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        mAnimFlipperOutNext = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);


        ImageButton actionBarHome = (ImageButton)findViewById(R.id.action_bar_home);
        actionBarHome.setOnClickListener(mActionBarHomeClickListener);

        mButtonPrevious = (ImageButton)findViewById(R.id.button_previous);
        mButtonPrevious.setOnClickListener(mPreviousClickListener);

        mButtonNext = (ImageButton)findViewById(R.id.button_next);
        mButtonNext.setOnClickListener(mNextClickListener);


        mTouchpadView = (TouchpadView)findViewById(R.id.touchpad);
        mTouchpadView.setHidMouse(mHidMouse);
        mTouchpadView.setHidKeyboard(mHidKeyboard);
        mTouchpadView.setGestureMode(mDeviceSettings.getTouchpadGestureMode());
        mTouchpadView.setShowButtons(false);
        mTouchpadView.setShowInfoGraphics(false);
        mTouchpadView.setMouseSensitivity(mDeviceSettings.getMouseSensitivity());
        mTouchpadView.setScrollSensitivity(mDeviceSettings.getScrollSensitivity());
        mTouchpadView.setPinchZoomSensitivity(mDeviceSettings.getPinchZoomSensitivity());
        mTouchpadView.setInvertScroll(mDeviceSettings.getInvertScroll());
        mTouchpadView.setFlingScroll(mDeviceSettings.getFlingScroll());


        mViewFlipper = (ViewFlipper)findViewById(R.id.flipper);

        initPageList();
    }

    public void onMouseButtonClick(int clickType, int button) {
        if (mTouchpadView != null) {
            mTouchpadView.onMouseButtonClick(clickType, button);
        }
    }

    private void initPageList() {
        mPageList = new ArrayList<View>();

        mPageList.add((View)findViewById(R.id.view_tutorial_introduction));
        mPageList.add((View)findViewById(R.id.view_tutorial_move));
        mPageList.add((View)findViewById(R.id.view_tutorial_tap));
        mPageList.add((View)findViewById(R.id.view_tutorial_drag));

        final String gestureMode = mDeviceSettings.getTouchpadGestureMode();
        if (DeviceSettings.TOUCHPAD_GESTURE_MODE_ANDROID.equals(gestureMode)) {
            mPageList.add((View)findViewById(R.id.view_tutorial_2f_scroll));
            mPageList.add((View)findViewById(R.id.view_tutorial_1f_scroll));
            mPageList.add((View)findViewById(R.id.view_tutorial_pinch_zoom));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_u_android));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_l_android));
        } else if (DeviceSettings.TOUCHPAD_GESTURE_MODE_GNOME_SHELL.equals(gestureMode)) {
            mPageList.add((View)findViewById(R.id.view_tutorial_2f_scroll));
            mPageList.add((View)findViewById(R.id.view_tutorial_1f_scroll));
            mPageList.add((View)findViewById(R.id.view_tutorial_pinch_zoom));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_d_gnome_shell));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_u_gnome_shell));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_lr_navigate));
            mPageList.add((View)findViewById(R.id.view_tutorial_4f_ud_gnome_shell));
            mPageList.add((View)findViewById(R.id.view_tutorial_4f_lr_snap_window));
        } else if (DeviceSettings.TOUCHPAD_GESTURE_MODE_OSX.equals(gestureMode)) {
            mPageList.add((View)findViewById(R.id.view_tutorial_2f_scroll));
            mPageList.add((View)findViewById(R.id.view_tutorial_1f_scroll));
            mPageList.add((View)findViewById(R.id.view_tutorial_pinch_zoom));
            mPageList.add((View)findViewById(R.id.view_tutorial_4f_u_osx));
            mPageList.add((View)findViewById(R.id.view_tutorial_4f_d_osx));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_lr_navigate));
            mPageList.add((View)findViewById(R.id.view_tutorial_4f_lr_osx));
        } else if (DeviceSettings.TOUCHPAD_GESTURE_MODE_PLAYSTATION3.equals(gestureMode)) {
            mPageList.add((View)findViewById(R.id.view_tutorial_2f_scroll));
            mPageList.add((View)findViewById(R.id.view_tutorial_1f_scroll));
            mPageList.add((View)findViewById(R.id.view_tutorial_pinch_zoom));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_lr_navigate));
        } else if (DeviceSettings.TOUCHPAD_GESTURE_MODE_UBUNTU_UNITY.equals(gestureMode)) {
            mPageList.add((View)findViewById(R.id.view_tutorial_2f_scroll));
            mPageList.add((View)findViewById(R.id.view_tutorial_1f_scroll));
            mPageList.add((View)findViewById(R.id.view_tutorial_pinch_zoom));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_d_ubuntu_unity));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_u_ubuntu_unity));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_lr_navigate));
            mPageList.add((View)findViewById(R.id.view_tutorial_4f_ud_max_min_window));
            mPageList.add((View)findViewById(R.id.view_tutorial_4f_lr_snap_window));
        } else if (DeviceSettings.TOUCHPAD_GESTURE_MODE_WINDOWS7.equals(gestureMode)) {
            mPageList.add((View)findViewById(R.id.view_tutorial_2f_scroll));
            mPageList.add((View)findViewById(R.id.view_tutorial_1f_scroll));
            mPageList.add((View)findViewById(R.id.view_tutorial_pinch_zoom));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_u_windows7));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_d_desktop));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_lr_navigate));
            mPageList.add((View)findViewById(R.id.view_tutorial_4f_ud_max_min_window));
            mPageList.add((View)findViewById(R.id.view_tutorial_4f_lr_snap_window));
        } else if (DeviceSettings.TOUCHPAD_GESTURE_MODE_WINDOWS8.equals(gestureMode)) {
            mPageList.add((View)findViewById(R.id.view_tutorial_2f_scroll));
            mPageList.add((View)findViewById(R.id.view_tutorial_1f_scroll));
            mPageList.add((View)findViewById(R.id.view_tutorial_pinch_zoom));
            mPageList.add((View)findViewById(R.id.view_tutorial_edge_l_windows8));
            mPageList.add((View)findViewById(R.id.view_tutorial_edge_r_windows8));
            mPageList.add((View)findViewById(R.id.view_tutorial_edge_t_windows8));
            mPageList.add((View)findViewById(R.id.view_tutorial_edge_b_windows8));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_u_windows8));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_d_desktop));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_lr_navigate));
            mPageList.add((View)findViewById(R.id.view_tutorial_4f_ud_max_min_window));
            mPageList.add((View)findViewById(R.id.view_tutorial_4f_lr_snap_window));
        } else {
            mPageList.add((View)findViewById(R.id.view_tutorial_2f_scroll));
            mPageList.add((View)findViewById(R.id.view_tutorial_1f_scroll));
            mPageList.add((View)findViewById(R.id.view_tutorial_pinch_zoom));
            mPageList.add((View)findViewById(R.id.view_tutorial_3f_lr_navigate));
        }

        changePage(0);
    }

    private void changePage(int direction) {
        if (direction > 0) {
            mCurrentPage++;
            mViewFlipper.setInAnimation(mAnimFlipperInNext);
            mViewFlipper.setOutAnimation(mAnimFlipperOutNext);
        } else if (direction < 0) {
            mCurrentPage--;
            mViewFlipper.setInAnimation(mAnimFlipperInPrevious);
            mViewFlipper.setOutAnimation(mAnimFlipperOutPrevious);
        } else {
            mViewFlipper.setInAnimation(null);
            mViewFlipper.setOutAnimation(null);
        }

        if (mCurrentPage < 0) {
            mCurrentPage = 0;
        } else if (mCurrentPage >= mPageList.size()) {
            mCurrentPage = mPageList.size() - 1;
        }

        final int childIndex = mViewFlipper.indexOfChild(mPageList.get(mCurrentPage));
        if ((childIndex > -1) && (childIndex != mViewFlipper.getDisplayedChild())) {
            mViewFlipper.setDisplayedChild(childIndex);
        }

        mButtonPrevious.setEnabled(mCurrentPage > 0);
        mButtonNext.setEnabled(mCurrentPage < (mPageList.size() - 1));
    }
}
