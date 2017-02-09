LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#opencv
OPENCVROOT:= C:\OpenCV-android-sdk
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED

include ${OPENCVROOT}\sdk\native\jni\OpenCV.mk

LOCAL_SRC_FILES := warp.cpp
LOCAL_LDLIBS += -llog
LOCAL_MODULE := warp

include $(BUILD_SHARED_LIBRARY)
include $(CLEAR_VARS)

