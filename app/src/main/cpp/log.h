#ifndef FREEME_NATIVE_LOG_H
#define FREEME_NATIVE_LOG_H

#include <android/log.h>

#define log_msg(level, tag, errFmt, ...) \
    do { \
            __android_log_print(level, tag, "[%s:%d] " errFmt , __FUNCTION__, __LINE__, ##__VA_ARGS__); \
    } while(0)

#define LOGV(fmt, ...) log_msg(ANDROID_LOG_VERBOSE, LOG_TAG, fmt, ##__VA_ARGS__)
#define LOGI(fmt, ...) log_msg(ANDROID_LOG_INFO, LOG_TAG, fmt, ##__VA_ARGS__)
#define LOGD(fmt, ...) log_msg(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ##__VA_ARGS__)
#define LOGI(fmt, ...) log_msg(ANDROID_LOG_INFO, LOG_TAG, fmt, ##__VA_ARGS__)
#define LOGW(fmt, ...) log_msg(ANDROID_LOG_WARN, LOG_TAG, fmt, ##__VA_ARGS__)
#define LOGE(fmt, ...) log_msg(ANDROID_LOG_ERROR, LOG_TAG, fmt, ##__VA_ARGS__)

#endif
