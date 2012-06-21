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

#ifndef __LOG_H
#define __LOG_H

/*
 * This function should be called before any other log function.
 *
 * Parameters:
 *     ident: The program identifier.
 *     detach: True if the daemon is running in the background.
 *     detach: True to activate the debug log.
 */
void log_init(const char *ident, int detach, int debug);


/*
 * Get the current debug log state.
 *
 * Returns:
 *     True if the debug log is active; False if not.
 */
int log_is_debug_active();

/*
 * Log a message with the Debug level.
 *
 * Parameters:
 *     format: A format string.
 *     ...: The format string values.
 */
void log_d(const char *format, ...);

/*
 * Log a message with the Info level.
 *
 * Parameters:
 *     format: A format string.
 *     ...: The format string values.
 */
void log_i(const char *format, ...);

/*
 * Log a message with the Error level.
 *
 * Parameters:
 *     format: A format string.
 *     ...: The format string values.
 */
void log_e(const char *format, ...);

/*
 * Log an error code and a message with the Error level.
 *
 * Parameters:
 *     errorcode: A project specific error code.
 *     format: A format string.
 *     ...: The format string values.
 */
void log_ec(int errorcode, const char *format, ...);

#endif
