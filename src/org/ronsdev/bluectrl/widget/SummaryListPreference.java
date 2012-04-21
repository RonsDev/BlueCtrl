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
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * A {@link Preference} that displays a list of entries as a dialog.
 *
 * This extended version can show the current value in the summary (backported feature of
 * Android 4).
 */
public class SummaryListPreference extends ListPreference {

    public SummaryListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        final CharSequence summary = super.getSummary();
        final CharSequence entry = getEntry();
        if (summary == null || entry == null) {
             return null;
        } else {
            return String.format(summary.toString(), entry);
        }
    }

    @Override
    public void setValue(String value)
    {
        super.setValue(value);
        notifyChanged();
    }
}