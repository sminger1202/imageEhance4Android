package com.android.enhance;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by shiming on 2017/5/25.
 */

public abstract class EngineBase implements IEngine{

    final String TAG = "Engine";
    protected static final int SIZEOF_FLOAT = 4;
    protected int mProgram;
    protected void init() {
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
        Log.e(TAG, label + "'s location is :" + location);
        if (location < 0) {
            throw new RuntimeException("Unable to locate '" + label + "' in program");
        }
    }

    protected static FloatBuffer createFloatBuffer(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
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

    public void release() {

    }
}
