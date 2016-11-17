package com.example.prife.tryopengl;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.*;

public class MainActivity extends AppCompatActivity {
    static float RADIUS = 9.0f;
    static float RADIUS_MAX = 20.0f;

    private boolean mRendererSet;
    private GLSurfaceView mGlSurfaceView;
    //private MyGLRenderer mRenderer;
    private GLSurfaceView.Renderer mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGlSurfaceView = (GLSurfaceView) findViewById(R.id.mGLSurfaceView);

        mGlSurfaceView.setEGLContextClientVersion(2);
        //mRenderer = new MyGLRenderer(getResources());
        //mRenderer = new Test7Renderer(this);
        mRenderer = new MyRenderer(getResources());
        mGlSurfaceView.setRenderer(mRenderer);
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mRendererSet = true;
        final TextView mRadiusTextView = (TextView)findViewById(R.id.radius_textview);
        final SeekBar mRadiusSeeker = (SeekBar)findViewById(R.id.radius_seeker);

        if (mRadiusTextView == null || mRadiusSeeker == null)
            return;

        mRadiusSeeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                RADIUS = progress/100.0f * RADIUS_MAX;
                mRadiusTextView.setText(String.format("%.1f", RADIUS));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRendererSet) {
            mGlSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRendererSet) {
            mGlSurfaceView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mRenderer.destroy();
    }

    class MyRenderer implements GLSurfaceView.Renderer {
        static final boolean LOG = false;

//        private final String VERTEX_SHADER = Utils.readTextFileFromResource(MainActivity.this, R.raw.blur_vert);
//        private final String FRAGMENT_SHADER = Utils.readTextFileFromResource(MainActivity.this, R.raw.blur_frag);

        private final String VERTEX_SHADER = Utils.readTextFileFromResource(MainActivity.this, R.raw.blur_vert);
        private final String FRAGMENT_SHADER = Utils.readTextFileFromResource(MainActivity.this, R.raw.blur_frag);
        private final float[] VERTEX = {   // in counterclockwise order:
                1, 1, 0,   // top right
                -1, 1, 0,  // top left
                -1, -1, 0, // bottom left
                1, -1, 0,  // bottom right
        };
        private final short[] VERTEX_INDEX = { 0, 1, 2, 2, 0, 3 };
        private final float[] UV_TEX_VERTEX = {   // in clockwise order:
                1, 0,  // bottom right
                0, 0,  // bottom left
                0, 1,  // top left
                1, 1,  // top right
        };

        private final FloatBuffer mVertexBuffer;
        private final ShortBuffer mVertexIndexBuffer;
        private final FloatBuffer mUvTexVertexBuffer;

        private final float[] mProjectionMatrix = new float[16];
        private final float[] mCameraMatrix = new float[16];
        private final float[] mMVPMatrix = new float[16];
        private final float[] mMVPIdentiyMatrix = new float[16];


        private int mProgram;
        private int mPositionHandle;
        private int mMatrixHandle;
        private int mTexCoordHandle;
        private int mTexSamplerHandle;
        private int mScaleUniformHandle;

        private final Resources mResources;

        private int mWidth;
        private int mHeight;
        private IntBuffer mTexNames = IntBuffer.allocate(1);
        private IntBuffer mFreamBufferObjects = IntBuffer.allocate(2);
        private IntBuffer mTextures = IntBuffer.allocate(2);

        MyRenderer(Resources resources) {
            mResources = resources;
            mVertexBuffer = ByteBuffer.allocateDirect(VERTEX.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(VERTEX);
            mVertexBuffer.position(0);

            mVertexIndexBuffer = ByteBuffer.allocateDirect(VERTEX_INDEX.length * 2)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer()
                    .put(VERTEX_INDEX);
            mVertexIndexBuffer.position(0);

            mUvTexVertexBuffer = ByteBuffer.allocateDirect(UV_TEX_VERTEX.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(UV_TEX_VERTEX);
            mUvTexVertexBuffer.position(0);
        }

        @Override
        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        }

        @Override
        public void onSurfaceChanged(GL10 unused, int width, int height) {
            mWidth = width;
            mHeight = height;
            GLES20.glViewport(0, 0, width, height);

            Log.v("shader", String.format("width=%d, h=%d", width, height));

            mProgram = GLES20.glCreateProgram();
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);
            GLES20.glLinkProgram(mProgram);

            if (LOG) {
                final int[] status = new int[1];
                GLES20.glGetShaderiv(mProgram, GLES20.GL_LINK_STATUS, status, 0);
                Log.v("shader", "Results of linking program:[" + status[0] + "]\n:"
                        + GLES20.glGetProgramInfoLog(mProgram));
            }

            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
            mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate");
            mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_ProjView");
            mTexSamplerHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture");
            mScaleUniformHandle = glGetUniformLocation(mProgram, "u_Scale");

            GLES20.glGenTextures(1, mTexNames);

            Bitmap bitmap = BitmapFactory.decodeResource(mResources, R.drawable.lena);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexNames.get(0));
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();

            float ratio = (float) height / width;
            Matrix.frustumM(mProjectionMatrix, 0, -1, 1, -ratio, ratio, 3, 7);
            Matrix.setLookAtM(mCameraMatrix, 0, 0, 0, 3, 0, 0, 0, 0, 1, 0);
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mCameraMatrix, 0);

            Matrix.setIdentityM(mMVPIdentiyMatrix, 0);
        }

        @Override
        public void onDrawFrame(GL10 unused) {
            glGenFramebuffers(1, mFreamBufferObjects);
            glGenTextures(2, mTextures);

            for (int i = 0; i < 2; i++) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(i));
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, mWidth, mHeight,
                        0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5, null);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            }

            glBindFramebuffer(GL_FRAMEBUFFER, mFreamBufferObjects.get(0)); // Use FBO
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(0));
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mTextures.get(0), 0);
            int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if(status != GLES20.GL_FRAMEBUFFER_COMPLETE)
            {
                Log.i("shader", "glCheckFramebufferStatus failed!");
                return;
            }

            GLES20.glActiveTexture(GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexNames.get(0));

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
//            GLES20.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);
            GLES20.glUseProgram(mProgram);

            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0,
                    mVertexBuffer);

            GLES20.glEnableVertexAttribArray(mTexCoordHandle);
            GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0,
                    mUvTexVertexBuffer);

            glUniform1i(mTexSamplerHandle, 0);
            glUniform2f(mScaleUniformHandle, 0, 1.0f/mHeight*RADIUS);
            GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMVPIdentiyMatrix, 0);
            GLES20.glUniform1i(mTexSamplerHandle, 0);

            glDrawElements(GLES20.GL_TRIANGLES, VERTEX_INDEX.length,
                    GLES20.GL_UNSIGNED_SHORT, mVertexIndexBuffer);

