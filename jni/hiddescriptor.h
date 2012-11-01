/*
 *  Human Interface Device (HID) Report Descriptor definitions
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

#ifndef __HIDDESCRIPTOR_H
#define __HIDDESCRIPTOR_H

#include <stdint.h>


/*
 * Report IDs
 */
#define	HIDC_REPORTID_KEYBOARD          0x01
#define	HIDC_REPORTID_SYSTEM_KEYS       0x10
#define	HIDC_REPORTID_HW_KEYS           0x11
#define	HIDC_REPORTID_MEDIA_KEYS        0x12
#define	HIDC_REPORTID_AC_KEYS           0x13
#define	HIDC_REPORTID_MOUSE             0x02
#define	HIDC_REPORTID_MOUSE_FEATURE     0x22
#define	HIDC_REPORTID_MOUSE_ABSOLUTE    0x23


/*
 * Report Descriptor items
 */
#define	HIDDESC_COLLECTION_1B           0xa1
#define	HIDDESC_COLLECTION_END          0xc0

#define	HIDDESC_COLLV_PHYSICAL          0x00
#define	HIDDESC_COLLV_APPLICATION       0x01
#define	HIDDESC_COLLV_LOGICAL           0x02

#define	HIDDESC_USAGE_PAGE_1B           0x05
#define	HIDDESC_UPV_GENERIC_DESKTOP     0x01
#define	HIDDESC_UPV_KEYBOARD            0x07
#define	HIDDESC_UPV_LED                 0x08
#define	HIDDESC_UPV_BUTTON              0x09
#define	HIDDESC_UPV_CONSUMER            0x0c

#define	HIDDESC_USAGE_1B                0x09
#define	HIDDESC_USAGE_2B                0x0a
#define	HIDDESC_USAGEV_GD_POINTER       0x01
#define	HIDDESC_USAGEV_GD_MOUSE         0x02
#define	HIDDESC_USAGEV_GD_KEYBOARD      0x06
#define	HIDDESC_USAGEV_GD_X             0x30
#define	HIDDESC_USAGEV_GD_Y             0x31
#define	HIDDESC_USAGEV_GD_Z             0x32
#define	HIDDESC_USAGEV_GD_WHEEL         0x38
#define	HIDDESC_USAGEV_GD_RES_MULTI     0x48
#define	HIDDESC_USAGEV_GD_SYSTEM_CTRL   0x80
#define	HIDDESC_USAGEV_GD_SYSTEM_POWER  0x81
#define	HIDDESC_USAGEV_GD_SYSTEM_SLEEP  0x82
#define	HIDDESC_USAGEV_C_CONTROL        0x01
#define HIDDESC_USAGEV_C_FAST_FORWARD   0xb3
#define HIDDESC_USAGEV_C_REWIND         0xb4
#define HIDDESC_USAGEV_C_SCAN_NEXT_TRACK        0xb5
#define HIDDESC_USAGEV_C_SCAN_PREVIOUS_TRACK    0xb6
#define HIDDESC_USAGEV_C_EJECT          0xb8
#define HIDDESC_USAGEV_C_PLAY_PAUSE     0xcd
#define HIDDESC_USAGEV_C_MUTE           0xe2
#define HIDDESC_USAGEV_C_VOLUME_INC     0xe9
#define HIDDESC_USAGEV_C_VOLUME_DEC     0xea
#define	HIDDESC_USAGEV_C_AC_HOME_1P     0x23
#define	HIDDESC_USAGEV_C_AC_HOME_2P     0x02
#define	HIDDESC_USAGEV_C_AC_BACK_1P     0x24
#define	HIDDESC_USAGEV_C_AC_BACK_2P     0x02
#define	HIDDESC_USAGEV_C_AC_FORWARD_1P  0x25
#define	HIDDESC_USAGEV_C_AC_FORWARD_2P  0x02
#define	HIDDESC_USAGEV_C_AC_PAN_1P      0x38
#define	HIDDESC_USAGEV_C_AC_PAN_2P      0x02

#define	HIDDESC_LOGICAL_MIN_1B          0x15
#define	HIDDESC_LOGICAL_MIN_2B          0x16
#define	HIDDESC_LOGICAL_MAX_1B          0x25
#define	HIDDESC_LOGICAL_MAX_2B          0x26

#define	HIDDESC_USAGE_MIN_1B            0x19
#define	HIDDESC_USAGE_MAX_1B            0x29
#define	HIDDESC_USAGE_MAX_2B            0x2a

#define	HIDDESC_PHYSICAL_MIN_1B         0x35
#define	HIDDESC_PHYSICAL_MIN_2B         0x36
#define	HIDDESC_PHYSICAL_MAX_1B         0x45
#define	HIDDESC_PHYSICAL_MAX_2B         0x46

#define	HIDDESC_UNIT_EXPONENT_1B        0x55
#define	HIDDESC_UNIT_1B                 0x65

#define	HIDDESC_UNIT_INCH               0x13

#define	HIDDESC_REPORT_SIZE             0x75
#define	HIDDESC_REPORT_ID               0x85
#define	HIDDESC_REPORT_COUNT            0x95

#define	HIDDESC_INPUT_1B                0x81
#define	HIDDESC_INPUTV_ARRAY            0x00
#define	HIDDESC_INPUTV_CONST            0x01
#define	HIDDESC_INPUTV_VAR_ABS          0x02
#define	HIDDESC_INPUTV_VAR_REL          0x06

#define	HIDDESC_OUTPUT_1B               0x91
#define	HIDDESC_OUTPUTV_ARRAY           0x00
#define	HIDDESC_OUTPUTV_CONST           0x01
#define	HIDDESC_OUTPUTV_VAR_ABS         0x02
#define	HIDDESC_OUTPUTV_VAR_REL         0x06

#define	HIDDESC_FEATURE_1B              0xb1
#define	HIDDESC_FEATUREV_ARRAY          0x00
#define	HIDDESC_FEATUREV_CONST          0x01
#define	HIDDESC_FEATUREV_VAR_ABS        0x02
#define	HIDDESC_FEATUREV_VAR_REL        0x06

#endif
