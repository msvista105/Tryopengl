LOCAL_PATH := $(call my-dir)

######################################################################
include $(CLEAR_VARS)
LOCAL_MODULE := libuiblur
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := .so
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_SRC_FILES := libuiblur_32bit.so
include $(BUILD_PREBUILT)

######################################################################
include $(CLEAR_VARS)

LOCAL_MODULE := testblur
LOCAL_SRC_FILES := \
    ../Blur.cpp \
	../main.cpp

LOCAL_SHARED_LIBRARIES :=  libgui libEGL libGLESv2 libuiblur libutils liblog
include $(BUILD_EXECUTABLE)
