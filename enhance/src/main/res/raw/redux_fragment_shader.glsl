#version 300 es
#define OPS(x,y) yourMethod
precision highp float;
uniform sampler2D textureImg;
out vec4 glfragColor;

void main() {

    vec4  color;

    ivec2 size = textureSize(textureImg, 0);

    ivec2 texelCoords = ivec2(gl_FragCoord) * 2 ;

    vec4  color00 = texelFetch(textureImg, texelCoords, 0);

    vec4  color01 = texelFetch(textureImg, texelCoords + ivec2(0, 1), 0);

    vec4  color10 = texelFetch(textureImg, texelCoords + ivec2(1, 0), 0);

    vec4  color11 = texelFetch(textureImg, texelCoords + ivec2(1, 1), 0);

___REDUX_OPERATION___

    color = size.x == texelCoords.x + 1 ?
    size.y == texelCoords.y + 1 ? color00 : OPS(color00, color01) :
    size.y == texelCoords.y + 1 ? OPS(color00, color10) : color;

    glfragColor = vec4(color.xyz, 0);
}


