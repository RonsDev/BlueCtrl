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

import org.ronsdev.bluectrl.daemon.DaemonService;

import android.util.Log;

/**
 * Virtual Mouse that sends HID Mouse Reports to the application daemon.
 */
public class HidMouse {

    private static final String TAG = "HidMouse";
    private static final boolean V = false;


    /** Indicates that the first Mouse button is pressed. */
    public static final int BUTTON_FIRST = 0x01;

    /** Indicates that the second Mouse button is pressed. */
    public static final int BUTTON_SECOND = 0x02;

    /** Indicates that the middle Mouse button is pressed. */
    public static final int BUTTON_MIDDLE = 0x04;

    /** Indicates that the Mouse button number 4 is pressed. */
    public static final int BUTTON_4 = 0x08;

    /** Indicates that the Mouse button number 5 is pressed. */
    public static final int BUTTON_5 = 0x10;


    /** Indicates that the Mouse button is pressed. */
    public static final int CLICK_TYPE_DOWN = 10;

    /** Indicates that the Mouse button is released. */
    public static final int CLICK_TYPE_UP = 20;

    /** Indicates that the Mouse button is pressed and immediately released for a single click. */
    public static final int CLICK_TYPE_CLICK = 30;


    /** The maximum absolute pointer position on the X-axis. */
    public static final int MAX_ABSOLUTE_VALUE_X = 2047;

    /** The maximum absolute pointer position on the Y-axis. */
    public static final int MAX_ABSOLUTE_VALUE_Y = 2047;


    private DaemonService mDaemon;

    private int mPressedButtons = 0;


    private OnMouseButtonClickListener mOnMouseButtonClickListener;


    public HidMouse(DaemonService daemon) {
        mDaemon = daemon;
    }


    private void onMouseButtonClick(int clickType, int button) {
        if (mOnMouseButtonClickListener != null) {
            mOnMouseButtonClickListener.onMouseButtonClick(clickType, button);
        }
    }

    public void setOnMouseButtonClickListener(OnMouseButtonClickListener listener) {
        mOnMouseButtonClickListener = listener;
    }

    public boolean isConnected() {
        return (mDaemon.isRunning() &&
                (mDaemon.getHidState() == DaemonService.HID_STATE_CONNECTED));
    }

    public int getPressedButtons() {
        return mPressedButtons;
    }

    public boolean isButtonPressed(int button) {
        return ((button & mPressedButtons) > 0);
    }

    public void pressButton(int button) {
        final int newButtons = mPressedButtons | button;
        if (mPressedButtons != newButtons) {
            mPressedButtons = newButtons;

            mDaemon.sendMouseReport(mPressedButtons, 0, 0, 0, 0);

            onMouseButtonClick(CLICK_TYPE_DOWN, button);

            if (V) Log.v(TAG, String.format("Mouse button pressed (0x%h)", button));
        }
    }

    public void releaseButton(int button) {
        final int newButtons = mPressedButtons & ~button;
        if (mPressedButtons != newButtons) {
            mPressedButtons = newButtons;

            mDaemon.sendMouseReport(mPressedButtons, 0, 0, 0, 0);

            onMouseButtonClick(CLICK_TYPE_UP, button);

            if (V) Log.v(TAG, String.format("Mouse button released (0x%h)", button));
        }
    }

    public void clickButton(int button) {
        final int newButtons = mPressedButtons | button;
        if (mPressedButtons != newButtons) {
            mDaemon.sendMouseReport(newButtons, 0, 0, 0, 0);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // Ignore non critical InterruptedException
            }
            mDaemon.sendMouseReport(mPressedButtons, 0, 0, 0, 0);

            onMouseButtonClick(CLICK_TYPE_CLICK, button);

            if (V) Log.v(TAG, String.format("Mouse button clicked (0x%h)", button));
        }
    }

    public void movePointer(int x, int y) {
        mDaemon.sendMouseReport(mPressedButtons, x, y, 0, 0);
    }

    public void movePointerAbsolute(int x, int y) {
        mDaemon.sendMouseAbsoluteReport(mPressedButtons, x, y);
    }

    public boolean isSmoothScrollYOn() {
        return mDaemon.isSmoothScrollYOn();
    }

    public boolean isSmoothScrollXOn() {
        return mDaemon.isSmoothScrollXOn();
    }

    public void scrollWheel(int y, int x) {
        mDaemon.sendMouseReport(mPressedButtons, 0, 0, y, x);
    }
}
