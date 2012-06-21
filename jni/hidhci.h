/*
 *  Host/Controller Interface (HCI) functions
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

#ifndef __HIDHCI_H
#define __HIDHCI_H

#include <bluetooth/bluetooth.h>


/*
 * Get the Bluetooth address for the given device ID.
 *
 * Parameters:
 *     device_id: The Bluetooth device ID.
 *     device_bdaddr: The result.
 *
 * Returns:
 *     0 on success or a negative error code (defined in error.h) on failure.
 */
int hidc_get_device_bdaddr(int device_id, bdaddr_t *device_bdaddr);


/*
 * Activate or deactivate the visibility of the Bluetooth adapter. If it is
 * visible it can be seen by other Bluetooth devices so they can pair.
 *
 * Parameters:
 *     is_discoverable: 1=activate; 0=deactivate
 *
 * Returns:
 *     0 on success or a negative error code (defined in error.h) on failure.
 */
int hidc_set_discoverable(int is_discoverable);

/*
 * Deactivate the visibility of the Bluetooth adapter if it was activated
 * with the hidc_set_discoverable function.
 *
 * Returns:
 *     0 on success or a negative error code (defined in error.h) on failure.
 */
int hidc_reset_discoverable();


/*
 * Return the original Bluetooth adapter Device Class if it was changed or
 * 0 if unchanged.
 *
 * Returns:
 *     The original Device Class or 0 if unchanged.
 */
int hidc_get_org_device_class();

/*
 * Change the Bluetooth adapter Device Class to a HID Device Class.
 *
 * The Device Class is used by hosts to filter for specific devices. It is not
 * as important as the SDP record. According to the documentation a HID Device
 * Class isn't required, but some Bluetooth stacks won't accept the HID
 * if it is not defined.
 *
 * The Device Class will be changed to "Peripheral, Keyboard" which seems to
 * have the best compatibility (iOS requires it); although the Device Class
 * "Peripheral, Combo Keyboard/Pointing device" would be more correct.
 *
 * Returns:
 *     0 on success or a negative error code (defined in error.h) on failure.
 */
int hidc_set_hid_device_class();

/*
 * Restore the original Device Class of the Bluetooth adapter.
 *
 * Returns:
 *     0 on success or a negative error code (defined in error.h) on failure.
 */
int hidc_reset_device_class();

/*
 * Wait until the Service part of the Bluetooth adapter Class is empty.
 *
 * Parameters:
 *     timeout: Maximum wait time in seconds.
 */
void hidc_wait_for_empty_service_class(int timeout);

#endif
