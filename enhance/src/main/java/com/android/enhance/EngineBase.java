package com.android.enhance;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;
import com.android.enhance.utils.SharderContainer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES30.GL_TEXTURE_2D;

/**
 * Created by shiming on 2017/5/25.
 */

public abstract class EngineBase implements IEngine{

    public String TAG = "EngineBase";
    protected String EngineName= "notset";
    protected boolean mIsInnerTexture = false;

    protected int mCurrentTextureId = -1;
    protected static final int FLOAT_SIZE_BYTES = 4;
    protected float[] mTriangleVerticesData = {//up
            // X, Y, U, V
            -1.0f, -1.0f, 0.f, 1.f,
            1.0f, -1.0f, 1.f, 1.f,
            -1.0f, 1.0f, 0.f, 0.f,
            1.0f, 1.0f, 1.f, 0.f,
    };
//    protected float[] mTriangleVerticesData = {//down
//            // X, Y, U, V
//            -1.0f, -1.0f, 0.f, 0.f,
//            1.0f, -1.0f, 1.f, 0.f,
//            -1.0f, 1.0f, 0.f, 1.f,
//            1.0f, 1.0f, 1.f, 1.f,
//    };
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
    protected int mFBO = -1;
    protected int mVBO = -1;
    protected int mWidth = 0;
    protected int mHeight = 0;
    protected int mChannels = 0;

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
        int shader = GLES30.glCreateShader(shaderType);
        if (shader != 0) {
            GLES30.glShaderSource(shader, source);
            GLES30.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                String info = GLES30.glGetShaderInfoLog(shader);
                GLES30.glDeleteShader(shader);
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
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, getVertexSource());
        if (vertexShader == 0) {
            Log.e(TAG, "load vertexShader failed.");
            return 0;
        }
        int pixelShader = loadShader(GLES30.GL_FRAGMENT_SHADER, getfragmentSource());
        if (pixelShader == 0) {

            Log.e(TAG, "load pixelShader failed.");
            return 0;
        }

        int program = GLES30.glCreateProgram();
        if (program != 0) {
            GLES30.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES30.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES30.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus,
                    0);
            if (linkStatus[0] != GLES30.GL_TRUE) {
                String info = GLES30.glGetProgramInfoLog(program);
                GLES30.glDeleteProgram(program);
                program = 0;
                Log.e(TAG, info);
                throw new RuntimeException("Could not link program: " + info);
            }
        }
        return program;
    }

    protected void checkGlError(String op) {
        int error;
        while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
            Log.e(TAG, "check GL Error "+ op + " in program");
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    protected void checkLocation(int location, String label) {
        if (location < 0) {
            Log.e(TAG, "Unable to locate '" + label + "' in program");
//            throw new RuntimeException("Unable to locate '" + label + "' in program");
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
        GLES30.glBindTexture(GL_TEXTURE_2D, 0);
        return texId;
    }

    protected void initTexParams() {
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S,
                GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T,
                GLES30.GL_CLAMP_TO_EDGE);
    }
    void saveGLState() {

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
    void restoreState() {
        // ======Restore state and cleanup.

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, previousFBO);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, previousVBO);
        GLES30.glViewport(previousViewport[0], previousViewport[1],
                previousViewport[2], previousViewport[3]);
        if (previousBlend) GLES30.glEnable(GLES30.GL_BLEND);
        if (previousCullFace) GLES30.glEnable(GLES30.GL_CULL_FACE);
        if (previousScissorTest) GLES30.glEnable(GLES30.GL_SCISSOR_TEST);
        if (previousStencilTest) GLES30.glEnable(GLES30.GL_STENCIL_TEST);
        if (previousDepthTest) GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        if (previousDither) GLES30.glEnable(GLES30.GL_DITHER);
    }

    public String getVertexShader() {
        Log.i(TAG, "current engine name :" + EngineName);
        return SharderContainer.getVertexShader(EngineName);
    }
    public String getFragmentShader() {
        return SharderContainer.getFragmentShader(EngineName);
    }
    @Override
    public abstract void release();
}
