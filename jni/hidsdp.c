/*
 *  Service Discovery Protocol (SDP) registration of Human Interface
 *  Devices (HID)
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
#include <bluetooth/bluetooth.h>
#include <bluetooth/sdp.h>
#include <bluetooth/sdp_lib.h>

#include "error.h"
#include "log.h"
#include "hidl2cap.h"
#include "hiddescriptor.h"
#include "bluectrld.h"
#include "hidsdp.h"


/*
 * The PSM definitions for the control (0x11) and interrupt (0x13) L2CAP
 * connections. Theoretically other values could be specified here (for
 * example if the HID server can't bind on the standard PSMs) but most
 * Bluetooth stacks seem to ignore these values.
 */
static const uint16_t hid_psm_ctrl = L2CAP_PSM_HIDP_CTRL;
static const uint16_t hid_psm_intr = L2CAP_PSM_HIDP_INTR;

/*
 * Service strings
 */
static const char hid_service_name[] = "BlueCtrl";
static const char hid_provider_name[] = "https://github.com/RonsDev/BlueCtrl";
static const char hid_service_description[] = "Virtual HID";

/*
 * 0x0100 = Version 1.0.0
 * 0x0111 = Version 1.1.1
 */
static const uint16_t hid_profile_version = 0x0100;
static const uint16_t hid_parser_version = 0x0111;

/*
 * The minor Device Class.
 */
static const uint8_t hid_device_subclass = HIDC_MDC_COMBO_KEY_POINT;

/*
 * 13 = International (ISO)
 */
static const uint8_t hid_country_code = 13;

/*
 * Disable virtual cable connections because it's simpler and doesn't seem to
 * be usefull anyway.
 */
static const int hid_virtual_cable = 0;

/*
 * Allow the client to initiate a connection to the host.
 */
static const int hid_reconnect_initiate = 1;

/*
 * HID Language ID List: Allows the localisation of the service strings. It's
 * a list of pairs where the first value defines the language and the second
 * value the base attribute ID. In this case the primary language
 * (base attribute ID = 0x0100) is English (United States) with the
 * language ID 0x0409.
 */
static const uint16_t hid_langid[] = { 0x0409, 0x0100 };

/*
 * Specify that the client is battery powered so that a host may notify the
 * client of power state changes (e.g. suspend).
 */
static const int hid_battery_power = 1;

/*
 * Allow the client to wake up the host.
 */
static const int hid_remote_wakeup = 1;

/*
 * 8000 slots is the value set by the Apple Wireless Keyboard.
 */
static const uint16_t hid_supervision_timeout = 8000;

/*
 * Support Boot protocol mode (aka HID Lite) for better compatibility.
 */
static const int hid_boot_device = 1;

/*
 * HID Class Descriptor type: 0x22 = Report.
 */
static const uint8_t hid_descriptor_type = 0x22;

/*
 * The HID Class Descriptor for the virtual Bluetooth HID. This describes
 * the byte format for the Input and Output Reports. For more information look
 * at the "USB - Device Class Definition for Human Interface Devices (HID)"
 * document.
 */
