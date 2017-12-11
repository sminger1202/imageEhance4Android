package com.android.enhance;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES30.GL_TEXTURE_2D;

/**
 * Created by shiming on 2017/5/25.
 */

public class Video8kEngine extends EngineBase{
    String TAG = this.getClass().getSimpleName();
    private int mvpMatrixLocEhn = -1;
    private int texMatrixLocEhn = -1;
    private int positionLocEhn = -1;
    private int textureCoordLocEhn = -1;

    public Video8kEngine(Context context){
        EngineName = CVFactory.VIDEO8K;
        init(context);
    }
    @Override
    public void init(Context context) {
        super.init(context);
        coordsPerVertex = 2;
        colorPerVertex = 2;
        vertexSize = coordsPerVertex + colorPerVertex; // x, y, u, v
        vertexCount = mTriangleVerticesData.length / vertexSize;
        vertexStride = vertexSize * FLOAT_SIZE_BYTES;
    }

    @Override
    public void localAttri() {
        positionLocEhn = GLES30.glGetAttribLocation(mProgram, "aPosition");
        checkLocation(positionLocEhn, "aPosition Ehn");
        textureCoordLocEhn = GLES30.glGetAttribLocation(mProgram, "aTextureCoord");
        checkLocation(textureCoordLocEhn, "aTextureCoord");

        mvpMatrixLocEhn = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkLocation(mvpMatrixLocEhn, "uMVPMatrix Ehn");
        texMatrixLocEhn = GLES30.glGetUniformLocation(mProgram, "uTexMatrix");
        checkLocation(texMatrixLocEhn, "uTexMatrix Ehn");
    }

    @Override
    public void setParameter(int field, float value) {
        if (field == IEngine.EFFECT_COEFFICIENT) {
            
        }
    }

    @Override
    public void setParameters(int field, float[] value) {
        Log.d(TAG, "set Pars"  + value);
        if (field == IEngine.EFFECT_MVP) {
            for (int i = 0; i < value.length; ++i) {
                mTriangleVerticesData[i] = value[i];
            }
            initVBO();
        }
    }

    @Override
    public void initFBO() {
        int[] glInt = new int[1];
        GLES30.glGenFramebuffers(1,glInt,0);
        mFBO = glInt[0];
    }

    @Override
    protected void initVBO() {
        if (mVBO >= 0) {
            int[] dd = new int[1];
            dd[0] = mVBO;
            GLES30.glDeleteBuffers(1,dd, 0 );
            mVBO = -1;
        }
        int[] glInt = new int[1];
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
    public void apply(final int srcTextureId, final int dstTextureId, final int width,final int height) {
//        Log.i(TAG, "Enhance apply");
        boolean isChanged = true;
        if (mWidth == width && mHeight == height && srcTextureId == mCurrentTextureId) {
            isChanged = false;
        } else {
            mWidth = width;
            mHeight = height;
            mCurrentTextureId = srcTextureId;
            isChanged = true;
        }
        saveGLState();
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GL_TEXTURE_2D, dstTextureId);
//        if (isChanged) {
            Log.d(TAG, "change dst texture:" + mWidth + "x" + mHeight);
            GLES30.glTexImage2D(GL_TEXTURE_2D, 0, GLES30.GL_RGBA,//allocate storage
                    mWidth, mHeight, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
            initTexParams();
//        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFBO);
//        if (isChanged) {
            GLES30.glFramebufferTexture2D(
                    GLES30.GL_FRAMEBUFFER,
                    GLES30.GL_COLOR_ATTACHMENT0,
                    GL_TEXTURE_2D, dstTextureId, 0);

            // check status
            int status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
            if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
                Log.e(TAG, "glCheckFramebufferStatus error" + status);
            }
//        }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBO);

        GLES30.glUseProgram(mProgram);
        GLES30.glViewport(0, 0,
                mWidth, mHeight);
        checkGlError("glUseProgram");

        if (mvpMatrixLocEhn >= 0) {
            GLES30.glUniformMatrix4fv(mvpMatrixLocEhn, 1, false, mvpMatrix, 0);
            checkGlError("glUniformMatrix4fv mvpMatrixLoc");
        }

        if (texMatrixLocEhn >= 0) {
            GLES30.glUniformMatrix4fv(texMatrixLocEhn, 1, false, texMatrix, 0);
            checkGlError("glUniformMatrix4fv texMatrixLoc");
        }

        // Enable the "aPosition" vertex attribute.
        GLES30.glEnableVertexAttribArray(positionLocEhn);
        checkGlError("glEnableVertexAttribArray positionLoc");

        // Connect vertexBuffer to "aPosition".
        GLES30.glVertexAttribPointer(positionLocEhn, coordsPerVertex,
                GLES30.GL_FLOAT, false, vertexStride, 0);
        checkGlError("glVertexAttribPointer positionLoc");


        // Enable the "aTextureCoord" vertex attribute.
        GLES30.glEnableVertexAttribArray(textureCoordLocEhn);
        checkGlError("glEnableVertexAttribArray textureCoordLoc");

        // Connect texBuffer to "aTextureCoord".
        GLES30.glVertexAttribPointer(textureCoordLocEhn, colorPerVertex,
                GLES30.GL_FLOAT, false, vertexStride, coordsPerVertex * FLOAT_SIZE_BYTES);
        checkGlError("glVertexAttribPointer textureCoordLoc");

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glClearColor(0, 0, 0, 0);
        // connect 'VideoTexture' to video source texture (srcTextureId) in texture unit 0.
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, srcTextureId);

        // Draw the rect.
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertexCount);
        checkGlError("glDrawArrays");
        restoreState();
    }

    @Override
    public void apply(int[] srcTextureId, int dstTextureId, int width, int height) {

    }

    @Override
    protected String getVertexSource() {
//        return TextResourceReader.readTextFileFromResource(mContext,R.raw.enhance_vertex_shader);
        return getVertexShader();
//        return getVertexShader("enhance");
    }
    @Override
    protected String getfragmentSource() {
//        return TextResourceReader.readTextFileFromResource(mContext, R.raw.enhance_fragment_shader);
        return getFragmentShader();
//        return getFragmentShader("enhance");
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
