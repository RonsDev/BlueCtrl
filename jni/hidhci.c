/*
 *  Host/Controller Interface (HCI) specific functions
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
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>

#include "error.h"
#include "log.h"
#include "hidsdp.h"
#include "bluectrld.h"
#include "hidhci.h"


/*
 * Bluetooth Device Class: Peripheral, Keyboard
 */
static const uint32_t DC_PERI_KEYBOARD = 0x00000500 | HIDC_MDC_KEYBOARD;

static int was_discoverable_set = 0;

/*
 * Original Bluetooth adapter Device Class
 */
static uint32_t org_device_class = 0;


/*
 * Open a connection to the HCI device.
 *
 * Returns:
 *     0 on success or a negative error code (defined in error.h) on failure.
 */
static int open_hci_dev()
{
	int errsv;  /* saved errno */
	int dd;

	if ((dd = hci_open_dev(hidc_get_app_dev_id())) < 0) {
		errsv = errno;
		log_ec(errsv, "Can't open HCI device");
		return hidc_convert_errno(errsv);
	}

	return dd;
}

/*
 * Close the connection to the HCI device.
 *
 * Parameters:
 *     dd: A opened HCI device.
 *
 * Returns:
 *     0 on success or a negative error code (defined in error.h) on failure.
 */
static int close_hci_dev(int dd)
{
	int errsv;  /* saved errno */

	if (hci_close_dev(dd) < 0) {
		errsv = errno;
		log_ec(errsv, "Can't close HCI device");
		return hidc_convert_errno(errsv);
	}

	return 0;
}

/*
 * Set the HCI device Scan Mode.
 *
 * Parameters:
 *     dd: A opened  HCI device.
 *     mode: The new Scan Mode (SCAN_* constants inside bluetooth/hci.h).
 *
 * Returns:
 *     0 on success or a negative error code (defined in error.h) on failure.
 */
static int set_scan_mode(int dd, uint32_t mode)
{
	int errsv;  /* saved errno */
	struct hci_dev_req dr;

	dr.dev_id  = hidc_get_app_dev_id();
	dr.dev_opt = mode;

	if (ioctl(dd, HCISETSCAN, (unsigned long)&dr) < 0) {
		errsv = errno;
		log_ec(errsv, "Can't set HCI Scan Mode");
		return hidc_convert_errno(errsv);
	}

	return 0;
}

/*
 * Get the current Bluetooth adapter Device Class.
 *
 * Parameters:
 *     dd: A opened HCI device.
 *     cls: The result.
 *
 * Returns:
 *     0 on success or a negative error code (defined in error.h) on failure.
 */
static int get_device_class(int dd, uint32_t *cls)
{
	int errsv;  /* saved errno */
	uint8_t cur_cls[3];  /* current Device Class */
	uint32_t result;

	if (hci_read_class_of_dev(dd, cur_cls, 1000) < 0) {
		errsv = errno;
		log_ec(errsv, "Can't read HCI Device Class");
		return hidc_convert_errno(errsv);
	}

	result = 0;
	result |= (uint32_t)cur_cls[0];
	result |= (uint32_t)cur_cls[1] << 8;
	result |= (uint32_t)cur_cls[2] << 16;

	*cls = result;

	return 0;
}

/*
 * Set the Bluetooth adapter Device Class.
 *
 * Parameters:
 *     dd: A opened HCI device.
 *     cls: The new Device Class value.
 *
 * Returns:
 *     0 on success or a negative error code (defined in error.h) on failure.
 */
static int set_device_class(int dd, uint32_t cls)
{
	int errsv;  /* saved errno */

	if (hci_write_class_of_dev(dd, cls, 2000) < 0) {
		errsv = errno;
		log_ec(errsv, "Can't write HCI Device Class");
		return hidc_convert_errno(errsv);
	}

	return 0;
}


int hidc_get_device_bdaddr(int device_id, bdaddr_t *device_bdaddr)
{
	int errsv;  /* saved errno */

	if (hci_devba(device_id, device_bdaddr) < 0) {
		errsv = errno;
		log_ec(errsv,
			"Can't get address for device ID '%d'",
			device_id);
		return hidc_convert_errno(errsv);
	}

	return 0;
}

int hidc_set_discoverable(int is_discoverable)
{
	int ec;  /* error code */
	int dd;
	uint32_t mode;

	if (is_discoverable)
		mode = SCAN_PAGE | SCAN_INQUIRY;
	else
		mode = SCAN_PAGE;

	if ((dd = open_hci_dev()) < 0)
		return dd;

	if ((ec = set_scan_mode(dd, mode)) < 0) {
		close_hci_dev(dd);
		return ec;
	}

	was_discoverable_set = is_discoverable;

	if ((ec = close_hci_dev(dd)) < 0)
		return ec;

	return 0;
}

int hidc_reset_discoverable()
{
	if (was_discoverable_set) {
		return hidc_set_discoverable(0);
	}
	return 0;
}

int hidc_get_org_device_class()
{
	return org_device_class;
}

int hidc_set_hid_device_class()
{
	int ec;  /* error code */
	int dd;
	uint32_t cur_cls;  /* current Device Class */
	uint32_t new_cls;  /* new Device Class */

	if ((dd = open_hci_dev()) < 0)
		return dd;

	if ((ec = get_device_class(dd, &cur_cls)) < 0) {
		close_hci_dev(dd);
		return ec;
	}

	if (org_device_class == 0)
		org_device_class = cur_cls;

	/* change only the last 12 bits */
	new_cls = cur_cls & 0xfffff000;
	new_cls |= DC_PERI_KEYBOARD;

	if (new_cls != cur_cls) {
		if ((ec = set_device_class(dd, new_cls)) < 0) {
			close_hci_dev(dd);
			return ec;
		}
	}

	if ((ec = close_hci_dev(dd)) < 0)
		return ec;

	return 0;
}

int hidc_reset_device_class()
{
	int ec;  /* error code */
	int dd;
	uint32_t cur_cls;  /* current Device Class */

	if (org_device_class == 0)
		return 0;

	if ((dd = open_hci_dev()) < 0)
		return dd;

	if ((ec = get_device_class(dd, &cur_cls)) < 0) {
		close_hci_dev(dd);
		return ec;
	}

	if (cur_cls != org_device_class) {
		if ((ec = set_device_class(dd, org_device_class)) < 0) {
			close_hci_dev(dd);
			return ec;
		}
	}

	if ((ec = close_hci_dev(dd)) < 0)
		return ec;

	org_device_class = 0;

	return 0;
}

void hidc_wait_for_empty_service_class(int timeout)
{
	int dd;
	uint32_t cur_cls = 0;  /* current Device Class */
	time_t timeout_end = time(NULL) + timeout;

	log_d("start waiting for empty service class");

	if ((dd = open_hci_dev()) < 0)
		return;

	get_device_class(dd, &cur_cls);
	while (((cur_cls & 0x00fff000) != 0) && (time(NULL) <= timeout_end)) {
		usleep(10 * 1000);
		get_device_class(dd, &cur_cls);
	}

	close_hci_dev(dd);

	log_d("stop waiting for empty service class");
}
