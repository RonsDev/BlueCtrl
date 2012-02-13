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

import org.ronsdev.bluectrl.daemon.DaemonService;
import org.ronsdev.bluectrl.daemon.DaemonService.DaemonBinder;
import org.ronsdev.bluectrl.widget.FloatSliderPreference;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

/**
 * Preference Activity for Bluetooth device specific settings.
 */
public class DevicePreferenceActivity extends PreferenceActivity {

    /**
     * Used as a Parcelable BluetoothDevice extra field in start Activity intents to get the
     * current Bluetooth device.
     */
    public static final String EXTRA_DEVICE =
            "org.ronsdev.bluectrl.devicepreference.extra.DEVICE";


    private static final String KEY_KEYMAP = "keymap";
    private static final String KEY_MOUSE_SENSITIVITY = "mouse_sensitivity";
    private static final String KEY_INVERT_SCROLL = "invert_scroll";


    private DeviceSettings mDeviceSettings;

    private ListPreference mKeyMap;
    private FloatSliderPreference mMouseSensitivity;
    private CheckBoxPreference mInvertScroll;


    /*
     * The DaemonService isn't really used by this Activity but we will bind to it so that the
     * daemon won't shutdown.
     */
    DaemonService mDaemon;
    private ServiceConnection mDaemonConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            DaemonBinder binder = (DaemonBinder)service;
            mDaemon = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mDaemon = null;
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        BluetoothDevice device = extras.getParcelable(EXTRA_DEVICE);

        mDeviceSettings = DeviceSettings.get(this, device);

        addPreferencesFromResource(R.xml.preferences_device);

        mKeyMap = (ListPreference)findPreference(KEY_KEYMAP);
        mMouseSensitivity = (FloatSliderPreference)findPreference(KEY_MOUSE_SENSITIVITY);
        mInvertScroll = (CheckBoxPreference)findPreference(KEY_INVERT_SCROLL);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent daemonIntent = new Intent(this, DaemonService.class);
        bindService(daemonIntent, mDaemonConnection, Context.BIND_AUTO_CREATE);

        loadSettings();
    }

    @Override
    protected void onPause() {
        super.onPause();

        updateSettings();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mDaemon != null) {
            unbindService(mDaemonConnection);
            mDaemon = null;
        }

        mDeviceSettings.saveToPreferences();
    }

    private void loadSettings() {
        mKeyMap.setValue(mDeviceSettings.getKeyMap());
        mMouseSensitivity.setValue(mDeviceSettings.getMouseSensitivity());
        mInvertScroll.setChecked(mDeviceSettings.getInvertScroll());
    }

    private void updateSettings() {
        mDeviceSettings.setKeyMap(mKeyMap.getValue());
        mDeviceSettings.setMouseSensitivity(mMouseSensitivity.getValue());
        mDeviceSettings.setInvertScroll(mInvertScroll.isChecked());
    }
}
