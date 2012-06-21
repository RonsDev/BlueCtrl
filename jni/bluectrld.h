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

#ifndef __BLUECTRLD_H
#define __BLUECTRLD_H

#include <bluetooth/bluetooth.h>


/*
 * Get the device ID of the Bluetooth adapter which is used by the daemon.
 *
 * Returns:
 *     The Bluetooth adapter device ID.
 */
int hidc_get_app_dev_id();

/*
 * Get the Bluetooth address of the Bluetooth adapter which is used by the
 * daemon.
 *
 * Returns:
 *     The Bluetooth address.
 */
bdaddr_t *hidc_get_app_dev_bdaddr();


/*
 * Shutdown the daemon.
 */
void hidc_shutdown();

#endif