static const uint8_t hid_descriptor[] = {
	/* Keyboard */
	HIDDESC_USAGE_PAGE_1B, HIDDESC_UPV_GENERIC_DESKTOP,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_GD_KEYBOARD,
	/* Collection Application begin */
	HIDDESC_COLLECTION_1B, HIDDESC_COLLV_APPLICATION,
	HIDDESC_REPORT_ID, HIDC_REPORTID_KEYBOARD,
	/* 1 byte: Modifier keys */
	HIDDESC_USAGE_PAGE_1B, HIDDESC_UPV_KEYBOARD,
	HIDDESC_USAGE_MIN_1B, 0xe0,
	HIDDESC_USAGE_MAX_1B, 0xe7,
	HIDDESC_REPORT_SIZE, 0x01,
	HIDDESC_REPORT_COUNT, 0x08,
	HIDDESC_LOGICAL_MIN_1B, 0x00,
	HIDDESC_LOGICAL_MAX_1B, 0x01,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	/* 1 byte: Reserved */
	HIDDESC_REPORT_SIZE, 0x08,
	HIDDESC_REPORT_COUNT, 0x01,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_CONST,
	/* 1 byte: LED Output Report */
	HIDDESC_USAGE_PAGE_1B, HIDDESC_UPV_LED,
	HIDDESC_USAGE_MIN_1B, 0x01,
	HIDDESC_USAGE_MAX_1B, 0x05,
	HIDDESC_REPORT_SIZE, 0x01,
	HIDDESC_REPORT_COUNT, 0x05,
	HIDDESC_OUTPUT_1B, HIDDESC_OUTPUTV_VAR_ABS,
	HIDDESC_REPORT_SIZE, 0x03,
	HIDDESC_REPORT_COUNT, 0x01,
	HIDDESC_OUTPUT_1B, HIDDESC_OUTPUTV_CONST,
	/* 6 bytes: Key codes */
	HIDDESC_USAGE_PAGE_1B, HIDDESC_UPV_KEYBOARD,
	HIDDESC_USAGE_MIN_1B, 0x00,
	HIDDESC_USAGE_MAX_2B, 0xff, 0x00,
	HIDDESC_REPORT_SIZE, 0x08,
	HIDDESC_REPORT_COUNT, 0x06,
	HIDDESC_LOGICAL_MIN_1B, 0x00,
	HIDDESC_LOGICAL_MAX_2B, 0xff, 0x00,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_ARRAY,
	/* Collection Application end */
	HIDDESC_COLLECTION_END,


	/* System Control */
	HIDDESC_USAGE_PAGE_1B, HIDDESC_UPV_GENERIC_DESKTOP,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_GD_SYSTEM_CTRL,
	/* Collection Application begin */
	HIDDESC_COLLECTION_1B, HIDDESC_COLLV_APPLICATION,
	/* 1 byte: System keys */
	HIDDESC_REPORT_ID, HIDC_REPORTID_SYSTEM_KEYS,
	HIDDESC_REPORT_SIZE, 0x01,
	HIDDESC_REPORT_COUNT, 0x01,
	HIDDESC_LOGICAL_MIN_1B, 0x00,
	HIDDESC_LOGICAL_MAX_1B, 0x01,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_GD_SYSTEM_POWER,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_GD_SYSTEM_SLEEP,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_REPORT_SIZE, 0x01,
	HIDDESC_REPORT_COUNT, 0x06,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_CONST,
	/* Collection Application end */
	HIDDESC_COLLECTION_END,


	/* Consumer Control */
	HIDDESC_USAGE_PAGE_1B, HIDDESC_UPV_CONSUMER,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_C_CONTROL,
	/* Collection Application begin */
	HIDDESC_COLLECTION_1B, HIDDESC_COLLV_APPLICATION,
	/* 1 byte: Hardware keys */
	HIDDESC_REPORT_ID, HIDC_REPORTID_HW_KEYS,
	HIDDESC_REPORT_SIZE, 0x01,
	HIDDESC_REPORT_COUNT, 0x03,
	HIDDESC_LOGICAL_MIN_1B, 0x00,
	HIDDESC_LOGICAL_MAX_1B, 0x01,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_CONST,
	HIDDESC_USAGE_PAGE_1B, HIDDESC_UPV_CONSUMER,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_C_EJECT,
	HIDDESC_REPORT_SIZE, 0x01,
	HIDDESC_REPORT_COUNT, 0x01,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_REPORT_SIZE, 0x01,
	HIDDESC_REPORT_COUNT, 0x04,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_CONST,
	/* 1 byte: Media keys */
	HIDDESC_REPORT_ID, HIDC_REPORTID_MEDIA_KEYS,
	HIDDESC_USAGE_PAGE_1B, HIDDESC_UPV_CONSUMER,
	HIDDESC_REPORT_SIZE, 0x01,
	HIDDESC_REPORT_COUNT, 0x01,
	HIDDESC_LOGICAL_MIN_1B, 0x00,
	HIDDESC_LOGICAL_MAX_1B, 0x01,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_C_PLAY_PAUSE,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_C_FAST_FORWARD,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_C_REWIND,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_C_SCAN_NEXT_TRACK,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_C_SCAN_PREVIOUS_TRACK,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_C_MUTE,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_C_VOLUME_INC,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_C_VOLUME_DEC,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	/* 1 byte: Application Control keys */
	HIDDESC_REPORT_ID, HIDC_REPORTID_AC_KEYS,
	HIDDESC_USAGE_PAGE_1B, HIDDESC_UPV_CONSUMER,
	HIDDESC_REPORT_SIZE, 0x01,
	HIDDESC_REPORT_COUNT, 0x01,
	HIDDESC_LOGICAL_MIN_1B, 0x00,
	HIDDESC_LOGICAL_MAX_1B, 0x01,
	HIDDESC_USAGE_2B, HIDDESC_USAGEV_C_AC_HOME_1P,
			HIDDESC_USAGEV_C_AC_HOME_2P,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_USAGE_2B, HIDDESC_USAGEV_C_AC_BACK_1P,
			HIDDESC_USAGEV_C_AC_BACK_2P,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_USAGE_2B, HIDDESC_USAGEV_C_AC_FORWARD_1P,
			HIDDESC_USAGEV_C_AC_FORWARD_2P,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_REPORT_SIZE, 0x01,
	HIDDESC_REPORT_COUNT, 0x05,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_CONST,
	/* Collection Application end */
	HIDDESC_COLLECTION_END,


	/* Mouse */
	HIDDESC_USAGE_PAGE_1B, HIDDESC_UPV_GENERIC_DESKTOP,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_GD_MOUSE,
	/* Collection Application begin */
	HIDDESC_COLLECTION_1B, HIDDESC_COLLV_APPLICATION,
	/* Collection Logical begin */
	HIDDESC_COLLECTION_1B, HIDDESC_COLLV_LOGICAL,
	HIDDESC_REPORT_ID, HIDC_REPORTID_MOUSE,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_GD_POINTER,
	/* Collection Physical begin */
	HIDDESC_COLLECTION_1B, HIDDESC_COLLV_PHYSICAL,
	/* 1 byte: Mouse buttons */
	HIDDESC_USAGE_PAGE_1B, HIDDESC_UPV_BUTTON,
	HIDDESC_USAGE_MIN_1B, 0x01,
	HIDDESC_USAGE_MAX_1B, 0x05,
	HIDDESC_REPORT_SIZE, 0x01,
	HIDDESC_REPORT_COUNT, 0x05,
	HIDDESC_LOGICAL_MIN_1B, 0x00,
	HIDDESC_LOGICAL_MAX_1B, 0x01,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_REPORT_SIZE, 0x03,
	HIDDESC_REPORT_COUNT, 0x01,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_CONST,
	/* 4 bytes: Mouse movement (X, Y) */
	HIDDESC_USAGE_PAGE_1B, HIDDESC_UPV_GENERIC_DESKTOP,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_GD_X,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_GD_Y,
	HIDDESC_REPORT_SIZE, 0x10,
	HIDDESC_REPORT_COUNT, 0x02,
	HIDDESC_LOGICAL_MIN_2B, 0x01, 0xf8,
	HIDDESC_LOGICAL_MAX_2B, 0xff, 0x07,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_REL,
	/* Collection Logical begin */
	HIDDESC_COLLECTION_1B, HIDDESC_COLLV_LOGICAL,
	/* 2 bits: Vertical wheel Resolution Multiplier Feature Report */
	HIDDESC_REPORT_ID, HIDC_REPORTID_MOUSE_FEATURE,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_GD_RES_MULTI,
	HIDDESC_REPORT_SIZE, 0x02,
	HIDDESC_REPORT_COUNT, 0x01,
	HIDDESC_LOGICAL_MIN_1B, 0x00,
	HIDDESC_LOGICAL_MAX_1B, 0x01,
	HIDDESC_PHYSICAL_MIN_1B, 0x01,
	HIDDESC_PHYSICAL_MAX_1B, 0x10,
	HIDDESC_FEATURE_1B, HIDDESC_FEATUREV_VAR_ABS,
	HIDDESC_PHYSICAL_MIN_1B, 0x00,
	HIDDESC_PHYSICAL_MAX_1B, 0x00,
	/* 1 byte: Vertical wheel */
	HIDDESC_REPORT_ID, HIDC_REPORTID_MOUSE,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_GD_WHEEL,
	HIDDESC_REPORT_SIZE, 0x08,
	HIDDESC_REPORT_COUNT, 0x01,
	HIDDESC_LOGICAL_MIN_1B, 0x81,
	HIDDESC_LOGICAL_MAX_1B, 0x7f,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_REL,
	/* Collection Logical end */
	HIDDESC_COLLECTION_END,
	/* Collection Logical begin */
	HIDDESC_COLLECTION_1B, HIDDESC_COLLV_LOGICAL,
	/* 6 bits: Horizontal wheel Resolution Multiplier Feature Report */
	HIDDESC_REPORT_ID, HIDC_REPORTID_MOUSE_FEATURE,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_GD_RES_MULTI,
	HIDDESC_REPORT_SIZE, 0x02,
	HIDDESC_REPORT_COUNT, 0x01,
	HIDDESC_LOGICAL_MIN_1B, 0x00,
	HIDDESC_LOGICAL_MAX_1B, 0x01,
	HIDDESC_PHYSICAL_MIN_1B, 0x01,
	HIDDESC_PHYSICAL_MAX_1B, 0x10,
	HIDDESC_FEATURE_1B, HIDDESC_FEATUREV_VAR_ABS,
	HIDDESC_PHYSICAL_MIN_1B, 0x00,
	HIDDESC_PHYSICAL_MAX_1B, 0x00,
	HIDDESC_REPORT_SIZE, 0x04,
	HIDDESC_FEATURE_1B, HIDDESC_FEATUREV_CONST,
	/* 1 byte: Horizontal wheel */
	HIDDESC_REPORT_ID, HIDC_REPORTID_MOUSE,
	HIDDESC_USAGE_PAGE_1B, HIDDESC_UPV_CONSUMER,
	HIDDESC_USAGE_2B, HIDDESC_USAGEV_C_AC_PAN_1P,
			HIDDESC_USAGEV_C_AC_PAN_2P,
	HIDDESC_REPORT_SIZE, 0x08,
	HIDDESC_REPORT_COUNT, 0x01,
	HIDDESC_LOGICAL_MIN_1B, 0x81,
	HIDDESC_LOGICAL_MAX_1B, 0x7f,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_REL,
	/* Collection Logical end */
	HIDDESC_COLLECTION_END,
	/* Collection Physical end */
	HIDDESC_COLLECTION_END,
	/* Collection Logical end */
	HIDDESC_COLLECTION_END,
	/* Collection Application end */
	HIDDESC_COLLECTION_END,


	/* Mouse (Absolute) */
	HIDDESC_USAGE_PAGE_1B, HIDDESC_UPV_GENERIC_DESKTOP,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_GD_MOUSE,
	/* Collection Application begin */
	HIDDESC_COLLECTION_1B, HIDDESC_COLLV_APPLICATION,
	HIDDESC_REPORT_ID, HIDC_REPORTID_MOUSE_ABSOLUTE,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_GD_POINTER,
	/* Collection Physical begin */
	HIDDESC_COLLECTION_1B, HIDDESC_COLLV_PHYSICAL,
	/* 1 byte: Mouse buttons */
	HIDDESC_USAGE_PAGE_1B, HIDDESC_UPV_BUTTON,
	HIDDESC_USAGE_MIN_1B, 0x01,
	HIDDESC_USAGE_MAX_1B, 0x05,
	HIDDESC_REPORT_SIZE, 0x01,
	HIDDESC_REPORT_COUNT, 0x05,
	HIDDESC_LOGICAL_MIN_1B, 0x00,
	HIDDESC_LOGICAL_MAX_1B, 0x01,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_REPORT_SIZE, 0x03,
	HIDDESC_REPORT_COUNT, 0x01,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_CONST,
	/* 4 bytes: Mouse position (X, Y) */
	HIDDESC_USAGE_PAGE_1B, HIDDESC_UPV_GENERIC_DESKTOP,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_GD_X,
	HIDDESC_REPORT_SIZE, 0x10,
	HIDDESC_REPORT_COUNT, 0x01,
	HIDDESC_LOGICAL_MIN_1B, 0x00,
	HIDDESC_LOGICAL_MAX_2B, 0xff, 0x07,
	HIDDESC_UNIT_EXPONENT_1B, 0x0e,  // 0x0e = -2
	HIDDESC_UNIT_1B, HIDDESC_UNIT_INCH,
	HIDDESC_PHYSICAL_MIN_1B, 0x00,
	HIDDESC_PHYSICAL_MAX_2B, 0xf4, 0x01,  // 0x1f4 = 500 = 5 Inch
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_USAGE_1B, HIDDESC_USAGEV_GD_Y,
	HIDDESC_INPUT_1B, HIDDESC_INPUTV_VAR_ABS,
	HIDDESC_UNIT_EXPONENT_1B, 0x00,
	HIDDESC_UNIT_1B, 0x00,
	HIDDESC_PHYSICAL_MIN_1B, 0x00,
	HIDDESC_PHYSICAL_MAX_1B, 0x00,
	/* Collection Physical end */
	HIDDESC_COLLECTION_END,
	/* Collection Application end */
	HIDDESC_COLLECTION_END,
};


