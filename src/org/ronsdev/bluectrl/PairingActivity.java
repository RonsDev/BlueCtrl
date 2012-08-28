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

import org.ronsdev.bluectrl.daemon.DaemonActivity;
import org.ronsdev.bluectrl.daemon.DaemonService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ViewFlipper;

/**
 * Activity that pairs the HID Bluetooth device with a host Bluetooth device.
 */
public class PairingActivity extends DaemonActivity {

    /**
     * Used as a String extra field in start Activity intents and Activity results to get the
     * device OS.
     */
    public static final String EXTRA_DEVICE_OS =
            "org.ronsdev.bluectrl.pairing.extra.DEVICE_OS";

    /**
     * Used as a Parcelable BluetoothDevice extra field in Activity results to get the current
     * Bluetooth device.
     */
    public static final String EXTRA_DEVICE =
            "org.ronsdev.bluectrl.pairing.extra.DEVICE";


    private static final String TAG = "PairingActivity";
    private static final boolean V = false;


    private static final int REQUEST_DISCOVERABLE = 1;

    private static final String SERVICE_CONFLICT_MORE_INFO_URL =
            "https://github.com/RonsDev/BlueCtrl/wiki/Bluetooth-input-service-conflict";

    /*
     * The maximum wait time until a newly paired device is declared as ready. Necessary if the
     * HID server is not running.
     */
    private static final int PAIRED_DEV_READY_TIMEOUT = 6 * 1000;

    /*
     * The maximum wait time until a already paired device that just connected is declared as
     * ready. Necessary because the Bond State Changed Event won't fire if the device is already
     * bonded on connect and some systems (iOS) don't show a pairing request dialog.
     */
    private static final int CONNECTED_PAIRED_DEV_READY_TIMEOUT = 8 * 1000;

    /*
     * The maximum wait time until a paired device is declared as ready if the HID server is
     * running. Fallback for the case that the host won't establish a connection after pairing.
     */
    private static final int HID_SERVER_PAIRED_DEV_READY_TIMEOUT = 20 * 1000;


    private ViewFlipper mViewFlipper;
    private View mViewStart;
    private View mViewSearch;
    private View mViewFailed;

    private Handler mHandler = new Handler();
    private BluetoothAdapter mBtAdapter = null;

    private String mDeviceOs = null;
    private BluetoothDevice mBondedDevice = null;

