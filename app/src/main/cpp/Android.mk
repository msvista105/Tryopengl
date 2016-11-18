LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libuiblur

LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS += -O3 -Wall -Wno-unused-parameter

# Get all cpp files but not hidden files
#LOCAL_SRC_FILES := $(patsubst ./%,%, $(shell cd $(LOCAL_PATH); \
		  find . -name "*.cpp" -and -not -name ".*"))

LOCAL_SRC_FILES := \
    ProgramCache.cpp \
    Program.cpp \
    qtiblur.cpp

LOCAL_C_INCLUDES := $(JNI_H_INCLUDE)

LOCAL_SHARED_LIBRARIES := libGLESv2 liblog

include $(BUILD_SHARED_LIBRARY)