/*
 * The SDP connection for the HID Service Record.
 */
static sdp_session_t *hidsdp_con = NULL;

/*
 * The HID Service Record.
 */
static sdp_record_t *hidsdp_rec = NULL;

/*
 * A list that contains the deactivated Service Records.
 */
static sdp_list_t *deactivated_services = NULL;


/*
 * Set the Browse Groups attribute in the SDP Record.
 *
 * Parameters:
 *     rec: The SDP Record.
 *     data: The attribute data.
 */
static void set_browse_groups(sdp_record_t *rec, uint16_t data)
{
	uuid_t uuid;
	sdp_list_t *sdp_list;

	sdp_uuid16_create(&uuid, data);
	sdp_list = sdp_list_append(NULL, &uuid);
	if (sdp_set_browse_groups(rec, sdp_list) < 0) {
		log_e("Can't set SDP Browse Groups");
	}

	sdp_list_free(sdp_list, NULL);
}

/*
 * Set the LanguageBase attribute in the SDP Record. These attribute defines
 * the character encoding for the service strings.
 * This function is based on the "add_lang_attr" function from the BlueZ code.
 *
 * Parameters:
 *     rec: The SDP Record.
 */
static void set_lang_attr(sdp_record_t *rec)
{
	sdp_lang_attr_t base_lang;
	sdp_list_t *langs;

	/* UTF-8 MIBenum (http://www.iana.org/assignments/character-sets) */
	base_lang.code_ISO639 = (0x65 << 8) | 0x6e;
	base_lang.encoding = 106;
	base_lang.base_offset = SDP_PRIMARY_LANG_BASE;

	langs = sdp_list_append(NULL, &base_lang);
	if (sdp_set_lang_attr(rec, langs) < 0) {
		log_e("Can't set SDP lang attribute");
	}

	sdp_list_free(langs, NULL);
}

