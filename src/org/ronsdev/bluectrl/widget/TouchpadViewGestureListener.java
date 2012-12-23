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

package org.ronsdev.bluectrl.widget;

import org.ronsdev.bluectrl.DeviceSettings;
import org.ronsdev.bluectrl.HidKeyboard;
import org.ronsdev.bluectrl.HidMouse;

/**
 * Handles gesture events of the TouchpadView.
 */
public class TouchpadViewGestureListener implements OnTouchpadGestureListener {

    private TouchpadView mTouchpadView = null;
    private String mGestureMode;


    public TouchpadViewGestureListener(TouchpadView touchpadView) {
        mTouchpadView = touchpadView;
        mGestureMode = DeviceSettings.DEFAULT_TOUCHPAD_GESTURE_MODE;
    }


    public String getGestureMode() {
        return mGestureMode;
    }
    public void setGestureMode(String value) {
        mGestureMode = value;
    }


    private boolean activateScrollMode(int scrollMode) {
        mTouchpadView.activateScrollMode(scrollMode);
        return true;
    }

    private boolean clickMouseButton(int button) {
        final HidMouse hidMouse = mTouchpadView.getHidMouse();

        if (hidMouse != null) {
            mTouchpadView.performGestureDetectedFeedback();
            hidMouse.clickButton(button);
            return true;
        } else {
            return false;
        }
    }

