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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Simple ArrayList for Integer values.
 */
public class IntArrayList extends ArrayList<Integer> {

    private static final long serialVersionUID = 7717710388705440454L;


    public IntArrayList() {
        super();
    }

    public IntArrayList(Collection<? extends Integer> collection) {
        super(collection);
    }

    public IntArrayList(int capacity) {
        super(capacity);
    }


    public int getValue(int index) {
        return (int)get(index);
    }

    public int indexOfValue(int value) {
        return indexOf((Integer)value);
    }

    public boolean containsValue(int value) {
        return contains((Integer)value);
    }

    public boolean addValue(int value) {
        return add((Integer)value);
    }

    public boolean removeValue(int value) {
        return remove((Integer)value);
    }
}
