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

/**
 * Interface definition for a callback to be invoked when the scroll mode has changed.
 */
public interface OnScrollModeChangedListener {

    /**
     * Called when the scroll mode has changed.
     * @param newMode
     * The new mode ('SCROLL_MODE_*' constants in the {@link TouchpadView} class)
     * @param oldMode
     * The old mode ('SCROLL_MODE_*' constants in the {@link TouchpadView} class)
     */
    public abstract void onScrollModeChanged(int newMode, int oldMode);
}
