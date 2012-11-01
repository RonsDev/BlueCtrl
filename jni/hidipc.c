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

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/poll.h>
#include <sys/socket.h>
#include <sys/un.h>

#include "error.h"
#include "log.h"
#include "hidhci.h"
#include "hidl2cap.h"
#include "bluectrld.h"
#include "hidipc.h"


/*
 * Server/client IPC sockets.
 */
static int server_ipc_sock = -1;
static int client_ipc_sock = -1;


/*
 * Send IPC data to the client.
 *
 * Parameters:
 *     data: The data to send.
 *     data_size: The size of the data parameter.
 *
 * Returns:
 *     0 on success or -1 on failure.
 */
static int send_ipc_data(const void *data, int data_size)
{
	if (client_ipc_sock < 0)
		return -1;

	if (send(client_ipc_sock, data, data_size, MSG_WAITALL) <= 0) {
		log_ec(errno, "Can't write to IPC socket");
		hidc_close_client_ipc();
		return -1;
	}

	return 0;
}

/*
 * Receive IPC data from the client.
 *
 * Parameters:
 *     buffer: A buffer where the result will be written.
 *     len: The length of the data that should be received.
 *
 * Returns:
 *     0 on success or -1 on failure.
 */
static int receive_ipc_data(void *buffer, int len)
{
	int rsize;  /* received size */

	rsize = recv(client_ipc_sock, buffer, len, MSG_WAITALL);
	if (rsize == len) {
		return 0;
	}
	else {
		if (rsize == 0 || (rsize < 0 && errno == ECONNRESET)) {
			log_d("Remote closed IPC connection");
		}
		else if (rsize < 0) {
			log_ec(errno, "Can't read on IPC socket");
		}
		else if (rsize < len) {
			log_d("Incomplete data received on IPC socket");
		}
		hidc_close_client_ipc();
		return -1;
	}
}

/*
 * Send a simple callback notification to the client.
 *
 * Parameters:
 *     cb: The callback type.
 */
void send_simple_ipc_cb(HidcIpcCallback cb)
{
	int32_t cmd;

	cmd = htonl(cb);
	send_ipc_data(&cmd, sizeof(cmd));
}

/*
 * Called when a "Shutdown" command is received.
 */
static void do_ipc_cmd_shutdown()
{
	log_d("IPC command: shutdown");

	hidc_shutdown();
}

/*
 * Called when a "Activate discoverable mode" command is received.
 */
static void do_ipc_cmd_discoverable_on()
{
	int ec;  /* error code */

	log_d("IPC command: activate Inquiry Scan Mode");

	if ((ec = hidc_set_discoverable(1)) < 0) {
		hidc_send_ipc_ecb(HIDC_IPC_ECB_DISCOVERABLE_ON, ec);
		return;
	}
}

/*
 * Called when a "Deactivate discoverable mode" command is received.
 */
static void do_ipc_cmd_discoverable_off()
{
	int ec;  /* error code */

	log_d("IPC command: deactivate Inquiry Scan Mode");

	if ((ec = hidc_set_discoverable(0)) < 0) {
		hidc_send_ipc_ecb(HIDC_IPC_ECB_DISCOVERABLE_OFF, ec);
		return;
	}
}

/*
 * Called when a "Set HID Device Class" command is received.
 */
static void do_ipc_cmd_set_hid_device_class()
{
	int ec;  /* error code */

	log_d("IPC command: set HID Device Class");

	if ((ec = hidc_set_hid_device_class()) < 0) {
		hidc_send_ipc_ecb(HIDC_IPC_ECB_SET_HID_DEVICE_CLASS, ec);
		return;
	}
}

/*
 * Called when a "Reset Device Class" command is received.
 */
static void do_ipc_cmd_reset_device_class()
{
	int ec;  /* error code */

	log_d("IPC command: reset Device Class");

	if ((ec = hidc_reset_device_class()) < 0) {
		hidc_send_ipc_ecb(HIDC_IPC_ECB_RESET_DEVICE_CLASS, ec);
		return;
	}
}

/*
 * Called when a "Deactivate other Service Records" command is received.
 */
static void do_ipc_cmd_deactivate_other_services()
{
	int ec;  /* error code */

	log_d("IPC command: deactivate other Service Records");

	if ((ec = hidc_deactivate_other_services()) < 0) {
		hidc_send_ipc_ecb(HIDC_IPC_ECB_DEACTIVATE_OTHER_SERVICES, ec);
		return;
	}
}

/*
 * Called when a "Reactivate other Service Records" command is received.
 */
