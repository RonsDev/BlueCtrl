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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * Service that interacts with the application daemon.
 */
public class DaemonService extends Service {

    /** Broadcast Action: The state of the daemon has changed. */
    public static final String ACTION_STATE_CHANGED =
            "org.ronsdev.bluectrl.daemon.action.STATE_CHANGED";

    /** Broadcast Action: The state of the HID connection has changed. */
    public static final String ACTION_HID_STATE_CHANGED =
            "org.ronsdev.bluectrl.daemon.action.HID_STATE_CHANGED";

    /** Broadcast Action: The HID server availability has changed. */
    public static final String ACTION_HID_SERVER_AVAILABILITY_CHANGED =
            "org.ronsdev.bluectrl.daemon.action.HID_SERVER_AVAILABILITY_CHANGED";

    /** Broadcast Action: A HID Mouse Feature Report has been received. */
    public static final String ACTION_HID_MOUSE_FEATURE_RECEIVED =
            "org.ronsdev.bluectrl.daemon.action.HID_MOUSE_FEATURE_RECEIVED";


    /**
     * Used as an int extra field in ACTION_STATE_CHANGED intents to request the current
     * daemon state.
     */
    public static final String EXTRA_STATE =
            "org.ronsdev.bluectrl.daemon.extra.STATE";

    /**
     * Used as an int extra field in ACTION_HID_STATE_CHANGED intents to request the current
     * HID connection state.
     */
    public static final String EXTRA_HID_STATE =
            "org.ronsdev.bluectrl.daemon.extra.HID_STATE";

    /**
     * Used as a Parcelable BluetoothDevice extra field in ACTION_HID_STATE_CHANGED intents to get
     * the current Bluetooth device.
     */
    public static final String EXTRA_DEVICE =
            "org.ronsdev.bluectrl.daemon.extra.DEVICE";

    /**
     * Used as an optional int extra field in ACTION_STATE_CHANGED and ACTION_HID_STATE_CHANGED
     * intents to get the error code.
     */
    public static final String EXTRA_ERROR_CODE =
            "org.ronsdev.bluectrl.daemon.extra.ERROR_CODE";


    /** Indicates the daemon is not running. */
    public static final int STATE_STOPPED = 10;

    /** Indicates the daemon is running and ready for use. */
    public static final int STATE_STARTED = 20;


    /** Indicates that the daemon is connecting to a HID host. */
    public static final int HID_STATE_CONNECTING = 10;

    /** Indicates that the daemon is connected with a HID host. */
    public static final int HID_STATE_CONNECTED = 20;

    /** Indicates that the daemon is disconnecting from a HID host. */
    public static final int HID_STATE_DISCONNECTING = 30;

    /** Indicates that the daemon is not connected with a HID host. */
    public static final int HID_STATE_DISCONNECTED = 40;


    /** Unknown error */
    public static final int ERROR_UNKNOWN = -10;
    /** Invalid Bluetooth address */
    public static final int ERROR_INVBDADDR = -20;

    /** Permission denied */
    public static final int ERROR_ACCES = -51;
    /** Operation not permitted */
    public static final int ERROR_PERM = -52;
    /** No such device */
    public static final int ERROR_NODEV = -53;
    /** The socket is not connected */
    public static final int ERROR_NOTCONN = -54;
    /** No such file or directory */
    public static final int ERROR_NOENT = -55;
    /** Address already in use */
    public static final int ERROR_ADDRINUSE = -56;
    /** Host is down */
    public static final int ERROR_HOSTDOWN = -57;
    /** Connection refused */
    public static final int ERROR_CONNREFUSED = -58;
    /** Connection timed out */
    public static final int ERROR_TIMEDOUT = -59;
    /** Connection already in progress */
    public static final int ERROR_ALREADY = -60;
    /** Invalid exchange */
    public static final int ERROR_BADE = -61;
    /** Connection reset by peer */
    public static final int ERROR_CONNRESET = -62;

