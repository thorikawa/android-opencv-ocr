#ifndef _COMMON_H_
#define _COMMON_H_

#ifdef __ANDROID__
#include <android/log.h>
#define LOG_TAG "OCR/Native"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__);
#else
#define LOGD(...) printf(__VA_ARGS__);
#endif

#endif
