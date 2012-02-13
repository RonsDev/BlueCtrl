/*
 *  Service Discovery Protocol (SDP) registration of Human Interface
 *  Devices (HID)
 */
/*
 *  Copyright (C) 2012
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

#ifndef __HIDSDP_H
#define __HIDSDP_H

/*
 * Minor Device Classes
 */
#define	HIDC_MDC_KEYBOARD               0x40
#define	HIDC_MDC_POINTER                0x80
#define	HIDC_MDC_COMBO_KEY_POINT        0xc0


/*
 * Register the HID Service Record. The Service Record describes the
 * capabilities and properties of the HID.
 *
 * Returns:
 *     0 on success or a negative error code (defined in error.h) on failure.
 */
int hidc_sdp_register();

/*
 * Unregister the HID Service Record.
 *
 * Returns:
 *     0 on success or a negative error code (defined in error.h) on failure.
 */
int hidc_sdp_unregister();

#endif