static void do_ipc_cmd_reactivate_other_services()
{
	int ec;  /* error code */

	log_d("IPC command: reactivate other Service Records");

	if ((ec = hidc_reactivate_other_services()) < 0) {
		hidc_send_ipc_ecb(HIDC_IPC_ECB_REACTIVATE_OTHER_SERVICES, ec);
		return;
	}
}

/*
 * Called when a "Connect HID" command is received.
 */
static void do_ipc_cmd_hid_connect()
{
	int ec;  /* error code */
	char str_addr[18];  /* address as a String */
	bdaddr_t dst_addr;  /* destination address */

	log_d("IPC command: connect HID");

	memset(str_addr, 0, sizeof(str_addr));
	if (receive_ipc_data(&str_addr, 17) < 0)
		return;

	if (str2ba(str_addr, &dst_addr) < 0) {
		log_e("Invalid Bluetooth address: %s", str_addr);
		hidc_send_ipc_ecb(HIDC_IPC_ECB_HID_CONNECT, HIDC_EC_INVBDADDR);
		return;
	}

	if ((ec = hidc_connect_hid(&dst_addr)) < 0) {
		hidc_send_ipc_ecb(HIDC_IPC_ECB_HID_CONNECT, ec);
		return;
	}
}

/*
 * Called when a "Disconnect HID" command is received.
 */
static void do_ipc_cmd_hid_disconnect()
{
	log_d("IPC command: disconnect HID");
	hidc_disconnect_hid();
}

/*
 * Called when a "Send Keyboard HID Report" command is received.
 */
static void do_ipc_cmd_hid_send_keys()
{
	unsigned char modifier;
	unsigned char keycodes[6];

	if (receive_ipc_data(&modifier, sizeof(modifier)) < 0)
		return;

	if (receive_ipc_data(keycodes, sizeof(keycodes)) < 0)
		return;

	if (hidc_is_hid_connected())
		hidc_send_hid_report_keys(modifier, keycodes);
}

/*
 * Called when a "Send Mouse HID Report" command is received.
 */
static void do_ipc_cmd_hid_send_mouse()
{
	unsigned char buttons;
	int16_t x;
	int16_t y;
	signed char scrollY;
	signed char scrollX;

	if (receive_ipc_data(&buttons, sizeof(buttons)) < 0)
		return;

	if (receive_ipc_data(&x, sizeof(x)) < 0)
		return;

	x = ntohs(x);

	if (receive_ipc_data(&y, sizeof(y)) < 0)
		return;

	y = ntohs(y);

	if (receive_ipc_data(&scrollY, sizeof(scrollY)) < 0)
		return;

	if (receive_ipc_data(&scrollX, sizeof(scrollX)) < 0)
		return;

	if (hidc_is_hid_connected())
		hidc_send_hid_report_mouse(buttons, x, y, scrollY, scrollX);
}

/*
 * Called when a "Send System Keys HID Report" command is received.
 */
static void do_ipc_cmd_hid_send_system_keys()
{
	unsigned char keys;

	if (receive_ipc_data(&keys, sizeof(keys)) < 0)
		return;

	if (hidc_is_hid_connected())
		hidc_send_hid_report_system_keys(keys);
}

/*
 * Called when a "Send Hardware Keys HID Report" command is received.
 */
static void do_ipc_cmd_hid_send_hw_keys()
{
	unsigned char keys;

	if (receive_ipc_data(&keys, sizeof(keys)) < 0)
		return;

	if (hidc_is_hid_connected())
		hidc_send_hid_report_hw_keys(keys);
}

/*
 * Called when a "Send Media Keys HID Report" command is received.
 */
static void do_ipc_cmd_hid_send_media_keys()
{
	unsigned char keys;

	if (receive_ipc_data(&keys, sizeof(keys)) < 0)
		return;

	if (hidc_is_hid_connected())
		hidc_send_hid_report_media_keys(keys);
}

/*
 * Called when a "Send Application Control Keys HID Report" command is
 * received.
 */
static void do_ipc_cmd_hid_send_ac_keys()
{
	unsigned char keys;

	if (receive_ipc_data(&keys, sizeof(keys)) < 0)
		return;

	if (hidc_is_hid_connected())
		hidc_send_hid_report_ac_keys(keys);
}

/*
 * Called when a "Change Mouse Feature Report" command is received.
 */
