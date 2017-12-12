//
// Created by 史明 on 2017/5/26.
//

#include <jni.h>
#include "utils.h"
#define FRAGMENT(name) name##_fragment
#define VERTEX(name)   name##_vertex
const char* TAG="shader";

const char* FRAGMENT(enhance) = "#version 320 es\n"
        "#extension GL_OES_EGL_image_external_essl3 : enable\n"
        "precision mediump float;\n"
        "uniform float coef;\n"
        "in vec2 vTextureCoord;\n"
        "in vec4 vPosition;\n"
        "out vec4 gl_FragColor;\n"
        "uniform samplerExternalOES sTexture;\n"
        "\n"
        "/**\n"
        " * opengl essl 3.0 按面片取像素\n"
        " */\n"
        "\n"
        "//void main() {\n"
        "//\n"
        "//     ivec2 coords  = ivec2(gl_FragCoord.xy + vec2(0.5, 0.5));\n"
        "//     vec4 left  = texelFetch(sTexture, coords + ivec2(-1,  0), 0);\n"
        "//     vec4 right = texelFetch(sTexture, coords + ivec2( 1,  0), 0);\n"
        "//     vec4 top   = texelFetch(sTexture, coords + ivec2( 0,  1), 0);\n"
        "//     vec4 down  = texelFetch(sTexture, coords + ivec2( 0, -1), 0);\n"
        "//     vec4 center = texelFetch(sTexture, coords, 0);\n"
        "//     vec4 effect = coef * (4.0 * center - left - right - top - down) ;\n"
        "//\n"
        "//     gl_FragColor = clamp(center + effect, 0.0, 1.0);\n"
        "//     //测试使用\n"
        "////     gl_FragColor = center;\n"
        "//}\n"
        "\n"
        "\n"
        "/**\n"
        " *opengl essl 3.0 分屏\n"
        " */\n"
        "//void main() {\n"
        "//    vec4 left,right, top, down,center;\n"
        "//    if(vPosition.x < 0.0) {\n"
        "//        left  = textureOffset(sTexture, vec2(2.0, 1.0)  * vTextureCoord, ivec2(-1,  0));\n"
        "//        right = textureOffset(sTexture, vec2(2.0, 1.0)  * vTextureCoord, ivec2( 1,  0));\n"
        "//        top   = textureOffset(sTexture, vec2(2.0, 1.0)  * vTextureCoord, ivec2( 0,  1));\n"
        "//        down  = textureOffset(sTexture, vec2(2.0, 1.0)  * vTextureCoord, ivec2( 0, -1));\n"
        "//        center = texture(sTexture, vec2(2.0, 1.0) * vTextureCoord);\n"
        "//    } else {\n"
        "//        left  = textureOffset(sTexture, vec2(2.0, 1.0) * (vTextureCoord - vec2(0.5, 0.0)), ivec2(-1,  0));\n"
        "//        right = textureOffset(sTexture, vec2(2.0, 1.0) * (vTextureCoord - vec2(0.5, 0.0)), ivec2( 1,  0));\n"
        "//        top   = textureOffset(sTexture, vec2(2.0, 1.0) * (vTextureCoord - vec2(0.5, 0.0)), ivec2( 0,  1));\n"
        "//        down  = textureOffset(sTexture, vec2(2.0, 1.0) * (vTextureCoord - vec2(0.5, 0.0)), ivec2( 0, -1));\n"
        "//        center = texture(sTexture, vec2(2.0, 1.0) * (vTextureCoord - vec2(0.5, 0.0)));\n"
        "//    }\n"
        "//    vec4 effect = coef * (4.0 * center - left - right - top - down);\n"
        "//    gl_FragColor = clamp(center + effect, 0.0, 1.0);\n"
        "//}\n"
        "\n"
        "/**\n"
        " * openglsl 3.0 外部传入dx, dy\n"
        " *／\n"
        "//vec4 getPixel(float dx, float dy) {\n"
        "//    return texture(sTexture, vTextureCoord + vec2(dx, dy));\n"
        "//}\n"
        "//void main() {\n"
        "//     vec4 left = getPixel(-dx, 0.0);\n"
        "//     vec4 right = getPixel(+dx, 0.0);\n"
        "//     vec4 top = getPixel(0.0, -dy);\n"
        "//     vec4 down = getPixel(0.0, dy);\n"
        "//     vec4 center = getPixel(0.0, 0.0);\n"
        "//     vec4 effect = coef * (4.0 * center - left - right - top - down);\n"
        "//     gl_FragColor = clamp(center + effect, 0.0, 1.0);\n"
        "//}\n"
        "\n"
        "/**\n"
        " * openglsl 3.0 使用textureOffset\n"
        " */\n"
        "\n"
        "void main() {\n"
        "    vec4 left,right, top, down,center;\n"
        "\n"
        "    left  = textureOffset(sTexture, vTextureCoord, ivec2(-1,  0));\n"
        "    right = textureOffset(sTexture, vTextureCoord, ivec2( 1,  0));\n"
        "    top   = textureOffset(sTexture, vTextureCoord, ivec2( 0,  1));\n"
        "    down  = textureOffset(sTexture, vTextureCoord, ivec2( 0, -1));\n"
        "    center = texture(sTexture, vTextureCoord);\n"
        "\n"
        "    vec4 effect = coef * (4.0 * center - left - right - top - down);\n"
        "    gl_FragColor = clamp(center + effect, 0.0, 1.0);\n"
        "}";
