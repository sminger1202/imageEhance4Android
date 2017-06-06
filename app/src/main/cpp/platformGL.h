//
// Created by 史明 on 2017/5/24.
//

#ifndef HELLO_JNI_PLATFORMGL_H
#define HELLO_JNI_PLATFORMGL_H

#include <GLES2/gl2.h>

class platformGL {

public:
    GLuint createProgram(const char* vertexSource, const char* fragmentSource);
    GLuint getProgram();
private:
    GLuint mProgram;

};
#endif //HELLO_JNI_PLATFORMGL_H
