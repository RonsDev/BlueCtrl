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

import android.app.Activity;
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


    private DeviceSettings mDeviceSettings;

    private ListPreference mKeyMap;
    private ListPreference mTouchpadGestureMode;
    private ListPreference mTouchpadButtons;
    private FloatSliderPreference mMouseSensitivity;
    private FloatSliderPreference mScrollSensitivity;
    private CheckBoxPreference mInvertScroll;
    private CheckBoxPreference mFlingScroll;
    private CheckBoxPreference mStayAwake;


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


    public static void startActivity(Activity curActivity, BluetoothDevice device) {
        Intent intent = new Intent(curActivity, DevicePreferenceActivity.class);
        intent.putExtra(EXTRA_DEVICE, device);
        curActivity.startActivity(intent);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        BluetoothDevice device = extras.getParcelable(EXTRA_DEVICE);

        mDeviceSettings = DeviceSettings.get(this, device);

        addPreferencesFromResource(R.xml.preferences_device);

        mKeyMap = (ListPreference)findPreference(DeviceSettings.PREF_KEY_KEYMAP);
        mMouseSensitivity = (FloatSliderPreference)findPreference(
                DeviceSettings.PREF_KEY_MOUSE_SENSITIVITY);

        mTouchpadGestureMode = (ListPreference)findPreference(
                DeviceSettings.PREF_KEY_TOUCHPAD_GESTURE_MODE);
        if (mDeviceSettings.getOperatingSystem().equals(DeviceSettings.OS_LINUX)) {
            mTouchpadGestureMode.setEntries(R.array.pref_touchpad_gesture_mode_linux_names);
            mTouchpadGestureMode.setEntryValues(R.array.pref_touchpad_gesture_mode_linux_values);
        } else if (mDeviceSettings.getOperatingSystem().equals(DeviceSettings.OS_WINDOWS)) {
            mTouchpadGestureMode.setEntries(R.array.pref_touchpad_gesture_mode_windows_names);
            mTouchpadGestureMode.setEntryValues(R.array.pref_touchpad_gesture_mode_windows_values);
        } else if (mDeviceSettings.getOperatingSystem().equals(DeviceSettings.OS_UNDEFINED)) {
            mTouchpadGestureMode.setEntries(R.array.pref_touchpad_gesture_mode_all_names);
            mTouchpadGestureMode.setEntryValues(R.array.pref_touchpad_gesture_mode_all_values);
        } else {
            getPreferenceScreen().removePreference(mTouchpadGestureMode);
        }

        mTouchpadButtons = (ListPreference)findPreference(
                DeviceSettings.PREF_KEY_TOUCHPAD_BUTTONS);
        mScrollSensitivity = (FloatSliderPreference)findPreference(
                DeviceSettings.PREF_KEY_SCROLL_SENSITIVITY);
        mInvertScroll = (CheckBoxPreference)findPreference(DeviceSettings.PREF_KEY_INVERT_SCROLL);
        mFlingScroll = (CheckBoxPreference)findPreference(DeviceSettings.PREF_KEY_FLING_SCROLL);
        mStayAwake = (CheckBoxPreference)findPreference(DeviceSettings.PREF_KEY_STAY_AWAKE);
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
        mTouchpadGestureMode.setValue(mDeviceSettings.getTouchpadGestureMode());
        mTouchpadButtons.setValue(mDeviceSettings.getTouchpadButtons());
        mScrollSensitivity.setValue(mDeviceSettings.getScrollSensitivity());
        mInvertScroll.setChecked(mDeviceSettings.getInvertScroll());
        mFlingScroll.setChecked(mDeviceSettings.getFlingScroll());
        mStayAwake.setChecked(mDeviceSettings.getStayAwake());
    }

    private void updateSettings() {
        mDeviceSettings.setKeyMap(mKeyMap.getValue());
        mDeviceSettings.setMouseSensitivity(mMouseSensitivity.getValue());
        mDeviceSettings.setTouchpadGestureMode(mTouchpadGestureMode.getValue());
        mDeviceSettings.setTouchpadButtons(mTouchpadButtons.getValue());
        mDeviceSettings.setScrollSensitivity(mScrollSensitivity.getValue());
        mDeviceSettings.setInvertScroll(mInvertScroll.isChecked());
        mDeviceSettings.setFlingScroll(mFlingScroll.isChecked());
        mDeviceSettings.setStayAwake(mStayAwake.isChecked());
    }
}
