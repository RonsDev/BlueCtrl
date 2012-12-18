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

import org.ronsdev.bluectrl.HidKeyboard;
import org.ronsdev.bluectrl.KeyEventFuture;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

/**
 * View that handles key events and redirects them to a HID Keyboard.
 */
public class KeyboardInputView extends View {

    private static final String TAG = "KeyboardInputView";
    private static final boolean V = false;


    private static final String SAVED_STATE_INSTANCE = "InstanceState";
    private static final String SAVED_STATE_SHOULD_SHOW_KEYBOARD = "ShouldShowKeyboard";
    private static final String SAVED_STATE_WAS_KEYBOARD_TOGGLED = "WasKeyboardToggled";


    private HidKeyboard mHidKeyboard = null;

    private boolean mShouldShowKeyboard = false;
    private boolean mWasKeyboardToggled = false;


    private OnKeyboardComposingTextListener mOnKeyboardComposingTextListener;


    /**
     * A custom InputConnection that immediately redirects text input as key events.
     */
    private final class KeyboardInputConnection extends BaseInputConnection {

        public KeyboardInputConnection(View targetView) {
            super(targetView, true);
        }

        @Override
        public Editable getEditable() {
            return null;
        }

        @Override
        public boolean setComposingText(CharSequence text, int newCursorPosition) {
            OnKeyboardComposingText(text);
            return true;
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(),
                    text.toString(), KeyCharacterMap.BUILT_IN_KEYBOARD, 0));
            return true;
        }
    }


    public KeyboardInputView(Context context) {
        super(context);

        initView();
    }

    public KeyboardInputView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initView();
    }

    public KeyboardInputView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initView();
    }


    private final void initView() {
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public boolean isActive() {
        return ((mHidKeyboard != null) && mHidKeyboard.isConnected());
    }

    public HidKeyboard getHidKeyboard() {
        return mHidKeyboard;
    }
    public void setHidKeyboard(HidKeyboard hidKeyboard) {
        mHidKeyboard = hidKeyboard;
    }

    private void OnKeyboardComposingText(CharSequence composingText) {
        if (mOnKeyboardComposingTextListener != null) {
            mOnKeyboardComposingTextListener.OnKeyboardComposingText(composingText);
        }
    }

    public void setOnKeyboardComposingTextListener(OnKeyboardComposingTextListener listener) {
        mOnKeyboardComposingTextListener = listener;
    }

    private InputMethodManager getInputManager() {
        return (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    public void showKeyboard() {
        boolean success = false;
        if (isShown()) {
            requestFocus();
            success = getInputManager().showSoftInput(this,
                    InputMethodManager.SHOW_FORCED,
                    new ResultReceiver(null) {
                        @Override
                        protected void onReceiveResult(int resultCode, Bundle resultData) {
                            if (resultCode == InputMethodManager.RESULT_SHOWN) {
                                mWasKeyboardToggled = true;
                            }
                        }
                    });
        }
        mShouldShowKeyboard = !success;
    }

    public void hideToggledKeyboard() {
        mShouldShowKeyboard = false;
        if (mWasKeyboardToggled) {
            mWasKeyboardToggled = false;
            getInputManager().hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    public void toggleKeyboard() {
        mWasKeyboardToggled = true;
        requestFocus();

        /*
         * We use the SHOW_FORCED flag instead of the preferred SHOW_IMPLICIT flag because the
         * latter didn't work in the landscape mode.
         */
        getInputManager().toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle outState = new Bundle();

        outState.putParcelable(SAVED_STATE_INSTANCE, super.onSaveInstanceState());
        outState.putBoolean(SAVED_STATE_SHOULD_SHOW_KEYBOARD, mShouldShowKeyboard);
        outState.putBoolean(SAVED_STATE_WAS_KEYBOARD_TOGGLED, mWasKeyboardToggled);

        return outState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle savedInstanceState = (Bundle)state;

            mShouldShowKeyboard = savedInstanceState.getBoolean(SAVED_STATE_SHOULD_SHOW_KEYBOARD);
            mWasKeyboardToggled = savedInstanceState.getBoolean(SAVED_STATE_WAS_KEYBOARD_TOGGLED);
            super.onRestoreInstanceState(savedInstanceState.getParcelable(SAVED_STATE_INSTANCE));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS &
                InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI;

        return new KeyboardInputConnection(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        if (hasWindowFocus) {
            onKeyboardActiveStateChanged(true);
        }
    }

    @Override
    public void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        onKeyboardActiveStateChanged(isShown());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isActive()) {
            return onSingleKeyEvent(keyCode, event);
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isActive()) {
            return onSingleKeyEvent(keyCode, event);
        } else {
            return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        if (isActive()) {
            return onMultipleKeyEvents(event);
        } else {
            return super.onKeyMultiple(keyCode, repeatCount, event);
        }
    }

    public void onActivityPause() {
        onKeyboardActiveStateChanged(false);
    }

    private void onKeyboardActiveStateChanged(boolean isActive) {
        if (isActive) {
            if (mShouldShowKeyboard) {
                showKeyboard();
            }
        } else {
            if (mWasKeyboardToggled) {
                mWasKeyboardToggled = false;
                getInputManager().hideSoftInputFromWindow(getWindowToken(),
                        0,
                        new ResultReceiver(null) {
                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {
                                if (resultCode == InputMethodManager.RESULT_HIDDEN) {
                                    mShouldShowKeyboard = true;
                                }
                            }
                        });
            }
        }
    }

    private boolean onSingleKeyEvent(int keyCode, KeyEvent event) {
        final int character = event.getUnicodeChar(0);

        if (V) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                Log.v(TAG, String.format("Key Down (Key=%d   Char=%c)", keyCode, character));
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                Log.v(TAG, String.format("Key Up (Key=%d   Char=%c)", keyCode, character));
            }
        }

        if (handleHardwareKey(keyCode, event) ||
                handleMediaKey(keyCode, event) ||
                handleNonCharKey(keyCode, event) ||
                handleCharKey(character, event)) {
            return true;
        }

        return false;
    }

    private boolean handleHardwareKey(int keyCode, KeyEvent event) {
        final int hardwareKey = convertToHidHardwareKey(keyCode);

        if (hardwareKey != 0) {
            switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
                mHidKeyboard.pressHardwareKey(hardwareKey);
                return true;
            case KeyEvent.ACTION_UP:
                mHidKeyboard.releaseHardwareKey(hardwareKey);
                return true;
            }
        }

        return false;
    }

    private boolean handleMediaKey(int keyCode, KeyEvent event) {
        final int mediaKey = convertToHidMediaKey(keyCode);

        if (mediaKey != 0) {
            switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
                mHidKeyboard.pressMediaKey(mediaKey);
                return true;
            case KeyEvent.ACTION_UP:
                mHidKeyboard.releaseMediaKey(mediaKey);
                return true;
            }
        }

        return false;
    }

    private boolean handleNonCharKey(int keyCode, KeyEvent event) {
        final int hidModifier = convertToHidModifier(keyCode);
        final int hidKeyCode = convertToHidKeyCode(keyCode);

        if ((hidModifier != 0) || (hidKeyCode != 0)) {
            switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
                if (hidModifier != 0) {
                    mHidKeyboard.pressModifierKey(hidModifier);
                }

                if (hidKeyCode != 0) {
                    mHidKeyboard.pressKey(hidKeyCode);
                }

                return true;
            case KeyEvent.ACTION_UP:
                if (hidKeyCode != 0) {
                    mHidKeyboard.releaseKey(hidKeyCode);
                }

                if (hidModifier != 0) {
                    mHidKeyboard.releaseModifierKey(hidModifier);
                }

                return true;
            }
        }

        return false;
    }

    private boolean handleCharKey(int character, KeyEvent event) {
        if (character != 0) {
            switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
                return mHidKeyboard.pressCharKey((char)character);
            case KeyEvent.ACTION_UP:
                return mHidKeyboard.releaseCharKey((char)character);
            }
        }

        return false;
    }

    private boolean onMultipleKeyEvents(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN) {
            if (V) Log.v(TAG, String.format("multiple key events (Text='%s')", event.getCharacters()));

            mHidKeyboard.typeText(event.getCharacters());
            return true;
        } else {
            if (V) Log.v(TAG, String.format("multiple key events (Repeat='%d')", event.getRepeatCount()));

            // Repeated keys are ignored because only key changes have to be reported
            return true;
        }
    }

    /**
     * Converts a Android key code to a HID Keyboard code.
     * Only language independent keys that doesn't create characters are handled.
     */
    private static int convertToHidKeyCode(int keyCode) {
        switch (keyCode) {
        case KeyEventFuture.KEYCODE_BREAK:
            return HidKeyboard.KEYCODE_PAUSE;
        case KeyEventFuture.KEYCODE_CAPS_LOCK:
            return HidKeyboard.KEYCODE_CAPS_LOCK;
        case KeyEvent.KEYCODE_CLEAR:
            return HidKeyboard.KEYCODE_CLEAR;
        case KeyEvent.KEYCODE_DEL:
            return HidKeyboard.KEYCODE_DEL;
        case KeyEvent.KEYCODE_DPAD_CENTER:
            return HidKeyboard.KEYCODE_ENTER;
        case KeyEvent.KEYCODE_DPAD_DOWN:
            return HidKeyboard.KEYCODE_DOWN_ARROW;
        case KeyEvent.KEYCODE_DPAD_LEFT:
            return HidKeyboard.KEYCODE_LEFT_ARROW;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            return HidKeyboard.KEYCODE_RIGHT_ARROW;
        case KeyEvent.KEYCODE_DPAD_UP:
            return HidKeyboard.KEYCODE_UP_ARROW;
        case KeyEvent.KEYCODE_ENTER:
            return HidKeyboard.KEYCODE_ENTER;
        case KeyEventFuture.KEYCODE_ESCAPE:
            return HidKeyboard.KEYCODE_ESCAPE;
        case KeyEventFuture.KEYCODE_F1:
            return HidKeyboard.KEYCODE_F1;
        case KeyEventFuture.KEYCODE_F10:
            return HidKeyboard.KEYCODE_F10;
        case KeyEventFuture.KEYCODE_F11:
            return HidKeyboard.KEYCODE_F11;
        case KeyEventFuture.KEYCODE_F12:
            return HidKeyboard.KEYCODE_F12;
        case KeyEventFuture.KEYCODE_F2:
            return HidKeyboard.KEYCODE_F2;
        case KeyEventFuture.KEYCODE_F3:
            return HidKeyboard.KEYCODE_F3;
        case KeyEventFuture.KEYCODE_F4:
            return HidKeyboard.KEYCODE_F4;
        case KeyEventFuture.KEYCODE_F5:
            return HidKeyboard.KEYCODE_F5;
        case KeyEventFuture.KEYCODE_F6:
            return HidKeyboard.KEYCODE_F6;
        case KeyEventFuture.KEYCODE_F7:
            return HidKeyboard.KEYCODE_F7;
        case KeyEventFuture.KEYCODE_F8:
            return HidKeyboard.KEYCODE_F8;
        case KeyEventFuture.KEYCODE_F9:
            return HidKeyboard.KEYCODE_F9;
        case KeyEventFuture.KEYCODE_FORWARD_DEL:
            return HidKeyboard.KEYCODE_FORWARD_DEL;
        case KeyEventFuture.KEYCODE_INSERT:
            return HidKeyboard.KEYCODE_INSERT;
        case KeyEventFuture.KEYCODE_MOVE_END:
            return HidKeyboard.KEYCODE_END;
        case KeyEventFuture.KEYCODE_MOVE_HOME:
            return HidKeyboard.KEYCODE_HOME;
        case KeyEventFuture.KEYCODE_NUMPAD_0:
            return HidKeyboard.KEYCODE_NUMPAD_0;
        case KeyEventFuture.KEYCODE_NUMPAD_1:
            return HidKeyboard.KEYCODE_NUMPAD_1;
        case KeyEventFuture.KEYCODE_NUMPAD_2:
            return HidKeyboard.KEYCODE_NUMPAD_2;
        case KeyEventFuture.KEYCODE_NUMPAD_3:
            return HidKeyboard.KEYCODE_NUMPAD_3;
        case KeyEventFuture.KEYCODE_NUMPAD_4:
            return HidKeyboard.KEYCODE_NUMPAD_4;
        case KeyEventFuture.KEYCODE_NUMPAD_5:
            return HidKeyboard.KEYCODE_NUMPAD_5;
        case KeyEventFuture.KEYCODE_NUMPAD_6:
            return HidKeyboard.KEYCODE_NUMPAD_6;
        case KeyEventFuture.KEYCODE_NUMPAD_7:
            return HidKeyboard.KEYCODE_NUMPAD_7;
        case KeyEventFuture.KEYCODE_NUMPAD_8:
            return HidKeyboard.KEYCODE_NUMPAD_8;
        case KeyEventFuture.KEYCODE_NUMPAD_9:
            return HidKeyboard.KEYCODE_NUMPAD_9;
        case KeyEventFuture.KEYCODE_NUMPAD_ADD:
            return HidKeyboard.KEYCODE_NUMPAD_ADD;
        case KeyEventFuture.KEYCODE_NUMPAD_COMMA:
            return HidKeyboard.KEYCODE_NUMPAD_COMMA;
        case KeyEventFuture.KEYCODE_NUMPAD_DIVIDE:
            return HidKeyboard.KEYCODE_NUMPAD_DIVIDE;
        case KeyEventFuture.KEYCODE_NUMPAD_DOT:
            return HidKeyboard.KEYCODE_NUMPAD_DOT;
        case KeyEventFuture.KEYCODE_NUMPAD_ENTER:
            return HidKeyboard.KEYCODE_NUMPAD_ENTER;
        case KeyEventFuture.KEYCODE_NUMPAD_EQUALS:
            return HidKeyboard.KEYCODE_NUMPAD_EQUALS;
        case KeyEventFuture.KEYCODE_NUMPAD_LEFT_PAREN:
            return HidKeyboard.KEYCODE_NUMPAD_LEFT_PAREN;
        case KeyEventFuture.KEYCODE_NUMPAD_MULTIPLY:
            return HidKeyboard.KEYCODE_NUMPAD_MULTIPLY;
        case KeyEventFuture.KEYCODE_NUMPAD_RIGHT_PAREN:
            return HidKeyboard.KEYCODE_NUMPAD_RIGHT_PAREN;
        case KeyEventFuture.KEYCODE_NUMPAD_SUBTRACT:
            return HidKeyboard.KEYCODE_NUMPAD_SUBTRACT;
        case KeyEventFuture.KEYCODE_NUM_LOCK:
            return HidKeyboard.KEYCODE_NUM_LOCK;
        case KeyEvent.KEYCODE_PAGE_DOWN:
            return HidKeyboard.KEYCODE_PAGE_DOWN;
        case KeyEvent.KEYCODE_PAGE_UP:
            return HidKeyboard.KEYCODE_PAGE_UP;
        case KeyEventFuture.KEYCODE_SCROLL_LOCK:
            return HidKeyboard.KEYCODE_SCROLL_LOCK;
        case KeyEvent.KEYCODE_SPACE:
            return HidKeyboard.KEYCODE_SPACE;
        case KeyEventFuture.KEYCODE_SYSRQ:
            return HidKeyboard.KEYCODE_SYSRQ;
        case KeyEvent.KEYCODE_TAB:
            return HidKeyboard.KEYCODE_TAB;
        default:
            return 0;
        }
    }

    /** Converts a Android key code to a HID Keyboard modifier. */
    private static int convertToHidModifier(int keyCode) {
        switch (keyCode) {
        case KeyEventFuture.KEYCODE_CTRL_LEFT:
            return HidKeyboard.MODIFIER_LEFT_CTRL;
        case KeyEventFuture.KEYCODE_CTRL_RIGHT:
            return HidKeyboard.MODIFIER_RIGHT_CTRL;
        case KeyEvent.KEYCODE_SHIFT_LEFT:
            return HidKeyboard.MODIFIER_LEFT_SHIFT;
        case KeyEvent.KEYCODE_SHIFT_RIGHT:
            return HidKeyboard.MODIFIER_RIGHT_SHIFT;
        case KeyEvent.KEYCODE_ALT_LEFT:
            return HidKeyboard.MODIFIER_LEFT_ALT;
        case KeyEvent.KEYCODE_ALT_RIGHT:
            return HidKeyboard.MODIFIER_RIGHT_ALT;
        case KeyEventFuture.KEYCODE_META_LEFT:
            return HidKeyboard.MODIFIER_LEFT_GUI;
        case KeyEventFuture.KEYCODE_META_RIGHT:
            return HidKeyboard.MODIFIER_RIGHT_GUI;
        default:
            return 0;
        }
    }

    /** Converts a Android key code to a HID Hardware key code. */
    private static int convertToHidHardwareKey(int keyCode) {
        switch (keyCode) {
        case KeyEventFuture.KEYCODE_MEDIA_EJECT:
            return HidKeyboard.HARDWARE_KEY_EJECT;
        default:
            return 0;
        }
    }

    /** Converts a Android key code to a HID Media key code. */
    private static int convertToHidMediaKey(int keyCode) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            return HidKeyboard.MEDIA_KEY_PLAY_PAUSE;
        case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            return HidKeyboard.MEDIA_KEY_FORWARD;
        case KeyEvent.KEYCODE_MEDIA_REWIND:
            return HidKeyboard.MEDIA_KEY_REWIND;
        case KeyEvent.KEYCODE_MEDIA_NEXT:
            return HidKeyboard.MEDIA_KEY_SCAN_NEXT_TRACK;
        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            return HidKeyboard.MEDIA_KEY_SCAN_PREV_TRACK;
        case KeyEvent.KEYCODE_MUTE:
            return HidKeyboard.MEDIA_KEY_MUTE;
        case KeyEvent.KEYCODE_VOLUME_UP:
            return HidKeyboard.MEDIA_KEY_VOLUME_INC;
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            return HidKeyboard.MEDIA_KEY_VOLUME_DEC;
        default:
            return 0;
        }
    }
}
