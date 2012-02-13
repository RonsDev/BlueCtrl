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

import android.content.Context;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

/**
 * View that handles keyboard input.
 */
public class KeyboardInputView extends View {

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
        public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
            return new ExtractedText();
        }

        @Override
        public boolean setComposingText(CharSequence text, int newCursorPosition) {
            /*
             * Directly commit composing text (such as voice input) because the user doesn't see
             * the text and cannot correct it anyway.
             */
            return commitText(text, newCursorPosition);
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

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI;

        return new KeyboardInputConnection(this);
    }

    public void pasteText(String text) {
        dispatchKeyEvent(new KeyEvent(SystemClock.uptimeMillis(),
                    text, KeyCharacterMap.BUILT_IN_KEYBOARD, 0));
    }
}