    private boolean pressShortcutKey(int modifier, int key) {
        final HidKeyboard hidKeyboard = mTouchpadView.getHidKeyboard();

        if (hidKeyboard != null) {
            mTouchpadView.performGestureDetectedFeedback();

            if (modifier != 0) {
                hidKeyboard.pressModifierKey(modifier);
            }

            if (key != 0) {
                hidKeyboard.pressKey(key);
                hidKeyboard.releaseKey(key);
            }

            if (modifier != 0) {
                hidKeyboard.releaseModifierKey(modifier);
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean pressShortcutKey(int modifier, char key) {
        final HidKeyboard hidKeyboard = mTouchpadView.getHidKeyboard();

        if (hidKeyboard != null) {
            mTouchpadView.performGestureDetectedFeedback();

            if (modifier != 0) {
                hidKeyboard.pressModifierKey(modifier);
            }

            hidKeyboard.pressCharKey(key);
            hidKeyboard.releaseCharKey(key);

            if (modifier != 0) {
                hidKeyboard.releaseModifierKey(modifier);
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean pressAppCtrlKey(int key) {
        final HidKeyboard hidKeyboard = mTouchpadView.getHidKeyboard();

        if (hidKeyboard != null) {
            mTouchpadView.performGestureDetectedFeedback();
            hidKeyboard.pressAppCtrlKey(key);
            hidKeyboard.releaseAppCtrlKey(key);
            return true;
        } else {
            return false;
        }
    }

    private boolean doWindows8TopEdgeGesture() {
        final HidMouse hidMouse = mTouchpadView.getHidMouse();

        if (hidMouse != null) {
            hidMouse.movePointerAbsolute((HidMouse.MAX_ABSOLUTE_VALUE_X / 2), 0);
            mTouchpadView.activateDragMode(HidMouse.BUTTON_FIRST, true);
            return true;
        } else {
            return false;
        }
    }

    private boolean doWindows8LeftEdgeGesture() {
        final HidMouse hidMouse = mTouchpadView.getHidMouse();

        if (hidMouse != null) {
            /*
             * HACK: The best way to emulate a Windows 8 touchscreen gesture from the left edge is
             *       to move the mouse in the top left hotcorner and start a drag action. However
             *       it is not quite as easy because this action requires a tricky timing (most
             *       likely because Windows wants to prevent unintended hotcorner activations).
             *       The next code was created per try-and-error and works reasonable well for
             *       different mouse sensitivities but might not be the best solution.
             */
            try {
                hidMouse.movePointerAbsolute(10, 10);
                hidMouse.movePointer(-20, -20);
                Thread.sleep(20);
                mTouchpadView.activateDragMode(HidMouse.BUTTON_FIRST, true);
                hidMouse.movePointerAbsolute(50, 100);
                Thread.sleep(20);
                hidMouse.movePointerAbsolute(100, (HidMouse.MAX_ABSOLUTE_VALUE_Y / 4));
            } catch (InterruptedException e) {
                // Ignore non critical InterruptedException
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean handleDefaultGesture(int gesture, int direction) {
        switch (gesture) {
        case TouchpadView.GESTURE_EDGE_RIGHT:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return activateScrollMode(TouchpadView.SCROLL_MODE_VERTICAL);
            }
            break;
        case TouchpadView.GESTURE_2FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return activateScrollMode(TouchpadView.SCROLL_MODE_VERTICAL);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                return activateScrollMode(TouchpadView.SCROLL_MODE_HORIZONTAL);
            }
            break;
        case TouchpadView.GESTURE_3FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_LEFT:
                return clickMouseButton(HidMouse.BUTTON_4);
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                return clickMouseButton(HidMouse.BUTTON_5);
            }
            break;
        }

        return false;
    }

    private boolean handleAndroidGesture(int gesture, int direction) {
        switch (gesture) {
        case TouchpadView.GESTURE_EDGE_RIGHT:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return activateScrollMode(TouchpadView.SCROLL_MODE_VERTICAL);
            }
            break;
        case TouchpadView.GESTURE_2FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return activateScrollMode(TouchpadView.SCROLL_MODE_VERTICAL);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                return activateScrollMode(TouchpadView.SCROLL_MODE_HORIZONTAL);
            }
            break;
        case TouchpadView.GESTURE_3FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
                return pressAppCtrlKey(HidKeyboard.AC_KEY_HOME);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
                return pressAppCtrlKey(HidKeyboard.AC_KEY_BACK);
            }
            break;
        }

        return false;
    }

    private boolean handleGnomeShellGesture(int gesture, int direction) {
        switch (gesture) {
        case TouchpadView.GESTURE_EDGE_RIGHT:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return activateScrollMode(TouchpadView.SCROLL_MODE_VERTICAL);
            }
            break;
        case TouchpadView.GESTURE_2FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return activateScrollMode(TouchpadView.SCROLL_MODE_VERTICAL);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                return activateScrollMode(TouchpadView.SCROLL_MODE_HORIZONTAL);
            }
            break;
        case TouchpadView.GESTURE_3FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
                // Super key + m = Show message tray
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI, 'm');
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                // Super key = Show activities overview
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI, 0);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
                return clickMouseButton(HidMouse.BUTTON_4);
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                return clickMouseButton(HidMouse.BUTTON_5);
            }
            break;
        case TouchpadView.GESTURE_4FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
                // Ctrl + Alt + Down = Move to workspace below
                return pressShortcutKey(
                        HidKeyboard.MODIFIER_LEFT_CTRL | HidKeyboard.MODIFIER_LEFT_ALT,
                        HidKeyboard.KEYCODE_DOWN_ARROW);
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                // Ctrl + Alt + Up = Move to workspace above
                return pressShortcutKey(
                        HidKeyboard.MODIFIER_LEFT_CTRL | HidKeyboard.MODIFIER_LEFT_ALT,
                        HidKeyboard.KEYCODE_UP_ARROW);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
                // Super key + Left = View split on left
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI,
                        HidKeyboard.KEYCODE_LEFT_ARROW);
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                // Super key + Right = View split on right
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI,
                        HidKeyboard.KEYCODE_RIGHT_ARROW);
            }
            break;
        }

        return false;
    }

    private boolean handleOsXGesture(int gesture, int direction) {
        switch (gesture) {
        case TouchpadView.GESTURE_EDGE_RIGHT:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return activateScrollMode(TouchpadView.SCROLL_MODE_VERTICAL);
            }
            break;
        case TouchpadView.GESTURE_2FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return activateScrollMode(TouchpadView.SCROLL_MODE_VERTICAL);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                return activateScrollMode(TouchpadView.SCROLL_MODE_HORIZONTAL);
            }
            break;
        case TouchpadView.GESTURE_3FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_LEFT:
                // Command key + Left = Back
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI,
                        HidKeyboard.KEYCODE_LEFT_ARROW);
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                // Command key + Right = Forward
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI,
                        HidKeyboard.KEYCODE_RIGHT_ARROW);
            }
            break;
        case TouchpadView.GESTURE_4FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
                // F9 = View Mission Control
                return pressShortcutKey(0, HidKeyboard.KEYCODE_F9);
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                // F10 = App Expose
                return pressShortcutKey(0, HidKeyboard.KEYCODE_F10);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
                // Ctrl + Right = Move to workspace right
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_CTRL,
                        HidKeyboard.KEYCODE_RIGHT_ARROW);
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                // Ctrl + Left = Move to workspace left
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_CTRL,
                        HidKeyboard.KEYCODE_LEFT_ARROW);
            }
            break;
        }

        return false;
    }

    private boolean handlePlaystation3Gesture(int gesture, int direction) {
        switch (gesture) {
        case TouchpadView.GESTURE_EDGE_RIGHT:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return activateScrollMode(TouchpadView.SCROLL_MODE_VERTICAL);
            }
            break;
        case TouchpadView.GESTURE_2FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return activateScrollMode(TouchpadView.SCROLL_MODE_VERTICAL);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                return activateScrollMode(TouchpadView.SCROLL_MODE_HORIZONTAL);
            }
            break;
        case TouchpadView.GESTURE_3FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_LEFT:
                // Alt + Left = Return to the previous page
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_ALT,
                        HidKeyboard.KEYCODE_LEFT_ARROW);
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                // Alt + Right = Go to the next page
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_ALT,
                        HidKeyboard.KEYCODE_RIGHT_ARROW);
            }
            break;
        }

        return false;
    }

    private boolean handleUbuntuUnityGesture(int gesture, int direction) {
        switch (gesture) {
        case TouchpadView.GESTURE_EDGE_RIGHT:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return activateScrollMode(TouchpadView.SCROLL_MODE_VERTICAL);
            }
            break;
        case TouchpadView.GESTURE_2FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return activateScrollMode(TouchpadView.SCROLL_MODE_VERTICAL);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                return activateScrollMode(TouchpadView.SCROLL_MODE_HORIZONTAL);
            }
            break;
        case TouchpadView.GESTURE_3FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
                // Super key + s = Show Workspace Switcher
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI, 's');
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                // Super key = Open Launcher
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI, 0);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
                return clickMouseButton(HidMouse.BUTTON_4);
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                return clickMouseButton(HidMouse.BUTTON_5);
            }
            break;
        case TouchpadView.GESTURE_4FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
                // Ctrl + Super + Up = Maximize current window
                return pressShortcutKey(
                        HidKeyboard.MODIFIER_LEFT_CTRL | HidKeyboard.MODIFIER_LEFT_GUI,
                        HidKeyboard.KEYCODE_UP_ARROW);
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                // Ctrl + Super + Down = Restore/minimize current window
                return pressShortcutKey(
                        HidKeyboard.MODIFIER_LEFT_CTRL | HidKeyboard.MODIFIER_LEFT_GUI,
                        HidKeyboard.KEYCODE_DOWN_ARROW);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
                // Ctrl + Super + Left = Maximize current window to the left
                return pressShortcutKey(
                        HidKeyboard.MODIFIER_LEFT_CTRL | HidKeyboard.MODIFIER_LEFT_GUI,
                        HidKeyboard.KEYCODE_LEFT_ARROW);
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                // Ctrl + Super + Right = Maximize current window to the right
                return pressShortcutKey(
                        HidKeyboard.MODIFIER_LEFT_CTRL | HidKeyboard.MODIFIER_LEFT_GUI,
                        HidKeyboard.KEYCODE_RIGHT_ARROW);
            }
            break;
        }

        return false;
    }

    private boolean handleWindows7Gesture(int gesture, int direction) {
        switch (gesture) {
        case TouchpadView.GESTURE_EDGE_RIGHT:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return activateScrollMode(TouchpadView.SCROLL_MODE_VERTICAL);
            }
            break;
        case TouchpadView.GESTURE_2FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return activateScrollMode(TouchpadView.SCROLL_MODE_VERTICAL);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                return activateScrollMode(TouchpadView.SCROLL_MODE_HORIZONTAL);
            }
            break;
        case TouchpadView.GESTURE_3FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
                // Windows key = Open start-menu
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI, 0);
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                // Windows key + d = Show desktop
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI, 'd');
            case TouchpadView.GESTURE_DIRECTION_LEFT:
                return clickMouseButton(HidMouse.BUTTON_4);
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                return clickMouseButton(HidMouse.BUTTON_5);
            }
            break;
        case TouchpadView.GESTURE_4FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
                // Windows key + Up = Maximize desktop window
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI,
                        HidKeyboard.KEYCODE_UP_ARROW);
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                // Windows key + Down = Restore/minimize desktop window
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI,
                        HidKeyboard.KEYCODE_DOWN_ARROW);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
                // Windows key + Left = Snap desktop window to the left
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI,
                        HidKeyboard.KEYCODE_LEFT_ARROW);
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                // Windows key + Right = Snap desktop window to the right
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI,
                        HidKeyboard.KEYCODE_RIGHT_ARROW);
            }
            break;
        }

        return false;
    }

    private boolean handleWindows8Gesture(int gesture, int direction) {
        switch (gesture) {
        case TouchpadView.GESTURE_EDGE_TOP:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return doWindows8TopEdgeGesture();
            }
            break;
        case TouchpadView.GESTURE_EDGE_BOTTOM:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
                // Windows key + z = Open app bar
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI, 'z');
            }
            break;
        case TouchpadView.GESTURE_EDGE_LEFT:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                return doWindows8LeftEdgeGesture();
            }
            break;
        case TouchpadView.GESTURE_EDGE_RIGHT:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return activateScrollMode(TouchpadView.SCROLL_MODE_VERTICAL);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
                // Windows key + c = Open charms
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI, 'c');
            }
            break;
        case TouchpadView.GESTURE_2FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                return activateScrollMode(TouchpadView.SCROLL_MODE_VERTICAL);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                return activateScrollMode(TouchpadView.SCROLL_MODE_HORIZONTAL);
            }
            break;
        case TouchpadView.GESTURE_3FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
                // Windows key = Show start screen
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI, 0);
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                // Windows key + d = Show desktop
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI, 'd');
            case TouchpadView.GESTURE_DIRECTION_LEFT:
                return clickMouseButton(HidMouse.BUTTON_4);
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                return clickMouseButton(HidMouse.BUTTON_5);
            }
            break;
        case TouchpadView.GESTURE_4FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
                // Windows key + Up = Maximize desktop window
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI,
                        HidKeyboard.KEYCODE_UP_ARROW);
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                // Windows key + Down = Restore/minimize desktop window
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI,
                        HidKeyboard.KEYCODE_DOWN_ARROW);
            case TouchpadView.GESTURE_DIRECTION_LEFT:
                // Windows key + Left = Snap desktop window to the left
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI,
                        HidKeyboard.KEYCODE_LEFT_ARROW);
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                // Windows key + Right = Snap desktop window to the right
                return pressShortcutKey(HidKeyboard.MODIFIER_LEFT_GUI,
                        HidKeyboard.KEYCODE_RIGHT_ARROW);
            }
            break;
        }

        return false;
    }

    public boolean onTouchpadGesture(int gesture, int direction) {
        if (DeviceSettings.TOUCHPAD_GESTURE_MODE_ANDROID.equals(mGestureMode)) {
            return handleAndroidGesture(gesture, direction);
        } else if (DeviceSettings.TOUCHPAD_GESTURE_MODE_GNOME_SHELL.equals(mGestureMode)) {
            return handleGnomeShellGesture(gesture, direction);
        } else if (DeviceSettings.TOUCHPAD_GESTURE_MODE_OSX.equals(mGestureMode)) {
            return handleOsXGesture(gesture, direction);
        } else if (DeviceSettings.TOUCHPAD_GESTURE_MODE_PLAYSTATION3.equals(mGestureMode)) {
            return handlePlaystation3Gesture(gesture, direction);
        } else if (DeviceSettings.TOUCHPAD_GESTURE_MODE_UBUNTU_UNITY.equals(mGestureMode)) {
            return handleUbuntuUnityGesture(gesture, direction);
        } else if (DeviceSettings.TOUCHPAD_GESTURE_MODE_WINDOWS7.equals(mGestureMode)) {
            return handleWindows7Gesture(gesture, direction);
        } else if (DeviceSettings.TOUCHPAD_GESTURE_MODE_WINDOWS8.equals(mGestureMode)) {
            return handleWindows8Gesture(gesture, direction);
        } else {
            return handleDefaultGesture(gesture, direction);
        }
    }
}