////////////////////////////////////////////////////////////////////////////////////////
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mTextures.get(1), 0);
            GLES20.glActiveTexture(GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(0));
            glUniform2f(mScaleUniformHandle, 1.0f/mWidth*RADIUS, 0);
            glDrawElements(GLES20.GL_TRIANGLES, VERTEX_INDEX.length,
                    GLES20.GL_UNSIGNED_SHORT, mVertexIndexBuffer);
////////////////////////////////////////////////////////////////////////////////////////
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glUniform2f(mScaleUniformHandle, 0, 0);
            GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(1));
            glDrawElements(GLES20.GL_TRIANGLES, VERTEX_INDEX.length,
                    GLES20.GL_UNSIGNED_SHORT, mVertexIndexBuffer);

            GLES20.glDisableVertexAttribArray(mPositionHandle);
            GLES20.glDisableVertexAttribArray(mTexCoordHandle);

            GLES20.glDeleteFramebuffers(1, mFreamBufferObjects);
            GLES20.glDeleteTextures(2, mTextures);

            //Utils.sendImage(mWidth, mHeight);
        }

        void destroy() {

        }

        int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);

            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            Log.v("shader", "Results of compiling source:[" + compileStatus[0] + "]\n" + shaderCode + "\n:"
                    + GLES20.glGetShaderInfoLog(shader));
            return shader;
        }
    }


    /**
     * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
     * renderers -- the static class GLES20 is used instead.
     */
    public class Test7Renderer implements GLSurfaceView.Renderer
    {
        /** Used for debug logs. */
        private static final String TAG = "Test7Renderer";

        private final Context mActivityContext;

        /**
         * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
         * of being located at the center of the universe) to world space.
         */
        private float[] mModelMatrix = new float[16];

        /**
         * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
         * it positions things relative to our eye.
         */
        private float[] mViewMatrix = new float[16];

        /** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
        private float[] mProjectionMatrix = new float[16];

        /** Allocate storage for the final combined matrix. This will be passed into the shader program. */
        private float[] mMVPMatrix = new float[16];

        /** Store our model data in a float buffer. */
        private final FloatBuffer mCubePositions;
        private final FloatBuffer mCubeColors;
        private final FloatBuffer mCubeTextureCoordinates;

        /** This will be used to pass in the transformation matrix. */
        private int mMVPMatrixHandle;

        /** This will be used to pass in the modelview matrix. */
        private int mMVMatrixHandle;

        /** This will be used to pass in the texture. */
        private int mTextureUniformHandle;

        /** This will be used to pass in model position information. */
        private int mPositionHandle;

        /** This will be used to pass in model color information. */
        private int mColorHandle;

        /** This will be used to pass in model texture coordinate information. */
        private int mTextureCoordinateHandle;

        /** How many bytes per float. */
        private final int mBytesPerFloat = 4;

        /** Size of the position data in elements. */
        private final int mPositionDataSize = 3;

        /** Size of the color data in elements. */
        private final int mColorDataSize = 4;

        /** Size of the texture coordinate data in elements. */
        private final int mTextureCoordinateDataSize = 2;

        /** This is a handle to our cube shading program. */
        private int mProgramHandle;

        /** This is a handle to our texture data. */
        private int mTextureDataHandle;

        /**
         * Initialize the model data.
         */
        public Test7Renderer(final Context activityContext)
        {
            mActivityContext = activityContext;

            // Define points for a cube.

            // X, Y, Z
            final float[] cubePositionData =
                    {
                            // In OpenGL counter-clockwise winding is default. This means that when we look at a triangle,
                            // if the points are counter-clockwise we are looking at the "front". If not we are looking at
                            // the back. OpenGL has an optimization where all back-facing triangles are culled, since they
                            // usually represent the backside of an object and aren't visible anyways.

                            // Front face
                            -1.0f, 1.0f, 1.0f,
                            -1.0f, -1.0f, 1.0f,
                            1.0f, 1.0f, 1.0f,
                            -1.0f, -1.0f, 1.0f,
                            1.0f, -1.0f, 1.0f,
                            1.0f, 1.0f, 1.0f,

                            // Right face
                            1.0f, 1.0f, 1.0f,
                            1.0f, -1.0f, 1.0f,
                            1.0f, 1.0f, -1.0f,
                            1.0f, -1.0f, 1.0f,
                            1.0f, -1.0f, -1.0f,
                            1.0f, 1.0f, -1.0f,

                            // Back face
                            1.0f, 1.0f, -1.0f,
                            1.0f, -1.0f, -1.0f,
                            -1.0f, 1.0f, -1.0f,
                            1.0f, -1.0f, -1.0f,
                            -1.0f, -1.0f, -1.0f,
                            -1.0f, 1.0f, -1.0f,

                            // Left face
                            -1.0f, 1.0f, -1.0f,
                            -1.0f, -1.0f, -1.0f,
                            -1.0f, 1.0f, 1.0f,
                            -1.0f, -1.0f, -1.0f,
                            -1.0f, -1.0f, 1.0f,
                            -1.0f, 1.0f, 1.0f,

                            // Top face
                            -1.0f, 1.0f, -1.0f,
                            -1.0f, 1.0f, 1.0f,
                            1.0f, 1.0f, -1.0f,
                            -1.0f, 1.0f, 1.0f,
                            1.0f, 1.0f, 1.0f,
                            1.0f, 1.0f, -1.0f,

                            // Bottom face
                            1.0f, -1.0f, -1.0f,
                            1.0f, -1.0f, 1.0f,
                            -1.0f, -1.0f, -1.0f,
                            1.0f, -1.0f, 1.0f,
                            -1.0f, -1.0f, 1.0f,
                            -1.0f, -1.0f, -1.0f,
                    };

            // R, G, B, A
            final float[] cubeColorData =
                    {
                            // Front face (red)
                            1.0f, 0.0f, 0.0f, 1.0f,
                            1.0f, 0.0f, 0.0f, 1.0f,
                            1.0f, 0.0f, 0.0f, 1.0f,
                            1.0f, 0.0f, 0.0f, 1.0f,
                            1.0f, 0.0f, 0.0f, 1.0f,
                            1.0f, 0.0f, 0.0f, 1.0f,

                            // Right face (green)
                            0.0f, 1.0f, 0.0f, 1.0f,
                            0.0f, 1.0f, 0.0f, 1.0f,
                            0.0f, 1.0f, 0.0f, 1.0f,
                            0.0f, 1.0f, 0.0f, 1.0f,
                            0.0f, 1.0f, 0.0f, 1.0f,
                            0.0f, 1.0f, 0.0f, 1.0f,

                            // Back face (blue)
                            0.0f, 0.0f, 1.0f, 1.0f,
                            0.0f, 0.0f, 1.0f, 1.0f,
                            0.0f, 0.0f, 1.0f, 1.0f,
                            0.0f, 0.0f, 1.0f, 1.0f,
                            0.0f, 0.0f, 1.0f, 1.0f,
                            0.0f, 0.0f, 1.0f, 1.0f,

                            // Left face (yellow)
                            1.0f, 1.0f, 0.0f, 1.0f,
                            1.0f, 1.0f, 0.0f, 1.0f,
                            1.0f, 1.0f, 0.0f, 1.0f,
                            1.0f, 1.0f, 0.0f, 1.0f,
                            1.0f, 1.0f, 0.0f, 1.0f,
                            1.0f, 1.0f, 0.0f, 1.0f,

                            // Top face (cyan)
                            0.0f, 1.0f, 1.0f, 1.0f,
                            0.0f, 1.0f, 1.0f, 1.0f,
                            0.0f, 1.0f, 1.0f, 1.0f,
                            0.0f, 1.0f, 1.0f, 1.0f,
                            0.0f, 1.0f, 1.0f, 1.0f,
                            0.0f, 1.0f, 1.0f, 1.0f,

                            // Bottom face (magenta)
                            1.0f, 0.0f, 1.0f, 1.0f,
                            1.0f, 0.0f, 1.0f, 1.0f,
                            1.0f, 0.0f, 1.0f, 1.0f,
                            1.0f, 0.0f, 1.0f, 1.0f,
                            1.0f, 0.0f, 1.0f, 1.0f,
                            1.0f, 0.0f, 1.0f, 1.0f
                    };

            // S, T (or X, Y)
            // Texture coordinate data.
            // Because images have a Y axis pointing downward (values increase as you move down the image) while
            // OpenGL has a Y axis pointing upward, we adjust for that here by flipping the Y axis.
            // What's more is that the texture coordinates are the same for every face.
            final float[] cubeTextureCoordinateData =
                    {
                            // Front face
                            0.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 1.0f,
                            1.0f, 0.0f,

                            // Right face
                            0.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 1.0f,
                            1.0f, 0.0f,

                            // Back face
                            0.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 1.0f,
                            1.0f, 0.0f,

                            // Left face
                            0.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 1.0f,
                            1.0f, 0.0f,

                            // Top face
                            0.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 1.0f,
                            1.0f, 0.0f,

                            // Bottom face
                            0.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 1.0f,
                            1.0f, 0.0f
                    };

            // Initialize the buffers.
            mCubePositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytesPerFloat)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mCubePositions.put(cubePositionData).position(0);

            mCubeColors = ByteBuffer.allocateDirect(cubeColorData.length * mBytesPerFloat)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mCubeColors.put(cubeColorData).position(0);

            mCubeTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinateData.length * mBytesPerFloat)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mCubeTextureCoordinates.put(cubeTextureCoordinateData).position(0);
        }

        protected String getVertexShader(int shader)
        {
            return ToolsUtil.readTextFileFromRawResource(mActivityContext, shader);
        }

        protected String getFragmentShader(int shader)
        {
            return ToolsUtil.readTextFileFromRawResource(mActivityContext, shader);
        }

        @Override
        public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
        {
            // Set the background clear color to black.
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

            // Use culling to remove back faces.
            GLES20.glEnable(GLES20.GL_CULL_FACE);

            // Enable depth testing
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);

            // The below glEnable() call is a holdover from OpenGL ES 1, and is not needed in OpenGL ES 2.
            // Enable texture mapping
            GLES20.glEnable(GLES20.GL_TEXTURE_2D);

            // Position the eye in front of the origin.
            final float eyeX = 0.0f;
            final float eyeY = 0.0f;
            final float eyeZ = -0.5f;

            // We are looking toward the distance
            final float lookX = 0.0f;
            final float lookY = 0.0f;
            final float lookZ = -5.0f;

            // Set our up vector. This is where our head would be pointing were we holding the camera.
            final float upX = 0.0f;
            final float upY = 1.0f;
            final float upZ = 0.0f;

            // Set the view matrix. This matrix can be said to represent the camera position.
            // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
            // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
            Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

            final String vertexShader = getVertexShader(R.raw.fbo_vert);
            final String fragmentShader = getFragmentShader(R.raw.fbo_frag);

            final int vertexShaderHandle = ToolsUtil.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
            final int fragmentShaderHandle = ToolsUtil.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

            mProgramHandle = ToolsUtil.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                    new String[] {"a_Position", "a_Color", "a_TexCoordinate"});

            // Load the texture
            mTextureDataHandle = ToolsUtil.loadTexture(mActivityContext, R.drawable.demo);
        }

        @Override
        public void onSurfaceChanged(GL10 glUnused, int width, int height)
        {
            // Set the OpenGL viewport to the same size as the surface.
            GLES20.glViewport(0, 0, width, height);

            // Create a new perspective projection matrix. The height will stay the same
            // while the width will vary as per aspect ratio.
            final float ratio = (float) width / height;
            final float left = -ratio;
            final float right = ratio;
            final float bottom = -1.0f;
            final float top = 1.0f;
            final float near = 1.0f;
            final float far = 10.0f;

            Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
        }

        @Override
        public void onDrawFrame(GL10 glUnused)
        {
            IntBuffer framebuffer = IntBuffer.allocate(1);
            IntBuffer depthRenderbuffer = IntBuffer.allocate(1);
            IntBuffer texture = IntBuffer.allocate(1);
            int texWidth = 480, texHeight = 480;
            IntBuffer maxRenderbufferSize = IntBuffer.allocate(1);
            GLES20.glGetIntegerv(GLES20.GL_MAX_RENDERBUFFER_SIZE, maxRenderbufferSize);
            // check if GL_MAX_RENDERBUFFER_SIZE is >= texWidth and texHeight
            if((maxRenderbufferSize.get(0) <= texWidth) ||
                    (maxRenderbufferSize.get(0) <= texHeight))
            {
                // cannot use framebuffer objects as we need to create
                // a depth buffer as a renderbuffer object
                // return with appropriate error
            }
            // generate the framebuffer, renderbuffer, and texture object names
            GLES20.glGenFramebuffers(1, framebuffer);
//            GLES20.glGenRenderbuffers(1, depthRenderbuffer);
            GLES20.glGenTextures(1, texture);
            // bind texture and load the texture mip-level 0
            // texels are RGB565
            // no texels need to be specified as we are going to draw into
            // the texture
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.get(0));
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, texWidth, texHeight,
                    0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5, null);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            // bind renderbuffer and create a 16-bit depth buffer
            // width and height of renderbuffer = width and height of
            // the texture