    /** Root permissions required */
    public static final int ERROR_ROOT_REQUIRED = -100;
    /** Bluetooth required */
    public static final int ERROR_BT_REQUIRED = -110;
    /** Daemon installation failed */
    public static final int ERROR_INSTALL = -120;
    /** Starting Daemon failed */
    public static final int ERROR_START = -130;
    /** IPC communication error */
    public static final int ERROR_IPC = -140;
    /** Incompatible Android version */
    public static final int ERROR_INCOMPATIBLE = -150;


    private static final String TAG = "DaemonService";
    private static final boolean V = false;
    private static final boolean DEBUG_DAEMON = false;


    /** The binary file name of the daemon. */
    private static final String BINARY_NAME = "bluectrld";

    /** The abstract Unix Domain socket address for the IPC communication with the daemon. */
    private static final String IPC_UNIXDOMAIN_NAME = "org.ronsdev.bluectrld";


    /*
     * Daemon IPC commands. Documented in the "hidipc.h" file.
     */
    private static final int IPC_CMD_SHUTDOWN = 10;
    private static final int IPC_CMD_DISCOVERABLE_ON = 20;
    private static final int IPC_CMD_DISCOVERABLE_OFF = 25;
    private static final int IPC_CMD_SET_HID_DEVICE_CLASS = 30;
    private static final int IPC_CMD_RESET_DEVICE_CLASS = 35;
    private static final int IPC_CMD_DEACTIVATE_OTHER_SERVICES = 40;
    private static final int IPC_CMD_REACTIVATE_OTHER_SERVICES = 45;
    private static final int IPC_CMD_HID_CONNECT = 90;
    private static final int IPC_CMD_HID_DISCONNECT = 95;
    private static final int IPC_CMD_HID_SEND_KEYS = 110;
    private static final int IPC_CMD_HID_SEND_MOUSE = 120;
    private static final int IPC_CMD_HID_SEND_SYSTEM_KEYS = 125;
    private static final int IPC_CMD_HID_SEND_HW_KEYS = 130;
    private static final int IPC_CMD_HID_SEND_MEDIA_KEYS = 140;
    private static final int IPC_CMD_HID_SEND_AC_KEYS = 145;
    private static final int IPC_CMD_HID_CHANGE_MOUSE_FEATURE = 150;
    private static final int IPC_CMD_HID_SEND_MOUSE_ABSOLUTE = 160;


    public class DaemonBinder extends Binder {
        public DaemonService getService() {
            return DaemonService.this;
        }
    }

