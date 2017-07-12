#version 320 es
#extension GL_OES_EGL_image_external_essl3 : enable
precision mediump float;
//uniform float dx;
//uniform float dy;
uniform float coef;
in vec2 vTextureCoord;
in vec4 vPosition;
out vec4 gl_FragColor;
uniform samplerExternalOES sTexture;

/**
 * opengl essl 3.0 按面片取像素
 */

//void main() {
//
//     ivec2 coords  = ivec2(gl_FragCoord.xy + vec2(0.5, 0.5));
//     vec4 left  = texelFetch(sTexture, coords + ivec2(-1,  0), 0);
//     vec4 right = texelFetch(sTexture, coords + ivec2( 1,  0), 0);
//     vec4 top   = texelFetch(sTexture, coords + ivec2( 0,  1), 0);
//     vec4 down  = texelFetch(sTexture, coords + ivec2( 0, -1), 0);
//     vec4 center = texelFetch(sTexture, coords, 0);
//     vec4 effect = coef * (4.0 * center - left - right - top - down) ;
//
//     gl_FragColor = clamp(center + effect, 0.0, 1.0);
//     //测试使用
////     gl_FragColor = center;
//}


/**
 *opengl essl 3.0 分屏
 */
//void main() {
//    vec4 left,right, top, down,center;
//    if(vPosition.x < 0.0) {
//        left  = textureOffset(sTexture, vec2(2.0, 1.0)  * vTextureCoord, ivec2(-1,  0));
//        right = textureOffset(sTexture, vec2(2.0, 1.0)  * vTextureCoord, ivec2( 1,  0));
//        top   = textureOffset(sTexture, vec2(2.0, 1.0)  * vTextureCoord, ivec2( 0,  1));
//        down  = textureOffset(sTexture, vec2(2.0, 1.0)  * vTextureCoord, ivec2( 0, -1));
//        center = texture(sTexture, vec2(2.0, 1.0) * vTextureCoord);
//    } else {
//        left  = textureOffset(sTexture, vec2(2.0, 1.0) * (vTextureCoord - vec2(0.5, 0.0)), ivec2(-1,  0));
//        right = textureOffset(sTexture, vec2(2.0, 1.0) * (vTextureCoord - vec2(0.5, 0.0)), ivec2( 1,  0));
//        top   = textureOffset(sTexture, vec2(2.0, 1.0) * (vTextureCoord - vec2(0.5, 0.0)), ivec2( 0,  1));
//        down  = textureOffset(sTexture, vec2(2.0, 1.0) * (vTextureCoord - vec2(0.5, 0.0)), ivec2( 0, -1));
//        center = texture(sTexture, vec2(2.0, 1.0) * (vTextureCoord - vec2(0.5, 0.0)));
//    }
//    vec4 effect = coef * (4.0 * center - left - right - top - down);
//    gl_FragColor = clamp(center + effect, 0.0, 1.0);
//}

/**
 * openglsl 3.0 外部传入dx, dy
 *／
//vec4 getPixel(float dx, float dy) {
//    return texture(sTexture, vTextureCoord + vec2(dx, dy));
//}
//void main() {
//     vec4 left = getPixel(-dx, 0.0);
//     vec4 right = getPixel(+dx, 0.0);
//     vec4 top = getPixel(0.0, -dy);
//     vec4 down = getPixel(0.0, dy);
//     vec4 center = getPixel(0.0, 0.0);
//     vec4 effect = coef * (4.0 * center - left - right - top - down);
//     gl_FragColor = clamp(center + effect, 0.0, 1.0);
//}

/**
 * openglsl 3.0 使用textureOffset
 */

void main() {
    vec4 left,right, top, down,center;

    left  = textureOffset(sTexture, vTextureCoord, ivec2(-1,  0));
    right = textureOffset(sTexture, vTextureCoord, ivec2( 1,  0));
    top   = textureOffset(sTexture, vTextureCoord, ivec2( 0,  1));
    down  = textureOffset(sTexture, vTextureCoord, ivec2( 0, -1));
    center = texture(sTexture, vTextureCoord);

    vec4 effect = coef * (4.0 * center - left - right - top - down);
    gl_FragColor = clamp(center + effect, 0.0, 1.0);
}