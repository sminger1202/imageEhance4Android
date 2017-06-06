package com.example.hellojni;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by shiming on 2017/5/17.
 */

public class VideoRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private String TAG = VideoRender.class.getSimpleName();
    /* All GL related fields from
     * http://developer.android.com/resources/samples/ApiDemos/src/com/example
     * /android/apis/graphics/GLES20TriangleRenderer.html
     */

    public static class VideoDumpConfig {
        // Currently we are running with a local copy of the video.
        // It should work with a "http://" sort of streaming url as well.
        public static final String VIDEO_URI = "/sdcard/mediadump/sample.mp4";
        public static final String ROOT_DIR = "/sdcard/mediadump/";
        public static final String IMAGES_LIST = "images.lst";
        public static final String IMAGE_PREFIX = "img";
        public static final String IMAGE_SUFFIX = ".rgb";
        public static final String PROPERTY_FILE = "prop.xml";

        // So far, glReadPixels only supports two (format, type) combinations
        //     GL_RGB  GL_UNSIGNED_SHORT_5_6_5   16 bits per pixel (default)
        //     GL_RGBA GL_UNSIGNED_BYTE          32 bits per pixel
        public static final int PIXEL_FORMAT = GLES20.GL_RGB;
        public static final int PIXEL_TYPE = PIXEL_FORMAT == GLES20.GL_RGBA
                ? GLES20.GL_UNSIGNED_BYTE : GLES20.GL_UNSIGNED_SHORT_5_6_5;
        public static final int BYTES_PER_PIXEL =
                PIXEL_FORMAT == GLES20.GL_RGBA ? 4 : 2;
        public static final boolean SET_CHOOSER
                = PIXEL_FORMAT == GLES20.GL_RGBA ? true : false;

        // On Motorola Xoom, it takes 100ms to read pixels and 180ms to write to a file
        // to dump a complete 720p(1280*720) video frame. It's much slower than the frame
        // playback interval (40ms). So we only dump a center block and it should be able
        // to catch all the e2e distortion. A reasonable size of the block is 256x256,
        // which takes 4ms to read pixels and 25 ms to write to a file.
        public static final int MAX_DUMP_WIDTH = 256;
        public static final int MAX_DUMP_HEIGHT = 256;

        // TODO: MediaPlayer doesn't give back the video frame rate and we'll need to
        // figure it by dividing the total number of frames by the duration.
        public static final int FRAME_RATE = 25;
    }
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    private final float[] mTriangleVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0.f, 0.f, 0.f,
            1.0f, -1.0f, 0.f, 1.f, 0.f,
            -1.0f,  1.0f, 0.f, 0.f, 1.f,
            1.0f,  1.0f, 0.f, 1.f, 1.f,
    };

    private FloatBuffer mTriangleVertices;

    /**
     * original
     */

    private final String mVertexShader =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    private final String mFragmentShader =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";


    /**
     * modified
     */
//    private final String mVertexShader =
//            "uniform mat4 uMVPMatrix;\n" +
//                    "uniform mat4 uSTMatrix;\n" +
//                    "attribute vec4 aPosition;\n" +
//                    "attribute vec4 aTextureCoord;\n" +
//                    "varying vec2 vTextureCoord;\n" +
//                    "void main() {\n" +
//                    "  gl_Position = aPosition;\n" +
//                    "  vTextureCoord = (aTextureCoord).xy;\n" +
//                    "}\n";
//
//    private final String mFragmentShader =
//            "#extension GL_OES_EGL_image_external : require\n" +
//                    "precision mediump float;\n" +
//                    "varying vec2 vTextureCoord;\n" +
//                    "uniform samplerExternalOES sTexture;\n" +
//                    "void main() {\n" +
//                    "  gl_FragColor = vec4(1.f, 1.f, 0.f, 1.f);\n" +
//                    "}\n";

