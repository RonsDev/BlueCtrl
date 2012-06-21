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

#include <errno.h>

#include "error.h"


int hidc_convert_errno(int en)
{
	switch (en) {
	case EACCES:
		return HIDC_EC_ACCES;
	case EPERM:
		return HIDC_EC_PERM;
	case ENODEV:
		return HIDC_EC_NODEV;
	case ENOTCONN:
		return HIDC_EC_NOTCONN;
	case ENOENT:
		return HIDC_EC_NOENT;
	case EADDRINUSE:
		return HIDC_EC_ADDRINUSE;
	case EHOSTDOWN:
		return HIDC_EC_HOSTDOWN;
	case ECONNREFUSED:
		return HIDC_EC_CONNREFUSED;
	case ETIMEDOUT:
		return HIDC_EC_TIMEDOUT;
	case EALREADY:
		return HIDC_EC_ALREADY;
	case EBADE:
		return HIDC_EC_BADE;
	case ECONNRESET:
		return HIDC_EC_CONNRESET;
	default:
		return HIDC_EC_UNKNOWN;
	}
}
