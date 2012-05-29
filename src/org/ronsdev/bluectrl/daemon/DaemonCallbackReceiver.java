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

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Background Thread that listens for callback events from the application daemon.
 */
public class DaemonCallbackReceiver extends Thread {

    /** Broadcast Action: Indicates that the Thread has stopped unexpectedly. */
    public static final String ACTION_PREMATURE_TERMINATION =
            "org.ronsdev.bluectrl.daemoncallbackreceiver.action.PREMATURE_TERMINATION";

    /** Broadcast Action: A callback event has been received. */
    public static final String ACTION_CALLBACK =
            "org.ronsdev.bluectrl.daemoncallbackreceiver.action.CALLBACK";


    /** Used as an int extra field in ACTION_CALLBACK intents to indicate the callback type. */
    public static final String EXTRA_TYPE =
            "org.ronsdev.bluectrl.daemoncallbackreceiver.extra.TYPE";

    /**
     * Used as a optional String extra field in ACTION_CALLBACK intents to get the associated
     * Bluetooth address.
     */
    public static final String EXTRA_BTADDRESS =
            "org.ronsdev.bluectrl.daemoncallbackreceiver.extra.BTADDRESS";

    /**
     * Used as a optional boolean extra field in ACTION_CALLBACK intents to get the state of
     * the smooth scroll feature for the vertical scroll wheel if it was an mouse feature callback.
     */
    public static final String EXTRA_SMOOTH_SCROLL_Y =
            "org.ronsdev.bluectrl.daemoncallbackreceiver.extra.SMOOTH_SCROLL_Y";

    /**
     * Used as a optional boolean extra field in ACTION_CALLBACK intents to get the state of
     * the smooth scroll feature for the horizontal scroll wheel if it was an mouse feature
     * callback.
     */
    public static final String EXTRA_SMOOTH_SCROLL_X =
            "org.ronsdev.bluectrl.daemoncallbackreceiver.extra.SMOOTH_SCROLL_X";

    /**
     * Used as an optional int extra field in ACTION_CALLBACK intents to get the error code if it
     * was an error callback.
     */
    public static final String EXTRA_ERROR_CODE =
            "org.ronsdev.bluectrl.daemoncallbackreceiver.extra.ERROR_CODE";


    /*
     * Daemon IPC callbacks. Documented in the "hidipc.h" file.
     */
    public static final int IPC_CB_HID_CONNECTED = 1010;
    public static final int IPC_CB_HID_DISCONNECTED = 1020;
    public static final int IPC_CB_INFO_NO_SERVER = 1030;
    public static final int IPC_CB_MOUSE_FEATURE = 1050;

    /*
     * Daemon IPC error callbacks. Documented in the "hidipc.h" file.
     */
    public static final int IPC_ECB_DISCOVERABLE_ON = 2020;
    public static final int IPC_ECB_DISCOVERABLE_OFF = 2025;
    public static final int IPC_ECB_SET_HID_DEVICE_CLASS = 2030;
    public static final int IPC_ECB_RESET_DEVICE_CLASS = 2035;
    public static final int IPC_ECB_DEACTIVATE_OTHER_SERVICES = 2040;
    public static final int IPC_ECB_REACTIVATE_OTHER_SERVICES = 2045;
    public static final int IPC_ECB_HID_CONNECT = 2090;


    private static final String TAG = "DaemonCallbackReceiver";
    private static final boolean V = false;


    private Context mContext;
    private DataInputStream mInStream;


    public DaemonCallbackReceiver(Context context, InputStream inputstream) {
        mContext = context.getApplicationContext();
        mInStream = new DataInputStream(inputstream);
    }

    private String receiveBtAddress() throws IOException {
        byte[] buffer = new byte[17];

        mInStream.readFully(buffer);

        return new String(buffer, Charset.forName("US-ASCII"));
    }

    public void run() {
        if (V) Log.v(TAG, "DaemonCallbackReceiver thread begin");

        while (!interrupted()) {
            try {
                // This read is blocking without bothering the CPU until a callback is received
                int cbtype = mInStream.readInt();
                int errorCode;

                if (V) Log.v(TAG, String.format("received IPC callback (%d)", cbtype));

                Intent intent = new Intent(ACTION_CALLBACK);
                intent.putExtra(EXTRA_TYPE, cbtype);

                switch (cbtype) {
                case IPC_CB_HID_CONNECTED:
                    String btAddress = receiveBtAddress();
                    intent.putExtra(EXTRA_BTADDRESS, btAddress);
                    break;
                case IPC_CB_HID_DISCONNECTED:
                    errorCode = mInStream.readInt();
                    if (errorCode != 0) {
                        intent.putExtra(EXTRA_ERROR_CODE, errorCode);
                    }
                    break;
                case IPC_CB_INFO_NO_SERVER:
                    break;
                case IPC_CB_MOUSE_FEATURE:
                    boolean smoothScrollY = mInStream.readBoolean();
                    boolean smoothScrollX = mInStream.readBoolean();
                    intent.putExtra(EXTRA_SMOOTH_SCROLL_Y, smoothScrollY);
                    intent.putExtra(EXTRA_SMOOTH_SCROLL_X, smoothScrollX);
                    break;
                case IPC_ECB_DISCOVERABLE_ON:
                case IPC_ECB_DISCOVERABLE_OFF:
                case IPC_ECB_SET_HID_DEVICE_CLASS:
                case IPC_ECB_RESET_DEVICE_CLASS:
                case IPC_ECB_DEACTIVATE_OTHER_SERVICES:
                case IPC_ECB_REACTIVATE_OTHER_SERVICES:
                case IPC_ECB_HID_CONNECT:
                    errorCode = mInStream.readInt();
                    intent.putExtra(EXTRA_ERROR_CODE, errorCode);
                    break;
                default:
                    Log.w(TAG, String.format("unknown daemon IPC callback (%d)", cbtype));
                    break;
                }

                mContext.sendBroadcast(intent);
            } catch (IOException e) {
                // The Exception is expected and ignored if the Thread was interrupted because
                // the daemon might stop faster than this thread
                if (!interrupted()) {
                    Log.e(TAG, "receive daemon IPC callback failed", e);
                    mContext.sendBroadcast(new Intent(ACTION_PREMATURE_TERMINATION));
                }
                break;
            }
        }

        try {
            mInStream.close();
        } catch (IOException e) {
            Log.w(TAG, "close IPC input stream failed", e);
        }

        if (V) Log.v(TAG, "DaemonCallbackReceiver thread end");
    }
}
