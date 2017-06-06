package com.example.hellojni;

import android.opengl.EGLDisplay;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Debug;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES20.GL_TEXTURE_2D;

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
                    "}\n";
    public static final String FRAGMENT_SHADER_EXT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES  sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    public static final String FRAGMENT_SHADER_EXT_ENH =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform float dx;\n" +
                    "uniform float dy;\n" +
                    "vec4 getPixel(float dx, float dy)  \n" +
                    "    { return texture2D(sTexture, vTextureCoord + vec2(dx, dy));}\n" +
                    "void main() {\n" +
                    "    vec4 five = vec4(5, 5, 5, 5);\n" +
                    "    vec4 left = getPixel(-dx, 0.0);\n" +
                    "    vec4 right = getPixel(+dx, 0.0);\n" +
                    "    vec4 top = getPixel(0.0, -dy);\n" +
                    "    vec4 down = getPixel(0.0, dy);\n" +
                    "    vec4 center = getPixel(0.0, 0.0);\n" +
                    "    gl_FragColor = clamp(five * center - left - right - top - down, 0.0, 1.0);\n" +
                    "}\n";

//    private static final float FULL_RECTANGLE_COORDS[] = {
//            -1.0f, -1.0f,   // 0 bottom left
//            1.0f, -1.0f,   // 1 bottom right
//            -1.0f, 1.0f,   // 2 top left
//            1.0f, 1.0f,   // 3 top right
//    };
    private static final float FULL_RECTANGLE_COORDS[] = {
        1.0f, 1.0f,   // 3 top right
        -1.0f, 1.0f,   // 2 top left
        1.0f, -1.0f,   // 1 bottom right
        -1.0f, -1.0f,   // 0 bottom left
    };
    private static final float FULL_RECTANGLE_TEX_COORDS[] = {
            1.0f, 1.0f,      // 3 top right
            0.0f, 1.0f,     // 2 top left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 0.0f,     // 0 bottom left

    };
    private static final FloatBuffer FULL_RECTANGLE_BUF =
            createFloatBuffer(FULL_RECTANGLE_COORDS);
    private static final FloatBuffer FULL_RECTANGLE_TEX_BUF =
            createFloatBuffer(FULL_RECTANGLE_TEX_COORDS);

    public static final float[] IDENTITY_MATRIX;

    static {
        IDENTITY_MATRIX = new float[16];
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
    }


    static int mvpMatrixLoc;
    static int texMatrixLoc;
    static int positionLoc;
    static int textureCoordLoc;

    static public boolean sIsOpenClPrepared = false;
    static public boolean sIsEnhance = false;
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
    static int sWidth = 600;
    static int sHeight = 300;
    static boolean isChanged = true;
    static public ByteBuffer mData;

    private static final int SIZEOF_FLOAT = 4;

    static public void initDrawable() {
        coordsPerVertex = 2;
        vertexArray = FULL_RECTANGLE_BUF;
        vertexStride = coordsPerVertex * SIZEOF_FLOAT;
        texCoordArray = FULL_RECTANGLE_TEX_BUF;
        texCoordStride = 2 * SIZEOF_FLOAT;
        vertexCount = FULL_RECTANGLE_COORDS.length / coordsPerVertex;
        if(GLUtil.mData != null ){
            GLUtil.mData.clear();
        }
        mData = ByteBuffer.allocateDirect(sWidth * sWidth * 4).order(ByteOrder.nativeOrder());
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
    static void draw(int programHandle, float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex,
                     int vertexCount, int coordsPerVertex, int vertexStride,
                     float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride) {
        checkGlError("draw start");
//        try {
//            Thread.currentThread().sleep(500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        // Select the program.
        if(sIsEnhance) {
            GLES20.glUseProgram(programHandle);
//            GLES20.glUseProgram(EnhanceActivity.GLGetProgram());

            checkGlError("glUseProgram");

            // Set the texture.
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId);

            // Copy the model / view / projection matrix over.
            GLES20.glUniformMatrix4fv(mvpMatrixLocEhn, 1, false, mvpMatrix, 0);
            checkGlError("glUniformMatrix4fv");

            // Copy the texture transformation matrix over.
            GLES20.glUniformMatrix4fv(texMatrixLocEhn, 1, false, texMatrix, 0);
            checkGlError("glUniformMatrix4fv");

            //dx,dy
            GLES20.glUniform1f(dxLoc, 1.f / sWidth);
            checkGlError("dxloc");


            GLES20.glUniform1f(dyLoc, 1.f / sHeight);
            checkGlError("dyloc");

            // Enable the "aPosition" vertex attribute.
            GLES20.glEnableVertexAttribArray(positionLocEhn);
            checkGlError("glEnableVertexAttribArray");

            // Connect vertexBuffer to "aPosition".
            GLES20.glVertexAttribPointer(positionLocEhn, coordsPerVertex,
                    GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
            checkGlError("glVertexAttribPointer");

            // Enable the "aTextureCoord" vertex attribute.
            GLES20.glEnableVertexAttribArray(textureCoordLocEhn);
            checkGlError("glEnableVertexAttribArray");

            // Connect texBuffer to "aTextureCoord".
            GLES20.glVertexAttribPointer(textureCoordLocEhn, 2,
                    GLES20.GL_FLOAT, false, texStride, texBuffer);
            checkGlError("glVertexAttribPointer");
        } else {
            GLES20.glUseProgram(programHandle);
            checkGlError("glUseProgram");

            // Set the texture.
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId);

            // Copy the model / view / projection matrix over.
            GLES20.glUniformMatrix4fv(mvpMatrixLoc, 1, false, mvpMatrix, 0);
            checkGlError("glUniformMatrix4fv");

            // Copy the texture transformation matrix over.
            GLES20.glUniformMatrix4fv(texMatrixLoc, 1, false, texMatrix, 0);
            checkGlError("glUniformMatrix4fv");

            // Enable the "aPosition" vertex attribute.
            GLES20.glEnableVertexAttribArray(positionLoc);
            checkGlError("glEnableVertexAttribArray");

            // Connect vertexBuffer to "aPosition".
            GLES20.glVertexAttribPointer(positionLoc, coordsPerVertex,
                    GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
            checkGlError("glVertexAttribPointer");

            // Enable the "aTextureCoord" vertex attribute.
            GLES20.glEnableVertexAttribArray(textureCoordLoc);
            checkGlError("glEnableVertexAttribArray");

            // Connect texBuffer to "aTextureCoord".
            GLES20.glVertexAttribPointer(textureCoordLoc, 2,
                    GLES20.GL_FLOAT, false, texStride, texBuffer);
            checkGlError("glVertexAttribPointer");
        }

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
        checkGlError("glDrawArrays");

        GLES20.glViewport(0, 0,
                sWidth, sHeight);
        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(positionLoc);
        GLES20.glDisableVertexAttribArray(textureCoordLoc);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);
    }
    private static long num1 = 0;
    static void drawWithCopy(int programHandle, float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex,
                     int vertexCount, int coordsPerVertex, int vertexStride,
                     float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride, int textureIddst, int frameBufObj) {
//        Log.d(TAG, "draw with Copy, is changed :" + isChanged);
//        Log.d(TAG, "onDrawFrame：" + num1++);
        GLES20.glFinish();
        if (!sIsEnhance) {
            copy(programHandle, mvpMatrix, vertexBuffer, firstVertex,
                    vertexCount, coordsPerVertex, vertexStride,
                    texMatrix, texBuffer, textureId, texStride,
                    textureIddst, frameBufObj);
        } else {
            copyEnhance(textureId, textureIddst, sWidth, sHeight);
        }
        GLES20.glUseProgram(VarifyRender.mProgram);
        checkGlError("glUseProgram");

        GLES20.glViewport(0, 0,
                sWidth, sHeight);
        GLES20.glUniformMatrix4fv(mvpMatrixLoc, 1, false, mvpMatrix, 0);
        checkGlError("glUniformMatrix4fv mvpMatrixLoc");

            // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(texMatrixLoc, 1, false, texMatrix, 0);
        checkGlError("glUniformMatrix4fv texMatrixLoc");

            // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(positionLoc);
        checkGlError("glEnableVertexAttribArray positionLoc");

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(positionLoc, coordsPerVertex,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        checkGlError("glVertexAttribPointer positionLoc");

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(textureCoordLoc);
        checkGlError("glEnableVertexAttribArray textureCoordLoc");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(textureCoordLoc, 2,
                GLES20.GL_FLOAT, false, texStride, texBuffer);
        checkGlError("glVertexAttribPointer textureCoordLoc");
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        // Set the input texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        checkGlError("glActiveTexture");
        GLES20.glBindTexture(GL_TEXTURE_2D, textureIddst);
        checkGlError("glBindTexture");

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(0, 0, 0, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(positionLoc);
        GLES20.glDisableVertexAttribArray(textureCoordLoc);
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
        GLES20.glFinish();
/*
        if (null != mData && num1 == 5)
        {
            //data.position(0);
            GLES20.glReadPixels(
                    0, 0, sWidth, sHeight,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                    mData);
            try {

                mData.position(0);
                File file = new File("/sdcard/image5.rgba");
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
        */
        isChanged = false;
    }

    static public void copyEnhance(int srcTextureId, int dstTextureId, int width, int height) {
        VarifyRender.mEnhanceEngine.apply(srcTextureId, dstTextureId, width, height);
    }
    static public void copy(int programHandle, float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex,
                            int vertexCount, int coordsPerVertex, int vertexStride,
                            float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride, int textureIddst, int frameBufObj) {
//        int[] glInt = new int[1];
//        boolean previousBlend = GLES20.glIsEnabled(GLES20.GL_BLEND);
//        boolean previousCullFace = GLES20.glIsEnabled(GLES20.GL_CULL_FACE);
//        boolean previousScissorTest = GLES20.glIsEnabled(GLES20.GL_SCISSOR_TEST);
//        boolean previousStencilTest = GLES20.glIsEnabled(GLES20.GL_STENCIL_TEST);
//        boolean previousDepthTest = GLES20.glIsEnabled(GLES20.GL_DEPTH_TEST);
//        boolean previousDither = GLES20.glIsEnabled(GLES20.GL_DITHER);
//        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, glInt, 0);
//        int previousFBO = glInt[0];
//        GLES20.glGetIntegerv(GLES20.GL_ARRAY_BUFFER_BINDING, glInt, 0);
//        int previousVBO = glInt[0];
//        int[] previousViewport = new int[4];
//        //GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, previousViewport, 0);
//
//        checkGlError("save state");
//
//        GLES20.glDisable(GLES20.GL_BLEND);
//        GLES20.glDisable(GLES20.GL_CULL_FACE);
//        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
//        GLES20.glDisable(GLES20.GL_STENCIL_TEST);
//        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
//        GLES20.glDisable(GLES20.GL_DITHER);
//        GLES20.glColorMask(true, true, true, true);
//
//        checkGlError("reset state");


        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_2D, textureIddst);
        if (isChanged) {
            Log.d(TAG, "change dst texture:" + sWidth + "x" + sHeight);
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GLES20.GL_RGBA,//allocate storage
                    sWidth, sHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufObj);
        if (isChanged) {
            GLES20.glFramebufferTexture2D(
                    GLES20.GL_FRAMEBUFFER,
                    GLES20.GL_COLOR_ATTACHMENT0,
                    GL_TEXTURE_2D, textureIddst, 0);

            // check status
            int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                Log.e(TAG, "glCheckFramebufferStatus error" + status);
            }
        }

        if (!GLUtil.sIsEnhance) {
            // Copy the model / view / projection matrix over.
            GLES20.glUseProgram(VarifyRender.mProgram);
            GLES20.glViewport(0, 0,
                    sWidth, sHeight);
            checkGlError("glUseProgram");
            GLES20.glUniformMatrix4fv(mvpMatrixLoc, 1, false, mvpMatrix, 0);
            checkGlError("glUniformMatrix4fv mvpMatrixLoc");

            // Copy the texture transformation matrix over.
            GLES20.glUniformMatrix4fv(texMatrixLoc, 1, false, texMatrix, 0);
            checkGlError("glUniformMatrix4fv texMatrixLoc");

            // Enable the "aPosition" vertex attribute.
            GLES20.glEnableVertexAttribArray(positionLoc);
            checkGlError("glEnableVertexAttribArray positionLoc");

            // Connect vertexBuffer to "aPosition".
            GLES20.glVertexAttribPointer(positionLoc, coordsPerVertex,
                    GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
            checkGlError("glVertexAttribPointer positionLoc");

            // Enable the "aTextureCoord" vertex attribute.
            GLES20.glEnableVertexAttribArray(textureCoordLoc);
            checkGlError("glEnableVertexAttribArray textureCoordLoc");

            // Connect texBuffer to "aTextureCoord".
            GLES20.glVertexAttribPointer(textureCoordLoc, 2,
                    GLES20.GL_FLOAT, false, texStride, texBuffer);
            checkGlError("glVertexAttribPointer textureCoordLoc");


            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glClearColor(0, 0, 0, 0);
            // connect 'VideoTexture' to video source texture (mTextureID) in texture unit 0.
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId);

            // Draw the rect.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
            checkGlError("glDrawArrays");
        } else {
            // Copy the model / view / projection matrix over.
            GLES20.glUseProgram(VarifyRender.mProgramEnhance);
            GLES20.glViewport(0, 0,
                    sWidth, sHeight);
            checkGlError("glUseProgram");
            GLES20.glUniformMatrix4fv(mvpMatrixLocEhn, 1, false, mvpMatrix, 0);
            checkGlError("glUniformMatrix4fv mvpMatrixLoc");

            // Copy the texture transformation matrix over.
            GLES20.glUniformMatrix4fv(texMatrixLocEhn, 1, false, texMatrix, 0);
            checkGlError("glUniformMatrix4fv texMatrixLoc");

            //dx,dy
            GLES20.glUniform1f(dxLoc, 1.f / sWidth);
            checkGlError("dxloc");

            GLES20.glUniform1f(dyLoc, 1.f / sHeight);
            checkGlError("dyloc");

            // Enable the "aPosition" vertex attribute.
            GLES20.glEnableVertexAttribArray(positionLocEhn);
            checkGlError("glEnableVertexAttribArray positionLoc");

            // Connect vertexBuffer to "aPosition".
            GLES20.glVertexAttribPointer(positionLocEhn, coordsPerVertex,
                    GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
            checkGlError("glVertexAttribPointer positionLoc");

            // Enable the "aTextureCoord" vertex attribute.
            GLES20.glEnableVertexAttribArray(textureCoordLocEhn);
            checkGlError("glEnableVertexAttribArray textureCoordLoc");

            // Connect texBuffer to "aTextureCoord".
            GLES20.glVertexAttribPointer(textureCoordLocEhn, 2,
                    GLES20.GL_FLOAT, false, texStride, texBuffer);
            checkGlError("glVertexAttribPointer textureCoordLoc");


            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glClearColor(0, 0, 0, 0);
            // connect 'VideoTexture' to video source texture (mTextureID) in texture unit 0.
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId);

            // Draw the rect.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
            checkGlError("glDrawArrays");
        }
//        // ======Restore state and cleanup.
//        if (previousFBO > 0) {
//            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFBO);
//        }
//        if (previousVBO > 0) {
//            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, previousVBO);
//        }
//        GLES20.glViewport(previousViewport[0], previousViewport[1],
//                previousViewport[2], previousViewport[3]);
//        if (previousBlend) GLES20.glEnable(GLES20.GL_BLEND);
//        if (previousCullFace) GLES20.glEnable(GLES20.GL_CULL_FACE);
//        if (previousScissorTest) GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
//        if (previousStencilTest) GLES20.glEnable(GLES20.GL_STENCIL_TEST);
//        if (previousDepthTest) GLES20.glEnable(GLES20.GL_DEPTH_TEST);
//        if (previousDither) GLES20.glEnable(GLES20.GL_DITHER);
        GLES20.glFinish();
    }

    static public int createExtTextureObject() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("glGenTextures");

        int texId = textures[0];
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texId);
        checkGlError("glBindTexture " + texId);

        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        checkGlError("glTexParameter");

        return texId;
    }

    public static int createInerTextureObject() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("glGenTextures");
        int texId = textures[0];
        GLES20.glBindTexture(GL_TEXTURE_2D, texId);
        GLES20.glTexParameteri(GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        return texId;
    }
    public static int createFBO(){
        int[] glInt = new int[1];
        GLES20.glGenFramebuffers(1,glInt,0);
        return glInt[0];
    }
    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlError("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + shaderType + ":");
            Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            Log.e(TAG, "Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    public static void localAttriAndOthers(int programHandle) {
        positionLoc = GLES20.glGetAttribLocation(programHandle, "aPosition");
        checkLocation(positionLoc, "aPosition");
        textureCoordLoc = GLES20.glGetAttribLocation(programHandle, "aTextureCoord");
        checkLocation(textureCoordLoc, "aTextureCoord");
        mvpMatrixLoc = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
        checkLocation(mvpMatrixLoc, "uMVPMatrix");
        texMatrixLoc = GLES20.glGetUniformLocation(programHandle, "uTexMatrix");
        checkLocation(texMatrixLoc, "uTexMatrix");
    }

    public static void localAttriAndOthersEhn(int programHandle) {
        positionLocEhn = GLES20.glGetAttribLocation(programHandle, "aPosition");
        checkLocation(positionLoc, "aPosition Ehn");
        textureCoordLocEhn = GLES20.glGetAttribLocation(programHandle, "aTextureCoord");
        checkLocation(textureCoordLoc, "aTextureCoord Ehn");
        mvpMatrixLocEhn = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
        checkLocation(mvpMatrixLoc, "uMVPMatrix Ehn");
        texMatrixLocEhn = GLES20.glGetUniformLocation(programHandle, "uTexMatrix");
        checkLocation(texMatrixLoc, "uTexMatrix Ehn");
        dxLoc = GLES20.glGetUniformLocation(programHandle, "dx");
        checkLocation(texMatrixLoc, "dx Ehn");
        dyLoc = GLES20.glGetUniformLocation(programHandle, "dy");
        checkLocation(texMatrixLoc, "dy Ehn");
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
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
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