    private boolean mIsPairingActive = false;
    private boolean mWasDiscoverableAsked = false;
    private boolean mWasDiscoverableSet = false;
    private boolean mWasBondedOnConnect = false;
    private boolean mIsPairedAndReady = false;


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, 0);
                onBluetoothAdapterScanModeChanged(scanMode);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0);
                onBluetoothDeviceBondStateChanged(device, bondState);
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                onBluetoothDeviceAclConnected(device);
            }
        }
    };


    private OnClickListener mActionBarHomeClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            PairingActivity.this.finish();
        }
    };

    private final Runnable mDevicePairedAndReadyRunnable = new Runnable() {
         @Override
         public void run() {
             onDevicePairedAndReady();
         }
    };



    public static void startActivityForResult(final Activity curActivity,
            final DaemonService daemon, final int requestCode) {
        Dialog dlg = new AlertDialog.Builder(curActivity)
            .setTitle(R.string.pairing_device_os_title)
            .setItems(R.array.operating_system_names, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    final Resources res = curActivity.getResources();
                    final String[] values = res.getStringArray(R.array.operating_system_values);
                    final String deviceOs = values[item];

                    if (deviceOs.equals(DeviceSettings.OS_WINDOWS) &&
                            !daemon.isHidServerAvailable()) {
                        showServiceConflictDialog(curActivity,
                                R.string.pairing_service_conflict_windows_text);
                    } else {
                        Intent intent = new Intent(curActivity, PairingActivity.class);
                        intent.putExtra(EXTRA_DEVICE_OS, deviceOs);
                        curActivity.startActivityForResult(intent, requestCode);
                    }
                }
            })
            .create();
        dlg.setOwnerActivity(curActivity);
        dlg.show();
    }

    private static void showServiceConflictDialog(final Activity curActivity, int messageId) {
        Dialog dlg = new AlertDialog.Builder(curActivity)
            .setMessage(messageId)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            })
            .setNeutralButton(R.string.more_info, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Uri moreInfoUri = Uri.parse(SERVICE_CONFLICT_MORE_INFO_URL);
                    curActivity.startActivity(new Intent(Intent.ACTION_VIEW, moreInfoUri));
                }
            })
            .create();
        dlg.setOwnerActivity(curActivity);
        dlg.show();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        mDeviceOs = extras.getString(EXTRA_DEVICE_OS);
        if (mDeviceOs == null) {
            mDeviceOs = DeviceSettings.OS_UNDEFINED;
        }

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        loadLayout();
    }

    @Override
    protected void onStart() {
        super.onStart();

        this.registerReceiver(mReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        this.registerReceiver(mReceiver,
                new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        this.registerReceiver(mReceiver,
                new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));

        startPairing();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * HACK: This detection that a pairing request dialog has been closed is extremely vague.
         */
        onPairingRequestDialogClosed();
    }

    @Override
    protected void onPause() {
        super.onPause();

        /*
         * HACK: This detection that a pairing request dialog has been opened is extremely vague.
         */
        onPairingRequestDialogOpened();
    }

    @Override
    protected void onStop() {
        stopPairing();

        if (isFinishing() && !mIsPairedAndReady) {
            if (isDaemonAvailable()) {
                final DaemonService daemon = getDaemon();

                // If a connection was already established; disconnect it
                if (daemon.getHidState() != DaemonService.HID_STATE_DISCONNECTED) {
                    daemon.disconnectHid();
                }
            }
        }

        this.unregisterReceiver(mReceiver);

        super.onStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        loadLayout();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_DISCOVERABLE:
            mWasDiscoverableAsked = false;
            if (resultCode != RESULT_CANCELED) {
                mWasDiscoverableSet = true;
            } else {
                this.finish();
            }
            break;
        }
    }

    @Override
    protected void onDaemonAvailable() {
        startPairing();
    }

    @Override
    protected void onDaemonUnavailable(int errorCode) {
        // This Activity is useless without the daemon
        this.finish();
    }

    @Override
    protected void onHidStateChanged(int hidState, BluetoothDevice btDevice, int errorCode) {
        if (hidState == DaemonService.HID_STATE_CONNECTED) {
            if ((mBondedDevice != null) && mBondedDevice.equals(btDevice)) {
                /*
                 * If a connection was established by the host, it is a sure sign that the device
                 * is ready. Unfortunately, this is only possible in the rare case that the HID
                 * server is running. Otherwise we can only guess with timeouts.
                 */
                if (V) Log.v(TAG, String.format("paired device was connected by the host (%s)", mBondedDevice.getAddress()));
                onDevicePairedAndReady();
            } else if (isDaemonAvailable()) {
                getDaemon().disconnectHid();
            }
        }
    }


    private void loadLayout() {
        // Save some control states before the setContentView method will reset them.
        int flipperDisplayedChild = 0;
        if (mViewFlipper != null) {
            flipperDisplayedChild = mViewFlipper.getDisplayedChild();
        }

        setContentView(R.layout.pairing);

        ImageButton actionBarHome = (ImageButton)findViewById(R.id.action_bar_home);
        actionBarHome.setOnClickListener(mActionBarHomeClickListener);

        mViewFlipper = (ViewFlipper)findViewById(R.id.flipper);
        mViewFlipper.setDisplayedChild(flipperDisplayedChild);

        mViewStart = (View)findViewById(R.id.view_start);

        mViewSearch = (View)findViewById(R.id.view_search);
        TextView infoTextSearch = (TextView)findViewById(R.id.info_text_search);
        infoTextSearch.setText(Html.fromHtml(
                getString(R.string.pairing_search_for_device_text,
                        mBtAdapter.getName().replace(" ", "&nbsp;"))));

        mViewFailed = (View)findViewById(R.id.view_failed);
    }

    private void setDiscoverable(boolean discoverable) {
        if (discoverable) {
            if (!mWasDiscoverableAsked) {
                mWasDiscoverableAsked = true;
                Intent discoverableIntent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
            }
        } else {
            if (isDaemonAvailable()) {
                getDaemon().setDiscoverable(false);
                mWasDiscoverableSet = false;
            }
        }
    }

    private void startPairing() {
        if (!mIsPairingActive && isDaemonAvailable()) {
            mIsPairingActive = true;
            final DaemonService daemon = getDaemon();

            if (mDeviceOs.equals(DeviceSettings.OS_IOS)) {
                /*
                 * Change the Bluetooth Device Class to a Keyboard Class. Otherwise iOS won't
                 * find the input device.
                 */
                daemon.setHidDeviceClass();
            } else if (mDeviceOs.equals(DeviceSettings.OS_PLAYSTATION3)) {
                /*
                 * Deactivate all Bluetooth services except of the HID service because the
                 * PlayStation 3 has problems with some services (discovered on a ICS device) and
                 * won't pair if they are active.
                 */
                daemon.deactivateOtherServices();

                /*
                 * Change the Bluetooth Device Class to a Keyboard Class. Otherwise the
                 * PlayStation 3 won't find the input device.
                 */
                daemon.setHidDeviceClass();
            }

            if (mBtAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                onDevicePairable();
            } else {
                setDiscoverable(true);
            }
        }
    }

    private void stopPairing() {
        if (mIsPairingActive && isDaemonAvailable()) {
            mIsPairingActive = false;
            final DaemonService daemon = getDaemon();

            if (mWasDiscoverableSet) {
                setDiscoverable(false);
            }

            if (mDeviceOs.equals(DeviceSettings.OS_IOS)) {
                daemon.resetDeviceClass();
            } else if (mDeviceOs.equals(DeviceSettings.OS_PLAYSTATION3)) {
                daemon.resetDeviceClass();
                daemon.reactivateOtherServices();
            }

            if (V) Log.v(TAG, "pairing stopped");
        }
    }

    private void onBluetoothAdapterScanModeChanged(int scanMode) {
        if (scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            onDevicePairable();
        } else if (mIsPairingActive) {
            /*
             * The pairing process can be finished without being discoverable; so don't become
             * discoverable if the device is already pairing.
             */
            if (mViewSearch.isShown()) {
                setDiscoverable(true);
            }
        }
    }

    private void onBluetoothDeviceBondStateChanged(BluetoothDevice device, int bondState) {
        switch (bondState) {
        case BluetoothDevice.BOND_BONDING:
            if (V) Log.v(TAG, String.format("new device is pairing (%s)", device.getAddress()));
            onDevicePairing();
            break;
        case BluetoothDevice.BOND_BONDED:
            if (V) Log.v(TAG, String.format("new device has been paired (%s)", device.getAddress()));
            mBondedDevice = device;
            mWasBondedOnConnect = false;
            if (isDaemonAvailable() && getDaemon().isHidServerAvailable()) {
                startDevicePairedAndReadyTimeout(HID_SERVER_PAIRED_DEV_READY_TIMEOUT);
            } else {
                startDevicePairedAndReadyTimeout(PAIRED_DEV_READY_TIMEOUT);
            }

            // Show that the device is pairing even if the state 'bonding' has been bypassed
            onDevicePairing();
            break;
        case BluetoothDevice.BOND_NONE:
            if ((mBondedDevice != null) && (mBondedDevice.equals(device))) {
                if (V) Log.v(TAG, String.format("paired device is not paired anymore (%s)", device.getAddress()));
                stopDevicePairedAndReadyTimeout();
                onDevicePairingFailed();
            }
            break;
        }
    }

    private void onBluetoothDeviceAclConnected(BluetoothDevice device) {
        if ((mBondedDevice == null) && (device.getBondState() == BluetoothDevice.BOND_BONDED)) {
            /*
             * Because the Bond State Changed Event isn't reliable (for example it won't fire if a
             * previously paired device was removed on the host but this device still think it is
             * bonded), we assume that any incoming connection that is already bonded is a new
             * pairing request.
             */
            if (V) Log.v(TAG, String.format("already paired device got connected (%s)", device.getAddress()));
            mBondedDevice = device;
            mWasBondedOnConnect = true;
            if (isDaemonAvailable() && getDaemon().isHidServerAvailable()) {
                startDevicePairedAndReadyTimeout(HID_SERVER_PAIRED_DEV_READY_TIMEOUT);
            } else {
                startDevicePairedAndReadyTimeout(CONNECTED_PAIRED_DEV_READY_TIMEOUT);
            }

            onDevicePairing();
        }
    }

    private void onPairingRequestDialogOpened() {
        if (mWasBondedOnConnect) {
            stopDevicePairedAndReadyTimeout();
        }
    }

    private void onPairingRequestDialogClosed() {
        if (mWasBondedOnConnect && (mBondedDevice != null) &&
                (mBondedDevice.getBondState() == BluetoothDevice.BOND_BONDED)) {
            if (V) Log.v(TAG, String.format("already paired device has been paired again (%s)", mBondedDevice.getAddress()));
            if (isDaemonAvailable() && getDaemon().isHidServerAvailable()) {
                startDevicePairedAndReadyTimeout(HID_SERVER_PAIRED_DEV_READY_TIMEOUT);
            } else {
                startDevicePairedAndReadyTimeout(PAIRED_DEV_READY_TIMEOUT);
            }
        }
    }

    private void onDevicePairable() {
        if (mViewStart.isShown()) {
            mViewFlipper.showNext();
        }

        if (V) Log.v(TAG, "ready to get paired");
    }

    private void onDevicePairing() {
        if (mViewSearch.isShown()) {
            mViewFlipper.showNext();
        }
    }

    private void onDevicePairingFailed() {
        mViewFlipper.setDisplayedChild(mViewFlipper.indexOfChild(mViewFailed));
    }

    private void onDevicePairedAndReady() {
        if (!mIsPairedAndReady && (mBondedDevice != null) &&
                (mBondedDevice.getBondState() == BluetoothDevice.BOND_BONDED)) {
            mIsPairedAndReady = true;

            if (V) Log.v(TAG, String.format("device paired and ready (%s)", mBondedDevice.getAddress()));

            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_DEVICE, mBondedDevice);
            resultIntent.putExtra(EXTRA_DEVICE_OS, mDeviceOs);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }
    }

    private void startDevicePairedAndReadyTimeout(int timeout) {
        stopDevicePairedAndReadyTimeout();
        mHandler.postDelayed(mDevicePairedAndReadyRunnable, timeout);
    }

    private void stopDevicePairedAndReadyTimeout() {
        mHandler.removeCallbacks(mDevicePairedAndReadyRunnable);
    }
}