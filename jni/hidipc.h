/*
 *  Inter-Process Communication (IPC) functions
 */
/*
 *  Copyright (C) 2012
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

#ifndef __HIDIPC_H
#define __HIDIPC_H

#include <bluetooth/bluetooth.h>
#include <sys/poll.h>


/*
 * The abstract Unix Domain socket address for the IPC communication.
 */
#define HIDC_UNIXDOMAIN_IPC	"org.ronsdev.bluectrld"


/*
 * Possible commands that the client can send to the daemon. All commands are
 * sent as a 4 byte Integer (network byte order). If a command needs
 * additional data it will be mentioned in the command comment.
 */
typedef enum HidcIpcCommand {
	/*
	 * Shutdown the daemon.
	 */
	HIDC_IPC_CMD_SHUTDOWN = 10,
	/*
	 * Activate discoverable mode.
	 */
	HIDC_IPC_CMD_DISCOVERABLE_ON = 20,
	/*
	 * Deactivate discoverable mode.
	 */
	HIDC_IPC_CMD_DISCOVERABLE_OFF = 25,
	/*
	 * Change the Bluetooth adapter Device Class to a HID Device Class.
	 */
	HIDC_IPC_CMD_SET_HID_DEVICE_CLASS = 30,
	/*
	 * Restore the original Device Class of the Bluetooth adapter.
	 */
	HIDC_IPC_CMD_RESET_DEVICE_CLASS = 35,
	/*
	 * Deactivate all Service Records except for the HID Service Record.
	 */
	HIDC_IPC_CMD_DEACTIVATE_OTHER_SERVICES = 40,
	/*
	 * Reactivate all previously deactivated Service Records.
	 */
	HIDC_IPC_CMD_REACTIVATE_OTHER_SERVICES = 45,
	/*
	 * Initiate a connection to a HID host.
	 * Additional data:
	 *     17 bytes: The destination Bluetooth address of the HID host as
	 *               a ANSI String (example "00:11:22:AA:BB:CC")
	 */
	HIDC_IPC_CMD_HID_CONNECT = 90,
	/*
	 * Disconnect a HID connection.
	 */
	HIDC_IPC_CMD_HID_DISCONNECT = 95,
	/*
	 * Send a Keyboard HID Report to the host.
	 * Additional data:
	 *     1 byte : A bitmask with the pressed modifier keys:
	 *              Bit 0 = Left Ctrl
	 *              Bit 1 = Left Shift
	 *              Bit 2 = Left Alt
	 *              Bit 3 = Left GUI
	 *              Bit 4 = Right Ctrl
	 *              Bit 5 = Right Shift
	 *              Bit 6 = Right Alt
	 *              Bit 7 = Right GUI
	 *     6 bytes: An array of 6 bytes where each byte represents a
	 *              pressed key. See chapter 10 in the
	 *              "USB HID Usage Tables" documentation.
	 */
	HIDC_IPC_CMD_HID_SEND_KEYS = 110,
	/*
	 * Send a Mouse HID Report to the host.
	 * Additional data:
	 *     1 byte : A bitmask with the pressed mouse buttons.
	 *     2 bytes: Relative left to right movement of the mouse (values
	 *              between -2047 and +2047 are allowed)
	 *     2 bytes: Relative far to near movement of the mouse (values
	 *              between -2047 and +2047 are allowed)
	 *     1 byte : Relative movement of the vertical Scroll Wheel (values
	 *              between -127 and +127 are allowed)
	 *     1 byte : Relative movement of the horizontal Scroll Wheel
	 *              (values between -127 and +127 are allowed)
	 */
	HIDC_IPC_CMD_HID_SEND_MOUSE = 120,
	/*
	 * Send a System Keys HID Report to the host.
	 * Additional data:
	 *     1 byte : A bitmask with the pressed keys:
	 *              Bit 0 = Power
	 *              Bit 1 = Sleep
	 *              Bit 2 = Reserved
	 *              Bit 3 = Reserved
	 *              Bit 4 = Reserved
	 *              Bit 5 = Reserved
	 *              Bit 6 = Reserved
	 *              Bit 7 = Reserved
	 */
	HIDC_IPC_CMD_HID_SEND_SYSTEM_KEYS = 125,
	/*
	 * Send a Hardware Keys HID Report to the host.
	 * Additional data:
	 *     1 byte : A bitmask with the pressed keys:
	 *              Bit 0 = Reserved
	 *              Bit 1 = Reserved
	 *              Bit 2 = Reserved
	 *              Bit 3 = Eject
	 *              Bit 4 = Reserved
	 *              Bit 5 = Reserved
	 *              Bit 6 = Reserved
	 *              Bit 7 = Reserved
	 */
	HIDC_IPC_CMD_HID_SEND_HW_KEYS = 130,
	/*
	 * Send a Media Keys HID Report to the host.
	 * Additional data:
	 *     1 byte : A bitmask with the pressed keys:
	 *              Bit 0 = Play/Pause
	 *              Bit 1 = Forward
	 *              Bit 2 = Rewind
	 *              Bit 3 = Scan Next Track
	 *              Bit 4 = Scan Previous Track
	 *              Bit 5 = Mute
	 *              Bit 6 = Volume Increment
	 *              Bit 7 = Volume Decrement
	 */
	HIDC_IPC_CMD_HID_SEND_MEDIA_KEYS = 140,
	/*
	 * Send a Application Control Keys HID Report to the host.
	 * Additional data:
	 *     1 byte : A bitmask with the pressed keys:
	 *              Bit 0 = Home
	 *              Bit 1 = Back
	 *              Bit 2 = Forward
	 *              Bit 3 = Reserved
	 *              Bit 4 = Reserved
	 *              Bit 5 = Reserved
	 *              Bit 6 = Reserved
	 *              Bit 7 = Reserved
	 */
	HIDC_IPC_CMD_HID_SEND_AC_KEYS = 145,
	/*
	 * Change the Mouse Feature Report.
	 * Additional data:
	 *     1 byte: Boolean value that defines if Smooth Scrolling is
	 *             active for the vertical scroll wheel.
	 *     1 byte: Boolean value that defines if Smooth Scrolling is
	 *             active for the horizontal scroll wheel.
	 */
	HIDC_IPC_CMD_HID_CHANGE_MOUSE_FEATURE = 150,
	/*
	 * Send a Mouse (Absolute) HID Report to the host.
	 * Additional data:
	 *     1 byte : A bitmask with the pressed mouse buttons.
	 *     2 bytes: Absolute X position of the Mouse (values between 0 and
	 *              2047 are allowed)
	 *     2 bytes: Absolute Y position of the Mouse (values between 0 and
	 *              2047 are allowed)
	 */
	HIDC_IPC_CMD_HID_SEND_MOUSE_ABSOLUTE = 160,
} HidcIpcCommand;

