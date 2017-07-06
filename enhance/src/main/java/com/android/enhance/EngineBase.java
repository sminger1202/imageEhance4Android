package com.android.enhance;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_TEXTURE_2D;

/**
 * Created by shiming on 2017/5/25.
 */

public abstract class EngineBase implements IEngine{

    final String TAG = "EngineBase";

    protected static final int FLOAT_SIZE_BYTES = 4;
//    protected float[] mTriangleVerticesData = {
//            // X, Y, U, V
//            -1.0f, -1.0f, 0.f, 1.f,
//            1.0f, -1.0f, 1.f, 1.f,
//            -1.0f, 1.0f, 0.f, 0.f,
//            1.0f, 1.0f, 1.f, 0.f,
//    };
    protected float[] mTriangleVerticesData = {
            // X, Y, U, V
            -1.0f, -1.0f, 0.f, 0.f,
            1.0f, -1.0f, 1.f, 0.f,
            -1.0f, 1.0f, 0.f, 1.f,
            1.0f, 1.0f, 1.f, 1.f,
    };
    protected float mvpMatrix[] = {
            1.0f, 0.f, 0.f, 0.f,
            0.f, 1.f, 0.f, 0.f,
            0.f, 0.f, 1.f, 0.f,
            0.f, 0.f, 0.f, 1.f,
    };
    protected float texMatrix[] = {
            1.0f, 0.f, 0.f, 0.f,
            0.f, 1.f, 0.f, 0.f,
            0.f, 0.f, 1.f, 0.f,
            0.f, 0.f, 0.f, 1.f,
    };

    protected Context mContext;
    protected java.nio.FloatBuffer mTriangleVertices;
    protected int mFBO;
    protected int mVBO;
    protected int mWidth = 0;
    protected int mHeight = 0;

    protected int coordsPerVertex;
    protected int colorPerVertex;
    protected int vertexSize;
    protected int vertexCount;
    protected int vertexStride;

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


    protected int mProgram;
    protected void init(Context context) {
        mContext = context;
        mProgram = createProgram();
        localAttri();
        initVBO();
        initFBO();
    }
    abstract protected void localAttri();
    abstract protected void initFBO();
    abstract protected void initVBO();

    /**
     *
     * @param srcTextureId 外部纹理作为输入
     * @param dstTextureId 输出纹理
     * @param width        输出纹理的宽
     * @param height       输出纹理的高
     */
    abstract public void apply(int srcTextureId, int dstTextureId, int width, int height);

    protected String getVertexSource() {
        return null;
    }

    protected String getfragmentSource() {
        return null;
    }
    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                String info = GLES20.glGetShaderInfoLog(shader);
                GLES20.glDeleteShader(shader);
                shader = 0;
                Log.e(TAG, " :Could not compile shader " +
                        shaderType + ":" + info);
                throw new RuntimeException(TAG +" :Could not compile shader " +
                        shaderType + ":" + info);
            }
        }
        return shader;
    }

    private int createProgram() {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, getVertexSource());
        if (vertexShader == 0) {
            Log.e(TAG, "load vertexShader failed.");
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, getfragmentSource());
        if (pixelShader == 0) {

            Log.e(TAG, "load pixelShader failed.");
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus,
                    0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                String info = GLES20.glGetProgramInfoLog(program);
                GLES20.glDeleteProgram(program);
                program = 0;
                Log.e(TAG, info);
                throw new RuntimeException("Could not link program: " + info);
            }
        }
        return program;
    }

    protected void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    protected void checkLocation(int location, String label) {
        if (location < 0) {
            throw new RuntimeException("Unable to locate '" + label + "' in program");
        }
    }

    protected static FloatBuffer createFloatBuffer(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * FLOAT_SIZE_BYTES);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    public int createInnerTextureObject() {
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

    protected void initTexParams() {
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
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
    public abstract void release();
}
