package com.android.enhance;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;
import com.android.enhance.utils.TextResourceReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import static android.opengl.GLES30.GL_TEXTURE_2D;

/**
 * Created by shiming on 2017/7/6.
 */

public class ReduxEngine extends EngineBase {

    private int positionLoc;

    private final int VALUE_MAX = 0;
    private final int VALUE_MIN = 1;
    private final int VALUE_MEAN = 2;
    private int mType = VALUE_MAX;
    private ImageGL mCurrentImg;
    private ArrayList<ImageGL> imgList = new ArrayList<ImageGL>();
    private ByteBuffer byteBuffer;
    private byte[] byteData;
    private String[] mMethodStr =  {
            "color = OPS(color00, color10); color = OPS(color, color01); color = OPS(color, color11);",
            "color = OPS(color00, color10); color = OPS(color, color01); color = OPS(color, color11);",
            "color = (color00 + color10 + color01 + color11) / 4.0;"
    };

    private String[] mOps = {
            "max(x, y)",
            "min(x, y)",
            "(x + y) / 2.0"
    };

    public ReduxEngine(Context context) {
        TAG = ReduxEngine.class.getSimpleName();
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
        GLES30.glGenTextures(1, textures, 0);
        checkGlError("glGenTextures");
        int textureID = textures[0];
        GLES30.glBindTexture(GL_TEXTURE_2D, textureID);
        initTexParams();
        GLES30.glTexImage2D(GL_TEXTURE_2D, 0, GLES30.GL_RGBA,//allocate storage
                width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        GLES30.glBindTexture(GL_TEXTURE_2D, 0);
        return textureID;
    }

    @Override
    public void init(Context context) {
        super.init(context);

        imgList.clear();
        int width = mWidth;
        int height = mHeight;
        while (!(width == 1 && height == 1)) {
            ImageGL imageGL = new ImageGL();
            width = divideByTwoWithEvenDividend(width);
            height = divideByTwoWithEvenDividend(height);
            imageGL.channels = mChannels;
            imageGL.width = width;
            imageGL.height = height;
            imageGL.textureID = createTexture(width, height);
            imgList.add(imageGL);
        }
        for (ImageGL img: imgList) {
            Log.d(TAG, "repeat size :" + img.width + "x" + img.height );
        }

        //申请最后一个块的大小
        byteBuffer = ByteBuffer.allocateDirect(width * height * mChannels);
        byteData = new byte[width * height * mChannels];
        Log.d(TAG, "final size : " + width + "x" + height);
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
        positionLoc = GLES30.glGetAttribLocation(mProgram, "vPosition");
        checkLocation(positionLoc, "vPosition ");
    }

    @Override
    protected void initFBO() {
        int[] glInt = new int[1];
        if (mFBO > 0) {
            glInt[0] = mFBO;
            GLES30.glDeleteBuffers(1, glInt, 0);
        }
        GLES30.glGenFramebuffers(1,glInt,0);
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
        for (ImageGL imgGl: imgList) {
            imgGl.VBO = mVBO;
        }
    }

    @Override
    public void apply(int srcTextureId, int dstTextureId, int width, int height) {
        saveGLState();
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GL_TEXTURE_2D, dstTextureId);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFBO);
        //因为每次的dstTextureID肯定是不一样的
        GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER,
                GLES30.GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_2D, dstTextureId, 0);

        // check status
        int status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "glCheckFramebufferStatus error" + status);
        }
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVBO);
        GLES30.glUseProgram(mProgram);
        GLES30.glViewport(0, 0,
                width, height);
        checkGlError("glUseProgram");
        // Enable the "aPosition" vertex attribute.
        GLES30.glEnableVertexAttribArray(positionLoc);
        checkGlError("glEnableVertexAttribArray positionLoc");

        // Connect vertexBuffer to "aPosition".
        GLES30.glVertexAttribPointer(positionLoc, coordsPerVertex,
                GLES30.GL_FLOAT, false, vertexStride, 0);
        checkGlError("glVertexAttribPointer positionLoc");

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glClearColor(0, 0, 0, 0);
        // connect 'VideoTexture' to video source texture (srcTextureId) in texture unit 0.
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, srcTextureId);
        checkGlError("bind texture");
        // Draw the rect.

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertexCount);
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
        }
        for (ImageGL imgGl: imgList) {
            mCurrentImg = imgGl;
            apply(srcTexture, imgGl.textureID, imgGl.width, imgGl.height);
            srcTexture = imgGl.textureID;
        }

        //读取数据
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFBO);
        byteBuffer.position(0);

        GLES30.glReadPixels(0, 0, mCurrentImg.width, mCurrentImg.height,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, byteBuffer);
        byteBuffer.position(0);
        int size = mCurrentImg.width * mCurrentImg.height * channels;
        byteBuffer.get(byteData, 0, size);

        float[] value = new float[size];
        for (int i = 0; i < size ;++i) {
            Log.d(TAG, "value" + i + ":" + byteData[i]);
            value[i] = (byteData[i] & 0xff) / 255.f;
            Log.d(TAG, "value float" + i + ":" + value[i]);
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
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
        fragmentStr = fragmentStr.replace("___REDUX_OPERATION___", mMethodStr[mType]);
        fragmentStr = fragmentStr.replace("yourMethod", mOps[mType]);
        return fragmentStr;
    }

    @Override
    public void release() {
        if (mFBO > 0) {
            glInt[0] = mFBO;
            GLES30.glDeleteBuffers(1, glInt, 0);
        }
        if (mVBO > 0) {
            glInt[0] = mVBO;
            GLES30.glDeleteBuffers(1, glInt, 0);
        }
        if (mProgram > 0){
            GLES30.glDeleteProgram(mProgram);
            mProgram = -1;
        }
        for (ImageGL img : imgList) {
            glInt[0] = img.textureID;
            GLES20.glDeleteTextures(1, glInt, 0);
        }
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
