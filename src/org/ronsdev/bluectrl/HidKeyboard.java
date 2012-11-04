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

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

/**
 * Virtual Keyboard that sends HID Keyboard Reports to the application daemon.
 */
public class HidKeyboard {

    private static final String TAG = "HidKeyboard";
    private static final boolean V = false;


    /*
     * HID key codes
     *
     * For more information look at the "USB - HID Usage Tables" document under the
     * chapter "10 Keyboard/Keypad Page".
     */
    public static final int KEYCODE_A = 4;
    public static final int KEYCODE_B = 5;
    public static final int KEYCODE_C = 6;
    public static final int KEYCODE_D = 7;
    public static final int KEYCODE_E = 8;
    public static final int KEYCODE_F = 9;
    public static final int KEYCODE_G = 10;
    public static final int KEYCODE_H = 11;
    public static final int KEYCODE_I = 12;
    public static final int KEYCODE_J = 13;
    public static final int KEYCODE_K = 14;
    public static final int KEYCODE_L = 15;
    public static final int KEYCODE_M = 16;
    public static final int KEYCODE_N = 17;
    public static final int KEYCODE_O = 18;
    public static final int KEYCODE_P = 19;
    public static final int KEYCODE_Q = 20;
    public static final int KEYCODE_R = 21;
    public static final int KEYCODE_S = 22;
    public static final int KEYCODE_T = 23;
    public static final int KEYCODE_U = 24;
    public static final int KEYCODE_V = 25;
    public static final int KEYCODE_W = 26;
    public static final int KEYCODE_X = 27;
    public static final int KEYCODE_Y = 28;
    public static final int KEYCODE_Z = 29;
    public static final int KEYCODE_1 = 30;
    public static final int KEYCODE_2 = 31;
    public static final int KEYCODE_3 = 32;
    public static final int KEYCODE_4 = 33;
    public static final int KEYCODE_5 = 34;
    public static final int KEYCODE_6 = 35;
    public static final int KEYCODE_7 = 36;
    public static final int KEYCODE_8 = 37;
    public static final int KEYCODE_9 = 38;
    public static final int KEYCODE_0 = 39;
    public static final int KEYCODE_ENTER = 40;
    public static final int KEYCODE_ESCAPE = 41;
    public static final int KEYCODE_DEL = 42;
    public static final int KEYCODE_TAB = 43;
    public static final int KEYCODE_SPACE = 44;
    public static final int KEYCODE_MINUS = 45;
    public static final int KEYCODE_EQUAL = 46;
    public static final int KEYCODE_LEFT_BRACKET = 47;
    public static final int KEYCODE_RIGHT_BRACKET = 48;
    public static final int KEYCODE_BACKSLASH = 49;
    public static final int KEYCODE_NON_US_POUND = 50;
    public static final int KEYCODE_SEMICOLON = 51;
    public static final int KEYCODE_APOSTROPHE = 52;
    public static final int KEYCODE_TILDE = 53;
    public static final int KEYCODE_COMMA = 54;
    public static final int KEYCODE_PERIOD = 55;
    public static final int KEYCODE_SLASH = 56;
    public static final int KEYCODE_CAPS_LOCK = 57;
    public static final int KEYCODE_F1 = 58;
    public static final int KEYCODE_F2 = 59;
    public static final int KEYCODE_F3 = 60;
    public static final int KEYCODE_F4 = 61;
    public static final int KEYCODE_F5 = 62;
    public static final int KEYCODE_F6 = 63;
    public static final int KEYCODE_F7 = 64;
    public static final int KEYCODE_F8 = 65;
    public static final int KEYCODE_F9 = 66;
    public static final int KEYCODE_F10 = 67;
    public static final int KEYCODE_F11 = 68;
    public static final int KEYCODE_F12 = 69;
    public static final int KEYCODE_PRINT_SCREEN = 70;
    public static final int KEYCODE_SCROLL_LOCK = 71;
    public static final int KEYCODE_PAUSE = 72;
    public static final int KEYCODE_INSERT = 73;
    public static final int KEYCODE_HOME = 74;
    public static final int KEYCODE_PAGE_UP = 75;
    public static final int KEYCODE_FORWARD_DEL = 76;
    public static final int KEYCODE_END = 77;
    public static final int KEYCODE_PAGE_DOWN = 78;
    public static final int KEYCODE_RIGHT_ARROW = 79;
    public static final int KEYCODE_LEFT_ARROW = 80;
    public static final int KEYCODE_DOWN_ARROW = 81;
    public static final int KEYCODE_UP_ARROW = 82;
    public static final int KEYCODE_NUM_LOCK = 83;
    public static final int KEYCODE_NUMPAD_DIVIDE = 84;
    public static final int KEYCODE_NUMPAD_MULTIPLY = 85;
    public static final int KEYCODE_NUMPAD_SUBTRACT = 86;
    public static final int KEYCODE_NUMPAD_ADD = 87;
    public static final int KEYCODE_NUMPAD_ENTER = 88;
    public static final int KEYCODE_NUMPAD_1 = 89;
    public static final int KEYCODE_NUMPAD_2 = 90;
    public static final int KEYCODE_NUMPAD_3 = 91;
    public static final int KEYCODE_NUMPAD_4 = 92;
    public static final int KEYCODE_NUMPAD_5 = 93;
    public static final int KEYCODE_NUMPAD_6 = 94;
    public static final int KEYCODE_NUMPAD_7 = 95;
    public static final int KEYCODE_NUMPAD_8 = 96;
    public static final int KEYCODE_NUMPAD_9 = 97;
    public static final int KEYCODE_NUMPAD_0 = 98;
    public static final int KEYCODE_NUMPAD_DOT = 99;
    public static final int KEYCODE_NON_US_BACKSLASH = 100;
    public static final int KEYCODE_APPLICATION = 101;
    public static final int KEYCODE_NUMPAD_EQUALS = 103;
    public static final int KEYCODE_F13 = 104;
    public static final int KEYCODE_F14 = 105;
    public static final int KEYCODE_F15 = 106;
    public static final int KEYCODE_F16 = 107;
    public static final int KEYCODE_F17 = 108;
    public static final int KEYCODE_F18 = 109;
    public static final int KEYCODE_F19 = 110;
    public static final int KEYCODE_F20 = 111;
    public static final int KEYCODE_F21 = 112;
    public static final int KEYCODE_F22 = 113;
    public static final int KEYCODE_F23 = 114;
    public static final int KEYCODE_F24 = 115;
    public static final int KEYCODE_EXECUTE = 116;
    public static final int KEYCODE_HELP = 117;
    public static final int KEYCODE_MENU = 118;
    public static final int KEYCODE_SELECT = 119;
    public static final int KEYCODE_STOP = 120;
    public static final int KEYCODE_AGAIN = 121;
    public static final int KEYCODE_UNDO = 122;
    public static final int KEYCODE_CUT = 123;
    public static final int KEYCODE_COPY = 124;
    public static final int KEYCODE_PASTE = 125;
    public static final int KEYCODE_FIND = 126;
    public static final int KEYCODE_MUTE = 127;
    public static final int KEYCODE_VOLUME_UP = 128;
    public static final int KEYCODE_VOLUME_DOWN = 129;
    public static final int KEYCODE_NUMPAD_COMMA = 133;
    public static final int KEYCODE_SYSRQ = 154;
    public static final int KEYCODE_CLEAR = 156;
    public static final int KEYCODE_NUMPAD_LEFT_PAREN = 182;
    public static final int KEYCODE_NUMPAD_RIGHT_PAREN = 183;


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

