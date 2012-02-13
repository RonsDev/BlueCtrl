LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := bluectrld

LOCAL_SRC_FILES := \
    bluectrld.c \
    error.c \
    hidhci.c \
    hidipc.c \
    hidl2cap.c \
    hidsdp.c \
    log.c \

LOCAL_LDLIBS := -lbluetooth -llog

include $(BUILD_EXECUTABLE)
