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

import org.ronsdev.bluectrl.daemon.DaemonListActivity;
import org.ronsdev.bluectrl.daemon.DaemonService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The Main Activity manages the Bluetooth devices.
 */
public class MainActivity extends DaemonListActivity
        implements OnItemClickListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PAIRING = 2;


    private static final int DIALOG_ABOUT = 1;
    private static final int DIALOG_CHANGELOG = 2;
    private static final int DIALOG_CRITICAL_ERROR = 3;

    private static final String DIALOG_ARG_ERROR_MSG =
            "org.ronsdev.bluectrl.dialog_arg.ERROR_MSG";


    private static final String SAVED_STATE_WAS_ENABLE_BT_ASKED = "WasEnableBtAsked";

    private static final List<PairedDevice> EMPTY_DEVICE_LIST = new ArrayList<PairedDevice>();


    private ImageButton mButtonAddDevice;
    private TextView mTextEmptyList;

    private DeviceManager mDeviceManager;
    private List<PairedDevice> mDevices;

    private boolean mIsConnectActivityStarting = false;
    private boolean mWasEnableBtAsked = false;


    private OnClickListener mAddDeviceClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isDaemonAvailable()) {
                PairingActivity.startActivityForResult(
                        MainActivity.this, getDaemon(), REQUEST_PAIRING);
            }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mWasEnableBtAsked = savedInstanceState.getBoolean(SAVED_STATE_WAS_ENABLE_BT_ASKED);
        }

        mDeviceManager = new DeviceManager(this);

        loadLayout();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mIsConnectActivityStarting = false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        loadLayout();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(SAVED_STATE_WAS_ENABLE_BT_ASKED, mWasEnableBtAsked);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_ENABLE_BT:
            mWasEnableBtAsked = false;
            break;
        case REQUEST_PAIRING:
            if (resultCode == Activity.RESULT_OK) {
                BluetoothDevice device = data.getParcelableExtra(PairingActivity.EXTRA_DEVICE);
                String deviceOs = data.getStringExtra(PairingActivity.EXTRA_DEVICE_OS);
                onDevicePaired(device, deviceOs);
            }
            break;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch(id) {
        case DIALOG_ABOUT:
            return createAboutDialog();
        case DIALOG_CHANGELOG:
            return ChangelogDialog.createDialog(this);
        case DIALOG_CRITICAL_ERROR:
            return createCriticalErrorDialog(args.getString(DIALOG_ARG_ERROR_MSG));
        default:
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_about:
            showDialog(DIALOG_ABOUT);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (menuInfo instanceof AdapterContextMenuInfo) {
            AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo)menuInfo;
            if (isDaemonAvailable() && (mDevices != null) && (adapterInfo.id < mDevices.size())) {
                PairedDevice pairedDevice = mDevices.get((int)adapterInfo.id);

                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.device_context, menu);

                menu.setHeaderTitle(pairedDevice.toString());
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final ContextMenuInfo menuInfo = item.getMenuInfo();

        if (menuInfo instanceof AdapterContextMenuInfo) {
            AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo)menuInfo;
            if (isDaemonAvailable() && (mDevices != null) && (adapterInfo.id < mDevices.size())) {
                PairedDevice pairedDevice = mDevices.get((int)adapterInfo.id);

                switch (item.getItemId()) {
                case R.id.menu_connect:
                    connectToDevice(pairedDevice.getDevice(), false);
                    return true;
                case R.id.menu_preferences:
                    DevicePreferenceActivity.startActivity(this, pairedDevice.getDevice());
                    return true;
                case R.id.menu_remove_device:
                    mDeviceManager.unregisterDevice(pairedDevice.getDevice());
                    refreshListView();
                    return true;
                }
            }
        }

        return super.onContextItemSelected(item);
    }

    @Override
    protected void onDaemonAvailable() {
        refreshListView();
    }

    @Override
    protected void onDaemonUnavailable(int errorCode) {
        if (errorCode != 0) {
            Bundle args;

            switch (errorCode) {
            case DaemonService.ERROR_ROOT_REQUIRED:
                // Do nothing at non critical errors
                break;
            case DaemonService.ERROR_BT_REQUIRED:
                if (!mWasEnableBtAsked) {
                    mWasEnableBtAsked = true;
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                break;
            case DaemonService.ERROR_INCOMPATIBLE:
                args = new Bundle();
                args.putString(DIALOG_ARG_ERROR_MSG, getString(R.string.error_incompatible));
                showDialog(DIALOG_CRITICAL_ERROR, args);
                break;
            default:
                args = new Bundle();
                args.putString(DIALOG_ARG_ERROR_MSG,
                        getString(R.string.error_critical, errorCode));
                showDialog(DIALOG_CRITICAL_ERROR, args);
                break;
            }
        }
        refreshListView();
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (isDaemonAvailable() && (mDevices != null) && (position < mDevices.size())) {
            connectToDevice(mDevices.get(position).getDevice(), false);
        }
    }

    private void onDevicePaired(BluetoothDevice device, String deviceOs) {
        mDeviceManager.registerDevice(device, deviceOs);
        refreshListView();

        connectToDevice(device, true);
    }


    private void loadLayout() {
        setContentView(R.layout.device_list);

        ListView listView = getListView();
        listView.setOnItemClickListener(this);
        registerForContextMenu(listView);

        mButtonAddDevice = (ImageButton)findViewById(R.id.button_add_device);
        mButtonAddDevice.setOnClickListener(mAddDeviceClickListener);

        mTextEmptyList = (TextView)listView.getEmptyView();

        refreshListView();
    }

    private void connectToDevice(BluetoothDevice device, boolean isNewDevice) {
        if (!mIsConnectActivityStarting) {
            mIsConnectActivityStarting = true;

            TouchpadActivity.startActivity(this, device, isNewDevice);
        }
    }

    private String getVersionString() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return getString(R.string.about_version, packageInfo.versionName);
        } catch (NameNotFoundException e) {
            return "";
        }
    }

    private Dialog createAboutDialog() {
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.about_dialog,
                (ViewGroup)findViewById(R.id.view_about));

        TextView versionText = (TextView)layout.findViewById(R.id.version_text);
        versionText.setText(getVersionString());

        TextView emailAddress = (TextView)layout.findViewById(R.id.about_email_address);
        emailAddress.setText(String.format("%s@%s", "ronsdev", "gmail.com"));

        TextView webpageAddress = (TextView)layout.findViewById(R.id.about_webpage_address);
        webpageAddress.setText("github.com/RonsDev/BlueCtrl");

        return new AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            })
            .setNeutralButton(R.string.changelog, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    showDialog(DIALOG_CHANGELOG);
                }
            })
            .create();
    }

    private Dialog createCriticalErrorDialog(String errorMsg) {
        return new AlertDialog.Builder(this)
            .setCancelable(false)
            .setMessage(errorMsg)
            .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    MainActivity.this.finish();
                }
            })
            .create();
    }

    private String getEmptyListReason() {
        final DaemonService daemon = getDaemon();

        if (daemon != null) {
            switch (daemon.getErrorCode()) {
            case DaemonService.ERROR_ROOT_REQUIRED:
                return getString(R.string.error_root_required);
            case DaemonService.ERROR_BT_REQUIRED:
                return getString(R.string.error_bluetooth_required);
            }
        }

        if (isDaemonAvailable()) {
            return getString(R.string.device_list_empty);
        } else {
            return "";
        }
    }

    private void refreshListView() {
        if (isDaemonAvailable()) {
            mDevices = mDeviceManager.getPairedDevices();

            mButtonAddDevice.setVisibility(View.VISIBLE);
        } else {
            mDevices = EMPTY_DEVICE_LIST;

            mButtonAddDevice.setVisibility(View.GONE);
        }

        if (mDevices.isEmpty()) {
            mTextEmptyList.setText(getEmptyListReason());
        }

        setListAdapter(new ArrayAdapter<PairedDevice>(this,
                android.R.layout.simple_list_item_1,
                mDevices));
    }
}