//            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRenderbuffer.get(0));
//            GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
//                    texWidth, texHeight);
            // bind the framebuffer
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer.get(0));
            // specify texture as color attachment
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, texture.get(0), 0);
            // specify depth_renderbufer as depth attachment
//            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
//                    GLES20.GL_RENDERBUFFER, depthRenderbuffer.get(0));
            // check for framebuffer complete
            int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if(status == GLES20.GL_FRAMEBUFFER_COMPLETE)
            {
                // render to texture using FBO
                GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                // Do a complete rotation every 10 seconds.
//                long time = SystemClock.uptimeMillis() % 10000L;
//                float angleInDegrees = (360.0f / 10000.0f) * (2 * (int) time);
//
//                GLES20.glUseProgram(mProgramHandle);
//
//                // Set program handles for cube drawing.
//                mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
//                mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
//                mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
//                mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
//                mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
//                mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
//
//                // Set the active texture unit to texture unit 0.
//                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//
//                // Bind the texture to this unit.
//                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);
//
//                // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
//                GLES20.glUniform1i(mTextureUniformHandle, 0);
//
//                Matrix.setIdentityM(mModelMatrix, 0);
//                Matrix.translateM(mModelMatrix, 0, 0.0f, -1.0f, -5.0f);
//                Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 1.0f, 0.0f);
//                drawCube();

                // render to window system provided framebuffer
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                // Do a complete rotation every 10 seconds.
                long time = SystemClock.uptimeMillis() % 10000L;
                float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

                GLES20.glUseProgram(mProgramHandle);

                // Set program handles for cube drawing.
                mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
                mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
                mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
                mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
                mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
                mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");

                // Set the active texture unit to texture unit 0.
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

                // Bind the texture to this unit.
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.get(0)/*mTextureDataHandle*/);

                // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
                GLES20.glUniform1i(mTextureUniformHandle, 0);

                Matrix.setIdentityM(mModelMatrix, 0);
                Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -5.0f);
                Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 1.0f, 0.0f);
                drawCube();
                //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            }

            // cleanup
//            GLES20.glDeleteRenderbuffers(1, depthRenderbuffer);
            GLES20.glDeleteFramebuffers(1, framebuffer);
            GLES20.glDeleteTextures(1, texture);
        }

        /**
         * Draws a cube.
         */
        private void drawCube()
        {
            // Pass in the position information
            mCubePositions.position(0);
            GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                    0, mCubePositions);

            GLES20.glEnableVertexAttribArray(mPositionHandle);

            // Pass in the color information
            mCubeColors.position(0);
            GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                    0, mCubeColors);
            GLES20.glEnableVertexAttribArray(mColorHandle);

            // Pass in the texture coordinate information
            mCubeTextureCoordinates.position(0);
            GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
                    0, mCubeTextureCoordinates);

            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

            // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
            // (which currently contains model * view).
            Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

            // Pass in the modelview matrix.
            GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

            // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
            // (which now contains model * view * projection).
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

            // Pass in the combined matrix.
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

            // Draw the cube.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        }

        void destroy() {

        }
    }
}