/*
 * Set the Service Classes attribute in the SDP Record.
 *
 * Parameters:
 *     rec: The SDP Record.
 *     data: The attribute data.
 */
static void set_service_classes(sdp_record_t *rec, uint16_t data)
{
	uuid_t uuid;
	sdp_list_t *sdp_list;

	sdp_uuid16_create(&uuid, data);
	sdp_list = sdp_list_append(NULL, &uuid);
	if (sdp_set_service_classes(rec, sdp_list) < 0) {
		log_e("Can't set SDP Service Classes");
	}

	sdp_list_free(sdp_list, NULL);
}

/*
 * Set the Profile Description attribute in the SDP Record.
 *
 * Parameters:
 *     rec: The SDP Record.
 *     version: The Profile Description version.
 */
static void set_hid_profile_descs(sdp_record_t *rec, uint16_t version)
{
	sdp_profile_desc_t profile[1];
	sdp_list_t *sdp_list;

	sdp_uuid16_create(&profile[0].uuid, HID_PROFILE_ID);
	profile[0].version = version;
	sdp_list = sdp_list_append(NULL, profile);
	if (sdp_set_profile_descs(rec, sdp_list) < 0) {
		log_e("Can't set SDP Profile Description");
	}

	sdp_list_free(sdp_list, NULL);
}

