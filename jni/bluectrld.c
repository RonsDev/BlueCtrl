/*
 *  BlueCtrl daemon
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
#include <getopt.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/poll.h>
#include <bluetooth/bluetooth.h>

#include "error.h"
#include "log.h"
#include "hidhci.h"
#include "hidipc.h"
#include "hidl2cap.h"
#include "hidsdp.h"
#include "bluectrld.h"


static const char *optString = "nh?";
static const struct option longOpts[] = {
	{ "devid", required_argument, NULL, 0 },
	{ "hid-device-class", no_argument, NULL, 0 },
	{ "nodaemon", no_argument, NULL, 'n' },
	{ "debug", no_argument, NULL, 0 },
	{ "help", no_argument, NULL, 'h' },
	{ NULL, no_argument, NULL, 0 }
};

static const int IPC_TIMEOUT_SEC = 10;

static volatile sig_atomic_t __io_canceled = 0;

static time_t ipc_timeout_end = 0;

static int app_dev_id = 0;
static bdaddr_t *papp_dev_bdaddr = BDADDR_ANY;


static void usage()
{
	printf("bluectrld - BlueCtrl daemon\n\n");

	printf("Usage:\n"
		"\tbluectrld [options]\n"
		"\n");

	printf("Options:\n"
		"\t-n, --nodaemon       Don't fork daemon to background\n"
		"\t--devid [number]     Use the specified Bluetooth adapter\n"
		"\t--hid-device-class   Use a Bluetooth HID Device Class\n"
		"\t--debug              Activate debug mode\n"
		"\t-h, --help           Display help\n"
		"\n");
}

static void sig_hup(int sig)
{
}

static void sig_term(int sig)
{
	__io_canceled = 1;
}


static int check_should_shutdown()
{
	if (__io_canceled)
		return 1;

	if (hidc_is_ipc_connected()) {
		ipc_timeout_end = 0;
	}
	else if (ipc_timeout_end == 0) {
		ipc_timeout_end = time(NULL) + IPC_TIMEOUT_SEC;
	}
	else if (ipc_timeout_end <= time(NULL))  {
		log_i("IPC timeout shutdown");
		return 1;
	}

	return 0;
}

int hidc_get_app_dev_id()
{
	return app_dev_id;
}

bdaddr_t *hidc_get_app_dev_bdaddr()
{
	return papp_dev_bdaddr;
}

void hidc_shutdown()
{
	__io_canceled = 1;
}


int main(int argc, char **argv)
{
	int result = 0;
	int ec;  /* error code */
	int errsv;  /* saved errno */
	int opt;
	int longIndex;
	int detach = 1;
	int hiddevcls = 0;
	int debug = 0;
	struct sigaction sa;
	struct pollfd ufds[6];

	while ((opt = getopt_long(argc,
				argv,
				optString,
				longOpts,
				&longIndex)) != -1) {
		switch(opt) {
		case 'n':
			detach = 0;
			break;
		case 'h':
			usage();
			exit(0);
		case 0:
			if (strcmp("devid",
					longOpts[longIndex].name) == 0 ) {
				app_dev_id = atoi(optarg);
			}
			else if (strcmp("hid-device-class",
					longOpts[longIndex].name) == 0 ) {
				hiddevcls = 1;
			}
			else if (strcmp("debug",
					longOpts[longIndex].name) == 0 ) {
				debug = 1;
			}
			break;
		default:
			exit(0);
		}
	}

	log_init("bluectrld", detach, debug);
	log_i("BlueCtrl daemon started");

	memset(&sa, 0, sizeof(sa));
	sa.sa_flags = SA_NOCLDSTOP;

	sa.sa_handler = sig_term;
	sigaction(SIGTERM, &sa, NULL);
	sigaction(SIGINT,  &sa, NULL);

	sa.sa_handler = sig_hup;
	sigaction(SIGHUP, &sa, NULL);

	sa.sa_handler = SIG_IGN;
	sigaction(SIGCHLD, &sa, NULL);
	sigaction(SIGPIPE, &sa, NULL);


	if ((result = hidc_get_device_bdaddr(app_dev_id, papp_dev_bdaddr)) < 0)
		goto done;

	if (hidc_start_hid_server() < 0) {
		log_i("Can't start HID server -> HID hosts won't be able to "
			"initiate a connection");
	}

	if (hiddevcls) {
		if ((result = hidc_set_hid_device_class()) < 0) {
			log_e("Can't set Bluetooth Device Class");
			goto done;
		}
	}

	if ((result = hidc_sdp_register()) < 0) {
		log_e("Can't register Service Record");
		goto done;
	}

	if ((result = hidc_start_ipc_server()) < 0) {
		log_e("Can't start IPC server");
		goto done;
	}

	if (detach) {
		if (daemon(0, 0)) {
			errsv = errno;
			log_ec(errsv, "Can't start daemon");
			result = hidc_convert_errno(errsv);
			goto done;
		}
	}

	while (!check_should_shutdown()) {
		hidc_init_ipc_pollfds(&ufds[0], &ufds[1]);
		hidc_init_l2cap_pollfds(&ufds[2], &ufds[3], &ufds[4],
					&ufds[5]);

		if (poll(ufds, 6, IPC_TIMEOUT_SEC * 1000) < 1)
			continue;

		hidc_handle_ipc_poll(&ufds[0], &ufds[1]);
		hidc_handle_l2cap_poll(&ufds[2], &ufds[3], &ufds[4],
					&ufds[5]);
	}

done:
	hidc_close_client_ipc();
	hidc_stop_ipc_server();

	hidc_disconnect_hid();
	hidc_stop_hid_server();

	if ((ec = hidc_reset_discoverable()) < 0) {
		log_e("Can't reset Inquiry Scan Mode");
		if (result == 0)
			result = ec;
	}

	if ((ec = hidc_reactivate_other_services()) < 0) {
		log_e("Can't reactivate Service Records");
		if (result == 0)
			result = ec;
	}

	if ((ec = hidc_reset_device_class()) < 0) {
		log_e("Can't reset Bluetooth Device Class (original class: "
			"0x%06x)", hidc_get_org_device_class());
		if (result == 0)
			result = ec;
	}

	if ((ec = hidc_sdp_unregister()) < 0) {
		log_e("Can't unregister Service Record");
		if (result == 0)
			result = ec;
	}

	log_i("BlueCtrl daemon stopped");

	exit(-result);
}
