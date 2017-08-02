package com.example.playerdemo;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES30.GL_ARRAY_BUFFER;
import static android.opengl.GLES30.GL_TEXTURE_2D;
import static android.opengl.GLES30.glFinish;

/**
 * Created by shiming on 2017/5/18.
 */

public class GLUtil {

    final static String TAG = "GLUtil";

    public static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    " //   vPosition = gl_Position;\n" +
                    "}\n";
    public static final String FRAGMENT_SHADER_INNER =
            "#extension GL_OES_EGL_image_external : require                                      \n" +
                    "precision mediump float;                                                    \n" +
                    "varying vec2 vTextureCoord;                                                 \n" +
                    "uniform sampler2D  sTexture;                                                \n" +
                    "void main() {                                                               \n" +
                    "    if(vTextureCoord.x < 0.5) {                                                   \n" +
                    "         gl_FragColor = texture2D(sTexture, vec2(2.0, 1.0) * vTextureCoord);\n" +
                    "    } else {                                                                \n" +
                    "         gl_FragColor = texture2D(sTexture, vec2(2.0, 1.0) * (vTextureCoord - vec2(0.5, 0.0)));\n" +
                    "   }                                                                        \n" +
                    "   //gl_FragColor = texture2D(sTexture,  vec2(2.0, 1.0) * vTextureCoord);\n" +
                    "}\n";
    public static final String FRAGMENT_SHADER_EXT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES  sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    private static final float VERTICES_DATA[] = {
            // X, Y, U, V
            -1.0f, -1.0f, 0.f, 0.f,
            1.0f, -1.0f, 1.f, 0.f,
            -1.0f, 1.0f, 0.f, 1.f,
            1.0f, 1.0f, 1.f, 1.f,
    };

//    private static final float VERTICES_DATA[] = {
//            // X, Y, U, V
//            -1.0f, -1.0f, 0.f, 1.f,
//            1.0f, -1.0f, 1.f, 1.f,
//            -1.0f, 1.0f, 0.f, 0.f,
//            1.0f, 1.0f, 1.f, 0.f,
//    };


//    private static final float VERTICES_DATA[] = {
//            // X, Y, U, V
//            -1.0f, -1.0f, 0.f, 1.f,
//            0.0f, -1.0f, 1.5f, 1.f
//            -1.0f, 1.0f, 0.f, 0.0f,
//            0.0f, 1.0f,  1.0f, 1.0f,
//            1.0f, -1.0f, 1.f, 0.f,
//            1.0f, 1.0f, 1.f, 1.f,
//    };
    private static final FloatBuffer VerticesBuffer =
            createFloatBuffer(VERTICES_DATA);


    private static int mFrameBufferObj;
    private static int mVBO;

    public static final float[] IDENTITY_MATRIX;

    public static String videoPath = "/sdcard/2014国际足联球迷庆典.mp4";
    static {
        IDENTITY_MATRIX = new float[16];
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
//        IDENTITY_MATRIX[0] = 0.5f;
//        IDENTITY_MATRIX[5] = 0.5f;
//        IDENTITY_MATRIX[10] = 0.5f;
//        IDENTITY_MATRIX[1] = 0.7f;
//        IDENTITY_MATRIX[4] = 0.7f;
    }

    static int mvpMatrixLocExt;
    static int texMatrixLocExt;
    static int positionLocExt;
    static int textureCoordLocExt;

    static int mvpMatrixLocIn;
    static int texMatrixLocIn;
    static int positionLocIn;
    static int textureCoordLocIn;

    static public boolean sIsOpenClPrepared = false;
    static public boolean sIsOpenGlPrepared = false;
    static public boolean sIsEnhance = true;
    static public boolean sWithCopy = false;
    static int dxLoc;
    static int dyLoc;
    static int mvpMatrixLocEhn;
    static int texMatrixLocEhn;
    static int positionLocEhn;
    static int textureCoordLocEhn;

    static int vertexCount;
    static int coordsPerVertex;
    static int vertexStride;
    static int texCoordStride;
    static FloatBuffer vertexArray;
    static FloatBuffer texCoordArray;

    static int vertexSize;
    static int colorPerVertex;

