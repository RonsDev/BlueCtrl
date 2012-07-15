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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Manages a list of configured Bluetooth devices.
 */
public class DeviceManager {

    private static final String PREF_KEY_DEVICES = "devices";


    private Context mContext;
    private BluetoothAdapter mBtAdapter;
    private SharedPreferences mPreferences;
    private HashSet<String> mDeviceSet;


    public DeviceManager(Context context) {
        mContext = context;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        mDeviceSet = new HashSet<String>();
        loadDeviceSet();
    }


    private void loadDeviceSet() {
        mDeviceSet.clear();

        final String devicesPrefValue = mPreferences.getString(PREF_KEY_DEVICES, "");
        for (String entry : devicesPrefValue.split("\\|")) {
            mDeviceSet.add(entry);
        }
    }

    private String getDevicesPreferenceValue() {
        StringBuffer result = new StringBuffer();

        for (String entry : mDeviceSet) {
            if (result.length() > 0) {
                result.append('|');
            }

            result.append(entry);
        }

        return result.toString();
    }

    private void saveDeviceSet() {
        SharedPreferences.Editor editor = mPreferences.edit();

        editor.putString(PREF_KEY_DEVICES, getDevicesPreferenceValue());

        editor.commit();
    }

    public void registerDevice(BluetoothDevice device, String deviceOs) {
        if (device != null) {
            mDeviceSet.add(device.getAddress());
            saveDeviceSet();

            DeviceSettings deviceSettings = DeviceSettings.get(mContext, device);
            deviceSettings.initPreferences(deviceOs);
        }
    }

    public void unregisterDevice(BluetoothDevice device) {
        if (device != null) {
            DeviceSettings deviceSettings = DeviceSettings.get(mContext, device);
            deviceSettings.resetPreferences();

            mDeviceSet.remove(device.getAddress());
            saveDeviceSet();
        }
    }

    public static String getDeviceName(Context context, BluetoothDevice device) {
        String result = device.getName();
        if (result.isEmpty()) {
            DeviceSettings deviceSettings = DeviceSettings.get(context, device);
            if (deviceSettings.getOperatingSystem().equals(DeviceSettings.OS_PLAYSTATION3)) {
                final Resources res = context.getResources();
                result = res.getString(R.string.unnamed_device_playstation3, device.getAddress());
            } else {
                result = device.getAddress();
            }
        }
        return result;
    }

    public List<PairedDevice> getPairedDevices() {
        List<PairedDevice> pairedDeviceList = new ArrayList<PairedDevice>();

        if (mBtAdapter != null) {
            for (BluetoothDevice device : mBtAdapter.getBondedDevices()) {
                if (mDeviceSet.contains(device.getAddress())) {
                    pairedDeviceList.add(new PairedDevice(device,
                            getDeviceName(mContext, device)));
                }
            }
        }

        Collections.sort(pairedDeviceList, PairedDevice.DefaultComparator);

        return pairedDeviceList;
    }
}
