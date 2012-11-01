/*
 *  Logical Link Control and Adaptation Protocol (L2CAP) communication for
 *  Human Interface Devices (HID)
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

#ifndef __HIDL2CAP_H
#define __HIDL2CAP_H

#include <bluetooth/bluetooth.h>
#include <sys/poll.h>


/*
 * Bluetooth HID Profile PSM numbers
 */
#define	L2CAP_PSM_HIDP_CTRL     0x11
#define	L2CAP_PSM_HIDP_INTR     0x13


/*
 * Bluetooth HID Transaction Header Types (BTTHT) and Parameters (BTTHP).
 * See chapter 7.3 in the "Bluetooth HID Profile" documentation.
 */
#define	BTTHT_HANDSHAKE                 0x00
#define	BTTHP_HANDSHAKE_SUCCESS         0x00
#define	BTTHP_HANDSHAKE_NOT_READY       0x01
#define	BTTHP_HANDSHAKE_ERR_INV_REPID   0x02
#define	BTTHP_HANDSHAKE_ERR_UNSUPPORTED 0x03
#define	BTTHP_HANDSHAKE_ERR_INV_PARAM   0x04
#define	BTTHP_HANDSHAKE_ERR_UNKNOWN     0x0e
#define	BTTHP_HANDSHAKE_ERR_FATAL       0x0f

#define	BTTHT_HID_CTRL                  0x10
#define	BTTHP_HID_CTRL_NOP              0x00
#define	BTTHP_HID_CTRL_HARD_RESET       0x01
#define	BTTHP_HID_CTRL_SOFT_RESET       0x02
#define	BTTHP_HID_CTRL_SUSPEND          0x03
#define	BTTHP_HID_CTRL_EXIT_SUSPEND     0x04
#define	BTTHP_HID_CTRL_VC_UNPLUG        0x05

#define	BTTHT_GET_REPORT                0x40
#define	BTTHP_GET_REPORT_INPUT          0x01
#define	BTTHP_GET_REPORT_OUTPUT         0x02
#define	BTTHP_GET_REPORT_FEATURE        0x03
#define	BTTHP_GET_REPORT_INPUT_BUFF     0x09
#define	BTTHP_GET_REPORT_OUTPUT_BUFF    0x0a
#define	BTTHP_GET_REPORT_FEATURE_BUFF   0x0b

#define	BTTHT_SET_REPORT                0x50
#define	BTTHP_SET_REPORT_INPUT          0x01
#define	BTTHP_SET_REPORT_OUTPUT         0x02
#define	BTTHP_SET_REPORT_FEATURE        0x03

#define	BTTHT_GET_PROTOCOL              0x60

#define	BTTHT_SET_PROTOCOL              0x70
#define	BTTHP_SET_PROTOCOL_BOOT         0x00
#define	BTTHP_SET_PROTOCOL_REPORT       0x01

#define	BTTHT_GET_IDLE                  0x80

#define	BTTHT_SET_IDLE                  0x90

#define	BTTHT_DATA                      0xa0
#define	BTTHP_DATA_OTHER                0x00
#define	BTTHP_DATA_INPUT                0x01
#define	BTTHP_DATA_OUTPUT               0x02
#define	BTTHP_DATA_FEATURE              0x03

#define	BTTHT_DATAC                     0xb0
#define	BTTHP_DATAC_OTHER               0x00
#define	BTTHP_DATAC_INPUT               0x01
#define	BTTHP_DATAC_OUTPUT              0x02
#define	BTTHP_DATAC_FEATURE             0x03


/*
 * Start the HID server so that HID hosts can initiate a connection.
 *
 * Returns:
 *     0 on success or a negative error code (defined in error.h) on failure.
 */
int hidc_start_hid_server();

/*
 * Stop the HID server.
 */
void hidc_stop_hid_server();

/*
 * Get the current HID server state.
 *
 * Returns:
 *     True if the server is running; False if not.
 */
int hidc_is_hid_server_running();


/*
 * Initiate a connection to the specified HID host.
 *
 * Parameters:
 *     dst_addr: The destination Bluetooth address of the host.
 *
 * Returns:
 *     0 on success or a negative error code (defined in error.h) on failure.
 */
int hidc_connect_hid(bdaddr_t *dst_addr);

/*
 * Disconnect a active HID connection.
 */
void hidc_disconnect_hid();

/*
 * Get the current HID connection state.
 *
 * Returns:
 *     True if a HID connection is open; False if not.
 */
int hidc_is_hid_connected();

/*
 * Get the Bluetooth address from the last or now connected HID host.
 *
 * Parameters:
 *     bdaddr: The result.
 */
void get_last_connected_bdaddr(bdaddr_t *bdaddr);


