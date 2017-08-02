//
// Created by 史明 on 2017/5/23.
//

#ifndef HELLO_JNI_PLATFORM_H
#define HELLO_JNI_PLATFORM_H

#include "CL/cl.h"
#include <cstring>


static size_t ext_mem_padding_in_bytes = 0;
static size_t device_page_size         = 0;

class platform {
public:
    platform();
    bool initDevice();
    void initCLBuffer();
    void initCLKernel();
    void runCL();
    void cleanupCL();
    void verify();

    inline void setSrcDstSize(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
        mSrcWidth = srcWidth;
        mSrcHeight = srcHeight;
        mDstWidth = dstWidth;
        mDstHeight = dstHeight;
    };
    void setTextureID(GLuint srcTxtID, GLuint dstTxtID);
    bool getDeviceInfo();

private:
    bool mInitialized;
    int mMaxGlobalSize;
    int mMaxLocalSize;
    bool mUseQcomExtShareBuffer;
    cl_int mStatus;
    cl_platform_id* mPlatforms;
    cl_device_id* mDevices;
    cl_context mContext;
    cl_device_id mDevice;
    cl_command_queue mQueue;
    cl_program mProgram;
    cl_kernel mKernel;
    /** GL Texture id & CL memory**/
    int mSrcWidth;
    int mSrcHeight;
    int mDstWidth;
    int mDstHeight;
    cl_GLuint mSrcTxtID;
    cl_GLuint mDstTxtID;
    cl_mem mFboMem;
    cl_mem mFilteredTexMem;

};

#endif //HELLO_JNI_PLATFORM_H
