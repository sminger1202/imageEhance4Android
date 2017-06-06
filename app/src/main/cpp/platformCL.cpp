//
// Created by 史明 on 2017/5/23.
//

#include <CL/cl_platform.h>
#include <CL/cl.h>
#include <CL/cl_ext.h>
#include <string.h>
#include <CL/cl_gl.h>
#include <GLES2/gl2.h>
#include <EGL/egl.h>
#include "platformCL.h"
#include "utils.h"

const char *kernelSource =                                           "\n" \
"__kernel void SimpleIndexing (                                       \n" \
"   __global uchar4 *input,                                           \n" \
"   __global uchar4 *output,                                          \n" \
        "int srcWidth, int srcHeight, int dstWidth, int dstHeight     \n" \
"   )                                                                 \n" \
"{                                                                    \n" \
"   uint id = get_local_id(0) + get_group_id(0)*get_local_size(0);    \n" \
"   output[id] = input[id];                                           \n" \
"}                                                                    \n" \
"\n";


platform::platform() {
    mInitialized = false;
    mUseQcomExtShareBuffer = false;
}

bool platform::initDevice() {
    if ( mInitialized )
        return 0;
    cl_uint numPlatforms;
    bool ret = false;
    mStatus = clGetPlatformIDs( 0, NULL, &numPlatforms );
    if ( mStatus != CL_SUCCESS || numPlatforms == 0 ) {
        LOGE("platform", "OpenCL: Unable to query installed platforms.err code : %d \n", mStatus);
        return false;
    }
    mPlatforms = new cl_platform_id[ numPlatforms ];
    mStatus = clGetPlatformIDs( numPlatforms, mPlatforms, NULL );
    if ( mStatus != CL_SUCCESS ) {
        LOGE("platform", "OpenCL: Unable to query installed platforms. err code : %d \n", mStatus);
        return false;
    } else {
        LOGI("platform", "platform num %d, num1 addr.is :%p", numPlatforms, mPlatforms[0]);
    }
    /* Select the first OpenCL platform with a GPU device */
    for ( cl_uint i = 0; i < numPlatforms; ++i ) {
        cl_uint gpu_count = 0;
        mStatus = clGetDeviceIDs( mPlatforms[i], CL_DEVICE_TYPE_GPU, 0, NULL,
                                 &gpu_count );
        if ( mStatus != CL_SUCCESS || !gpu_count )
            continue;
        mDevices = new cl_device_id[ gpu_count ];
        mStatus = clGetDeviceIDs( mPlatforms[ i ], CL_DEVICE_TYPE_GPU, gpu_count,
                                 mDevices, NULL );
        if ( mStatus != CL_SUCCESS ) {
            continue;
        } else {
            LOGI("platform", "gpu count : %d", gpu_count);
        }

        /* Find a GPU device that supports our image formats */
        for ( cl_uint gpu = 0; gpu < gpu_count; gpu++ ) {
            cl_device_id device = mDevices[ gpu ];
            LOGI("platform", " %d,%d， %d", eglGetCurrentContext(), eglGetCurrentDisplay(), mPlatforms[i]);
            cl_context_properties properties[] = {
                    CL_GL_CONTEXT_KHR, (cl_context_properties) eglGetCurrentContext(), //获得OpenGL上下文
                    CL_EGL_DISPLAY_KHR, (cl_context_properties) eglGetCurrentDisplay(), //获得OpenGl设备信息
                    CL_CONTEXT_PLATFORM, (cl_context_properties) mPlatforms[i],  //获得平台信息
                    0};
            cl_context context = clCreateContext( properties, 1, &device, NULL, NULL,
                                                  &mStatus );
            if ( mStatus != CL_SUCCESS ) {
                LOGE("platform", "clCreateContext failed .err : %d", mStatus);
                continue;
            }
            mContext = context;
            mDevice = device;

            ret = true;
            break;
        }

        if ( ret ) {
            break;
        }
    }
    if ( ret ) {
        char devname[64];
        char devvendor[64];
        char driverversion[256];
        mStatus = clGetDeviceInfo(mDevice, CL_DEVICE_NAME, sizeof(devname),
                                 devname, NULL);
        mStatus |= clGetDeviceInfo(mDevice, CL_DEVICE_VENDOR, sizeof(devvendor),
                                  devvendor, NULL);
        if (mStatus != CL_SUCCESS) {
            LOGE("platform", "clGetDeviceInfo failed! %d\n", mStatus);
            return false;
        }
        mStatus |= clGetDeviceInfo(mDevice, CL_DRIVER_VERSION,
                                  sizeof(driverversion), driverversion, NULL);
        if (mStatus != CL_SUCCESS) {
            LOGE("OpenCL", "CL_DRIVER_VERSION failed! %d\n", mStatus);
            return false;
        }
        LOGI("platform", "OpenCL acceleration with %s %s\n", devvendor, devname);

        // Query the device's page size and the amount of padding necessary at the end of the buffer.
        mStatus = clGetDeviceInfo(mDevice, CL_DEVICE_PAGE_SIZE_QCOM, sizeof(device_page_size),
                                 &device_page_size, NULL);
        mStatus |= clGetDeviceInfo(mDevice, CL_DEVICE_EXT_MEM_PADDING_IN_BYTES_QCOM,
                                  sizeof(ext_mem_padding_in_bytes),
                                  &ext_mem_padding_in_bytes, NULL);
        if (mStatus != CL_SUCCESS) {
            LOGE("platform", "get CL_DEVICE_PAGE_SIZE_QCOM or "
                    "CL_DEVICE_EXT_MEM_PADDING_IN_BYTES_QCOM failed! %d\n", mStatus);
            mUseQcomExtShareBuffer = false;
        }

        mQueue = clCreateCommandQueue(mContext, mDevice,
                                      CL_QUEUE_PROFILING_ENABLE, &mStatus);
        if (!mQueue) {
            LOGE("platform", "cannot create opencl queue. err code : %d \n", mStatus);
            return false;
        }
    }
    mInitialized = true;
    return mInitialized;
}