/*
 * Set the Access Protocols attribute in the SDP Record.
 *
 * Parameters:
 *     rec: The SDP Record.
 *     psm_ctrl: The control socket PSM.
 */
static void set_hid_access_protos(sdp_record_t *rec, uint16_t psm_ctrl)
{
	uuid_t l2cap_uuid;
	uuid_t hidp_uuid;
	sdp_data_t *channel;
	sdp_list_t *proto1;
	sdp_list_t *proto2;
	sdp_list_t *apseq;
	sdp_list_t *aproto;

	sdp_uuid16_create(&l2cap_uuid, L2CAP_UUID);
	proto1 = sdp_list_append(NULL, &l2cap_uuid);
	channel = sdp_data_alloc(SDP_UINT16, &psm_ctrl);
	proto1 = sdp_list_append(proto1, channel);
	apseq = sdp_list_append(NULL, proto1);

	sdp_uuid16_create(&hidp_uuid, HIDP_UUID);
	proto2 = sdp_list_append(NULL, &hidp_uuid);
	apseq = sdp_list_append(apseq, proto2);

	aproto = sdp_list_append(NULL, apseq);
	if (sdp_set_access_protos(rec, aproto) < 0) {
		log_e("Can't set SDP Access Protocols");
	}

	sdp_data_free(channel);
	sdp_list_free(proto1, NULL);
	sdp_list_free(proto2, NULL);
	sdp_list_free(apseq, NULL);
	sdp_list_free(aproto, NULL);
}

/*
 * Set the Additional Access Protocols attribute in the SDP Record.
 *
 * Parameters:
 *     rec: The SDP Record.
 *     psm_intr: The interrupt socket PSM.
 */
static void set_hid_add_access_protos(sdp_record_t *rec, uint16_t psm_intr)
{
	uuid_t l2cap_uuid;
	uuid_t hidp_uuid;
	sdp_data_t *channel;
	sdp_list_t *proto1;
	sdp_list_t *proto2;
	sdp_list_t *apseq;
	sdp_list_t *aproto;

	sdp_uuid16_create(&l2cap_uuid, L2CAP_UUID);
	proto1 = sdp_list_append(NULL, &l2cap_uuid);
	channel = sdp_data_alloc(SDP_UINT16, &psm_intr);
	proto1 = sdp_list_append(proto1, channel);
	apseq = sdp_list_append(NULL, proto1);

	sdp_uuid16_create(&hidp_uuid, HIDP_UUID);
	proto2 = sdp_list_append(NULL, &hidp_uuid);
	apseq = sdp_list_append(apseq, proto2);

	aproto = sdp_list_append(NULL, apseq);
	if (sdp_set_add_access_protos(rec, aproto) < 0) {
		log_e("Can't set SDP Additional Access Protocols");
	}

	sdp_data_free(channel);
	sdp_list_free(proto1, NULL);
	sdp_list_free(proto2, NULL);
	sdp_list_free(apseq, NULL);
	sdp_list_free(aproto, NULL);
}

/*
 * Get the correct SDP datatype descriptor for a text with the given length.
 *
 * Parameters:
 *     length: The text length.
 *
 * Returns:
 *     A SDP datatype descriptor.
 */
static uint8_t get_sdp_text_dtds(int length)
{
	if (length < UINT8_MAX) {
		return SDP_TEXT_STR8;
	}
	else if (length < UINT16_MAX) {
		return SDP_TEXT_STR16;
	}
	else {
		return SDP_TEXT_STR32;
	}
}

/*
 * Get the correct SDP datatype descriptor for a SDP sequence.
 *
 * Parameters:
 *     seq_data: The SDP sequence.
 *
 * Returns:
 *     A SDP datatype descriptor.
 */
