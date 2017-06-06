//
// Created by 史明 on 2017/5/26.
//

//{
//return env->NewStringUTF("Enhance from JNI !  Compiled with ABI " ABI "." );
//}


#include <jni.h>
#include <cstring>
#include "utils.h"
const char* TAG="shader";

JNIEXPORT jstring JNICALL
getVertexShader( JNIEnv* env, jobject thiz ) {
    return env->NewStringUTF("uniform mat4 uMVPMatrix;\n"
                             "uniform mat4 uTexMatrix;\n"
                             "attribute vec4 aPosition;\n"
                             "attribute vec4 aTextureCoord;\n"
                             "varying vec2 vTextureCoord;\n"
                             "void main() {\n"
                             "    gl_Position = uMVPMatrix * aPosition;\n"
                             "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n"
                             "}\n");
}
JNIEXPORT jstring JNICALL
getFragmentShader( JNIEnv* env, jobject thiz ) {
    return env->NewStringUTF("#extension GL_OES_EGL_image_external : require\n"
                             "precision mediump float;\n"
                             "varying vec2 vTextureCoord;\n"
                             "uniform samplerExternalOES sTexture;\n"
                             "uniform float dx;\n"
                             "uniform float dy;\n"
                             "vec4 getPixel(float dx, float dy)  \n"
                             "    { return texture2D(sTexture, vTextureCoord + vec2(dx, dy));}\n"
                             "void main() {\n"
                             "    vec4 five = vec4(5, 5, 5, 5);\n"
                             "    vec4 left = getPixel(-dx, 0.0);\n"
                             "    vec4 right = getPixel(+dx, 0.0);\n"
                             "    vec4 top = getPixel(0.0, -dy);\n"
                             "    vec4 down = getPixel(0.0, dy);\n"
                             "    vec4 center = getPixel(0.0, 0.0);\n"
                             "    gl_FragColor = clamp(five * center - left - right - top - down, 0.0, 1.0);\n"
                             "}\n" );
}
static JNINativeMethod gMethods[] = {
        {"getVertexShader", "()Ljava/lang/String;",(void*)getVertexShader},
        {"getFragmentShader", "()Ljava/lang/String;",(void*)getFragmentShader},
};


JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *jvm, void *reserved)
{
    JNIEnv *env = NULL;
    if ((jvm)->GetEnv( (void**)&env, JNI_VERSION_1_6)){
        LOGE(TAG, "%d", __LINE__);
        return JNI_ERR;
    }
    jclass cls = (env)->FindClass( "com/android/enhance/EnhanceEngine");
    if (cls == NULL)
    {
        LOGE(TAG, "%d", __LINE__);
        return JNI_ERR;
    }
    jint nRes = (env)->RegisterNatives( cls, gMethods, sizeof(gMethods)/sizeof(gMethods[0]));
    if (nRes < 0)
    {
        LOGE(TAG, "%d", __LINE__);
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
JNI_OnUnLoad(JavaVM *jvm, void *reserved)
{
JNIEnv *env = NULL;
if ((jvm)->GetEnv( (void**)&env, JNI_VERSION_1_6)){
return;
}
jclass cls = (env)->FindClass( "com/android/enhance/EnhanceEngine");
if (cls == NULL)
{
return;
}
jint nRes = (env)->UnregisterNatives(cls);
return;
}