/*
 * Possible callbacks that the daemon can send to the client. All callbacks are
 * sent as a 4 byte Integer (network byte order). If a callbacks sends
 * additional data it will be mentioned in the callback comment.
 */
typedef enum HidcIpcCallback {
	/*
	 * Notification when a HID connection is established.
	 * Additional data:
	 *     17 bytes: The Bluetooth address of the HID host as a ANSI
	 *               String (example "00:11:22:AA:BB:CC").
	 */
	HIDC_IPC_CB_HID_CONNECTED = 1010,
	/*
	 * Notification when a HID connection is closed or lost.
	 * Additional data:
	 *     4 bytes: Integer (network byte order) which contains the
	 *              error code value if the connection was closed because
	 *              of an error or 0 if it is an ordinary disconnect.
	 */
	HIDC_IPC_CB_HID_DISCONNECTED = 1020,
	/*
	 * Information that is sent when the HID server isn't running.
	 */
	HIDC_IPC_CB_INFO_NO_SERVER = 1030,
	/*
	 * Notification when a Mouse Feature Report is received.
	 * Additional data:
	 *     1 byte: Boolean value that defines if Smooth Scrolling is
	 *             active for the vertical scroll wheel.
	 *     1 byte: Boolean value that defines if Smooth Scrolling is
	 *             active for the horizontal scroll wheel.
	 */
	HIDC_IPC_CB_MOUSE_FEATURE = 1050,
} HidcIpcCallback;