static void do_ipc_cmd_hid_change_mouse_feature()
{
	unsigned char smooth_scroll_y;  /* vertical Smooth Scrolling */
	unsigned char smooth_scroll_x;  /* horizontal Smooth Scrolling */

	log_d("IPC command: change Mouse Feature Report");

	if (receive_ipc_data(&smooth_scroll_y, sizeof(smooth_scroll_y)) < 0)
		return;

	if (receive_ipc_data(&smooth_scroll_x, sizeof(smooth_scroll_x)) < 0)
		return;

	if (hidc_is_hid_connected())
		hidc_change_mouse_feature(smooth_scroll_y, smooth_scroll_x);
}

/*
 * Called when a "Send Mouse (Absolute) HID Report" command is received.
 */
static void do_ipc_cmd_hid_send_mouse_abs()
{
	unsigned char buttons;
	uint16_t x;
	uint16_t y;

	if (receive_ipc_data(&buttons, sizeof(buttons)) < 0)
		return;

	if (receive_ipc_data(&x, sizeof(x)) < 0)
		return;

	x = ntohs(x);

	if (receive_ipc_data(&y, sizeof(y)) < 0)
		return;

	y = ntohs(y);

	if (hidc_is_hid_connected())
		hidc_send_hid_report_mouse_abs(buttons, x, y);
}

/*
 * Handle a poll input event on the server IPC socket.
 */
static void pollin_server_ipc_sock()
{
	struct timeval tv;
	bdaddr_t bdaddr;

	client_ipc_sock = accept(server_ipc_sock, NULL, NULL);
	if (client_ipc_sock < 0) {
		log_ec(errno, "Can't accept IPC socket");
		return;
	}

	tv.tv_sec  = 5;  
	tv.tv_usec = 0;
	setsockopt(client_ipc_sock, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));

	log_d("IPC client connected");

	/* send current connection state */
	if (hidc_is_hid_connected()) {
		get_last_connected_bdaddr(&bdaddr);
		hidc_send_ipc_cb_connected(&bdaddr);
	}

	if (!hidc_is_hid_server_running()) {
		send_simple_ipc_cb(HIDC_IPC_CB_INFO_NO_SERVER);
	}
}

/*
 * Handle a poll input event on the client IPC socket.
 */
static void pollin_client_ipc_sock()
{
	int32_t cmd;

	if (receive_ipc_data(&cmd, sizeof(cmd)) < 0)
		return;

	cmd = ntohl(cmd);

	switch ((HidcIpcCommand)cmd) {
	case HIDC_IPC_CMD_SHUTDOWN:
		do_ipc_cmd_shutdown();
		break;
	case HIDC_IPC_CMD_DISCOVERABLE_ON:
		do_ipc_cmd_discoverable_on();
		break;
	case HIDC_IPC_CMD_DISCOVERABLE_OFF:
		do_ipc_cmd_discoverable_off();
		break;
	case HIDC_IPC_CMD_SET_HID_DEVICE_CLASS:
		do_ipc_cmd_set_hid_device_class();
		break;
	case HIDC_IPC_CMD_RESET_DEVICE_CLASS:
		do_ipc_cmd_reset_device_class();
		break;
	case HIDC_IPC_CMD_DEACTIVATE_OTHER_SERVICES:
		do_ipc_cmd_deactivate_other_services();
		break;
	case HIDC_IPC_CMD_REACTIVATE_OTHER_SERVICES:
		do_ipc_cmd_reactivate_other_services();
		break;
	case HIDC_IPC_CMD_HID_CONNECT:
		do_ipc_cmd_hid_connect();
		break;
	case HIDC_IPC_CMD_HID_DISCONNECT:
		do_ipc_cmd_hid_disconnect();
		break;
	case HIDC_IPC_CMD_HID_SEND_KEYS:
		do_ipc_cmd_hid_send_keys();
		break;
	case HIDC_IPC_CMD_HID_SEND_MOUSE:
		do_ipc_cmd_hid_send_mouse();
		break;
	case HIDC_IPC_CMD_HID_SEND_SYSTEM_KEYS:
		do_ipc_cmd_hid_send_system_keys();
		break;
	case HIDC_IPC_CMD_HID_SEND_HW_KEYS:
		do_ipc_cmd_hid_send_hw_keys();
		break;
	case HIDC_IPC_CMD_HID_SEND_MEDIA_KEYS:
		do_ipc_cmd_hid_send_media_keys();
		break;
	case HIDC_IPC_CMD_HID_SEND_AC_KEYS:
		do_ipc_cmd_hid_send_ac_keys();
		break;
	case HIDC_IPC_CMD_HID_CHANGE_MOUSE_FEATURE:
		do_ipc_cmd_hid_change_mouse_feature();
		break;
	case HIDC_IPC_CMD_HID_SEND_MOUSE_ABSOLUTE:
		do_ipc_cmd_hid_send_mouse_abs();
		break;
	}
}

