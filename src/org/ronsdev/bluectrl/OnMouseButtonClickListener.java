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

/**
 * Interface definition for a callback to be invoked when a Mouse button is clicked.
 */
public interface OnMouseButtonClickListener {

    /**
     * Called when a Mouse button is clicked.
     * @param clickType
     * Specifies how the button is clicked ('CLICK_TYPE_*' constants in the HidMouse class)
     * @param button
     * The clicked button
     */
    public abstract void onMouseButtonClick(int clickType, int button);
}
