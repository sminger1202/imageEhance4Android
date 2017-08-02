//
// Created by 史明 on 2017/5/23.
//

#include <jni.h>
#include <cstring>
#include <GLES2/gl2.h>
#include "platformCL.h"
#include "utils.h"
#include "platformGL.h"

platform *pPlatform = NULL;
JNIEXPORT jstring JNICALL
TestJNI( JNIEnv* env, jobject thiz, jint a) {

#if defined(__arm__)
#if defined(__ARM_ARCH_7A__)
#if defined(__ARM_NEON__)
    #if defined(__ARM_PCS_VFP)
        #define ABI "armeabi-v7a/NEON (hard-float)"
      #else
        #define ABI "armeabi-v7a/NEON"
      #endif
#else
#if defined(__ARM_PCS_VFP)
#define ABI "armeabi-v7a (hard-float)"
#else
#define ABI "armeabi-v7a"
#endif
#endif
#else
#define ABI "armeabi"
#endif
#elif defined(__i386__)
    #define ABI "x86"
#elif defined(__x86_64__)
#define ABI "x86_64"
#elif defined(__mips64)  /* mips64el-* toolchain defines __mips__ too */
#define ABI "mips64"
#elif defined(__mips__)
#define ABI "mips"
#elif defined(__aarch64__)
#define ABI "arm64-v8a"
#else
#define ABI "unknown"
#endif
    return env->NewStringUTF("Enhance from JNI platform!  Compiled with ABI " ABI "." );
}
JavaVM *javaVM = NULL;
jboolean initComputerPlatform(JNIEnv* env, jobject thiz) {

//    javaVM->AttachCurrentThread (&env, NULL);
    LOGI("EnhanceInterface", "initComputerPlatform.");
    pPlatform = new platform();
    pPlatform->initDevice();
    pPlatform->initCLKernel();
//    javaVM->DetachCurrentThread();
    return true;
}
void setTextureIds(JNIEnv* env, jobject thiz,
                   int srcTextureId, jint dstTextureId,
                   int srcWidth, int srcHeight,
                   int dstWidht, int dstHeight) {

    pPlatform->setTextureID(srcTextureId, dstTextureId);
    pPlatform->setSrcDstSize(srcWidth, srcHeight, dstWidht, dstHeight);
    pPlatform->initCLBuffer();
}

void renderCL(JNIEnv* env, jobject thiz) {
    pPlatform->runCL();
}

void release(JNIEnv* env, jobject thiz) {
    pPlatform->cleanupCL();
}

jint GLGetProgram(JNIEnv* env, jobject thiz) {
    platformGL mPlatformGL;
    return  mPlatformGL.getProgram();
}
static JNINativeMethod gMethods[] = {
        {"TestJNI", "(I)Ljava/lang/String;",(void*)TestJNI},
        {"initComputerPlatform", "()Z", (void*)initComputerPlatform},
        {"setTextureIds", "(IIIIII)V", (void*) setTextureIds},
        {"renderCL","()V", (void*) renderCL},
        {"release","()V", (void*) release},
        {"GLGetProgram","()I", (void*) GLGetProgram},
};


JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *jvm, void *reserved)
{
    javaVM = jvm;
    JNIEnv *env = NULL;
    if ((jvm)->GetEnv( (void**)&env, JNI_VERSION_1_6)){
        return JNI_ERR;
    }

    jclass cls = (env)->FindClass( "com/example/hellojni/EnhanceActivity");
    if (cls == NULL)
    {
        return JNI_ERR;
    }
    jint nRes = (env)->RegisterNatives( cls, gMethods, sizeof(gMethods)/sizeof(gMethods[0]));
    if (nRes < 0)
    {
        return JNI_ERR;
    }
//    if(jvm->AttachCurrentThread (&env, NULL)){
//
//    }
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
JNI_OnUnLoad(JavaVM *jvm, void *reserved)
{
    javaVM = NULL;
    JNIEnv *env = NULL;
    if ((jvm)->GetEnv( (void**)&env, JNI_VERSION_1_6)){
        return;
    }
    jclass cls = (env)->FindClass( "com/example/hellojni/EnhanceActivity");
    if (cls == NULL)
    {
        return;
    }
    jint nRes = (env)->UnregisterNatives(cls);
//    jvm->DetachCurrentThread ();
    return;
}