/*
 * Possible error callbacks that the daemon can send to the client. All
 * error callbacks are sent as a 4 byte Integer (network byte order) followed
 * by another 4 byte Integer (network byte order) which contains the error
 * code value.
 */
typedef enum HidcIpcErrorCallback {
	/*
	 * Activate discoverable mode failed.
	 */
	HIDC_IPC_ECB_DISCOVERABLE_ON = 2020,
	/*
	 * Deactivate discoverable mode failed.
	 */
	HIDC_IPC_ECB_DISCOVERABLE_OFF = 2025,
	/*
	 * Change the Bluetooth adapter Device Class failed.
	 */
	HIDC_IPC_ECB_SET_HID_DEVICE_CLASS = 2030,
	/*
	 * Restore the original Device Class failed.
	 */
	HIDC_IPC_ECB_RESET_DEVICE_CLASS = 2035,
	/*
	 * Deactivate other Service Records failed.
	 */
	HIDC_IPC_ECB_DEACTIVATE_OTHER_SERVICES = 2040,
	/*
	 * Reactivate other Service Records failed.
	 */
	HIDC_IPC_ECB_REACTIVATE_OTHER_SERVICES = 2045,
	/*
	 * Initiate a connection to a HID host failed.
	 */
	HIDC_IPC_ECB_HID_CONNECT = 2090,
} HidcIpcErrorCallback;


/*
 * Start the IPC server.
 *
 * Returns:
 *     0 on success or a negative error code (defined in error.h) on failure.
 */
int hidc_start_ipc_server();

/*
 * Stop the IPC server.
 */
void hidc_stop_ipc_server();

/*
 * Close the client IPC connection.
 */
void hidc_close_client_ipc();

/*
 * Get the current IPC connection state.
 *
 * Returns:
 *     True if a IPC client is connected; False if not.
 */
int hidc_is_ipc_connected();


/*
 * Send a HID connected notification to the client.
 *
 * Parameters:
 *     bdaddr: The Bluetooth address of the HID host.
 */
void hidc_send_ipc_cb_connected(bdaddr_t *bdaddr);

/*
 * Send a HID disconnected notification.
 *
 * Parameters:
 *     ec: An error code if the function was called because of an error or 0 if
 *         it is an ordinary disconnect.
 */
void hidc_send_ipc_cb_disconnected(int ec);

/*
 * Send a Mouse Feature Report notification.
 *
 * Parameters:
 *     smoothscrolly: The Smooth Scroll feature value for the vertical scroll
 *                    wheel.
 *     smoothscrollx: The Smooth Scroll feature value for the horizontal scroll
 *                    wheel.
 */
void hidc_send_ipc_cb_mouse_feature(int smoothscrolly, int smoothscrollx);


/*
 * Send a error callback notification to the client.
 *
 * Parameters:
 *     cb: The error callback type.
 *     ec: The error code value.
 */
void hidc_send_ipc_ecb(HidcIpcErrorCallback cb, int ec);


/*
 * Init the unit specific poll file descriptors for the mainloop.
 *
 * Parameters:
 *     spollfd: Server IPC socket.
 *     cpollfd: Client IPC socket.
 */
void hidc_init_ipc_pollfds(struct pollfd *spollfd, struct pollfd *cpollfd);

/*
 * Handle the unit specific poll result of the mainloop.
 *
 * Parameters:
 *     spollfd: Server IPC socket.
 *     cpollfd: Client IPC socket.
 */
void hidc_handle_ipc_poll(struct pollfd *spollfd, struct pollfd *cpollfd);

#endif
