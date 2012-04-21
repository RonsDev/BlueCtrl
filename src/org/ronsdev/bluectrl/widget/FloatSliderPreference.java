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
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;

/**
 * A Preference that displays a SeekBar as a dialog.
 *
 * This preference will store a float into the SharedPreferences.
 */
public class FloatSliderPreference extends DialogPreference
        implements SeekBar.OnSeekBarChangeListener {

    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    private static final int INT_FACTOR = 10;


    private Context mContext;
    private SeekBar mSeekBar;

    private float mDefault = 0.0f;
    private float mMax = 0.0f;
    private float mValue = 0.0f;


    public FloatSliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        mDefault = attrs.getAttributeIntValue(ANDROID_NS, "defaultValue", 0);
        mMax = attrs.getAttributeIntValue(ANDROID_NS, "max", 10);
        mValue = mDefault;
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        super.onSetInitialValue(restore, defaultValue);

        if (restore) {
            mValue = shouldPersist() ? getPersistedFloat(mDefault) : 0.0f;
        }
        else {
            mValue = (Float)defaultValue;
        }
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(8, 16, 8, 16);

        mSeekBar = new SeekBar(mContext);
        mSeekBar.setMax(convertToInt(mMax));
        mSeekBar.setOnSeekBarChangeListener(this);
        layout.addView(mSeekBar,
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        return layout;
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        updateDialogTitle(mValue);
        mSeekBar.setProgress(convertToInt(mValue));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        final float newValue = convertToFloat(mSeekBar.getProgress());
        if (positiveResult && callChangeListener(Float.valueOf(newValue))) {
            setValue(newValue);
            if (shouldPersist()) {
                persistFloat(mValue);
            }
        }
    }

    @Override
    public CharSequence getSummary() {
        final CharSequence summary = super.getSummary();
        if (summary == null) {
            return null;
        } else {
            return String.format(summary.toString(), mValue);
        }
    }

    private int convertToInt(float value) {
        return (int)(value * INT_FACTOR);
    }

    private float convertToFloat(int value) {
        return ((float)value) / INT_FACTOR;
    }

    private void updateDialogTitle(float value) {
        if (getDialog() != null) {
            getDialog().setTitle(String.format("%s: %.1f", getDialogTitle(), value));
        }
    }

    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
        updateDialogTitle(convertToFloat(value));
    }

    public void onStartTrackingTouch(SeekBar seek) {
    }

    public void onStopTrackingTouch(SeekBar seek) {
    }

    public float getDefaultValue() {
        return mDefault;
    }

    public void setDefaultValue(float defaultValue) {
        mDefault = defaultValue;
    }

    public float getMax() {
        return mMax;
    }

    public void setMax(float max) {
        mMax = max;
    }

    public float getValue() {
        return mValue;
    }

    public void setValue(float value) {
        mValue = value;
        notifyChanged();
        if (mSeekBar != null) {
            mSeekBar.setProgress(convertToInt(value));
        }
    }
}
