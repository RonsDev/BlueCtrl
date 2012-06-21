/*
 *  Log functions
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

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>

#ifdef __ANDROID__
#include <android/log.h>
#else
#include <syslog.h>
#endif

#include "log.h"


static int is_debug_active = 0;

#ifdef __ANDROID__
static char *log_tag = "";
#endif


void log_init(const char *ident, int detach, int debug)
{
#ifdef __ANDROID__
	log_tag = (char *)ident;
#else
	int option = LOG_NDELAY | LOG_PID;
	if (!detach)
		option |= LOG_PERROR;

	openlog(ident, option, LOG_DAEMON);
#endif

	is_debug_active = debug;
}

int log_is_debug_active()
{
	return is_debug_active;
}

void log_d(const char *format, ...)
{
	if (is_debug_active) {
		va_list ap;

		va_start(ap, format);

#ifdef __ANDROID__
		__android_log_vprint(ANDROID_LOG_DEBUG, log_tag, format, ap);
#else
		vsyslog(LOG_DEBUG, format, ap);
#endif

		va_end(ap);
	}
}

void log_i(const char *format, ...)
{
	va_list ap;

	va_start(ap, format);

#ifdef __ANDROID__
	__android_log_vprint(ANDROID_LOG_INFO, log_tag, format, ap);
#else
	vsyslog(LOG_INFO, format, ap);
#endif

	va_end(ap);
}

void log_e(const char *format, ...)
{
	va_list ap;

	va_start(ap, format);

#ifdef __ANDROID__
	__android_log_vprint(ANDROID_LOG_ERROR, log_tag, format, ap);
#else
	vsyslog(LOG_ERR, format, ap);
#endif

	va_end(ap);
}

void log_ec(int errorcode, const char *format, ...)
{
	size_t msg_size;
	char *msg;
	char *ec_msg;

	va_list ap;

	va_start(ap, format);

	if (errorcode == 0) {
#ifdef __ANDROID__
		__android_log_vprint(ANDROID_LOG_ERROR, log_tag, format, ap);
#else
		vsyslog(LOG_ERR, format, ap);
#endif
	}
	else {
		ec_msg = strerror(errorcode);

		msg_size = strlen(format) + strlen(ec_msg) + 20;
		msg = malloc(msg_size);
		if (!msg) {
			log_e("Can't log error code message: malloc error");
			return;
		}

		snprintf(msg, msg_size, "%s: (%d) %s",
					format, errorcode, ec_msg);

#ifdef __ANDROID__
		__android_log_vprint(ANDROID_LOG_ERROR, log_tag, msg, ap);
#else
		vsyslog(LOG_ERR, msg, ap);
#endif

		free(msg);
	}

	va_end(ap);
}