const char* VERTEX(enhance)   = "#version 320 es\n"
        "//\n"
        "uniform mat4 uMVPMatrix;\n"
        "uniform mat4 uTexMatrix;\n"
        "in vec4 aPosition;\n"
        "in vec4 aTextureCoord;\n"
        "out vec2 vTextureCoord;\n"
        "out vec4 vPosition;\n"
        "\n"
        "void main() {\n"
        "    gl_Position = uMVPMatrix * aPosition;\n"
        "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n"
        "    vPosition = gl_Position;\n"
        "}";


const char* VERTEX(video8k)   = "#version 320 es\n"
        "//\n"
        "in vec4 aPosition;\n"
        "in vec4 aTextureCoord;\n"
        "out vec2 vTextureCoord;\n"
        "\n"
        "void main() {\n"
        "    gl_Position = aPosition;\n"
        "    vTextureCoord = aTextureCoord.xy;\n"
        "}";

const char* FRAGMENT(video8k) = "#version 320 es\n"
        "#extension GL_OES_EGL_image_external_essl3 : enable\n"
        "precision mediump float;\n"
        "in vec2 vTextureCoord;\n"
        "out vec4 gl_FragColor;\n"
        "uniform samplerExternalOES sTexture;\n"
        "\n"
        "void main() {\n"
        "    vec4 center;\n"
        "    center = texture(sTexture, vTextureCoord);\n"
        "    gl_FragColor = clamp(center, 0.0, 1.0);\n"
        "}";

JNIEXPORT jstring JNICALL
getVertexShader( JNIEnv* env, jobject thiz, jstring name) {
    std::string nameStr = jstringToChar(env, name);
    if(nameStr.compare("enhance") == 0) {
        return env->NewStringUTF(VERTEX(enhance));
    }
    if(nameStr.compare("video8k") == 0) {
        return env->NewStringUTF(VERTEX(video8k));
    }

//    return env->NewStringUTF("uniform mat4 uMVPMatrix;\n"
//                             "uniform mat4 uTexMatrix;\n"
//                             "attribute vec4 aPosition;\n"
//                             "attribute vec4 aTextureCoord;\n"
//                             "varying vec2 vTextureCoord;\n"
//                             "void main() {\n"
//                             "    gl_Position = uMVPMatrix * aPosition;\n"
//                             "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n"
//                             "}\n");
}
JNIEXPORT jstring JNICALL
getFragmentShader( JNIEnv* env, jobject thiz, jstring name) {
    std::string nameStr = jstringToChar(env, name);
    LOGE(TAG, "shader name : %s", nameStr.c_str());
    if (nameStr.compare("enhance") == 0) {
        return env->NewStringUTF(FRAGMENT(enhance));
    }
    if (nameStr.compare("video8k") == 0) {
        return env->NewStringUTF(FRAGMENT(video8k));
    }

//    return env->NewStringUTF("#extension GL_OES_EGL_image_external : require\n"
//                             "precision mediump float;\n"
//                             "varying vec2 vTextureCoord;\n"
//                             "uniform samplerExternalOES sTexture;\n"
//                             "uniform float dx;\n"
//                             "uniform float dy;\n"
//                             "uniform float coef;\n"
//                             "vec4 getPixel(float dx, float dy)  \n"
//                             "    { return texture2D(sTexture, vTextureCoord + vec2(dx, dy));}\n"
//                             "void main() {\n"
//                             "    vec4 left = getPixel(-dx, 0.0);\n"
//                             "    vec4 right = getPixel(+dx, 0.0);\n"
//                             "    vec4 top = getPixel(0.0, -dy);\n"
//                             "    vec4 down = getPixel(0.0, dy);\n"
//                             "    vec4 center = getPixel(0.0, 0.0);\n"
//                             "    vec4 effect = coef * (4.0 * center - left - right - top - down);\n"
//                             "    gl_FragColor = clamp(center + effect, 0.0, 1.0);\n"
//                             "}\n" );
}
static JNINativeMethod gMethods[] = {
        {"getVertexShader", "(Ljava/lang/String;)Ljava/lang/String;",(void*)getVertexShader},
        {"getFragmentShader", "(Ljava/lang/String;)Ljava/lang/String;",(void*)getFragmentShader},
};


JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *jvm, void *reserved)
{
    JNIEnv *env = NULL;
    if ((jvm)->GetEnv( (void**)&env, JNI_VERSION_1_6)){
        LOGE(TAG, "%d", __LINE__);
        return JNI_ERR;
    }
    jclass cls = (env)->FindClass( "com/android/enhance/utils/SharderContainer");
    if (cls == NULL)
    {
        LOGE(TAG, "do not find class  %d", __LINE__);
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
    jclass cls = (env)->FindClass( "com/android/enhance/utils/SharderContainer");
    if (cls == NULL)
    {
        LOGE(TAG, "do not find class  %d", __LINE__);
        return;
    }
    jint nRes = (env)->UnregisterNatives(cls);
    return;
}