//    private final String mVertexShader =
//            "uniform mat4 uMVPMatrix;\n" +
//                    "uniform mat4 uSTMatrix;\n" +
//                    "attribute vec4 aPosition;\n" +
//                    "attribute vec4 aTextureCoord;\n" +
//                    "varying vec2 vTextureCoord;\n" +
//                    "void main() {\n" +
//                    "  gl_Position = uMVPMatrix * aPosition;\n" +
//                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
//                    "}\n";

//    private final String mFragmentShader =
//            "#extension GL_OES_EGL_image_external : require\n" +
//                    "precision mediump float;\n" +
//                    "varying vec2 vTextureCoord;\n" +
//                    "uniform samplerExternalOES sTexture;\n" +
//                    "void main() {\n" +
//                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
//                    "}\n";

    private float[] mMVPMatrix = new float[16];
    private float[] mSTMatrix = new float[16];

    private int mProgram;

    private int muMVPMatrixHandle;
    private int muSTMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;

    private int mTextureID;
    private SurfaceTexture surfaceTexture;
    private Surface mSurface;
    private boolean updateSurface = false;

    // Magic key
    //private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;


    /**
     * Fields that reads video source and dumps to file.
     */
    // The media player that loads and decodes the video.
    // Not owned by this class.
    private MediaPlayer mMediaPlayer;
    // The frame number from media player.
    private int mFrameNumber = 0;
    // The frame number that is drawing on screen.
    private int mDrawNumber = 0;
    // The width and height of dumping block.
    private int mWidth = 0;
    private int mHeight = 0;
    // The offset of the dumping block.
    private int mStartX = 0;
    private int mStartY = 0;
    // A buffer to hold the dumping pixels.
    private ByteBuffer mBuffer = null;
    // A file writer to write the filenames of images.
    private BufferedWriter mImageListWriter;
    private WeakReference<VideoPreviewView> mVideoPreview;

    private int textureTarget;
    public VideoRender(Context context) {
        Log.d(TAG, "VideoRender");
        mTriangleVertices = ByteBuffer.allocateDirect(
                mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(mTriangleVerticesData).position(0);
        textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
        Matrix.setIdentityM(mSTMatrix, 0);
    }

    public void setMediaPlayer(MediaPlayer player) {
        mMediaPlayer = player;
    }

    public void setImageListWriter(BufferedWriter imageListWriter) {
        mImageListWriter = imageListWriter;
    }

    /**
     * Called to draw the current frame.
     * This method is responsible for drawing the current frame.
     */
    @Override
    public void onDrawFrame(GL10 glUnused) {
        Log.d(TAG, "onDrawFrame：" );
        boolean isNewFrame = false;
        int frameNumber = 0;

        synchronized(this) {
            if (updateSurface) {
                Log.d(TAG, "onDrawFrame：in =======" + surfaceTexture.getTimestamp() );
                isNewFrame = true;
                frameNumber = mFrameNumber;
                surfaceTexture.updateTexImage();

                surfaceTexture.getTransformMatrix(mSTMatrix);
                updateSurface = false;
            }
        }


        // Initial clear.
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        // Load the program, which is the basics rules to draw the vertexes and textures.
        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");

        // Activate the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);

        // Load the vertexes coordinates. Simple here since it only draw a rectangle
        // that fits the whole screen.
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        checkGlError("glEnableVertexAttribArray maPositionHandle");
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        checkGlError("glVertexAttribPointer maPosition");

        // Load the texture coordinates, which is essentially a rectangle that fits
        // the whole video frame.
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        checkGlError("glEnableVertexAttribArray maTextureHandle");
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        checkGlError("glVertexAttribPointer maTextureHandle");

        // Set up the GL matrices.
        Matrix.setIdentityM(mMVPMatrix, 0);
        Matrix.setIdentityM(mSTMatrix, 0);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

        // Draw a rectangle and render the video frame as a texture on it.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        checkGlError("glDrawArrays");
        //GLES20.glFinish();

        GLES20.glDisableVertexAttribArray(maTextureHandle);
        GLES20.glDisableVertexAttribArray(maPositionHandle);
        GLES20.glBindTexture(textureTarget, 0);
        GLES20.glUseProgram(0);

//        if (isNewFrame) {  // avoid duplicates.
//            Log.d(TAG, mDrawNumber + "/" + frameNumber + " before dumping "
//                    + System.currentTimeMillis());
//            DumpToFile(frameNumber);
//            Log.d(TAG, mDrawNumber + "/" + frameNumber + " after  dumping "
//                    + System.currentTimeMillis());
//
//            mDrawNumber++;
//        }
    }

    // Call the GL function that dumps the screen into a buffer, then write to a file.
    private void DumpToFile(int frameNumber) {
        GLES20.glReadPixels(mStartX, mStartY, mWidth, mHeight,
                VideoDumpConfig.PIXEL_FORMAT,
                VideoDumpConfig.PIXEL_TYPE,
                mBuffer);

        checkGlError("glReadPixels");
        String filename = "/sdcard/img.argb";
        try {
            mImageListWriter.write(filename);
            mImageListWriter.newLine();
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(mBuffer.array());
            fos.close();
        } catch (java.io.IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }


//        GLES20.glReadPixels(mStartX, mStartY, mWidth, mHeight,
//                VideoDumpConfig.PIXEL_FORMAT,
//                VideoDumpConfig.PIXEL_TYPE,
//                mBuffer);
//        checkGlError("glReadPixels");
//
//        Log.d(TAG, mDrawNumber + "/" + frameNumber + " after  glReadPixels "
//                + System.currentTimeMillis());
//
//        String filename =  VideoDumpConfig.ROOT_DIR + VideoDumpConfig.IMAGE_PREFIX
//                + frameNumber + VideoDumpConfig.IMAGE_SUFFIX;
//        try {
//            mImageListWriter.write(filename);
//            mImageListWriter.newLine();
//            FileOutputStream fos = new FileOutputStream(filename);
//            fos.write(mBuffer.array());
//            fos.close();
//        } catch (java.io.IOException e) {
//            Log.e(TAG, e.getMessage(), e);
//        }
    }

    /**
     * Called when the surface changed size.
     * Called after the surface is created and whenever the OpenGL surface size changes.
     */
    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        Log.d(TAG, "Surface size: " + width + "x" + height);

        int video_width = mMediaPlayer.getVideoWidth();
        int video_height = mMediaPlayer.getVideoHeight();
        Log.d(TAG, "Video size: " + video_width
                + "x" + video_height);

        // TODO: adjust video_width and video_height with the surface size.
        //GLES20.glViewport(0, 0, width, height);
       // GLES20.glViewport(0, 0, video_width, video_height);
//
//        mWidth = Math.min(VideoDumpConfig.MAX_DUMP_WIDTH, video_width);
//        mHeight = Math.min(VideoDumpConfig.MAX_DUMP_HEIGHT, video_height);
//        mStartX = video_width / mWidth / 2 * mWidth;
//        mStartY = video_height / mHeight / 2 * mHeight;
//
//        Log.d(TAG, "dumping block start at (" + mStartX + "," + mStartY + ") "
//                + "size " + mWidth + "x" + mHeight);
//
//        int image_size = mWidth * mHeight * VideoDumpConfig.BYTES_PER_PIXEL;
//        mBuffer = ByteBuffer.allocate(image_size);
//
//        int bpp[] = new int[3];
//        GLES20.glGetIntegerv(GLES20.GL_RED_BITS, bpp, 0);
//        GLES20.glGetIntegerv(GLES20.GL_GREEN_BITS, bpp, 1);
//        GLES20.glGetIntegerv(GLES20.GL_BLUE_BITS, bpp, 2);
//        Log.d(TAG, "rgb bits: " + bpp[0] + "-" + bpp[1] + "-" + bpp[2]);
//
//        // Save the properties into a xml file
//        // so the RgbPlayer can understand the output format.
//        Properties prop = new Properties();
//        prop.setProperty("width", Integer.toString(mWidth));
//        prop.setProperty("height", Integer.toString(mHeight));
//        prop.setProperty("startX", Integer.toString(mStartX));
//        prop.setProperty("startY", Integer.toString(mStartY));
//        prop.setProperty("bytesPerPixel",
//                Integer.toString(VideoDumpConfig.BYTES_PER_PIXEL));
//        prop.setProperty("frameRate", Integer.toString(VideoDumpConfig.FRAME_RATE));
//        try {
//            prop.storeToXML(new FileOutputStream(VideoDumpConfig.ROOT_DIR
//                    + VideoDumpConfig.PROPERTY_FILE), "");
//        } catch (java.io.IOException e) {
//            Log.e(TAG, e.getMessage(), e);
//        }
    }

    public static int getLineNumber(Exception e){
        StackTraceElement[] trace =e.getStackTrace();
        if(trace==null||trace.length==0) return -1; //
        return trace[0].getLineNumber();
    }

    public int createTextureObject() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("glGenTextures");

        int texId = textures[0];
        GLES20.glBindTexture(textureTarget, texId);
        checkGlError("glBindTexture " + texId);

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        checkGlError("glTexParameter");

        return texId;
    }

    public void startPreview() {
        try {
            mMediaPlayer.setLooping(false);
            mMediaPlayer.setSurface(new Surface(this.surfaceTexture));
            mMediaPlayer.setDataSource("/sdcard/test.mp4");
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            throw new RuntimeException("Could not open input video!", e);
        }
    }
    /**
     * Called when the surface is created or recreated.
     * Called when the rendering thread starts and whenever the EGL context is lost.
     * A place to put code to create resources that need to be created when the rendering
     * starts, and that need to be recreated when the EGL context is lost e.g. texture.
     * Note that when the EGL context is lost, all OpenGL resources associated with
     * that context will be automatically deleted.
     */
    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated");
            /* Set up shaders and handles to their variables */
        mProgram = createProgram(mVertexShader, mFragmentShader);
        if (mProgram == 0) {
            checkGlError("create Program ");
            return;
        }
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }
        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
        checkGlError("glGetUniformLocation uSTMatrix");
        if (muSTMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }


        // Create our texture. This has to be done each time the surface is created.
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        mTextureID = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        checkGlError("glBindTexture mTextureID");

        // Can't do mipmapping with mediaplayer source
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        // Clamp to edge is the only option
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        checkGlError("glTexParameteri mTextureID");

            /*
             * Create the SurfaceTexture that will feed this textureID,
             * and pass it to the MediaPlayer
             */
        surfaceTexture = new SurfaceTexture(mTextureID);
        surfaceTexture.setOnFrameAvailableListener(this);

        mSurface = new Surface(surfaceTexture);
        mMediaPlayer.setSurface(mSurface);
//        surface.release();

        try {
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "media player prepare failed");
        }

        synchronized(this) {
            updateSurface = false;
        }

    }

    @Override
    synchronized public void onFrameAvailable(SurfaceTexture surface) {
            /* For simplicity, SurfaceTexture calls here when it has new
             * data available.  Call may come in from some random thread,
             * so let's be safe and use synchronize. No OpenGL calls can be done here.
             */
        mAvailabel.trigger();
        mFrameNumber++;
        Log.d(TAG, "availabel frame num :" + mFrameNumber);
        updateSurface = true;
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
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
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }
    public onFrameAvalibaleListening mAvailabel;
    public void setOnFrameAvaliable(VideoPreviewView previewView, onFrameAvalibaleListening onFrameAvalibaleListen) {
        Log.d(TAG, "set onFrameAvaliable");
        mAvailabel = onFrameAvalibaleListen;
        mVideoPreview = new WeakReference<VideoPreviewView>(previewView);
    }


    public interface onFrameAvalibaleListening {
        public void trigger();
    }
}
