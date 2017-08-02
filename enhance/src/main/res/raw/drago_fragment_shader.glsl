#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 textureCoordinate;
uniform samplerExternalOES s_texture;
uniform sampler2D u_lum;
uniform vec4 pars;
//uniform float	  constant1;
//uniform float	  constant2;
//uniform float     LMax;
//uniform float     Lwa;
//
//void main() {
//    vec3  color = texture2D( s_texture, textureCoordinate ).xyz;
//    float L = texture2D(u_lum, textureCoordinate).x;
//    float L_scaled = L / Lwa;
//    float tmp      = pow((L_scaled / LMax), constant1);
//    float Ld       = constant2 * log(1.0 + L_scaled) / log(2.0 + 8.0 * tmp);
//    color		   = (color * Ld) / L;
//    gl_FragColor = vec4(color, 1.0);
//}

//vec4 pars: constant1, constant2, LMax, Lwa

void main() {
    vec3  color = pow(texture2D( s_texture, textureCoordinate ).xyz, vec3(2.2,2.2,2.2));
    float L = texture2D(u_lum, textureCoordinate).x;
    float L_scaled = L / pars.w;
    float tmp      = pow((L_scaled / pars.z), pars.x);
    float Ld       = pars.y * log(1.0 + L_scaled) / log(2.0 + 8.0 * tmp);
    color		   = (color * Ld) / L;
    color = pow(color,vec3(1.0/2.2, 1.0/2.2, 1.0/2.2));
    gl_FragColor = vec4(color, 1.0);
}