static uint8_t get_sdp_seq_dtds(sdp_data_t *seq_data)
{
	sdp_data_t *seq;
	int length = 0;

	seq = seq_data;
	for (; seq; seq = seq->next) {
		/*
		 * FIXME: Find a better way to get the total length of the
		 *        sdp_data_t object.
		 */
		length += seq->unitSize;

		/*
		 * It seems that unitSize doesn't contain the total length,
		 * so we are adding some sizes manually.
		 */
		switch (seq->dtd) {
		case SDP_TEXT_STR8:
		case SDP_SEQ8:
			length += 1;
			break;
		case SDP_TEXT_STR16:
		case SDP_SEQ16:
			length += 2;
			break;
		case SDP_TEXT_STR32:
		case SDP_SEQ32:
			length += 3;
			break;
		}
	}

	if (length < UINT8_MAX) {
		return SDP_SEQ8;
	}
	else if (length < UINT16_MAX) {
		return SDP_SEQ16;
	}
	else {
		return SDP_SEQ32;
	}
}

/*
 * Set the HID Descriptor attribute in the SDP Record.
 *
 * Parameters:
 *     rec: The SDP Record.
 *     hid_spec_type: The HID Descriptor type.
 *     hid_spec: The HID Descriptor specification.
 *     hid_spec_length: Size of the hid_spec parameter.
 */
static void set_hid_descriptor(sdp_record_t *rec,
				uint8_t hid_spec_type,
				uint8_t *hid_spec,
				int hid_spec_length)
{
	sdp_data_t *ds_type;
	sdp_data_t *ds_text;
	sdp_data_t *ds_item;
	sdp_data_t *ds_list;

	ds_type = sdp_data_alloc(SDP_UINT8, &hid_spec_type);

	ds_text = sdp_data_alloc_with_length(get_sdp_text_dtds(hid_spec_length),
						hid_spec,
						hid_spec_length);

	sdp_seq_append(ds_type, ds_text);

	ds_item = sdp_data_alloc(get_sdp_seq_dtds(ds_type), ds_type);
	ds_list = sdp_data_alloc(get_sdp_seq_dtds(ds_item), ds_item);
	if (sdp_attr_add(rec, SDP_ATTR_HID_DESCRIPTOR_LIST, ds_list) < 0) {
		log_e("Can't set SDP HID Descriptor");
	}
}

/*
 * Set the HID Language attribute in the SDP Record.
 *
 * Parameters:
 *     rec: The SDP Record.
 *     hid_lang: The HID Language values.
 *     hid_lang_length: Size of the hid_lang parameter.
 */
static void set_hid_lang(sdp_record_t *rec,
			uint16_t *hid_lang,
			int hid_lang_length)
{
	int i;
	uint8_t sdp_uint16 = SDP_UINT16;
	void **dtds;
	void **values;
	sdp_data_t *sdp_seq;
	sdp_data_t *sdp_data;

	dtds = malloc(hid_lang_length * sizeof(void *));
	if (!dtds) {
		log_e("Can't set SDP HID Language: dtds malloc error");
		return;
	}

	values = malloc(hid_lang_length * sizeof(void *));
	if (!values) {
		log_e("Can't set SDP HID Language: values malloc error");
		free(dtds);
		return;
	}

	for (i = 0; i < hid_lang_length; i++) {
		dtds[i] = &sdp_uint16;
		values[i] = &hid_lang[i];
	}

	sdp_seq = sdp_seq_alloc(dtds, values, hid_lang_length);
	sdp_data = sdp_data_alloc(SDP_SEQ8, sdp_seq);
	if (sdp_attr_add(rec, SDP_ATTR_HID_LANG_ID_BASE_LIST, sdp_data) < 0) {
		log_e("Can't set SDP HID Language");
	}

	free(dtds);
	free(values);
}

/*
 * Add a bool attribute to the SDP Record.
 *
 * Parameters:
 *     rec: The SDP Record.
 *     attr: The attribute ID.
 *     value: The attribute value.
 */
static void add_bool_attr(sdp_record_t *rec, uint16_t attr, int value)
{
	if (sdp_attr_add_new(rec, attr, SDP_BOOL, &value) < 0) {
		log_e("Can't add SDP attribute (ID=0x%x)", attr);
	}
}

/*
 * Add a 1 byte unsigned integer attribute to the SDP Record.
 *
 * Parameters:
 *     rec: The SDP Record.
 *     attr: The attribute ID.
 *     value: The attribute value.
 */
static void add_uint8_attr(sdp_record_t *rec, uint16_t attr, uint8_t value)
{
	if (sdp_attr_add_new(rec, attr, SDP_UINT8, &value) < 0) {
		log_e("Can't add SDP attribute (ID=0x%x)", attr);
	}
}

/*
 * Add a 2 byte unsigned integer attribute to the SDP Record.
 *
 * Parameters:
 *     rec: The SDP Record.
 *     attr: The attribute ID.
 *     value: The attribute value.
 */
static void add_uint16_attr(sdp_record_t *rec, uint16_t attr, uint16_t value)
{
	if (sdp_attr_add_new(rec, attr, SDP_UINT16, &value) < 0) {
		log_e("Can't add SDP attribute (ID=0x%x)", attr);
	}
}

