/*
 *  Error constants and helper functions
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

#ifndef __ERROR_H
#define __ERROR_H

/* Error Codes */
enum {
	HIDC_EC_UNKNOWN = -10,  /* Unknown error */
	HIDC_EC_INVBDADDR = -20,  /* Invalid Bluetooth address */

	HIDC_EC_ACCES = -51,  /* Permission denied */
	HIDC_EC_PERM = -52,  /* Operation not permitted */
	HIDC_EC_NODEV = -53,  /* No such device */
	HIDC_EC_NOTCONN = -54,  /* The socket is not connected */
	HIDC_EC_NOENT = -55,  /* No such file or directory */
	HIDC_EC_ADDRINUSE = -56,  /* Address already in use */
	HIDC_EC_HOSTDOWN = -57,  /* Host is down */
	HIDC_EC_CONNREFUSED = -58,  /* Connection refused */
	HIDC_EC_TIMEDOUT = -59,  /* Connection timed out */
	HIDC_EC_ALREADY = -60,  /* Connection already in progress */
	HIDC_EC_BADE = -61,  /* Invalid exchange */
	HIDC_EC_CONNRESET = -62,  /* Connection reset by peer */
};


/*
 * Convert an errno.h error number into a project specific error code.
 *
 * Parameters:
 *     en: error number defined in errno.h.
 *
 * Returns:
 *     A project specific error code.
 */
int hidc_convert_errno(int en);

#endif