    /** Indicates that the media key 'Mute' is pressed. */
    public static final int MEDIA_KEY_MUTE = 0x20;

    /** Indicates that the media key 'Volume Increment' is pressed. */
    public static final int MEDIA_KEY_VOLUME_INC = 0x40;

    /** Indicates that the media key 'Volume Decrement' is pressed. */
    public static final int MEDIA_KEY_VOLUME_DEC = 0x80;


    /** Indicates that the application control key 'Home' is pressed. */
    public static final int AC_KEY_HOME = 0x01;

    /** Indicates that the application control key 'Back' is pressed. */
    public static final int AC_KEY_BACK = 0x02;

    /** Indicates that the application control key 'Forward' is pressed. */
    public static final int AC_KEY_FORWARD = 0x04;


    private DaemonService mDaemon;

    private String mKeyMap = "";
    private CharKeyReportMap mCharKeyMap = null;

    private int mPressedModifier = 0;
    private int mPressedSystemKeys = 0;
    private int mPressedHardwareKeys = 0;
    private int mPressedMediaKeys = 0;
    private int mPressedAppCtrlKeys = 0;
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

    public String getKeyMap() {
        return mKeyMap;
    }
    public void setKeyMap(Context context, String keyMap) {
        if ((keyMap == null) || keyMap.isEmpty()) {
            mKeyMap = "";
            mCharKeyMap = null;
        } else if (!keyMap.equals(mKeyMap)) {
            mKeyMap = keyMap;
            mCharKeyMap = new CharKeyReportMap(keyMap, context.getAssets());
        }
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

    public void pressAppCtrlKey(int key) {
        final int newKeys = mPressedAppCtrlKeys | key;
        if (mPressedAppCtrlKeys != newKeys) {
            mPressedAppCtrlKeys = newKeys;

            mDaemon.sendAppCtrlKeyReport(mPressedAppCtrlKeys);

            if (V) Log.v(TAG, String.format("application control key pressed (0x%h)", key));
        }
    }

    public void releaseAppCtrlKey(int key) {
        final int newKeys = mPressedAppCtrlKeys & ~key;
        if (mPressedAppCtrlKeys != newKeys) {
            mPressedAppCtrlKeys = newKeys;

            mDaemon.sendAppCtrlKeyReport(mPressedAppCtrlKeys);

            if (V) Log.v(TAG, String.format("application control key released (0x%h)", key));
        }
    }

    /**
     * Presses the corresponding key for the specified character. Only simple characters that can
     * be produced with one key and one modifier key (in other words no dead keys) are supported.
     * Also the character must be specified in the current Keymap file.
     * @param key
     * The character of the key that should be pressed.
     * @return
     * Returns {@code false} if the character cannot be mapped to a key.
     */
    public boolean pressCharKey(char key) {
        if (mCharKeyMap == null) {
            return false;
        }

        CharKeyReportMap.KeyReportSequence keyReportSequence = mCharKeyMap.get(key);
        if ((keyReportSequence != null) && (keyReportSequence.size() == 1)) {
            final int hidModifier = keyReportSequence.get(0).getModifier();
            if (hidModifier != 0) {
                pressModifierKey(hidModifier);
            }

            final int hidKeyCode = keyReportSequence.get(0).getKeyCode();
            if (hidKeyCode != 0) {
                pressKey(hidKeyCode);
            }

            return true;
        }

        return false;
    }

    /**
     * Releases the corresponding key for the specified character. Only simple characters that can
     * be produced with one key and one modifier key (in other words no dead keys) are supported.
     * Also the character must be specified in the current Keymap file.
     * @param key
     * The character of the key that should be released.
     * @return
     * Returns {@code false} if the character cannot be mapped to a key.
     */
    public boolean releaseCharKey(char key) {
        if (mCharKeyMap == null) {
            return false;
        }

        CharKeyReportMap.KeyReportSequence keyReportSequence = mCharKeyMap.get(key);
        if ((keyReportSequence != null) && (keyReportSequence.size() == 1)) {
            final int hidKeyCode = keyReportSequence.get(0).getKeyCode();
            if (hidKeyCode != 0) {
                releaseKey(hidKeyCode);
            }

            final int hidModifier = keyReportSequence.get(0).getModifier();
            if (hidModifier != 0) {
                releaseModifierKey(hidModifier);
            }

            return true;
        }

        return false;
    }

    /** Types a complete text. */
    public void typeText(String text) {
        if (mCharKeyMap == null) {
            Log.w(TAG, "Keymap not set");
            return;
        }

        ArrayList<CharKeyReportMap.KeyReport> reportList = convertToKeyReports(text);

        if (reportList.size() < 1) {
            return;
        }

        int index = 0;
        CharKeyReportMap.KeyReport keyReport;
        CharKeyReportMap.KeyReport nextKeyReport = reportList.get(index);
        while (nextKeyReport != null) {
            keyReport = nextKeyReport;
            index++;
            nextKeyReport = (index < reportList.size()) ? reportList.get(index) : null;

            final int modifier = keyReport.getModifier();
            final int keyCode = keyReport.getKeyCode();

            pressModifierKey(modifier);
            pressKey(keyCode);
            releaseKey(keyCode);

            // If the next Modifier value equals the current value then don't reset the Modifier.
            // This saves two unnecessary HID Keyboard Reports.
            if ((nextKeyReport == null) || (nextKeyReport.getModifier() != modifier)) {
                releaseModifierKey(modifier);
            }
        }
    }

    /** Converts a text to a list of Keyboard Reports. */
    private ArrayList<CharKeyReportMap.KeyReport> convertToKeyReports(String text) {
        ArrayList<CharKeyReportMap.KeyReport> result = new ArrayList<CharKeyReportMap.KeyReport>();

        for (int i = 0; i < text.length(); i++) {
            final char character = text.charAt(i);

            CharKeyReportMap.KeyReportSequence keyReportSequence = mCharKeyMap.get(character);
            if (keyReportSequence != null) {
                result.addAll(keyReportSequence);
            } else {
                Log.w(TAG, String.format("unknown Keymap character '%c'", character));
            }
        }

        return result;
    }
}