/*
 * Create a new HID SDP Record.
 *
 * Returns:
 *     The initialized HID SDP Record.
 */
static sdp_record_t* create_hid_record()
{
	sdp_record_t *rec;

	rec = sdp_record_alloc();
	if (!rec) {
		log_e("Can't allocate SDP Record");
		return NULL;
	}
	memset((void*)rec, 0, sizeof(sdp_record_t));

	/* auto generate Record handle */
	rec->handle = 0xffffffff;

	set_service_classes(rec, HID_SVCLASS_ID);

	set_hid_access_protos(rec, hid_psm_ctrl);
	set_hid_add_access_protos(rec, hid_psm_intr);

	set_browse_groups(rec, PUBLIC_BROWSE_GROUP);

	set_lang_attr(rec);

	set_hid_profile_descs(rec, hid_profile_version);

	sdp_set_info_attr(rec,
			hid_service_name,
			hid_provider_name,
			hid_service_description);

	add_uint16_attr(rec, SDP_ATTR_HID_PARSER_VERSION, hid_parser_version);

	add_uint8_attr(rec,
			SDP_ATTR_HID_DEVICE_SUBCLASS,
			hid_device_subclass);

	add_uint8_attr(rec, SDP_ATTR_HID_COUNTRY_CODE, hid_country_code);

	add_bool_attr(rec, SDP_ATTR_HID_VIRTUAL_CABLE, hid_virtual_cable);

	add_bool_attr(rec,
			SDP_ATTR_HID_RECONNECT_INITIATE,
			hid_reconnect_initiate);

	set_hid_descriptor(rec,
			hid_descriptor_type,
			(uint8_t*)hid_descriptor,
			sizeof(hid_descriptor));

	set_hid_lang(rec, (uint16_t*)hid_langid, sizeof(hid_langid) / 2);

	add_bool_attr(rec, SDP_ATTR_HID_BATTERY_POWER, hid_battery_power);

	add_bool_attr(rec, SDP_ATTR_HID_REMOTE_WAKEUP, hid_remote_wakeup);

	add_uint16_attr(rec,
			SDP_ATTR_HID_PROFILE_VERSION,
			hid_profile_version);

	add_uint16_attr(rec,
			SDP_ATTR_HID_SUPERVISION_TIMEOUT,
			hid_supervision_timeout);

	/*
	 * Allow the host to initiate a connection only if the HID server
	 * could be started. It should be noted that running without the
	 * HID server could be problematic because the Bluetooth HID
	 * documentation mentions that keyboards should always set this
	 * attribute to True.
	 */
	add_bool_attr(rec,
			SDP_ATTR_HID_NORMALLY_CONNECTABLE,
			hidc_is_hid_server_running());

	add_bool_attr(rec, SDP_ATTR_HID_BOOT_DEVICE, hid_boot_device);

	return rec;
}

/*
 * Get a list of all Service Records except for the HID Service Record.
 *
 * Parameters:
 *     sdp_con: The SDP connection.
 *     result_list: The resulting list.
 *
 * Returns:
 *     0 on success or a negative error code (defined in error.h) on failure.
 */
static int get_other_services(sdp_session_t *sdp_con, sdp_list_t **result_list)
{
	int errsv;  /* saved errno */
	uuid_t uuid;
	uint32_t range;
	sdp_list_t *search;
	sdp_list_t *attrid_list;
	sdp_list_t *seq;
	sdp_list_t *next;
	sdp_list_t *rec_list = NULL;
	sdp_record_t *rec;

	sdp_uuid16_create(&uuid, PUBLIC_BROWSE_GROUP);
	search = sdp_list_append(NULL, &uuid);

	/* get all attributes */
	range = 0x0000ffff;
	attrid_list = sdp_list_append(NULL, &range);

	if (sdp_service_search_attr_req(sdp_con,
					search,
					SDP_ATTR_REQ_RANGE,
					attrid_list,
					&seq) < 0) {
		errsv = errno;
		log_ec(errsv, "Can't find SDP Records");
		return hidc_convert_errno(errsv);
	}

	sdp_list_free(search, NULL);
	sdp_list_free(attrid_list, NULL);

	for (; seq; seq = next) {
		rec = (sdp_record_t *)seq->data;

		if (!hidsdp_rec || (rec->handle != hidsdp_rec->handle)) {
			rec_list = sdp_list_append(rec_list, rec);
		} else {
			sdp_record_free(rec);
		}

		next = seq->next;
		free(seq);
	}

	*result_list = rec_list;
	return 0;
}