    static int sWidth = 600;
    static int sHeight = 300;
    static int videoWidth = 600;
    static int videoHeight = 300;
    static boolean isChanged = true;
    static public ByteBuffer mData;
    static public boolean sShapeChanged = true;
    private static final int SIZEOF_FLOAT = 4;

    static public void initDrawable() {

        coordsPerVertex = 2;
        colorPerVertex = 2;
        vertexSize = coordsPerVertex + colorPerVertex; // x, y, u, v
        vertexCount = VERTICES_DATA.length / vertexSize;
        vertexStride = vertexSize * SIZEOF_FLOAT;
        if(GLUtil.mData != null ){
            GLUtil.mData.clear();
        }
        mData = ByteBuffer.allocateDirect(sWidth * sWidth * 4).order(ByteOrder.nativeOrder());
        mFrameBufferObj = createFBO();
        mVBO = createVBO();
        loadDataVBF(mVBO);
    }

    /**
     * 加载vbf数据到缓存中
     * @param vbo
     */
    static public void loadDataVBF(int vbo) {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, VERTICES_DATA.length * SIZEOF_FLOAT,
                VerticesBuffer, GLES30.GL_STATIC_DRAW);
    }
    /**
     * Issues the draw call.  Does the full setup on every call.
     *
     * @param mvpMatrix       The 4x4 projection matrix.
     * @param vertexBuffer    Buffer with vertex position data.
     * @param firstVertex     Index of first vertex to use in vertexBuffer.
     * @param vertexCount     Number of vertices in vertexBuffer.
     * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
     * @param vertexStride    Width, in bytes, of the position data for each vertex (often
     *                        vertexCount * sizeof(float)).
     * @param texMatrix       A 4x4 transformation matrix for texture coords.  (Primarily intended
     *                        for use with SurfaceTexture.)
     * @param texBuffer       Buffer with vertex texture data.
     * @param texStride       Width, in bytes, of the texture data for each vertex.
     */
    static void draw(int programHandle, float[] mvpMatrix,float[] texMatrix, int textureId) {
        // Select the program.

        GLES30.glUseProgram(programHandle);
        checkGlError("glUseProgram");

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBO);
        // Set the texture.
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId);

        // Copy the model / view / projection matrix over.
        GLES30.glUniformMatrix4fv(mvpMatrixLocExt, 1, false, mvpMatrix, 0);
        checkGlError("glUniformMatrix4fv");

        // Copy the texture transformation matrix over.
        GLES30.glUniformMatrix4fv(texMatrixLocExt, 1, false, texMatrix, 0);
        checkGlError("glUniformMatrix4fv");

        // Enable the "aPosition" vertex attribute.
        GLES30.glEnableVertexAttribArray(positionLocExt);
        checkGlError("glEnableVertexAttribArray");

            // Connect vertexBuffer to "aPosition".
        GLES30.glVertexAttribPointer(positionLocExt, coordsPerVertex,
                GLES30.GL_FLOAT, false, vertexCount * SIZEOF_FLOAT, 0);
        checkGlError("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        GLES30.glEnableVertexAttribArray(textureCoordLocExt);
        checkGlError("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        GLES30.glVertexAttribPointer(textureCoordLocExt, colorPerVertex,
                GLES30.GL_FLOAT, false, vertexCount * SIZEOF_FLOAT, coordsPerVertex * SIZEOF_FLOAT);
        checkGlError("glVertexAttribPointer");

        // Draw the rect.
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertexCount);
        checkGlError("glDrawArrays");

        GLES30.glViewport(0, 0,
                sWidth, sHeight);
        // Done -- disable vertex array, texture, and program.
        GLES30.glDisableVertexAttribArray(positionLocExt);
        GLES30.glDisableVertexAttribArray(textureCoordLocExt);
        GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
        GLES30.glBindBuffer(GL_ARRAY_BUFFER, 0);
        GLES30.glUseProgram(0);
    }

    static void drawWithCopy( float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex,
                     int vertexCount, int coordsPerVertex, int vertexStride,
                     float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride, int textureIddst) {

        GLES30.glFinish();

        if (GLUtil.sIsEnhance) {
            copyEnhance(textureId, textureIddst, videoWidth, videoHeight);
        } else {
            copy2( VarifyRender.mProgramCopy, mvpMatrix, texMatrix, textureId,
                    textureIddst,  mFrameBufferObj);
        }
//        if(sIsEnhance) {
//            saveImg("/sdcard/enhanced.rgba");
//        } else {
//            saveImg("/sdcard/unEnhance.rgba");
//        }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBO);
        GLES30.glUseProgram(VarifyRender.mProgramInner);
        checkGlError("glUseProgram");

//        GLES30.glViewport(500, 500,
//                videoWidth, videoHeight);

        GLES30.glViewport(0, 0,
                sWidth , sHeight);

        GLES30.glUniformMatrix4fv(mvpMatrixLocIn, 1, false, mvpMatrix, 0);
        checkGlError("glUniformMatrix4fv mvpMatrixLoc");

            // Copy the texture transformation matrix over.
        GLES30.glUniformMatrix4fv(texMatrixLocIn, 1, false, texMatrix, 0);
        checkGlError("glUniformMatrix4fv texMatrixLoc");

            // Enable the "aPosition" vertex attribute.
        GLES30.glEnableVertexAttribArray(positionLocIn);
        checkGlError("glEnableVertexAttribArray positionLoc");

        // Connect vertexBuffer to "aPosition".
        GLES30.glVertexAttribPointer(positionLocIn, coordsPerVertex,
                GLES30.GL_FLOAT, false, vertexStride, 0);
        checkGlError("glVertexAttribPointer positionLoc");

        // Enable the "aTextureCoord" vertex attribute.
        GLES30.glEnableVertexAttribArray(textureCoordLocIn);
        checkGlError("glEnableVertexAttribArray textureCoordLoc");

        // Connect texBuffer to "aTextureCoord".
        GLES30.glVertexAttribPointer(textureCoordLocIn, colorPerVertex,
                GLES30.GL_FLOAT, false, vertexStride, coordsPerVertex * SIZEOF_FLOAT);
        checkGlError("glVertexAttribPointer textureCoordLoc");
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);

        // Set the input texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        checkGlError("glActiveTexture");
        GLES30.glBindTexture(GL_TEXTURE_2D, textureIddst);
        checkGlError("glBindTexture");

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glClearColor(0, 0, 0, 0);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, firstVertex, vertexCount);

        // Done -- disable vertex array, texture, and program.
        GLES30.glDisableVertexAttribArray(positionLocIn);
        GLES30.glDisableVertexAttribArray(textureCoordLocIn);
        GLES30.glBindTexture(GL_TEXTURE_2D, 0);
        GLES30.glBindBuffer(GL_ARRAY_BUFFER, 0);
        GLES30.glUseProgram(0);
        GLES30.glFinish();
        isChanged = false;
    }

    static public void copyEnhance(int srcTextureId, int dstTextureId, int width, int height) {
        VarifyRender.mEnhanceEngine.apply(srcTextureId, dstTextureId, width, height);
//        VarifyRender.mDragoTMO.apply(srcTextureId, dstTextureId, width, height);
//        VarifyRender.mLuminance.apply(srcTextureId, dstTextureId, width, height);
        glFinish();
    }
    static public void copy(int programHandle, float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex,
                            int vertexCount, int coordsPerVertex, int vertexStride,
                            float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride, int textureIddst, int frameBufObj) {
        saveGLState();

        // Set the texture.
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GL_TEXTURE_2D, textureIddst);
        if (isChanged) {
            Log.d(TAG, "change dst texture:" + sWidth + "x" + sHeight);
            GLES30.glTexImage2D(GL_TEXTURE_2D, 0, GLES30.GL_RGBA,//allocate storage
                    sWidth, sHeight, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
            GLES30.glTexParameteri(GL_TEXTURE_2D,
                    GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GL_TEXTURE_2D,
                    GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GL_TEXTURE_2D,
                    GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GL_TEXTURE_2D,
                    GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBufObj);
        if (isChanged) {
            GLES30.glFramebufferTexture2D(
                    GLES30.GL_FRAMEBUFFER,
                    GLES30.GL_COLOR_ATTACHMENT0,
                    GL_TEXTURE_2D, textureIddst, 0);

            // check status
            int status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
            if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
                Log.e(TAG, "glCheckFramebufferStatus error" + status);
            }
        }

        if(sShapeChanged) {
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBO);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, VERTICES_DATA.length * SIZEOF_FLOAT,
                    vertexBuffer, GLES30.GL_STATIC_DRAW);
            sShapeChanged = false;
        }
        // Copy the model / view / projection matrix over.
        GLES30.glUseProgram(VarifyRender.mProgramExt);
        GLES30.glViewport(0, 0,
                sWidth, sHeight);
        checkGlError("glUseProgram");
        GLES30.glUniformMatrix4fv(mvpMatrixLocExt, 1, false, IDENTITY_MATRIX, 0);
        checkGlError("glUniformMatrix4fv mvpMatrixLoc");

        // Copy the texture transformation matrix over.
        GLES30.glUniformMatrix4fv(texMatrixLocExt, 1, false, IDENTITY_MATRIX, 0);
        checkGlError("glUniformMatrix4fv texMatrixLoc");

        // Enable the "aPosition" vertex attribute.
        GLES30.glEnableVertexAttribArray(positionLocExt);
        checkGlError("glEnableVertexAttribArray positionLoc");

        // Connect vertexBuffer to "aPosition".
        GLES30.glVertexAttribPointer(positionLocExt, coordsPerVertex,
                GLES30.GL_FLOAT, false, vertexStride, 0);
        checkGlError("glVertexAttribPointer positionLoc");

        // Enable the "aTextureCoord" vertex attribute.
        GLES30.glEnableVertexAttribArray(textureCoordLocExt);
        checkGlError("glEnableVertexAttribArray textureCoordLoc");

        // Connect texBuffer to "aTextureCoord".
        GLES30.glVertexAttribPointer(textureCoordLocExt, colorPerVertex,
                GLES30.GL_FLOAT, false, vertexStride, coordsPerVertex * SIZEOF_FLOAT);
        checkGlError("glVertexAttribPointer textureCoordLoc");


        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glClearColor(0, 0, 0, 0);
        // connect 'VideoTexture' to video source texture (mTextureID) in texture unit 0.
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId);

        // Draw the rect.
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        checkGlError("glDrawArrays");
        GLES30.glFinish();
        restoreState();
    }

    static public void copy2(int programHandle, float[] mvpMatrix, float[] texMatrix,
                             int textureId, int textureIddst, int frameBufObj) {
        saveGLState();
        // Set the texture.
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GL_TEXTURE_2D, textureIddst);
        if (isChanged) {
            Log.d(TAG, "change dst texture:" + videoWidth + "x" + videoHeight);
            GLES30.glTexImage2D(GL_TEXTURE_2D, 0, GLES30.GL_RGBA,//allocate storage
                    videoWidth, videoHeight, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
            GLES30.glTexParameteri(GL_TEXTURE_2D,
                    GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GL_TEXTURE_2D,
                    GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GL_TEXTURE_2D,
                    GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GL_TEXTURE_2D,
                    GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBufObj);
        if (isChanged) {
            GLES30.glFramebufferTexture2D(
                    GLES30.GL_FRAMEBUFFER,
                    GLES30.GL_COLOR_ATTACHMENT0,
                    GL_TEXTURE_2D, textureIddst, 0);

            // check status
            int status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
            if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
                Log.e(TAG, "glCheckFramebufferStatus error" + status);
            }
        }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBO);

        // Copy the model / view / projection matrix over.
        GLES30.glUseProgram(VarifyRender.mProgramExt);
        GLES30.glViewport(0, 0,
                videoWidth, videoHeight);
        checkGlError("glUseProgram");
        GLES30.glUniformMatrix4fv(mvpMatrixLocExt, 1, false, IDENTITY_MATRIX, 0);
        checkGlError("glUniformMatrix4fv mvpMatrixLoc");

        // Copy the texture transformation matrix over.
        GLES30.glUniformMatrix4fv(texMatrixLocExt, 1, false, IDENTITY_MATRIX, 0);
        checkGlError("glUniformMatrix4fv texMatrixLoc");

        // Enable the "aPosition" vertex attribute.
        GLES30.glEnableVertexAttribArray(positionLocExt);
        checkGlError("glEnableVertexAttribArray positionLoc");

        // Connect vertexBuffer to "aPosition".
        GLES30.glVertexAttribPointer(positionLocExt, coordsPerVertex,
                GLES30.GL_FLOAT, false, vertexStride, 0);
        checkGlError("glVertexAttribPointer positionLoc");

        // Enable the "aTextureCoord" vertex attribute.
        GLES30.glEnableVertexAttribArray(textureCoordLocExt);
        checkGlError("glEnableVertexAttribArray textureCoordLoc");

        // Connect texBuffer to "aTextureCoord".
        GLES30.glVertexAttribPointer(textureCoordLocExt, colorPerVertex,
                GLES30.GL_FLOAT, false, vertexStride, coordsPerVertex * SIZEOF_FLOAT);
        checkGlError("glVertexAttribPointer textureCoordLoc");


        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glClearColor(0, 0, 0, 0);
        // connect 'VideoTexture' to video source texture (mTextureID) in texture unit 0.
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId);

        // Draw the rect.
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        checkGlError("glDrawArrays");
        GLES30.glFinish();
        restoreState();
    }

    static int[] glInt = new int[1];
    static boolean previousBlend;
    static boolean previousCullFace;
    static boolean previousScissorTest;
    static boolean previousStencilTest;
    static boolean previousDepthTest;
    static boolean previousDither;
    static int previousFBO;
    static int previousVBO;
    static int[] previousViewport = new int[4];
    static void saveGLState() {

        previousBlend = GLES30.glIsEnabled(GLES30.GL_BLEND);
        previousCullFace = GLES30.glIsEnabled(GLES30.GL_CULL_FACE);
        previousScissorTest = GLES30.glIsEnabled(GLES30.GL_SCISSOR_TEST);
        previousStencilTest = GLES30.glIsEnabled(GLES30.GL_STENCIL_TEST);
        previousDepthTest = GLES30.glIsEnabled(GLES30.GL_DEPTH_TEST);
        previousDither = GLES30.glIsEnabled(GLES30.GL_DITHER);
        GLES30.glGetIntegerv(GLES30.GL_FRAMEBUFFER_BINDING, glInt, 0);
        previousFBO = glInt[0];
        GLES30.glGetIntegerv(GLES30.GL_ARRAY_BUFFER_BINDING, glInt, 0);
        previousVBO = glInt[0];
        GLES30.glGetIntegerv(GLES30.GL_VIEWPORT, previousViewport, 0);
        checkGlError("save state");

        GLES30.glDisable(GLES30.GL_BLEND);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glDisable(GLES30.GL_SCISSOR_TEST);
        GLES30.glDisable(GLES30.GL_STENCIL_TEST);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glDisable(GLES30.GL_DITHER);
        GLES30.glColorMask(true, true, true, true);

        checkGlError("reset state");
    }
    static void restoreState() {
        // ======Restore state and cleanup.
//        Log.i(TAG, "restore vbo id :" + previousVBO);
        if (previousFBO > 0) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, previousFBO);
        }
