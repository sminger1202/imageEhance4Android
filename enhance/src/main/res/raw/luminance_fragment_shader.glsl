#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 textureCoordinate;
uniform samplerExternalOES s_texture;
uniform vec3 weights;

void main() {
    float l = dot(weights , texture2D( s_texture, textureCoordinate ).xyz);
    gl_FragColor = vec4(l, l, l, 1.0);
}
