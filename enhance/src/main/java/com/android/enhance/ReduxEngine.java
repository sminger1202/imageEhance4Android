package com.android.enhance;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;
import com.android.enhance.utils.TextResourceReader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import static android.opengl.GLES20.GL_TEXTURE_2D;

/**
 * Created by shiming on 2017/7/6.
 */

public class ReduxEngine extends EngineBase {
    static final String TAG = ReduxEngine.class.getSimpleName();

    private int dxLoc;
    private int dyLoc;
    private int mvpMatrixLoc;
    private int texMatrixLoc;
    private int positionLoc;
    private int textureCoordLoc;

    private float dx = 0.f;
    private float dy = 0.f;

    private int mType = -1;
    private final int VALUE_MAX = 0;
    private final int VALUE_MIN = 1;
    private final int VALUE_MEAN = 2;
    private ImageGL mCurrentImg;
    private ArrayList<ImageGL> imgList = new ArrayList<ImageGL>();
    private ByteBuffer byteBuffer;
    private byte[] byteData;
    private String[] mMethodStr =  {
            "color = max(color00, color10);\n color = max(color, color01);\n color = max(color, color11);\n",
            "color = min(color00, color10);\n color = min(color, color01);\n color = min(color, color11);\n",
            "color = (color00 + color10 + color01 + color11) / 4.0; \n"
    };

    public ReduxEngine(Context context) {
        mContext = context;//这里必须初始化上下文，否则手动调用init的时候会有问题
    }

    private int divideByTwoWithEvenDividend(int x) {
        if((x % 2) != 0) {
            x++;
        }
        return (x >> 1);
    }