void platform::initCLKernel() {
    /** Load kernel source and build the program */
    mProgram = clCreateProgramWithSource(mContext, 1, (const char**)&kernelSource, NULL, &mStatus);
    if(CL_SUCCESS != mStatus)
    {
        LOGE("platform", "clCreateProgramWithSource failed: error: %d \n", mStatus);
        return;
    }
    else
    {
        LOGI("platform","clCreateProgramWithSource succeed: programID: %x", (unsigned int)mProgram);
    }

    mStatus = clBuildProgram(mProgram, 0, NULL, NULL, NULL, NULL);

    const char* buildOptionsArray = "-DINTER_NEAREST -cl-fast-relaxed-math";
    if (mProgram) {
        cl_int status = clBuildProgram( mProgram, 1, &mDevice,
                                        buildOptionsArray, NULL, NULL );
        if ( status != CL_SUCCESS ) {
            LOGE("Unwrap", "there is some error in bulid program.\n");
            if ( status == CL_BUILD_PROGRAM_FAILURE ) {
                char * buildLog = NULL;
                size_t buildLogSize = 0;
                cl_int logState = clGetProgramBuildInfo( mProgram, mDevice,
                                                         CL_PROGRAM_BUILD_LOG, buildLogSize, NULL,
                                                         &buildLogSize );
                if ( logState != CL_SUCCESS ) {
                    LOGE("platform", "get buildlog failed.\n");

                }

                buildLog = ( char * ) malloc( buildLogSize );
                memset( buildLog, '0', buildLogSize );
                logState = clGetProgramBuildInfo( mProgram, mDevice,
                                                  CL_PROGRAM_BUILD_LOG, buildLogSize, buildLog, NULL );

                LOGE("platform", " \n\t\t\tBUILD LOG\n");
                LOGE("platform", " ************************************************\n");
                LOGE("platform", "%s\n", buildLog);
                LOGE("platform", " ************************************************\n");
                free( buildLog );
            }
        }
        if ( status != CL_SUCCESS ) {
            LOGE("platform", "clBuildProgram failed.err code : %d\n", status);
            clReleaseProgram( mProgram );
            mProgram = 0;
            return;
        }
    }

    /** Create the kernel */
    mKernel = clCreateKernel(mProgram, "SimpleIndexing", &mStatus);
    if(CL_SUCCESS != mStatus)
    {
        LOGE("platform", "clCreateKernel failed: error: %d\n", mStatus);
        return;
    }
    else
    {
        LOGI("platform", "clCreateKernel succeed: kernelID: %x", (unsigned int)mKernel);
    }

}

