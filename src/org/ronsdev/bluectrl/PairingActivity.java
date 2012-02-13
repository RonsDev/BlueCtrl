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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewFlipper;

/**
 * Activity that pairs the HID Bluetooth device with a host Bluetooth device.
 */
public class PairingActivity extends DaemonActivity {

    /**
     * Used as a Parcelable BluetoothDevice extra field in start Activity intents to get the
     * current Bluetooth device.
     */
    public static final String EXTRA_DEVICE =
            "org.ronsdev.bluectrl.pairing.extra.DEVICE";


    private ViewFlipper mViewFlipper;
    private ScrollView mScrollPrepare;
    private View mViewServiceConflict;
    private View mViewServiceConflictMoreInfo;
    private TextView mTextServiceConflict;
    private View mViewWait;
    private View mViewFinish;
    private Button mButtonConnect;

    private BluetoothAdapter mBtAdapter;
    private BluetoothDevice mBtDevice;

    private boolean mIsServiceConflictMoreInfoVisible = false;
    private boolean mIsPairingActive = false;
    private boolean mWasDiscoverableSet = false;
    private boolean mIsPaired = false;


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


    private OnClickListener mServiceConflictMoreInfoClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mIsServiceConflictMoreInfoVisible = true;
            updateServiceConflictVisibility();
            scrollToView(mScrollPrepare, mViewServiceConflictMoreInfo);
        }
    };

    private OnClickListener mCancelPairingClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            PairingActivity.this.finish();
        }
    };

    private OnClickListener mNextStepClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mViewFlipper.showNext();

            if (isPairingViewShown()) {
                startPairing();
            }
        }
    };

    private OnClickListener mConnectClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            onDevicePaired();
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
    }

    @Override
    protected void onStop() {
        stopPairing();

        if (isFinishing() && !mIsPaired) {
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
    protected void onDaemonAvailable() {
        updateServiceConflictVisibility();

        if (isPairingViewShown()) {
            startPairing();
        }
    }

    @Override
    protected void onDaemonUnavailable(int errorCode) {
        // This Activity is useless without the daemon
        this.finish();
    }

    @Override
    protected void onHidServerAvailabilityChanged() {
        updateServiceConflictVisibility();
    }


    private void loadLayout() {
        /*
         * Save some control states before the setContentView method will reset them.
         */
        int flipperDisplayedChild = 0;
        if (mViewFlipper != null) {
            flipperDisplayedChild = mViewFlipper.getDisplayedChild();
        }


        setContentView(R.layout.pairing);


        mViewFlipper = (ViewFlipper)findViewById(R.id.flipper);
        mViewFlipper.setDisplayedChild(flipperDisplayedChild);

        /* Prepare pairing */
        mScrollPrepare = (ScrollView)findViewById(R.id.scroll_prepare);

        mViewServiceConflict = findViewById(R.id.service_conflict);
        mViewServiceConflictMoreInfo = findViewById(R.id.service_conflict_more_info);
        mTextServiceConflict = (TextView)findViewById(R.id.service_conflict_text);
        mTextServiceConflict.setOnClickListener(mServiceConflictMoreInfoClickListener);
        updateServiceConflictVisibility();

        Button buttonCancelPrepare = (Button)findViewById(R.id.button_cancel_prepare);
        buttonCancelPrepare.setOnClickListener(mCancelPairingClickListener);

        Button buttonContinuePrepare = (Button)findViewById(R.id.button_continue_prepare);
        buttonContinuePrepare.setOnClickListener(mNextStepClickListener);


        /* Wait for Request */
        mViewWait = (View)findViewById(R.id.view_wait);

        TextView textSearchForDevice = (TextView)findViewById(R.id.text_search_for_device);
        textSearchForDevice.setText(Html.fromHtml(
                getString(R.string.pairing_search_for_device_text, mBtAdapter.getName())));

        Button buttonCancelWait = (Button)findViewById(R.id.button_cancel_wait);
        buttonCancelWait.setOnClickListener(mCancelPairingClickListener);


        /* Finish pairing */
        mViewFinish = (View)findViewById(R.id.view_finish);

        Button buttonCancelFinish = (Button)findViewById(R.id.button_cancel_finish);
        buttonCancelFinish.setOnClickListener(mCancelPairingClickListener);

        mButtonConnect = (Button)findViewById(R.id.button_connect);
        mButtonConnect.setOnClickListener(mConnectClickListener);
    }

    private void onBluetoothAdapterScanModeChanged(int scanMode) {
        if (mIsPairingActive && !mWasDiscoverableSet && isDaemonAvailable() &&
                (scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)) {
            final DaemonService daemon = getDaemon();

            mWasDiscoverableSet = true;
            daemon.setDiscoverable(true);
        }
    }

    private void onBluetoothDeviceBondStateChanged(BluetoothDevice device, int bondState) {
        updateButtonConnectEnabled();
    }

    private void onBluetoothDeviceAclConnected(BluetoothDevice device) {
        /*
         * Because the Bond State Changed Event isn't reliable (for example it won't fire if a
         * previously paired device was removed on the host but the device still think it is
         * bonded), we assume that any incoming connection is a pairing request.
         */

        mBtDevice = device;
        updateButtonConnectEnabled();

        if (mViewWait.isShown()) {
            mViewFlipper.showNext();
        }
    }

    private void onDevicePaired() {
        if (!mIsPaired && (mBtDevice != null)) {
            mIsPaired = true;

            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_DEVICE, mBtDevice);
            setResult(Activity.RESULT_OK, resultIntent);
            this.finish();
        }
    }

    private static void scrollToView(final ScrollView scrollView, final View target) {
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                Rect targetRect = new Rect();
                scrollView.offsetDescendantRectToMyCoords(target, targetRect);
                scrollView.smoothScrollTo(targetRect.left, targetRect.top);
            }
        });
    }

    private void updateServiceConflictVisibility() {
        if (!isDaemonAvailable() || getDaemon().isHidServerAvailable()) {
            mViewServiceConflict.setVisibility(View.GONE);
        } else {
            mViewServiceConflict.setVisibility(View.VISIBLE);

            if (mIsServiceConflictMoreInfoVisible) {
                mTextServiceConflict.setText(Html.fromHtml(
                        getString(R.string.service_conflict_text)));
                mTextServiceConflict.setClickable(false);

                mViewServiceConflictMoreInfo.setVisibility(View.VISIBLE);
            } else {
                mTextServiceConflict.setText(Html.fromHtml(String.format(
                        "%s <a href=\"#\">%s</a>",
                        getString(R.string.service_conflict_text),
                        getString(R.string.service_conflict_more_info))));

                mViewServiceConflictMoreInfo.setVisibility(View.GONE);
            }
        }
    }

    private void updateButtonConnectEnabled() {
        boolean bonded = (mBtDevice != null) &&
                (mBtDevice.getBondState() == BluetoothDevice.BOND_BONDED);

        mButtonConnect.setEnabled(bonded);
    }

    private boolean isPairingViewShown() {
        return (mViewWait.isShown() || mViewFinish.isShown());
    }

    private void startPairing() {
        if (!mIsPairingActive && isDaemonAvailable()) {
            mIsPairingActive = true;
            final DaemonService daemon = getDaemon();

            /*
             * Change the Bluetooth Device Class to a Keyboard Class. Otherwise some Operating
             * Systems (for example iOS) won't accept the input device.
             */
            daemon.setHidDeviceClass();

            if (mBtAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                mWasDiscoverableSet = true;
                daemon.setDiscoverable(true);
            }
        }
    }

    private void stopPairing() {
        if (mIsPairingActive && isDaemonAvailable()) {
            mIsPairingActive = false;
            final DaemonService daemon = getDaemon();

            if (mWasDiscoverableSet) {
                mWasDiscoverableSet = false;
                daemon.setDiscoverable(false);
            }

            daemon.resetDeviceClass();
        }
    }
}