int hidc_start_ipc_server()
{
	int errsv;  /* saved errno */
	struct sockaddr_un unaddr;  /* Unix Domain socket address */
	socklen_t addrlen;
	char unixdomain[] = HIDC_UNIXDOMAIN_IPC;

	if (server_ipc_sock > -1)
		return 0;

	server_ipc_sock = socket(AF_UNIX, SOCK_STREAM, 0);
	if (server_ipc_sock < 0) {
		errsv = errno;
		log_ec(errsv, "Can't create IPC socket");
		return hidc_convert_errno(errsv);
	}

	unaddr.sun_family = AF_UNIX;
	/* abstract namespace starts with '\0' */
	unaddr.sun_path[0] = '\0';
	strncpy(unaddr.sun_path + 1, unixdomain, sizeof(unaddr.sun_path) - 1);

	addrlen = sizeof(unaddr.sun_family) + 1 + strlen(unixdomain);

	if (bind(server_ipc_sock, (struct sockaddr*) &unaddr, addrlen) < 0) {
		errsv = errno;
		log_ec(errsv, "Can't bind IPC socket");
		hidc_stop_ipc_server();
		return hidc_convert_errno(errsv);
	}

	if (listen(server_ipc_sock, 1)) {
		errsv = errno;
		log_ec(errsv, "Can't listen on IPC socket");
		hidc_stop_ipc_server();
		return hidc_convert_errno(errsv);
	}

	log_d("IPC server started");

	return 0;
}

void hidc_stop_ipc_server()
{
	if (server_ipc_sock > -1) {
		close(server_ipc_sock);
		server_ipc_sock = -1;
		log_d("IPC server stopped");
	}
}

void hidc_close_client_ipc()
{
	if (client_ipc_sock > -1) {
		close(client_ipc_sock);
		client_ipc_sock = -1;
		log_d("IPC connection closed");
	}
}

int hidc_is_ipc_connected()
{
	return (client_ipc_sock > -1);
}

void hidc_send_ipc_cb_connected(bdaddr_t *bdaddr)
{
	int32_t cmd;
	char str_addr[18];
	unsigned char data[21];

	memset(str_addr, 0, sizeof(str_addr));
	memset(data, 0, sizeof(data));

	cmd = htonl(HIDC_IPC_CB_HID_CONNECTED);
	memcpy(data, &cmd, 4);

	ba2str(bdaddr, str_addr);
	memcpy(data + 4, str_addr, 17);

	send_ipc_data(data, sizeof(data));
}

void hidc_send_ipc_cb_disconnected(int ec)
{
	int32_t data[2];

	data[0] = htonl(HIDC_IPC_CB_HID_DISCONNECTED);
	data[1] = htonl(ec);
	send_ipc_data(data, sizeof(data));
}

void hidc_send_ipc_cb_mouse_feature(int smoothscrolly, int smoothscrollx)
{
	int32_t cmd;
	unsigned char data[6];

	memset(data, 0, sizeof(data));

	cmd = htonl(HIDC_IPC_CB_MOUSE_FEATURE);
	memcpy(data, &cmd, 4);

	data[4] = (signed char)smoothscrolly;
	data[5] = (signed char)smoothscrollx;

	send_ipc_data(data, sizeof(data));
}

void hidc_send_ipc_ecb(HidcIpcErrorCallback cb, int ec)
{
	int32_t data[2];

	data[0] = htonl(cb);
	data[1] = htonl(ec);
	send_ipc_data(data, sizeof(data));
}

void hidc_init_ipc_pollfds(struct pollfd *spollfd, struct pollfd *cpollfd)
{
	spollfd->fd = server_ipc_sock;
	spollfd->events = POLLIN;
	spollfd->revents = 0;

	cpollfd->fd = client_ipc_sock;
	cpollfd->events = POLLIN | POLLERR | POLLHUP;
	cpollfd->revents = 0;
}

void hidc_handle_ipc_poll(struct pollfd *spollfd, struct pollfd *cpollfd)
{
	if (spollfd->revents & POLLIN)
		pollin_server_ipc_sock();


	if ((cpollfd->revents & POLLIN) && (client_ipc_sock > -1)) {
		pollin_client_ipc_sock();
	}
	if ((cpollfd->revents & POLLERR) && (client_ipc_sock > -1)) {
		log_e("Error on IPC socket");
		hidc_close_client_ipc();
	}
	if ((cpollfd->revents & POLLHUP) && (client_ipc_sock > -1)) {
		log_d("Remote closed IPC connection");
		hidc_close_client_ipc();
	}
}
