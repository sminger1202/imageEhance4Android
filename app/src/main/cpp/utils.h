//
// Created by 史明 on 2017/5/23.
//

#ifndef HELLO_JNI_UTILS_H
#define HELLO_JNI_UTILS_H

#ifdef OnPC
#define LOGE( TAG,... ) printf( __VA_ARGS__ )
#ifdef DEBUG
#define LOGD( TAG,... ) printf( __VA_ARGS__ )
#else
#define LOGD( TAG,... )
#endif
#define LOGI( TAG,... ) printf( __VA_ARGS__ )
#else
#include <android/log.h>
#define LOGI( ... ) __android_log_print( ANDROID_LOG_INFO, __VA_ARGS__ )
#define LOGE( ... ) __android_log_print( ANDROID_LOG_ERROR, __VA_ARGS__ )
#ifdef DEBUG
#define LOGD( ... ) __android_log_print( ANDROID_LOG_DEBUG, __VA_ARGS__ )
#else
#define LOGD( ... )
#endif //end of DEBUG
#endif //end of OnPC

#endif //HELLO_JNI_UTILS_H
