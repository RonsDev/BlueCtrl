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
 * Virtual Keyboard that sends HID Keyboard Reports to the application daemon.
 */
public class HidKeyboard {

    private static final String TAG = "HidKeyboard";
    private static final boolean V = false;


    /** Indicates that the left Control modifier key is pressed. */
    public static final int MODIFIER_LEFT_CTRL = 0x01;

    /** Indicates that the left Shift modifier key is pressed. */
    public static final int MODIFIER_LEFT_SHIFT = 0x02;

    /** Indicates that the left Alt modifier key is pressed. */
    public static final int MODIFIER_LEFT_ALT = 0x04;

    /** Indicates that the left GUI modifier key is pressed. */
    public static final int MODIFIER_LEFT_GUI = 0x08;

    /** Indicates that the right Control modifier key is pressed. */
    public static final int MODIFIER_RIGHT_CTRL = 0x10;

    /** Indicates that the right Shift modifier key is pressed. */
    public static final int MODIFIER_RIGHT_SHIFT = 0x20;

    /** Indicates that the right Alt modifier key is pressed. */
    public static final int MODIFIER_RIGHT_ALT = 0x40;

    /** Indicates that the right GUI modifier key is pressed. */
    public static final int MODIFIER_RIGHT_GUI = 0x80;


    /** Indicates that the system key 'Power' is pressed. */
    public static final int SYSTEM_KEY_POWER = 0x01;

    /** Indicates that the system key 'Sleep' is pressed. */
    public static final int SYSTEM_KEY_SLEEP = 0x02;


    /** Indicates that the hardware key 'Eject' is pressed. */
    public static final int HARDWARE_KEY_EJECT = 0x08;


    /** Indicates that the media key 'Play/Pause' is pressed. */
    public static final int MEDIA_KEY_PLAY_PAUSE = 0x01;

    /** Indicates that the media key 'Forward' is pressed. */
    public static final int MEDIA_KEY_FORWARD = 0x02;

    /** Indicates that the media key 'Rewind' is pressed. */
    public static final int MEDIA_KEY_REWIND = 0x04;

    /** Indicates that the media key 'Scan Next Track' is pressed. */
    public static final int MEDIA_KEY_SCAN_NEXT_TRACK = 0x08;

    /** Indicates that the media key 'Scan Previous Track' is pressed. */
    public static final int MEDIA_KEY_SCAN_PREV_TRACK = 0x10;


    private DaemonService mDaemon;

    private int mPressedModifier = 0;
    private int mPressedSystemKeys = 0;
    private int mPressedHardwareKeys = 0;
    private int mPressedMediaKeys = 0;
    private IntArrayList mPressedKeys = new IntArrayList(6);


    public HidKeyboard(DaemonService daemon) {
        mDaemon = daemon;
    }


    private int[] getPressedKeysArray() {
        int result[] = new int[mPressedKeys.size()];
        for (int i=0; i < mPressedKeys.size(); i++) {
            result[i] = mPressedKeys.getValue(i);
        }
        return result;
    }

    public boolean isConnected() {
        return (mDaemon.isRunning() &&
                (mDaemon.getHidState() == DaemonService.HID_STATE_CONNECTED));
    }

    public void pressModifierKey(int hidModifier) {
        final int newModifier = mPressedModifier | hidModifier;
        if (mPressedModifier != newModifier) {
            mPressedModifier = newModifier;

            mDaemon.sendKeyboardReport(mPressedModifier, getPressedKeysArray());

            if (V) Log.v(TAG, String.format("modifier key pressed (0x%h)", hidModifier));
        }
    }

    public void releaseModifierKey(int hidModifier) {
        final int newModifier = mPressedModifier & ~hidModifier;
        if (mPressedModifier != newModifier) {
            mPressedModifier = newModifier;

            mDaemon.sendKeyboardReport(mPressedModifier, getPressedKeysArray());

            if (V) Log.v(TAG, String.format("modifier key released (0x%h)", hidModifier));
        }
    }

    public void pressKey(int hidKeyCode) {
        if (!mPressedKeys.containsValue(hidKeyCode)) {
            mPressedKeys.addValue(hidKeyCode);

            mDaemon.sendKeyboardReport(mPressedModifier, getPressedKeysArray());

            if (V) Log.v(TAG, String.format("key pressed (%d)", hidKeyCode));
        }
    }

    public void releaseKey(int hidKeyCode) {
        if (mPressedKeys.containsValue(hidKeyCode)) {
            mPressedKeys.removeValue(hidKeyCode);

            mDaemon.sendKeyboardReport(mPressedModifier, getPressedKeysArray());

            if (V) Log.v(TAG, String.format("key released (%d)", hidKeyCode));
        }
    }

    public void pressSystemKey(int key) {
        final int newKeys = mPressedSystemKeys | key;
        if (mPressedSystemKeys != newKeys) {
            mPressedSystemKeys = newKeys;

            mDaemon.sendSystemKeyReport(mPressedSystemKeys);

            if (V) Log.v(TAG, String.format("system key pressed (0x%h)", key));
        }
    }

    public void releaseSystemKey(int key) {
        final int newKeys = mPressedSystemKeys & ~key;
        if (mPressedSystemKeys != newKeys) {
            mPressedSystemKeys = newKeys;

            mDaemon.sendSystemKeyReport(mPressedSystemKeys);

            if (V) Log.v(TAG, String.format("system key released (0x%h)", key));
        }
    }

    public void pressHardwareKey(int key) {
        final int newKeys = mPressedHardwareKeys | key;
        if (mPressedHardwareKeys != newKeys) {
            mPressedHardwareKeys = newKeys;

            mDaemon.sendHardwareKeyReport(mPressedHardwareKeys);

            if (V) Log.v(TAG, String.format("hardware key pressed (0x%h)", key));
        }
    }

    public void releaseHardwareKey(int key) {
        final int newKeys = mPressedHardwareKeys & ~key;
        if (mPressedHardwareKeys != newKeys) {
            mPressedHardwareKeys = newKeys;

            mDaemon.sendHardwareKeyReport(mPressedHardwareKeys);

            if (V) Log.v(TAG, String.format("hardware key released (0x%h)", key));
        }
    }

    public void pressMediaKey(int key) {
        final int newKeys = mPressedMediaKeys | key;
        if (mPressedMediaKeys != newKeys) {
            mPressedMediaKeys = newKeys;

            mDaemon.sendMediaKeyReport(mPressedMediaKeys);

            if (V) Log.v(TAG, String.format("media key pressed (0x%h)", key));
        }
    }

    public void releaseMediaKey(int key) {
        final int newKeys = mPressedMediaKeys & ~key;
        if (mPressedMediaKeys != newKeys) {
            mPressedMediaKeys = newKeys;

            mDaemon.sendMediaKeyReport(mPressedMediaKeys);

            if (V) Log.v(TAG, String.format("media key released (0x%h)", key));
        }
    }
}
