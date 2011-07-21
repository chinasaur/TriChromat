LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := yuv420sp2mg

LOCAL_SRC_FILES := \
	yuvProcessors/yuv420sp2mg.c

LOCAL_CFLAGS := -DANDROID_NDK \
                -DDISABLE_IMPORTGL

LOCAL_LDLIBS := -ldl -llog

include $(BUILD_SHARED_LIBRARY)