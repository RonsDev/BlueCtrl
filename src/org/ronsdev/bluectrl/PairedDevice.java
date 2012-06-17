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

import android.bluetooth.BluetoothDevice;

import java.util.Comparator;

/**
 * Container class for a paired Bluetooth device.
 */
public class PairedDevice {

    /** Default Comparator for the PairedDevice class. */
    public static Comparator<PairedDevice> DefaultComparator = new Comparator<PairedDevice>() {
        @Override
        public int compare(PairedDevice d1, PairedDevice d2) {
            return d1.toString().compareToIgnoreCase(d2.toString());
        }
    };


    private BluetoothDevice mDevice;
    private String mName;


    public PairedDevice(BluetoothDevice device, String name) {
        mDevice = device;
        mName = name;
    }


    @Override
    public String toString() {
        return mName;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }
}