/*
 * Send a Keyboard HID Report to the host.
 *
 * Parameters:
 *     modifiers: A bitmask with the pressed modifier keys:
 *                Bit 0 = Left Ctrl
 *                Bit 1 = Left Shift
 *                Bit 2 = Left Alt
 *                Bit 3 = Left GUI
 *                Bit 4 = Right Ctrl
 *                Bit 5 = Right Shift
 *                Bit 6 = Right Alt
 *                Bit 7 = Right GUI
 *     keycodes: An array of 6 bytes where each byte represents a pressed key.
 *               See chapter 10 in the "USB HID Usage Tables" documentation.
 */
void hidc_send_hid_report_keys(unsigned char modifiers,
	const unsigned char *keycodes);

/*
 * Send a System Keys HID Report to the host.
 *
 * Parameters:
 *     keys: A bitmask with the pressed keys:
 *           Bit 0 = Power
 *           Bit 1 = Sleep
 *           Bit 2 = Reserved
 *           Bit 3 = Reserved
 *           Bit 4 = Reserved
 *           Bit 5 = Reserved
 *           Bit 6 = Reserved
 *           Bit 7 = Reserved
 */
void hidc_send_hid_report_system_keys(unsigned char keys);

/*
 * Send a Hardware Keys HID Report to the host.
 *
 * Parameters:
 *     keys: A bitmask with the pressed keys:
 *           Bit 0 = Reserved
 *           Bit 1 = Reserved
 *           Bit 2 = Reserved
 *           Bit 3 = Eject
 *           Bit 4 = Reserved
 *           Bit 5 = Reserved
 *           Bit 6 = Reserved
 *           Bit 7 = Reserved
 */
void hidc_send_hid_report_hw_keys(unsigned char keys);

/*
 * Send a Media Keys HID Report to the host.
 *
 * Parameters:
 *     keys: A bitmask with the pressed keys:
 *           Bit 0 = Play/Pause
 *           Bit 1 = Forward
 *           Bit 2 = Rewind
 *           Bit 3 = Scan Next Track
 *           Bit 4 = Scan Previous Track
 *           Bit 5 = Mute
 *           Bit 6 = Volume Increment
 *           Bit 7 = Volume Decrement
 */
void hidc_send_hid_report_media_keys(unsigned char keys);

/*
 * Send a Application Control Keys HID Report to the host.
 *
 * Parameters:
 *     keys: A bitmask with the pressed keys:
 *           Bit 0 = Home
 *           Bit 1 = Back
 *           Bit 2 = Forward
 *           Bit 3 = Reserved
 *           Bit 4 = Reserved
 *           Bit 5 = Reserved
 *           Bit 6 = Reserved
 *           Bit 7 = Reserved
 */
void hidc_send_hid_report_ac_keys(unsigned char keys);

/*
 * Send a Mouse HID Report to the host.
 *
 * Parameters:
 *     buttons: A bitmask with the pressed Mouse buttons.
 *     x: Relative left to right movement of the Mouse (values between -2047
 *        and +2047 are allowed)
 *     y: Relative far to near movement of the Mouse (values between -2047
 *        and +2047 are allowed)
 *     scrollY: Relative movement of the vertical Scroll Wheel (values between
 *              -127 and +127 are allowed)
 *     scrollX: Relative movement of the horizontal Scroll Wheel (values
 *              between -127 and +127 are allowed)
 */
void hidc_send_hid_report_mouse(unsigned char buttons, int16_t x, int16_t y,
				signed char scrollY, signed char scrollX);

/*
 * Send a Mouse (Absolute) HID Report to the host.
 *
 * Parameters:
 *     buttons: A bitmask with the pressed Mouse buttons.
 *     x: Absolute X position of the Mouse (values between 0 and 2047 are
 *        allowed)
 *     y: Absolute Y position of the Mouse (values between 0 and 2047 are
 *        allowed)
 */
void hidc_send_hid_report_mouse_abs(unsigned char buttons, uint16_t x,
				uint16_t y);

/*
 * Change the Mouse Feature Report.
 *
 * Parameters:
 *     smooth_scroll_y: Smooth Scrolling for the vertical scroll wheel.
 *     smooth_scroll_x: Smooth Scrolling for the horizontal scroll wheel.
 */
void hidc_change_mouse_feature(int smooth_scroll_y, int smooth_scroll_x);


/*
 * Init the unit specific poll file descriptors for the mainloop.
 *
 * Parameters:
 *     scpollfd: Server control socket.
 *     sipollfd: Server interrupt socket.
 *     ccpollfd: Client control socket.
 *     cipollfd: Client interrupt socket.
 */
void hidc_init_l2cap_pollfds(struct pollfd *scpollfd, struct pollfd *sipollfd,
			struct pollfd *ccpollfd, struct pollfd *cipollfd);

/*
 * Handle the unit specific poll result of the mainloop.
 *
 * Parameters:
 *     scpollfd: Server control socket.
 *     sipollfd: Server interrupt socket.
 *     ccpollfd: Client control socket.
 *     cipollfd: Client interrupt socket.
 */
void hidc_handle_l2cap_poll(struct pollfd *scpollfd, struct pollfd *sipollfd,
			struct pollfd *ccpollfd, struct pollfd *cipollfd);

#endif
