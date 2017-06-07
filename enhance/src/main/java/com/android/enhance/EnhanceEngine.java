package com.android.enhance;


import android.opengl.GLES20;
import android.util.Log;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES20.GL_TEXTURE_2D;

/**
 * Created by shiming on 2017/5/25.
 */

public class EnhanceEngine extends EngineBase{
    String TAG = this.getClass().getSimpleName();
    private static final int FLOAT_SIZE_BYTES = 4;

    private float[] mTriangleVerticesData = {
            // X, Y, U, V
            -1.0f, -1.0f, 0.f, 0.f,
            1.0f, -1.0f, 1.f, 0.f,
            -1.0f, 1.0f, 0.f, 1.f,
            1.0f, 1.0f, 1.f, 1.f,
    };

    private float mvpMatrix[] = {
            1.0f, 0.f, 0.f, 0.f,
            0.f, 1.f, 0.f, 0.f,
            0.f, 0.f, 1.f, 0.f,
            0.f, 0.f, 0.f, 1.f,
    };
    private float texMatrix[] = {
            1.0f, 0.f, 0.f, 0.f,
            0.f, 1.f, 0.f, 0.f,
            0.f, 0.f, 1.f, 0.f,
            0.f, 0.f, 0.f, 1.f,
    };

    private java.nio.FloatBuffer mTriangleVertices;

    private int coordsPerVertex;
    private int colorPerVertex;
    private int vertexSize;
    private int vertexCount;
    private int vertexStride;


    private int dxLoc;
    private int dyLoc;
    private int mvpMatrixLocEhn;
    private int texMatrixLocEhn;
    private int positionLocEhn;
    private int textureCoordLocEhn;
    private int mFBO;
    private int mVBO;

    private int mWidht = 0;
    private int mHeight = 0;
    private float dx = 0.f;
    private float dy = 0.f;


    int[] glInt = new int[1];
    boolean previousBlend;
    boolean previousCullFace;
    boolean previousScissorTest;
    boolean previousStencilTest;
    boolean previousDepthTest;
    boolean previousDither;
    int previousFBO;
    int previousVBO;
    int[] previousViewport = new int[4];

    public EnhanceEngine(){
        init();
    }
    @Override
    public void init() {
        super.init();
        coordsPerVertex = 2;
        colorPerVertex = 2;
        vertexSize = coordsPerVertex + colorPerVertex; // x, y, u, v
        vertexCount = mTriangleVerticesData.length / vertexSize;
        vertexStride = vertexSize * SIZEOF_FLOAT;
    }

    @Override
    public void localAttri() {
        positionLocEhn = GLES20.glGetAttribLocation(mProgram, "aPosition");
        checkLocation(positionLocEhn, "aPosition Ehn");
        textureCoordLocEhn = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        checkLocation(textureCoordLocEhn, "aTextureCoord Ehn");
        mvpMatrixLocEhn = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkLocation(mvpMatrixLocEhn, "uMVPMatrix Ehn");
        texMatrixLocEhn = GLES20.glGetUniformLocation(mProgram, "uTexMatrix");
        checkLocation(texMatrixLocEhn, "uTexMatrix Ehn");
        dxLoc = GLES20.glGetUniformLocation(mProgram, "dx");
        checkLocation(dxLoc, "dx Ehn");
        dyLoc = GLES20.glGetUniformLocation(mProgram, "dy");
        checkLocation(dyLoc, "dy Ehn");
    }

    @Override
    public void setParameter(String field, float value) {

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
        boolean isChanged = true;
        if (mWidht == width && mHeight == height) {
            isChanged = false;
        } else {
            mWidht = width;
            mHeight = height;
            dx = 1.f / mWidht;
            dy = 1.f / mHeight;
            isChanged = true;
        }
        saveGLState();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_2D, dstTextureId);
        if (isChanged) {
            Log.d(TAG, "change dst texture:" + mWidht + "x" + mHeight);
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GLES20.GL_RGBA,//allocate storage
                    mWidht, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
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
                mWidht, mHeight);
        checkGlError("glUseProgram");

        GLES20.glUniformMatrix4fv(mvpMatrixLocEhn, 1, false, mvpMatrix, 0);
        checkGlError("glUniformMatrix4fv mvpMatrixLoc");

        GLES20.glUniformMatrix4fv(texMatrixLocEhn, 1, false, texMatrix, 0);
        checkGlError("glUniformMatrix4fv texMatrixLoc");

        //dx,dy
        GLES20.glUniform1f(dxLoc, dx);
        checkGlError("dxloc");

        GLES20.glUniform1f(dyLoc, dy);
        checkGlError("dyloc");

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

    void saveGLState() {

        previousBlend = GLES20.glIsEnabled(GLES20.GL_BLEND);
        previousCullFace = GLES20.glIsEnabled(GLES20.GL_CULL_FACE);
        previousScissorTest = GLES20.glIsEnabled(GLES20.GL_SCISSOR_TEST);
        previousStencilTest = GLES20.glIsEnabled(GLES20.GL_STENCIL_TEST);
        previousDepthTest = GLES20.glIsEnabled(GLES20.GL_DEPTH_TEST);
        previousDither = GLES20.glIsEnabled(GLES20.GL_DITHER);
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, glInt, 0);
        previousFBO = glInt[0];
        GLES20.glGetIntegerv(GLES20.GL_ARRAY_BUFFER_BINDING, glInt, 0);
        previousVBO = glInt[0];
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, previousViewport, 0);

        checkGlError("save state");

        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        GLES20.glDisable(GLES20.GL_STENCIL_TEST);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_DITHER);
        GLES20.glColorMask(true, true, true, true);

        checkGlError("reset state");
    }
    void restoreState() {
        // ======Restore state and cleanup.

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFBO);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, previousVBO);
        GLES20.glViewport(previousViewport[0], previousViewport[1],
                previousViewport[2], previousViewport[3]);
        if (previousBlend) GLES20.glEnable(GLES20.GL_BLEND);
        if (previousCullFace) GLES20.glEnable(GLES20.GL_CULL_FACE);
        if (previousScissorTest) GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        if (previousStencilTest) GLES20.glEnable(GLES20.GL_STENCIL_TEST);
        if (previousDepthTest) GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        if (previousDither) GLES20.glEnable(GLES20.GL_DITHER);
    }

    @Override
    protected String getVertexSource() {
        return getVertexShader();
    }
    @Override
    protected String getfragmentSource() {
        return getFragmentShader();
    }

    public static native String getVertexShader();
    public static native String getFragmentShader();
    static {
        System.loadLibrary("shader");
    }
}
