package com.android.enhance;


import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.android.enhance.utils.TextResourceReader;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES20.GL_TEXTURE_2D;

/**
 * Created by shiming on 2017/5/25.
 */

public class EnhanceEngine extends EngineBase{
    String TAG = this.getClass().getSimpleName();

    private int dxLoc = -1;
    private int dyLoc = -1;
    private int coefLoc = -1;
    private int mvpMatrixLocEhn = -1;
    private int texMatrixLocEhn = -1;
    private int positionLocEhn = -1;
    private int textureCoordLocEhn = -1;

    private float dx = 0.f;
    private float dy = 0.f;
    private float coef = 1.f;

    public EnhanceEngine(Context context){
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
        positionLocEhn = GLES20.glGetAttribLocation(mProgram, "aPosition");
        checkLocation(positionLocEhn, "aPosition Ehn");
        textureCoordLocEhn = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        checkLocation(textureCoordLocEhn, "aTextureCoord");

        mvpMatrixLocEhn = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkLocation(mvpMatrixLocEhn, "uMVPMatrix Ehn");
        texMatrixLocEhn = GLES20.glGetUniformLocation(mProgram, "uTexMatrix");
        checkLocation(texMatrixLocEhn, "uTexMatrix Ehn");
//
//        dxLoc = GLES20.glGetUniformLocation(mProgram, "dx");
//        checkLocation(dxLoc, "dx Ehn");
//        dyLoc = GLES20.glGetUniformLocation(mProgram, "dy");
//        checkLocation(dyLoc, "dy Ehn");
        coefLoc = GLES20.glGetUniformLocation(mProgram, "coef");
        checkLocation(coefLoc, "coefficient");

    }

    @Override
    public void setParameter(int field, float value) {
        if (field == IEngine.EFFECT_COEFFICIENT) {
            coef = value;
        }
    }

    @Override
    public void setParameters(int field, float[] value) {

    }

    @Override
    public void initFBO() {
        int[] glInt = new int[1];
        GLES20.glGenFramebuffers(1,glInt,0);
        mFBO = glInt[0];
    }

    @Override
    protected void initVBO() {
        int[] glInt = new int[1];
        GLES20.glGenBuffers(1, glInt, 0);
        mVBO = glInt[0];
        mTriangleVertices = java.nio.ByteBuffer.allocateDirect(
                mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.position(0);
        mTriangleVertices.put(mTriangleVerticesData).position(0);

        GLES20.glGetIntegerv(GLES20.GL_ARRAY_BUFFER_BINDING, glInt, 0);
        int previousVBO = glInt[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBO);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
                mTriangleVerticesData.length*FLOAT_SIZE_BYTES,
                mTriangleVertices, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, previousVBO);
    }

    @Override
    public void apply(final int srcTextureId, final int dstTextureId, final int width,final int height) {
//        Log.i(TAG, "Enhance apply");
        boolean isChanged = true;
        if (mWidth == width && mHeight == height) {
            isChanged = false;
        } else {
            mWidth = width;
            mHeight = height;
            dx = 1.f / mWidth;
            dy = 1.f / mHeight;
            isChanged = true;
        }
        saveGLState();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_2D, dstTextureId);
        if (isChanged) {
            Log.d(TAG, "change dst texture:" + mWidth + "x" + mHeight);
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GLES20.GL_RGBA,//allocate storage
                    mWidth, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            initTexParams();
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFBO);
        if (isChanged) {
            GLES20.glFramebufferTexture2D(
                    GLES20.GL_FRAMEBUFFER,
                    GLES20.GL_COLOR_ATTACHMENT0,
                    GL_TEXTURE_2D, dstTextureId, 0);

            // check status
            int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                Log.e(TAG, "glCheckFramebufferStatus error" + status);
            }
        }

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBO);

        GLES20.glUseProgram(mProgram);
        GLES20.glViewport(0, 0,
                mWidth, mHeight);
        checkGlError("glUseProgram");

        if (mvpMatrixLocEhn >= 0) {
            GLES20.glUniformMatrix4fv(mvpMatrixLocEhn, 1, false, mvpMatrix, 0);
            checkGlError("glUniformMatrix4fv mvpMatrixLoc");
        }

        if (mvpMatrixLocEhn >= 0) {
            GLES20.glUniformMatrix4fv(texMatrixLocEhn, 1, false, texMatrix, 0);
            checkGlError("glUniformMatrix4fv texMatrixLoc");
        }

        //dx,dy
        if (dxLoc >= 0 && dxLoc >= 0) {
            GLES20.glUniform1f(dxLoc, dx);
            checkGlError("dxloc");

            GLES20.glUniform1f(dyLoc, dy);
            checkGlError("dyloc");
        }
        GLES20.glUniform1f(coefLoc, coef);
        checkGlError("effect coefficient");


        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(positionLocEhn);
        checkGlError("glEnableVertexAttribArray positionLoc");

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(positionLocEhn, coordsPerVertex,
                GLES20.GL_FLOAT, false, vertexStride, 0);
        checkGlError("glVertexAttribPointer positionLoc");


        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(textureCoordLocEhn);
        checkGlError("glEnableVertexAttribArray textureCoordLoc");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(textureCoordLocEhn, colorPerVertex,
                GLES20.GL_FLOAT, false, vertexStride, coordsPerVertex * FLOAT_SIZE_BYTES);
        checkGlError("glVertexAttribPointer textureCoordLoc");

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(0, 0, 0, 0);
        // connect 'VideoTexture' to video source texture (srcTextureId) in texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, srcTextureId);

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount);
        checkGlError("glDrawArrays");
        restoreState();
    }

    @Override
    public void apply(int[] srcTextureId, int dstTextureId, int width, int height) {

    }

    @Override
    protected String getVertexSource() {
        return TextResourceReader.readTextFileFromResource(mContext,R.raw.enhance_vertex_shader);
//        return getVertexShader();
    }
    @Override
    protected String getfragmentSource() {
        return TextResourceReader.readTextFileFromResource(mContext, R.raw.enhance_fragment_shader);
//        return getFragmentShader();
    }

    @Override
    public void release() {
        if (mFBO > 0){
            int[] dd = new int[1];
            dd[0] = mFBO;
            GLES20.glDeleteFramebuffers(1,dd, 0 );
            mFBO = -1;
        }
        if (mVBO > 0) {
            int[] dd = new int[1];
            dd[0] = mVBO;
            GLES20.glDeleteBuffers(1,dd, 0 );
            mVBO = -1;
        }
        if (mProgram > 0){
            GLES20.glDeleteProgram(mProgram);
            mProgram = -1;
        }
    }

    public static native String getVertexShader();
    public static native String getFragmentShader();
    static {
        System.loadLibrary("shader");
    }
}