GLuint getGLtextID() {

    GLuint  txtID;
    glGenTextures(1, &txtID);
    glActiveTexture(GL_TEXTURE_2D);
    glBindTexture(GL_TEXTURE_2D, txtID);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA,//allocate storage
                 640, 480, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
    return txtID;
}
void platform::initCLBuffer() {

//    mFboMem = clCreateFromGLTexture( mContext, CL_MEM_READ_ONLY, GL_TEXTURE_2D, 0, mSrcTxtID, &mStatus );
    if(CL_SUCCESS != mStatus) {
        LOGE("platform", "create Frame buffer object failed: err : %d", mStatus);
    } else {
        LOGI("platform", "create Frame buffer object success");
    }
//    mFilteredTexMem = clCreateFromGLTexture(mContext, CL_MEM_WRITE_ONLY, GL_TEXTURE_2D, 0, mDstTxtID, &mStatus);

    if(CL_SUCCESS != mStatus) {
        LOGE("platform", "create filter buffer object failed: err : %d", mStatus);
    } else {

        LOGI("platform", "create filter buffer object success");
    }
}

void platform::runCL() {
    glFinish();

    // Acquire the FBO and texture from OpenGL
    cl_mem memObjects[2] = {
            mFboMem,
            mFilteredTexMem
    };
    size_t Local[] = {0, 0, 0};
    size_t global[] = {mDstWidth, mDstHeight };
    mStatus = clEnqueueAcquireGLObjects(mQueue, 2, memObjects, 0, NULL, NULL);
    if (mStatus != CL_SUCCESS) {
        LOGE("platform", "lock gl buffer failed! err: %d", mStatus);
    }

    mStatus |= clSetKernelArg( mKernel, 0, sizeof(cl_mem), &mFboMem );
    mStatus |= clSetKernelArg( mKernel, 1, sizeof(cl_mem), &mFilteredTexMem );
    mStatus |= clSetKernelArg( mKernel, 2, sizeof(cl_int), &mSrcWidth );
    mStatus |= clSetKernelArg( mKernel, 3, sizeof(cl_int), &mSrcHeight );
    mStatus |= clSetKernelArg( mKernel, 3, sizeof(cl_int), &mDstWidth );
    mStatus |= clSetKernelArg( mKernel, 3, sizeof(cl_int), &mDstHeight );
    if (mStatus != CL_SUCCESS) {
        LOGE("platform", "set kernel arguments failed err: %d", mStatus);
    }

    size_t locallWorkSize[2] = { mDstWidth, mDstHeight };
    size_t globalWorkSize[2] = { mDstWidth, mDstHeight };
    mStatus = clEnqueueNDRangeKernel( mQueue, mKernel, 2, locallWorkSize, globalWorkSize,
                                     NULL, 0, NULL, NULL );
    if( mStatus != CL_SUCCESS ) {
        LOGE("platform", "Error queuing kernel for execution." );
        return ;
    }

    // Finish executing kernel
    clFinish( mQueue );
    mStatus = clEnqueueReleaseGLObjects( mQueue, 2, &memObjects[0], 0, NULL, NULL );
    if( mStatus != CL_SUCCESS ) {
        LOGE("platform", "Error releasing VBO and texture from OpenCL back to OpenGL." );
        return ;
    }
}

void platform::setTextureID(GLuint srcTxtID, GLuint dstTxtID) {
    mSrcTxtID = srcTxtID;
    mDstTxtID = dstTxtID;
}
void platform::cleanupCL() {

    //release the cl mem pointing to GL buf;
    if (mFilteredTexMem != NULL) {
        clReleaseMemObject(mFilteredTexMem);
        mFilteredTexMem = NULL;
    }

    if (mFboMem != NULL) {
        clReleaseMemObject(mFboMem);
        mFboMem = NULL;
    }

    if(mKernel != NULL) {
        clReleaseKernel(mKernel);
        mKernel = NULL;
    }

    if(mProgram != NULL) {
        clReleaseProgram(mProgram);
        mProgram = NULL;
    }

    if(mQueue != NULL) {
        clReleaseCommandQueue(mQueue);
        mQueue = NULL;
    }
    if(mContext != NULL) {
        clReleaseContext(mContext);
        mContext = NULL;
    }
}
bool platform::getDeviceInfo() {
    LOGI("platform","getDevicesInfo");

    return 0;

}