//        if (previousVBO > 0) {
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, previousVBO);
//        }
        GLES30.glViewport(previousViewport[0], previousViewport[1],
                previousViewport[2], previousViewport[3]);
        if (previousBlend) GLES30.glEnable(GLES30.GL_BLEND);
        if (previousCullFace) GLES30.glEnable(GLES30.GL_CULL_FACE);
        if (previousScissorTest) GLES30.glEnable(GLES30.GL_SCISSOR_TEST);
        if (previousStencilTest) GLES30.glEnable(GLES30.GL_STENCIL_TEST);
        if (previousDepthTest) GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        if (previousDither) GLES30.glEnable(GLES30.GL_DITHER);
    }
    static int num1 = 0;
    static void saveImg(String name){
        num1++;
        if (null != mData && num1 == 5)
        {
            mData.position(0);
            GLES30.glReadPixels(
                    0, 0, sWidth, sHeight,
                    GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE,
                    mData);
            try {

                mData.position(0);
                File file = new File(name);
                file.createNewFile();
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                byte[] data = new byte[sHeight * sWidth * 4];
                mData.get(data, 0, sHeight * sWidth * 4);
                fileOutputStream.write(data);
                fileOutputStream.close();
                mData.position(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    static public int createExtTextureObject() {
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        checkGlError("glGenTextures");

        int texId = textures[0];
        GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texId);
        checkGlError("glBindTexture " + texId);

        GLES30.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S,
                GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T,
                GLES30.GL_CLAMP_TO_EDGE);

//        GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_SWIZZLE_R,
//                GLES30.GL_RED);
//
//        GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_SWIZZLE_B,
//                GLES30.GL_ZERO);
//
//        GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_SWIZZLE_G,
//                GLES30.GL_ZERO);

        checkGlError("glTexParameter");

        return texId;
    }

    public static int createInerTextureObject() {
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        checkGlError("glGenTextures");
        int texId = textures[0];
        GLES30.glBindTexture(GL_TEXTURE_2D, texId);
        GLES30.glTexParameteri(GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        return texId;
    }
    public static int createFBO(){
        int[] glInt = new int[1];
        GLES30.glGenFramebuffers(1,glInt,0);
        return glInt[0];
    }

    public static int createVBO() {
        int[] glInt = new int[1];
        GLES30.glGenBuffers(1, glInt, 0);
        return glInt[0];
    }
    public static int loadShader(int shaderType, String source) {
        int shader = GLES30.glCreateShader(shaderType);
        checkGlError("glCreateShader type=" + shaderType);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + shaderType + ":");
            Log.e(TAG, " " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES30.glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            Log.e(TAG, "Could not create program");
        }
        GLES30.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES30.glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        GLES30.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES30.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES30.glGetProgramInfoLog(program));
            GLES30.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    public static void localAttriExt(int programHandle) {
        positionLocExt = GLES30.glGetAttribLocation(programHandle, "aPosition");
        checkLocation(positionLocExt, "aPosition");
        textureCoordLocExt = GLES30.glGetAttribLocation(programHandle, "aTextureCoord");
        checkLocation(textureCoordLocExt, "aTextureCoord");
        mvpMatrixLocExt = GLES30.glGetUniformLocation(programHandle, "uMVPMatrix");
        checkLocation(mvpMatrixLocExt, "uMVPMatrix");
        texMatrixLocExt= GLES30.glGetUniformLocation(programHandle, "uTexMatrix");
        checkLocation(texMatrixLocExt, "uTexMatrix");
    }
    public static void localAttriInner(int programHandle) {
        positionLocIn = GLES30.glGetAttribLocation(programHandle, "aPosition");
        checkLocation(positionLocIn, "aPosition");
        textureCoordLocIn = GLES30.glGetAttribLocation(programHandle, "aTextureCoord");
        checkLocation(textureCoordLocIn, "aTextureCoord");
        mvpMatrixLocIn = GLES30.glGetUniformLocation(programHandle, "uMVPMatrix");
        checkLocation(mvpMatrixLocIn, "uMVPMatrix");
        texMatrixLocIn = GLES30.glGetUniformLocation(programHandle, "uTexMatrix");
        checkLocation(texMatrixLocIn, "uTexMatrix");
    }

    public static void localAttriAndOthersEhn(int programHandle) {
        positionLocEhn = GLES30.glGetAttribLocation(programHandle, "aPosition");
        checkLocation(positionLocEhn, "aPosition Ehn");
        textureCoordLocEhn = GLES30.glGetAttribLocation(programHandle, "aTextureCoord");
        checkLocation(textureCoordLocEhn, "aTextureCoord Ehn");
        mvpMatrixLocEhn = GLES30.glGetUniformLocation(programHandle, "uMVPMatrix");
        checkLocation(mvpMatrixLocEhn, "uMVPMatrix Ehn");
        texMatrixLocEhn = GLES30.glGetUniformLocation(programHandle, "uTexMatrix");
        checkLocation(texMatrixLocEhn, "uTexMatrix Ehn");
        dxLoc = GLES30.glGetUniformLocation(programHandle, "dx");
        checkLocation(dxLoc, "dx Ehn");
        dyLoc = GLES30.glGetUniformLocation(programHandle, "dy");
        checkLocation(dyLoc, "dy Ehn");
    }

    public static FloatBuffer createFloatBuffer(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }


    public static void checkGlError(String op) {
        int error = GLES30.glGetError();
        if (error != GLES30.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e(TAG, msg);
            throw new RuntimeException(msg);
        }
    }
    public static void checkLocation(int location, String label) {
        if (location < 0) {
            throw new RuntimeException("Unable to locate '" + label + "' in program");
        }
    }
}