    private int createTexture(int width, int height) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("glGenTextures");
        int textureID = textures[0];
        GLES20.glBindTexture(GL_TEXTURE_2D, textureID);
        initTexParams();
        GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GLES20.GL_RGBA,//allocate storage
                width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);
        return textureID;

    }
    @Override
    public void init(Context context) {
        super.init(context);
        imgList.clear();
        int width = mWidth;
        int height = mHeight;
        int minSize = 2;
        int checkSize = divideByTwoWithEvenDividend(Math.min(mWidth, mHeight));
        while (checkSize >= minSize) {
            ImageGL imageGL = new ImageGL();
            width = divideByTwoWithEvenDividend(width);
            height = divideByTwoWithEvenDividend(height);
            imageGL.channels = mChannels;
            imageGL.width = width;
            imageGL.height = height;
            imageGL.textureID = createTexture(width, height);
            imgList.add(imageGL);
            checkSize = Math.min(width, height);
        }
        for (ImageGL img: imgList) {
            Log.d(TAG, "repeat size :" + img.width + "x" + img.height );
        }
        //申请最后一个块的大小

        byteBuffer = ByteBuffer.allocateDirect(width * height * mChannels);
        byteData = new byte[width * height * mChannels];
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

    }

    @Override
    public void apply(int[] srcTextureId, int dstTextureId, int width, int height) {

    }

    @Override
    protected void localAttri() {
        positionLoc = GLES20.glGetAttribLocation(mProgram, "vPosition");
        checkLocation(positionLoc, "vPosition ");
        textureCoordLoc = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        checkLocation(textureCoordLoc, "inputTextureCoordinate ");
        mvpMatrixLoc = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkLocation(mvpMatrixLoc, "uMVPMatrix ");

        dxLoc = GLES20.glGetUniformLocation(mProgram, "dx");
        checkLocation(dxLoc, "dx ");
        dyLoc = GLES20.glGetUniformLocation(mProgram, "dy");
        checkLocation(dyLoc, "dy ");

    }

    @Override
    protected void initFBO() {
        int[] glInt = new int[1];
        if (mFBO > 0) {
            glInt[0] = mFBO;
            GLES20.glDeleteBuffers(1, glInt, 0);
        }
        GLES20.glGenFramebuffers(1,glInt,0);
        mFBO=  glInt[0];
        for (ImageGL imgGl: imgList) {
            imgGl.FBO = mFBO;
        }
    }

    @Override
    protected void initVBO() {
        int[] glInt = new int[1];
        if (mVBO > 0) {
            glInt[0] = mVBO;
            GLES20.glDeleteBuffers(1, glInt, 0);
        }
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
        for (ImageGL imgGl: imgList) {
            imgGl.VBO = mVBO;
        }
    }

    @Override
    public void apply(int srcTextureId, int dstTextureId, int width, int height) {
        saveGLState();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_2D, dstTextureId);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFBO);
        //因为每次的dstTextureID肯定是不一样的
        GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_2D, dstTextureId, 0);

        // check status
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "glCheckFramebufferStatus error" + status);
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBO);
        GLES20.glUseProgram(mProgram);
        GLES20.glViewport(0, 0,
                width, height);
        checkGlError("glUseProgram");
        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(positionLoc);
        checkGlError("glEnableVertexAttribArray positionLoc");

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(positionLoc, coordsPerVertex,
                GLES20.GL_FLOAT, false, vertexStride, 0);
        checkGlError("glVertexAttribPointer positionLoc");

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(textureCoordLoc);
        checkGlError("glEnableVertexAttribArray textureCoordLoc");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(textureCoordLoc, colorPerVertex,
                GLES20.GL_FLOAT, false, vertexStride, coordsPerVertex * FLOAT_SIZE_BYTES);
        checkGlError("glVertexAttribPointer textureCoordLoc");


        GLES20.glUniformMatrix4fv(mvpMatrixLoc, 1, false, mvpMatrix, 0);
        checkGlError("glUniformMatrix4fv mvpMatrixLoc");

        //dx,dy
        GLES20.glUniform1f(dxLoc, dx);
        checkGlError("dxloc");

        GLES20.glUniform1f(dyLoc, dy);
        checkGlError("dyloc");

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(0, 0, 0, 0);
        // connect 'VideoTexture' to video source texture (srcTextureId) in texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, srcTextureId);
        checkGlError("bind texture");
        // Draw the rect.

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount);
        checkGlError("glDrawArrays");
        restoreState();
    }

    public float[] getValue(int srcTexture, int width, int heigh, int channels, int type) {
        if (width != mWidth ||
                heigh != mHeight||
                channels != mChannels||
                mType != type) {
            mWidth = width;
            mHeight = heigh;
            mChannels = channels;
            mType = type;
            init(mContext);
            dx = 1.0f / width;
            dy = 1.0f / heigh;
        }
        for (ImageGL imgGl: imgList) {
            mCurrentImg = imgGl;
            apply(srcTexture, imgGl.textureID, imgGl.width, imgGl.height);
            srcTexture = imgGl.textureID;
            dx = dx / 2;
            dy = dy / 2;
        }

        //读取数据
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_2D, mCurrentImg.textureID);
        byteBuffer.position(0);
        GLES20.glReadPixels(0, 0, mCurrentImg.width, mCurrentImg.height,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);
        byteBuffer.position(0);
        int size = mCurrentImg.width * mCurrentImg.height * channels;
        byteBuffer.get(byteData, 0, size);
        float[] value = new float[size];
        for (int i = 0; i < size ;++i) {
            value[i] = (byteData[i] & 0xff) / 255;
            Log.d(TAG, "value" + i + ":" + value[i]);
        }
        return value;
    }
    public float[] getMaxValue(int srcTexture, int width, int heigh, int channels) {
        return getValue(srcTexture, width, heigh, channels, VALUE_MAX);
    }

    public float[] getMinValue(int srcTexture, int width, int heigh, int channels) {
        return getValue(srcTexture, width, heigh, channels, VALUE_MIN);
    }

    public float[] getMeanValue(int srcTexture, int width, int heigh, int channels) {
        return getValue(srcTexture, width, heigh, channels, VALUE_MEAN);
    }

    @Override
    protected String getVertexSource() {
        String vertexStr = TextResourceReader.readTextFileFromResource(mContext, R.raw.redux_vertex_shader);
        return vertexStr;
    }
    @Override
    protected String getfragmentSource() {
        String fragmentStr = TextResourceReader.readTextFileFromResource(mContext, R.raw.redux_fragment_shader);
        fragmentStr.replace("___REDUX_OPERATION___", mMethodStr[mType]);
        return fragmentStr;
    }


    @Override
    public void release() {

    }

    class ImageGL {
        int channels;
        int width;
        int height;
        int textureID;
        int FBO;
        int VBO;
    }
}