int hidc_sdp_register()
{
	int errsv;  /* saved errno */

	if (hidsdp_con && hidsdp_rec)
		return 0;

	if (!hidsdp_rec) {
		hidsdp_rec = create_hid_record();
		if (!hidsdp_rec)
			return HIDC_EC_UNKNOWN;
	}

	hidsdp_con = sdp_connect(hidc_get_app_dev_bdaddr(),
					BDADDR_LOCAL,
					SDP_RETRY_IF_BUSY);
	if (!hidsdp_con) {
		errsv = errno;
		log_ec(errsv, "Can't connect to the SDP");
		return hidc_convert_errno(errsv);
	}

	if (sdp_device_record_register(hidsdp_con,
					hidc_get_app_dev_bdaddr(),
					hidsdp_rec,
					0) < 0) {
		errsv = errno;
		log_ec(errsv, "Can't register SDP Record");
		sdp_record_free(hidsdp_rec);
		hidsdp_rec = NULL;
		sdp_close(hidsdp_con);
		hidsdp_con = NULL;
		return hidc_convert_errno(errsv);
	}

	return 0;
}

int hidc_sdp_unregister()
{
	int errsv;  /* saved errno */

	if (hidsdp_con && hidsdp_rec) {
		if (sdp_record_unregister(hidsdp_con, hidsdp_rec) < 0) {
			errsv = errno;
			log_ec(errsv, "Can't unregister SDP Record");
			return hidc_convert_errno(errsv);
		}

		/* hidsdp_rec was freed inside sdp_record_unregister */
		hidsdp_rec = NULL;
	}

	if (hidsdp_con) {
		if(sdp_close(hidsdp_con) < 0) {
			errsv = errno;
			log_ec(errsv, "Can't close SDP connection");
			return hidc_convert_errno(errsv);
		}

		/* hidsdp_con was freed inside sdp_close */
		hidsdp_con = NULL;
	}

	return 0;
}

int hidc_deactivate_other_services()
{
	int errsv;  /* saved errno */
	int ec;  /* error code */
	sdp_session_t *sdp_con;  /* SDP connection */
	sdp_list_t *seq;
	sdp_list_t *next;
	sdp_record_t *rec;

	if (deactivated_services)
		return 0;

	sdp_con = sdp_connect(hidc_get_app_dev_bdaddr(),
				BDADDR_LOCAL,
				SDP_RETRY_IF_BUSY);
	if (!sdp_con) {
		errsv = errno;
		log_ec(errsv, "Can't connect to the SDP");
		return hidc_convert_errno(errsv);
	}

	if ((ec = get_other_services(sdp_con, &deactivated_services)) < 0) {
		deactivated_services = NULL;
		sdp_close(sdp_con);
		return ec;
	}

	seq = deactivated_services;
	for (; seq; seq = next) {
		rec = (sdp_record_t *)seq->data;

		log_d("Deactivate SDP Record (0x%x)", rec->handle);

		if (sdp_device_record_unregister_binary(sdp_con,
						hidc_get_app_dev_bdaddr(),
						rec->handle) < 0) {
			errsv = errno;
			log_ec(errsv, "Can't unregister SDP Record");
		}

		next = seq->next;
	}

	if(sdp_close(sdp_con) < 0) {
		errsv = errno;
		log_ec(errsv, "Can't close SDP connection");
		return hidc_convert_errno(errsv);
	}

	/*
	 * The Service part of the Bluetooth adapter Class gets updated
	 * asynchronously when the SDP Records are unregistered. This could
	 * lead to a conflict if a method that changes the Device Class is
	 * executed immediately after this method. Therefore wait until all
	 * Services of the Bluetooth adapter Class are gone.
	 */
	hidc_wait_for_empty_service_class(1);

	return 0;
}

int hidc_reactivate_other_services()
{
	int result = 0;
	int errsv;  /* saved errno */
	sdp_session_t *sdp_con;  /* SDP connection */
	sdp_list_t *seq;
	sdp_list_t *next;
	sdp_record_t *rec;

	if (!deactivated_services)
		return 0;

	sdp_con = sdp_connect(hidc_get_app_dev_bdaddr(),
				BDADDR_LOCAL,
				SDP_RETRY_IF_BUSY);
	if (!sdp_con) {
		errsv = errno;
		log_ec(errsv, "Can't connect to the SDP");
		return hidc_convert_errno(errsv);
	}

	seq = deactivated_services;
	for (; seq; seq = next) {
		rec = (sdp_record_t *)seq->data;

		log_d("Reactivate SDP Record (0x%x)", rec->handle);

		if (sdp_device_record_register(sdp_con,
						hidc_get_app_dev_bdaddr(),
						rec,
						SDP_RECORD_PERSIST) < 0) {
			errsv = errno;
			log_ec(errsv,
				"Can't reactivate SDP Record (0x%x)",
				rec->handle);
			if (result == 0)
				result = hidc_convert_errno(errsv);
		}

		next = seq->next;
		free(seq);
		sdp_record_free(rec);
	}

	deactivated_services = NULL;

	if(sdp_close(sdp_con) < 0) {
		errsv = errno;
		log_ec(errsv, "Can't close SDP connection");
		return hidc_convert_errno(errsv);
	}

	return result;
}
