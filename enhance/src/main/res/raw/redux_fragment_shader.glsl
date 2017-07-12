precision mediump float;
varying vec2 textureCoordinate;
uniform sampler2D textureImg;
uniform float dx;
uniform float dy;
void main() {
    vec4  color;
    vec2 coords = 2.0 * textureCoordinate;

    vec4  color00 = texture2D(textureImg, coords);

    vec4  color01 = texture2D(textureImg, coords + vec2(dx,  0.0));

    vec4  color10 = texture2D(textureImg, coords + vec2(0.0, dy));

    vec4  color11 = texture2D(textureImg, coords + vec2(dx,  dy));

//    ___REDUX_OPERATION___;
    color = max(color00, color10);
    color = max(color, color01);
    color = max(color, color11);
    color = (coords == vec2(1.0, 1.0)) ? color00 : color;

    gl_FragColor = vec4(color.xyz, 0.4);

}
