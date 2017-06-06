//
// Created by 史明 on 2017/5/24.
//
#include <GLES2/gl2.h>
#include <EGL/egl.h>
#include "utils.h"
#include "platformGL.h"

const char* mVertexShader =  "\n" \
"uniform mat4 uMVPMatrix;     \n" \
"uniform mat4 uSTMatrix;      \n" \
"attribute vec4 aPosition;    \n" \
"attribute vec4 aTextureCoord;\n" \
"varying vec2 vTextureCoord;  \n" \
"void main() {                \n" \
"  gl_Position = uMVPMatrix * aPosition;          \n" \
"  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" \
"}                                                \n";

const char* mFragmentShader =                 "\n" \
"#extension GL_OES_EGL_image_external : require\n" \
"precision mediump float;                      \n" \
"varying vec2 vTextureCoord;                   \n" \
"uniform samplerExternalOES sTexture;          \n" \
"void main() {                                 \n" \
"  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" \
"}\n";
const char* TAG = "platformGL";

bool linkShaderProgram( uint32_t hShaderProgram )
{
    // Link the whole program together
    glLinkProgram( hShaderProgram );

    // Check for link success
    GLint LinkStatus;
    glGetProgramiv( hShaderProgram, GL_LINK_STATUS, &LinkStatus );
    if(false == LinkStatus )
    {
        char  strInfoLog[1024];
        int nLength;
        glGetProgramInfoLog( hShaderProgram, 1024, &nLength, strInfoLog );
        LOGE( strInfoLog, "\n" );
        return false;
    }

    return true;
}

bool loadShader(const char* strShaderSource, GLuint hShaderHandle) {
    glShaderSource( hShaderHandle, 1, &strShaderSource, NULL );

    glCompileShader( hShaderHandle );

    // Check for compile success
    GLint nCompileResult = 0;
    glGetShaderiv( hShaderHandle, GL_COMPILE_STATUS, &nCompileResult );
    if( 0 == nCompileResult )
    {
        char strInfoLog[1024];
        GLint nLength;
        glGetShaderInfoLog( hShaderHandle, 1024, &nLength, strInfoLog );
        LOGE(TAG, "shader failed : %s \n\n", strInfoLog );
        return false;
    }
    return true;

}

GLuint platformGL::createProgram(const char* vertexSource, const char* fragmentSource) {
    GLuint hVertexShader   = glCreateShader( GL_VERTEX_SHADER );
    GLuint hFragmentShader = glCreateShader( GL_FRAGMENT_SHADER );

    if( !loadShader( vertexSource, hVertexShader ) )
    {
        glDeleteShader( hVertexShader );
        glDeleteShader( hFragmentShader );
        return 0;
    }
    if( !loadShader( fragmentSource, hFragmentShader ) )
    {
        glDeleteShader( hVertexShader );
        glDeleteShader( hFragmentShader );
        return 0;
    }

    // Attach the individual shaders to the common shader program
    GLuint hShaderProgram  = glCreateProgram();
    glAttachShader( hShaderProgram, hVertexShader );
    glAttachShader( hShaderProgram, hFragmentShader );

    // Link the vertex shader and fragment shader together
    if(!linkShaderProgram( hShaderProgram ) )
    {
        glDeleteProgram( hShaderProgram );
        return 0;
    }

    return  hShaderProgram;

}
GLuint platformGL::getProgram() {

    return createProgram(mVertexShader, mFragmentShader);
}