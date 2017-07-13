package com.android.enhance;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.android.enhance.utils.TextResourceReader;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES30.GL_TEXTURE_2D;

/**
 * Created by shiming on 2017/7/3.
 */

public class DragoEngine extends EngineBase {

    private int mvpMatrixLoc;
    private int texMatrixLoc;
    private int positionLoc;
    private int textureCoordLoc;
    private int parsLoc;
    private int lumLoc;
    private float constant1;
    private float constant2;
    private float LMax;
    private float Lwa;
    DragoEngine(Context context) {
        init(context);
    }
    @Override
    public void init(Context context){
        super.init(context);
        coordsPerVertex = 2;
        colorPerVertex = 2;
        vertexSize = coordsPerVertex + colorPerVertex; // x, y, u, v
        vertexCount = mTriangleVerticesData.length / vertexSize;
        vertexStride = vertexSize * FLOAT_SIZE_BYTES;
    }
    @Override
    public void setParameter(int field, float value) {

    }

    @Override
    public void setParameters(int field, float[] value) {
        constant1 = value[0];
        constant2 = value[1];
        LMax      = value[2];
        Lwa       = value[3];
    }

    @Override
    protected void localAttri() {
        positionLoc = GLES30.glGetAttribLocation(mProgram, "vPosition");
        checkLocation(positionLoc, "vPosition ");
        textureCoordLoc = GLES30.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        checkLocation(textureCoordLoc, "inputTextureCoordinate ");
        mvpMatrixLoc = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkLocation(mvpMatrixLoc, "uMVPMatrix ");
        parsLoc = GLES30.glGetUniformLocation(mProgram, "pars");
        checkLocation(parsLoc, "parameters ");
        lumLoc = GLES30.glGetUniformLocation(mProgram, "u_lum");
        checkLocation(lumLoc, "luminance");

    }

    @Override
    protected void initFBO() {
        int[] glInt = new int[1];
        if (mFBO > 0) {
            glInt[0] = mFBO;
            GLES30.glDeleteBuffers(1, glInt, 0);
        }
        GLES30.glGenFramebuffers(1,glInt,0);
        mFBO = glInt[0];
    }

    @Override
    protected void initVBO() {
        int[] glInt = new int[1];
        if (mVBO > 0) {
            glInt[0] = mVBO;
            GLES30.glDeleteBuffers(1, glInt, 0);
        }
        GLES30.glGenBuffers(1, glInt, 0);
        mVBO = glInt[0];
        mTriangleVertices = java.nio.ByteBuffer.allocateDirect(
                mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.position(0);
        mTriangleVertices.put(mTriangleVerticesData).position(0);

        GLES30.glGetIntegerv(GLES30.GL_ARRAY_BUFFER_BINDING, glInt, 0);
        int previousVBO = glInt[0];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBO);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                mTriangleVerticesData.length*FLOAT_SIZE_BYTES,
                mTriangleVertices, GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, previousVBO);
    }

    @Override
    public void apply(int srcTextureId, int dstTextureId, int width, int height) {

    }

    @Override
    public void apply(int[] srcTextureId, int dstTextureId, int width, int height) {
//        Log.d(TAG, "Drago apply");
        boolean isChanged = true;
        if (width != mWidth || height != mHeight) {
            isChanged = true;
            mWidth = width;
            mHeight = height;
        } else {
            isChanged = false;
        }
        saveGLState();
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GL_TEXTURE_2D, dstTextureId);
        if (isChanged) {
            Log.d(TAG, "change dst texture:" + mWidth + "x" + mHeight);
            GLES30.glTexImage2D(GL_TEXTURE_2D, 0, GLES30.GL_RGBA,//allocate storage
                    mWidth, mHeight, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
            initTexParams();
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFBO);
        if (isChanged) {
            GLES30.glFramebufferTexture2D(
                    GLES30.GL_FRAMEBUFFER,
                    GLES30.GL_COLOR_ATTACHMENT0,
                    GL_TEXTURE_2D, dstTextureId, 0);

            // check status
            int status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
            if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
                Log.e(TAG, "glCheckFramebufferStatus error" + status);
            }
        }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBO);
        GLES30.glUseProgram(mProgram);
        GLES30.glViewport(0, 0,
                mWidth, mHeight);
        checkGlError("glUseProgram");


        // Enable the "aPosition" vertex attribute.
        GLES30.glEnableVertexAttribArray(positionLoc);
        checkGlError("glEnableVertexAttribArray positionLoc");

        // Connect vertexBuffer to "aPosition".
        GLES30.glVertexAttribPointer(positionLoc, coordsPerVertex,
                GLES30.GL_FLOAT, false, vertexStride, 0);
        checkGlError("glVertexAttribPointer positionLoc");

        // Enable the "aTextureCoord" vertex attribute.
        GLES30.glEnableVertexAttribArray(textureCoordLoc);
        checkGlError("glEnableVertexAttribArray textureCoordLoc");

        // Connect texBuffer to "aTextureCoord".
        GLES30.glVertexAttribPointer(textureCoordLoc, colorPerVertex,
                GLES30.GL_FLOAT, false, vertexStride, coordsPerVertex * FLOAT_SIZE_BYTES);
        checkGlError("glVertexAttribPointer textureCoordLoc");


        GLES30.glUniformMatrix4fv(mvpMatrixLoc, 1, false, mvpMatrix, 0);
        checkGlError("glUniformMatrix4fv mvpMatrixLoc");

        GLES30.glUniform4f(parsLoc, constant1, constant2, LMax, Lwa);
        checkGlError("glUniform4f pars");

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glClearColor(0, 0, 0, 0);
        // connect 'VideoTexture' to video source texture (srcTextureId) in texture unit 0.
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, srcTextureId[0]);

        for (int i = 1; i < srcTextureId.length; ++i) {
            GLES30.glUniform1i(lumLoc, i);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + i);
            GLES30.glBindTexture(GL_TEXTURE_2D, srcTextureId[i]);
        }
        checkGlError("bind texture");
        // Draw the rect.

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertexCount);
        checkGlError("glDrawArrays");
        restoreState();
    }

    @Override
    protected String getVertexSource() {
        return TextResourceReader.readTextFileFromResource(mContext, R.raw.drago_vertex_shader);
    }
    @Override
    protected String getfragmentSource() {
        return TextResourceReader.readTextFileFromResource(mContext, R.raw.drago_fragment_shader);
    }

    @Override
    public void release() {
        if (mFBO > 0){
            int[] dd = new int[1];
            dd[0] = mFBO;
            GLES30.glDeleteFramebuffers(1,dd, 0 );
            mFBO = -1;
        }
        if (mVBO > 0) {
            int[] dd = new int[1];
            dd[0] = mVBO;
            GLES30.glDeleteBuffers(1,dd, 0 );
            mVBO = -1;
        }
        if (mProgram > 0){
            GLES30.glDeleteProgram(mProgram);
            mProgram = -1;
        }
    }
}
