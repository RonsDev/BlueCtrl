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

import org.ronsdev.bluectrl.R;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 * A View with a text input and a send button.
 */
public class ComposeTextLayout extends LinearLayout {

    private static final String SAVED_STATE_INSTANCE = "InstanceState";
    private static final String SAVED_STATE_IS_VISIBLE = "IsVisible";
    private static final String SAVED_STATE_WAS_KEYBOARD_TOGGLED = "WasKeyboardToggled";


    private EditText mEditText;
    private ImageButton mSendButton;

    private boolean mIsVisible;
    private boolean mWasKeyboardToggled;

    private OnSendComposeTextListener mOnSendComposeTextListener;


    private OnEditorActionListener mEditorActionListener = new OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                mSendButton.performClick();
                return true;
            }

            return false;
        }
    };

    private OnClickListener mSendButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mEditText != null) {
                OnSendComposeText(mEditText.getText());
                mEditText.setText("");
                getInputManager().restartInput(mEditText);
            }
        }
    };


    public ComposeTextLayout(Context context) {
        super(context);

        initView();
    }

    public ComposeTextLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        initView();
    }


    private final void initView() {
        mIsVisible = false;
        setVisibility(GONE);

        mWasKeyboardToggled = false;
    }

    private void OnSendComposeText(CharSequence text) {
        if (mOnSendComposeTextListener != null) {
            mOnSendComposeTextListener.OnSendComposeText(text);
        }
    }

    public void setOnSendComposeTextListener(OnSendComposeTextListener listener) {
        mOnSendComposeTextListener = listener;
    }

    private InputMethodManager getInputManager() {
        return (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    private void showKeyboard() {
        if (isShown() && (mEditText != null)) {
            mEditText.requestFocus();
            getInputManager().showSoftInput(mEditText,
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
    }

    private void hideToggledKeyboard() {
        if (mWasKeyboardToggled && (mEditText != null)) {
            mWasKeyboardToggled = false;
            getInputManager().hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
        }
    }

    public void show() {
        show(null);
    }

    public void show(CharSequence composingText) {
        mIsVisible = true;
        setVisibility(VISIBLE);

        if ((composingText != null) && (mEditText != null)) {
            mEditText.setText(composingText);
            mEditText.setSelection(composingText.length());
            getInputManager().restartInput(mEditText);
        }
    }

    public void hide() {
        hideToggledKeyboard();

        mIsVisible = false;
        setVisibility(GONE);
    }

    public void toggleVisibility() {
        if (isShown()) {
            hide();
        } else {
            show();
        }
    }


    @Override
    public void saveHierarchyState(SparseArray<Parcelable> container) {
        super.saveHierarchyState(container);

        if (mEditText != null) {
            mEditText.saveHierarchyState(container);
        }
    }

    @Override
    public void restoreHierarchyState(SparseArray<Parcelable> container) {
        super.restoreHierarchyState(container);

        if (mEditText != null) {
            mEditText.restoreHierarchyState(container);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle outState = new Bundle();

        outState.putParcelable(SAVED_STATE_INSTANCE, super.onSaveInstanceState());
        outState.putBoolean(SAVED_STATE_IS_VISIBLE, mIsVisible);
        outState.putBoolean(SAVED_STATE_WAS_KEYBOARD_TOGGLED, mWasKeyboardToggled);

        return outState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle savedInstanceState = (Bundle)state;

            mIsVisible = savedInstanceState.getBoolean(SAVED_STATE_IS_VISIBLE);
            setVisibility(mIsVisible ? VISIBLE : GONE);

            mWasKeyboardToggled = savedInstanceState.getBoolean(SAVED_STATE_WAS_KEYBOARD_TOGGLED);

            super.onRestoreInstanceState(savedInstanceState.getParcelable(SAVED_STATE_INSTANCE));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        mEditText = (EditText)findViewById(R.id.edit_compose_text);
        mEditText.setImeOptions(EditorInfo.IME_ACTION_SEND);
        mEditText.setOnEditorActionListener(mEditorActionListener);

        mSendButton = (ImageButton)findViewById(R.id.btn_compose_text);
        mSendButton.setOnClickListener(mSendButtonClickListener);
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

        if (isShown()) {
            /*
             * HACK: Without the short wait time the keyboard didn't open reliable.
             */
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    onKeyboardActiveStateChanged(true);
                }
            }, 100);
        } else {
            onKeyboardActiveStateChanged(false);
        }
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        if ((event.getKeyCode() == KeyEvent.KEYCODE_BACK) &&
                (event.getAction() == KeyEvent.ACTION_UP)) {
            hide();
        }

        return super.dispatchKeyEventPreIme(event);
    }

    public void onActivityPause() {
        onKeyboardActiveStateChanged(false);
    }

    private void onKeyboardActiveStateChanged(boolean isActive) {
        if (isActive) {
            showKeyboard();
        } else {
            hideToggledKeyboard();
        }
    }
}