    private final IBinder mBinder = new DaemonBinder();


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                // Notify the daemon about the Bluetooth availability
                int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                if (newState == BluetoothAdapter.STATE_ON) {
                    if (mErrorCode == ERROR_BT_REQUIRED) {
                        startDaemon();
                    }
                } else {
                    shutdownDaemon(ERROR_BT_REQUIRED);
                }
            } else if (DaemonCallbackReceiver.ACTION_PREMATURE_TERMINATION.equals(action)) {
                stopDaemon(ERROR_IPC);
            } else if (DaemonCallbackReceiver.ACTION_CALLBACK.equals(action)) {
                int cbType = intent.getIntExtra(DaemonCallbackReceiver.EXTRA_TYPE, 0);
                String btAddress = intent.getStringExtra(DaemonCallbackReceiver.EXTRA_BTADDRESS);
                int errorCode = intent.getIntExtra(DaemonCallbackReceiver.EXTRA_ERROR_CODE, 0);

                switch (cbType) {
                case DaemonCallbackReceiver.IPC_CB_HID_CONNECTED:
                    if (!isHidConnectionCanceled()) {
                        onHidConnected(btAddress);
                    }
                    break;
                case DaemonCallbackReceiver.IPC_CB_HID_DISCONNECTED:
                    onHidDisconnected(errorCode);
                    break;
                case DaemonCallbackReceiver.IPC_CB_INFO_NO_SERVER:
                    mIsHidServerAvailable = false;
                    sendBroadcast(new Intent(ACTION_HID_SERVER_AVAILABILITY_CHANGED));
                    break;
                case DaemonCallbackReceiver.IPC_CB_MOUSE_FEATURE:
                    mIsSmoothScrollYOn = intent.getBooleanExtra(
                            DaemonCallbackReceiver.EXTRA_SMOOTH_SCROLL_Y, false);
                    mIsSmoothScrollXOn = intent.getBooleanExtra(
                            DaemonCallbackReceiver.EXTRA_SMOOTH_SCROLL_X, false);
                    sendBroadcast(new Intent(ACTION_HID_MOUSE_FEATURE_RECEIVED));
                    break;
                case DaemonCallbackReceiver.IPC_ECB_DISCOVERABLE_OFF:
                case DaemonCallbackReceiver.IPC_ECB_DISCOVERABLE_ON:
                case DaemonCallbackReceiver.IPC_ECB_SET_HID_DEVICE_CLASS:
                case DaemonCallbackReceiver.IPC_ECB_RESET_DEVICE_CLASS:
                case DaemonCallbackReceiver.IPC_ECB_DEACTIVATE_OTHER_SERVICES:
                case DaemonCallbackReceiver.IPC_ECB_REACTIVATE_OTHER_SERVICES:
                    if (errorCode == 0) {
                        shutdownDaemon(ERROR_UNKNOWN);
                    } else {
                        shutdownDaemon(errorCode);
                    }
                    break;
                case DaemonCallbackReceiver.IPC_ECB_HID_CONNECT:
                    if (isHidConnectionCanceled()) {
                        onHidDisconnected(0);
                    } else {
                        onHidDisconnected(errorCode);
                    }
                    break;
                }
            }
        }
    };

    private BluetoothAdapter mBtAdapter;
    private int mState = STATE_STOPPED;
    private int mErrorCode = 0;
    private boolean mIsHidServerAvailable = true;
    private int mHidState = HID_STATE_DISCONNECTED;
    private int mHidErrorCode = 0;
    private BluetoothDevice mConnectedDevice;
    private boolean mIsSmoothScrollYOn = false;
    private boolean mIsSmoothScrollXOn = false;

    private DaemonCallbackReceiver mCallbackReceiver;
    private LocalSocket mLocalSocket;
    private DataOutputStream mOutStream;


    @Override
    public void onCreate() {
        if (V) Log.v(TAG, "DaemonService.onCreate()");

        super.onCreate();

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        this.registerReceiver(mReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        this.registerReceiver(mReceiver,
                new IntentFilter(DaemonCallbackReceiver.ACTION_PREMATURE_TERMINATION));
        this.registerReceiver(mReceiver,
                new IntentFilter(DaemonCallbackReceiver.ACTION_CALLBACK));

        startDaemon();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        if (V) Log.v(TAG, "DaemonService.onDestroy()");

        super.onDestroy();

        this.unregisterReceiver(mReceiver);

        shutdownDaemon(0);
    }


    public int getState() {
        return mState;
    }

    private void setState(int state, int errorCode) {
        if ((mState != state) || (mErrorCode != errorCode)) {
            mState = state;
            mErrorCode = errorCode;

            if (V) Log.v(TAG, String.format("DaemonService state changed (%d, %d)", state, errorCode));

            Intent intent = new Intent(ACTION_STATE_CHANGED);
            intent.putExtra(EXTRA_STATE, state);
            if (errorCode != 0) {
                intent.putExtra(EXTRA_ERROR_CODE, errorCode);
            }
            sendBroadcast(intent);
        }
    }

    /** Gets an error code if the daemon has encountered a problem or 0 if everything is fine. */
    public int getErrorCode() {
        return mErrorCode;
    }

    public boolean isRunning() {
        return (mState == STATE_STARTED);
    }


    public boolean isHidServerAvailable() {
        return mIsHidServerAvailable;
    }


    public int getHidState() {
        return mHidState;
    }

    private void setHidState(int hidState, int errorCode) {
        if ((mHidState != hidState) || (mHidErrorCode != errorCode)) {
            mHidState = hidState;
            mHidErrorCode = errorCode;

            if (V) Log.v(TAG, String.format("DaemonService HID state changed (%d, %d)", hidState, errorCode));

            Intent intent = new Intent(ACTION_HID_STATE_CHANGED);
            intent.putExtra(EXTRA_HID_STATE, hidState);
            intent.putExtra(EXTRA_DEVICE, getConnectedDevice());
            if (errorCode != 0) {
                intent.putExtra(EXTRA_ERROR_CODE, errorCode);
            }
            sendBroadcast(intent);
        }
    }

    private void onHidConnecting(String btAddress) {
        setConnectedDeviceFromAddress(btAddress);

        setHidState(HID_STATE_CONNECTING, 0);
    }

    private void onHidConnected(String btAddress) {
        setConnectedDeviceFromAddress(btAddress);

        mIsSmoothScrollYOn = false;
        mIsSmoothScrollXOn = false;

        setHidState(HID_STATE_CONNECTED, 0);
    }

    private void onHidDisconnecting() {
        setHidState(HID_STATE_DISCONNECTING, 0);
    }

    private void onHidDisconnected(int errorCode) {
        setConnectedDeviceFromAddress("");

        setHidState(HID_STATE_DISCONNECTED, errorCode);
    }

    /**
     * Returns true if the HID connection process was canceled.
     * Can be checked to prevent firing unintended connection established/failed callbacks if
     * the connection process was already canceled.
     */
    private boolean isHidConnectionCanceled() {
        return mHidState == HID_STATE_DISCONNECTING;
    }

    private void setConnectedDeviceFromAddress(String btAddress) {
        if (btAddress.isEmpty()) {
            mConnectedDevice = null;
        } else {
            if ((mConnectedDevice == null) ||
                    !mConnectedDevice.getAddress().equalsIgnoreCase(btAddress)) {
                mConnectedDevice = mBtAdapter.getRemoteDevice(btAddress);
            }
        }
    }

    public BluetoothDevice getConnectedDevice() {
        return mConnectedDevice;
    }

    /** Gets an error code from a HID connection problem or 0 if everything is fine. */
    public int getHidErrorCode() {
        return mHidErrorCode;
    }

    public boolean isSmoothScrollYOn() {
        return mIsSmoothScrollYOn;
    }

    public boolean isSmoothScrollXOn() {
        return mIsSmoothScrollXOn;
    }

    public void setSmoothScroll(boolean y, boolean x) {
        if ((y != mIsSmoothScrollYOn) || (x != mIsSmoothScrollXOn)) {
            mIsSmoothScrollYOn = y;
            mIsSmoothScrollXOn = x;
            changeMouseFeature(y, x);
        }
    }


    private boolean checkLibraries() {
        File checkFile = new File("/system/lib/", "libbluetooth.so");
        return checkFile.exists();
    }

    private File getBinaryFile() {
        return new File(getFilesDir().getAbsolutePath(), BINARY_NAME);
    }

    private long getApplicationUpdateTime() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.lastUpdateTime;
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Can't get the update time of the application", e);
            return new Date().getTime();
        }
    }

    private int installDaemonBinary() {
        if (V) Log.v(TAG, "DaemonService.installDaemonBinary()");

        File file = getBinaryFile();

        // Only install the binary if it doesn't exist or the application was updated
        if (file.lastModified() < getApplicationUpdateTime()) {
            if (file.exists()) {
                file.delete();
            }

            try {
                InputStream inputStream = getAssets().open(BINARY_NAME);
                try {
                    FileOutputStream outputStream = new FileOutputStream(file);
                    try {
                        byte[] buffer = new byte[1024];
                        int rsize = inputStream.read(buffer);
                        while (rsize > -1) {
                            outputStream.write(buffer, 0, rsize);
                            rsize = inputStream.read(buffer);
                        }
                    } finally {
                        outputStream.close();
                    }
                } finally {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "install Daemon binary failed", e);
                return ERROR_INSTALL;
            }

            file.setExecutable(true);
        }

        return 0;
    }

    private int startDaemonProcess() {
        File file = getBinaryFile();

        if (!file.exists()) {
            Log.e(TAG, "daemon binary doesn't exist");
            return ERROR_START;
        }

        String cmd = file.getAbsolutePath();
        if (DEBUG_DAEMON) {
            cmd += " --debug";
        }
        final String[] progArray = { "su", "-c", cmd };

        Process proc;
        try {
            proc = Runtime.getRuntime().exec(progArray);
        } catch (IOException e) {
            Log.e(TAG, "execute daemon with su failed", e);
            return ERROR_ROOT_REQUIRED;
        }

        int suResult;
        try {
            suResult = proc.waitFor();
        } catch (InterruptedException e) {
            Log.e(TAG, "wait for daemon result failed", e);
            return ERROR_START;
        }

        /*
         * The 'su' command will return 1 or 255 if the Superuser permissions are denied.
         * Otherwise it returns 0 on success or a positive version of an daemon error code.
         */
        if ((suResult == 1) || (suResult == 255)) {
            return ERROR_ROOT_REQUIRED;
        } else if (suResult != 0) {
            int errorCode = (-suResult);
            switch (errorCode) {
            case ERROR_NODEV:
                // If Android reports that a Bluetooth device is enabled (the daemon wouldn't
                // start otherwise) but the daemon can't access it, it is a sign of an
                // incompatible Android version.
                return ERROR_INCOMPATIBLE;
            default:
                return errorCode;
            }
        } else {
            return 0;
        }
    }

    private int connectToDaemon() {
        mLocalSocket = new LocalSocket();
        LocalSocketAddress endpoint = new LocalSocketAddress(IPC_UNIXDOMAIN_NAME);
        try {
            mLocalSocket.connect(endpoint);
        } catch (IOException e) {
            Log.e(TAG, "connect with daemon IPC failed", e);
            return ERROR_IPC;
        }

        try {
            mOutStream = new DataOutputStream(mLocalSocket.getOutputStream());
        } catch (IOException e) {
            Log.e(TAG, "get IPC output stream failed", e);
            return ERROR_IPC;
        }

        try {
            mCallbackReceiver = new DaemonCallbackReceiver(this, mLocalSocket.getInputStream());
        } catch (IOException e) {
            Log.e(TAG, "get IPC input stream failed", e);
            return ERROR_IPC;
        }
        mCallbackReceiver.start();

        return 0;
    }

    private void stopCallbackReceiver() {
        if (mCallbackReceiver != null) {
            mCallbackReceiver.interrupt();
            mCallbackReceiver = null;
        }
    }

    private void closeDaemonConnection() {
        stopCallbackReceiver();

        if (mOutStream != null) {
            try {
                mOutStream.close();
            } catch (IOException e) {
                Log.w(TAG, "close IPC output stream failed", e);
            }
            mOutStream = null;
        }

        if (mLocalSocket != null) {
            try {
                mLocalSocket.close();
            } catch (IOException e) {
                Log.w(TAG, "close daemon IPC connection failed", e);
            }
            mLocalSocket = null;
        }
    }

    private void startDaemon() {
        if (V) Log.v(TAG, "DaemonService.startDaemon()");

        if (isRunning()) {
            return;
        }

        int errorCode = 0;

        if (!checkLibraries()) {
            errorCode = ERROR_INCOMPATIBLE;
        }

        if ((errorCode == 0) && ((mBtAdapter == null) || !mBtAdapter.isEnabled())) {
            errorCode = ERROR_BT_REQUIRED;
        }

        if (errorCode == 0) {
            errorCode = installDaemonBinary();
        }

        if (errorCode == 0) {
            errorCode = startDaemonProcess();
        }

        // Only connect if the daemon could be started or if it is already running
        if ((errorCode == 0) || (errorCode == ERROR_ADDRINUSE)) {
            errorCode = connectToDaemon();
        }

        if (errorCode == 0) {
            setState(STATE_STARTED, 0);
        } else {
            stopDaemon(errorCode);
        }
    }

    private void stopDaemon(int errorCode) {
        if (V) Log.v(TAG, "DaemonService.stopDaemon()");

        setState(STATE_STOPPED, errorCode);
        onHidDisconnected(0);

        closeDaemonConnection();
    }

    private boolean sendSimpleIpcCmd(int cmd) {
        try {
            mOutStream.writeInt(cmd);
            mOutStream.flush();
        } catch (IOException e) {
            Log.e(TAG, "send daemon IPC command failed", e);
            stopDaemon(ERROR_IPC);
            return false;
        }

        return true;
    }

    private void shutdownDaemon(int errorCode) {
        // Stop the CallbackReceiver before the daemon connection is closed
        stopCallbackReceiver();

        if (isRunning()) {
            sendSimpleIpcCmd(IPC_CMD_SHUTDOWN);
        }

        stopDaemon(errorCode);
    }

    /** Activates/Deactivates the discoverable mode of the Bluetooth adapter. */
    public void setDiscoverable(boolean discoverable) {
        if (isRunning()) {
            if (discoverable) {
                sendSimpleIpcCmd(IPC_CMD_DISCOVERABLE_ON);
            } else {
                sendSimpleIpcCmd(IPC_CMD_DISCOVERABLE_OFF);
            }
        }
    }

    /** Changes the Bluetooth adapter Device Class to a HID Device Class. */
    public void setHidDeviceClass() {
        if (isRunning()) {
            sendSimpleIpcCmd(IPC_CMD_SET_HID_DEVICE_CLASS);
        }
    }

    /** Restores the original Device Class of the Bluetooth adapter. */
    public void resetDeviceClass() {
        if (isRunning()) {
            sendSimpleIpcCmd(IPC_CMD_RESET_DEVICE_CLASS);
        }
    }

    /** Deactivates all Service Records except for the HID Service Record. */
    public void deactivateOtherServices() {
        if (isRunning()) {
            sendSimpleIpcCmd(IPC_CMD_DEACTIVATE_OTHER_SERVICES);
        }
    }

    /** Reactivates all previously deactivated Service Records. */
    public void reactivateOtherServices() {
        if (isRunning()) {
            sendSimpleIpcCmd(IPC_CMD_REACTIVATE_OTHER_SERVICES);
        }
    }

    /** Initiates a connection to a HID host. */
    public void connectHid(String btAddress) {
        if (isRunning()) {
            byte[] asciiText = btAddress.getBytes(Charset.forName("US-ASCII"));
            if (asciiText.length != 17) {
                Log.e(TAG, String.format("invalid Bluetooth address \"%1$s\"", btAddress));
                stopDaemon(ERROR_IPC);
                return;
            }

            try {
                mOutStream.writeInt(IPC_CMD_HID_CONNECT);
                mOutStream.write(asciiText);
                mOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "send daemon IPC command 'HID connect' failed", e);
                stopDaemon(ERROR_IPC);
                return;
            }

            onHidConnecting(btAddress);
        }
    }

    /** Disconnects a HID connection. */
    public void disconnectHid() {
        if (isRunning()) {
            if (sendSimpleIpcCmd(IPC_CMD_HID_DISCONNECT)) {
                if (getHidState() != HID_STATE_DISCONNECTED) {
                    onHidDisconnecting();
                }
            }
        }
    }

    /** Sends a Keyboard HID Report to the host. */
    public void sendKeyboardReport(int modifier, int keycodes[]) {
        if (isRunning()) {
            try {
                mOutStream.writeInt(IPC_CMD_HID_SEND_KEYS);
                mOutStream.writeByte(modifier);

                if (keycodes == null) {
                    for (int i = 0; i < 6; i++) {
                        mOutStream.writeByte(0);
                    }
                } else if (keycodes.length <= 6) {
                    for (int i = 0; i < keycodes.length; i++) {
                        mOutStream.writeByte(keycodes[i]);
                    }
                    for (int i = 0; i < 6 - keycodes.length; i++) {
                        mOutStream.writeByte(0);
                    }
                }
                else {
                    // send ErrorRollOver
                    for (int i = 0; i < 6; i++) {
                        mOutStream.writeByte(1);
                    }
                }

                mOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "send daemon IPC command 'HID Keyboard Report' failed", e);
                stopDaemon(ERROR_IPC);
            }
        }
    }

    private int limitIntValue(int value, int min, int max) {
        if (value > max) {
            return max;
        } else if (value < min) {
            return min;
        } else {
            return value;
        }
    }

    /** Sends a Mouse HID Report to the host. */
    public void sendMouseReport(int buttons, int x, int y, int scrollY, int scrollX) {
        if (isRunning()) {
            try {
                mOutStream.writeInt(IPC_CMD_HID_SEND_MOUSE);
                mOutStream.writeByte(buttons);
                mOutStream.writeShort(limitIntValue(x, -2047, 2047));
                mOutStream.writeShort(limitIntValue(y, -2047, 2047));
                mOutStream.writeByte(limitIntValue(scrollY, -127, 127));
                mOutStream.writeByte(limitIntValue(scrollX, -127, 127));
                mOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "send daemon IPC command 'HID Mouse Report' failed", e);
                stopDaemon(ERROR_IPC);
            }
        }
    }

    /** Sends a System Keys HID Report to the host. */
    public void sendSystemKeyReport(int keys) {
        if (isRunning()) {
            try {
                mOutStream.writeInt(IPC_CMD_HID_SEND_SYSTEM_KEYS);
                mOutStream.writeByte(keys);
                mOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "send daemon IPC command 'HID System Key Report' failed", e);
                stopDaemon(ERROR_IPC);
            }
        }
    }

    /** Sends a Hardware Keys HID Report to the host. */
    public void sendHardwareKeyReport(int keys) {
        if (isRunning()) {
            try {
                mOutStream.writeInt(IPC_CMD_HID_SEND_HW_KEYS);
                mOutStream.writeByte(keys);
                mOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "send daemon IPC command 'HID Hardware Key Report' failed", e);
                stopDaemon(ERROR_IPC);
            }
        }
    }

    /** Sends a Media Keys HID Report to the host. */
    public void sendMediaKeyReport(int keys) {
        if (isRunning()) {
            try {
                mOutStream.writeInt(IPC_CMD_HID_SEND_MEDIA_KEYS);
                mOutStream.writeByte(keys);
                mOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "send daemon IPC command 'HID Media Key Report' failed", e);
                stopDaemon(ERROR_IPC);
            }
        }
    }

    /** Sends a Application Control Keys HID Report to the host. */
    public void sendAppCtrlKeyReport(int keys) {
        if (isRunning()) {
            try {
                mOutStream.writeInt(IPC_CMD_HID_SEND_AC_KEYS);
                mOutStream.writeByte(keys);
                mOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "send daemon IPC command 'HID Application Control Key Report' failed", e);
                stopDaemon(ERROR_IPC);
            }
        }
    }

    /** Change the Mouse Feature Report. */
    private void changeMouseFeature(boolean isSmoothScrollYOn, boolean isSmoothScrollXOn) {
        if (isRunning()) {
            try {
                mOutStream.writeInt(IPC_CMD_HID_CHANGE_MOUSE_FEATURE);
                mOutStream.writeBoolean(isSmoothScrollYOn);
                mOutStream.writeBoolean(isSmoothScrollXOn);
                mOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "send daemon IPC command 'Change Mouse Feature Report' failed", e);
                stopDaemon(ERROR_IPC);
            }
        }
    }

    /** Sends a Mouse (Absolute) HID Report to the host. */
    public void sendMouseAbsoluteReport(int buttons, int x, int y) {
        if (isRunning()) {
            try {
                mOutStream.writeInt(IPC_CMD_HID_SEND_MOUSE_ABSOLUTE);
                mOutStream.writeByte(buttons);
                mOutStream.writeShort(limitIntValue(x, 0, 2047));
                mOutStream.writeShort(limitIntValue(y, 0, 2047));
                mOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "send daemon IPC command 'HID Mouse (Absolute) Report' failed", e);
                stopDaemon(ERROR_IPC);
            }
        }
    }
}
