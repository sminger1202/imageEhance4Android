package com.android.enhance;


import android.opengl.GLES20;
import android.util.Log;

import java.nio.FloatBuffer;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES20.GL_TEXTURE_2D;

/**
 * Created by shiming on 2017/5/25.
 */

class EnhanceEngine extends Engine{
    String TAG = this.getClass().getSimpleName();
    private static final int FLOAT_SIZE_BYTES = 4;
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

    private float[] mTriangleVerticesData = {
            // X, Y, U, V
            -1.0f, -1.0f, 0.f, 1.f,
            1.0f, -1.0f, 1.f, 1.f,
            -1.0f, 1.0f, 0.f, 0.f,
            1.0f, 1.0f, 1.f, 0.f,
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
    private static final FloatBuffer FULL_RECTANGLE_BUF =
            createFloatBuffer(FULL_RECTANGLE_COORDS);
    private static final FloatBuffer FULL_RECTANGLE_TEX_BUF =
            createFloatBuffer(FULL_RECTANGLE_TEX_COORDS);

    private boolean mTriangleVerticesDirty = true;
    private java.nio.FloatBuffer mTriangleVertices;

    int vertexCount;
    int coordsPerVertex;
    int vertexStride;
    int texCoordStride;
    FloatBuffer vertexArray;
    FloatBuffer texCoordArray;


    int dxLoc;
    int dyLoc;
    int mvpMatrixLocEhn;
    int texMatrixLocEhn;
    int positionLocEhn;
    int textureCoordLocEhn;

    int mFrameBufferObj;
    private int mBlitBuffer;

    private int mWidht;
    private int mHeight;


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
        vertexArray = FULL_RECTANGLE_BUF;
        vertexStride = coordsPerVertex * SIZEOF_FLOAT;
        texCoordArray = FULL_RECTANGLE_TEX_BUF;
        texCoordStride = 2 * SIZEOF_FLOAT;
        vertexCount = FULL_RECTANGLE_COORDS.length / coordsPerVertex;

        GLES20.glGenBuffers(1,glInt,0);
        mBlitBuffer = glInt[0];

        // Create blit mesh.
        mTriangleVertices = java.nio.ByteBuffer.allocateDirect(
                mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVerticesDirty = true;
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
        mFrameBufferObj = glInt[0];
    }

    @Override
    public void apply(final int srcTextureId, final int dstTextureId, final int width,final int height) {
        boolean isChanged = true;
        if (mWidht == width && mHeight == height) {
            isChanged = false;
        } else {
            mWidht = width;
            mHeight = height;
            isChanged = true;
        }
        saveGLState();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_2D, dstTextureId);
        if (isChanged) {
            Log.d(TAG, "change dst texture:" + mWidht + "x" + mHeight);
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GLES20.GL_RGBA,//allocate storage
                    mWidht, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObj);
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
        // Copy the model / view / projection matrix over.
        GLES20.glUseProgram(mProgram);
        GLES20.glViewport(0, 0,
                mWidht, mHeight);
        checkGlError("glUseProgram");
        GLES20.glUniformMatrix4fv(mvpMatrixLocEhn, 1, false, mvpMatrix, 0);
        checkGlError("glUniformMatrix4fv mvpMatrixLoc");

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(texMatrixLocEhn, 1, false, texMatrix, 0);
        checkGlError("glUniformMatrix4fv texMatrixLoc");

        //dx,dy
        GLES20.glUniform1f(dxLoc, 1.f / mWidht);
        checkGlError("dxloc");

        GLES20.glUniform1f(dyLoc, 1.f / mHeight);
        checkGlError("dyloc");


        UpdateVertexData();
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mBlitBuffer);

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(positionLocEhn);
        checkGlError("glEnableVertexAttribArray positionLoc");

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(positionLocEhn, coordsPerVertex,
                GLES20.GL_FLOAT, false, 4 * FLOAT_SIZE_BYTES, 0);
        checkGlError("glVertexAttribPointer positionLoc");

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(textureCoordLocEhn);
        checkGlError("glEnableVertexAttribArray textureCoordLoc");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(textureCoordLocEhn, 2,
                GLES20.GL_FLOAT, false, 4 * FLOAT_SIZE_BYTES, 2 * FLOAT_SIZE_BYTES);
        checkGlError("glVertexAttribPointer textureCoordLoc");

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(0, 0, 0, 0);
        // connect 'VideoTexture' to video source texture (mTextureID) in texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, srcTextureId);

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        checkGlError("glDrawArrays");
        restoreState();
    }

    private void UpdateVertexData()
    {
        if (!mTriangleVerticesDirty || mBlitBuffer <= 0)
        {
            return;
        }
        // fill it in
        mTriangleVertices.position(0);
        mTriangleVertices.put(mTriangleVerticesData).position(0);

        // save VBO state
        int[] glInt = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_ARRAY_BUFFER_BINDING, glInt, 0);
        int previousVBO = glInt[0];

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mBlitBuffer);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
                mTriangleVerticesData.length*FLOAT_SIZE_BYTES,
                mTriangleVertices, GLES20.GL_STATIC_DRAW);

        // restore VBO state
//        if (previousVBO > 0)
//        {
//            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, previousVBO);
//        }

        mTriangleVerticesDirty = false;
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
        if (previousFBO > 0) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFBO);
        }
        if (previousVBO > 0) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, previousVBO);
        }
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
