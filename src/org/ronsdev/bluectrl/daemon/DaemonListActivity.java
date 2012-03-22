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

package org.ronsdev.bluectrl.daemon;


import org.ronsdev.bluectrl.daemon.DaemonService.DaemonBinder;

import android.app.ListActivity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * Basic implementation for List-Activities that are using the application daemon.
 * It is a one-on-one copy of the DaemonActivity class except that it extends ListActivity.
 */
public abstract class DaemonListActivity extends ListActivity {

    private DaemonService mDaemon;

    private ServiceConnection mDaemonConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            DaemonBinder binder = (DaemonBinder)service;
            mDaemon = binder.getService();
            if (mDaemon.isRunning()) {
                onDaemonAvailable();
            } else {
                onDaemonUnavailable(mDaemon.getErrorCode());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            if (mDaemon != null) {
                final int errorCode = mDaemon.getErrorCode();
                mDaemon = null;
                onDaemonUnavailable(errorCode);
            }
        }
    };


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (DaemonService.ACTION_STATE_CHANGED.equals(action)) {
                int daemonState = intent.getIntExtra(DaemonService.EXTRA_STATE, 0);
                int errorCode = intent.getIntExtra(DaemonService.EXTRA_ERROR_CODE, 0);
                onDaemonStateChanged(daemonState, errorCode);
            } else if (DaemonService.ACTION_HID_STATE_CHANGED.equals(action)) {
                int hidState = intent.getIntExtra(DaemonService.EXTRA_HID_STATE, 0);
                BluetoothDevice btDevice = intent.getParcelableExtra(DaemonService.EXTRA_DEVICE);
                int errorCode = intent.getIntExtra(DaemonService.EXTRA_ERROR_CODE, 0);
                onHidStateChanged(hidState, btDevice, errorCode);
            } else if (DaemonService.ACTION_HID_SERVER_AVAILABILITY_CHANGED.equals(action)) {
                onHidServerAvailabilityChanged();
            } else if (DaemonService.ACTION_HID_MOUSE_FEATURE_RECEIVED.equals(action)) {
                onHidMouseFeatureReceived();
            }
        }
    };


    @Override
    protected void onStart() {
        super.onStart();

        this.registerReceiver(mReceiver,
                new IntentFilter(DaemonService.ACTION_STATE_CHANGED));
        this.registerReceiver(mReceiver,
                new IntentFilter(DaemonService.ACTION_HID_STATE_CHANGED));
        this.registerReceiver(mReceiver,
                new IntentFilter(DaemonService.ACTION_HID_SERVER_AVAILABILITY_CHANGED));
        this.registerReceiver(mReceiver,
                new IntentFilter(DaemonService.ACTION_HID_MOUSE_FEATURE_RECEIVED));

        if (mDaemon == null) {
            Intent daemonIntent = new Intent(this, DaemonService.class);
            bindService(daemonIntent, mDaemonConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mDaemon != null) {
            unbindService(mDaemonConnection);
            mDaemon = null;
        }

        this.unregisterReceiver(mReceiver);
    }


    /** Called when the daemon state has changed. */
    protected void onDaemonStateChanged(int daemonState, int errorCode) {
        if (mDaemon != null) {
            if (daemonState == DaemonService.STATE_STARTED) {
                onDaemonAvailable();
            } else {
                onDaemonUnavailable(errorCode);
            }
        }
    }

    /** Called when the daemon has become available. */
    protected void onDaemonAvailable() {

    }

    /** Called when the daemon has become unavailable. */
    protected void onDaemonUnavailable(int errorCode) {

    }

    /** Called when the HID connection state from the daemon has changed. */
    protected void onHidStateChanged(int hidState, BluetoothDevice btDevice, int errorCode) {

    }

    /** Called when the HID server availability has changed. */
    protected void onHidServerAvailabilityChanged() {

    }

    /** Called when a HID Mouse Feature Report has been received. */
    protected void onHidMouseFeatureReceived() {

    }


    protected boolean isDaemonAvailable() {
        return ((mDaemon != null) && mDaemon.isRunning());
    }

    /** Gets the DaemonService object or {@code null} if not connected. */
    protected DaemonService getDaemon() {
        return mDaemon;
    }
}
