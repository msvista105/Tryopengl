package com.example.prife.tryopengl;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Bundle;
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

import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform2f;

public class NativeBlurActivity extends AppCompatActivity {
    static float RADIUS = 100.0f;
    static float RADIUS_MAX = 255.0f;

    GLSurfaceView mGlSurfaceView;
    GLSurfaceView.Renderer mRenderer;
    static final boolean LOG = false;

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

    String VERTEX_SHADER;
    String FRAGMENT_SHADER;

    private FloatBuffer mVertexBuffer;
    private ShortBuffer mVertexIndexBuffer;
    private FloatBuffer mUvTexVertexBuffer;

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

    private int mWidth;
    private int mHeight;
    private IntBuffer mTexNames = IntBuffer.allocate(1);
    private IntBuffer mFreamBufferObjects = IntBuffer.allocate(2);
    private IntBuffer mTextures = IntBuffer.allocate(2);
    private Resources mResources;

    void init () {
        VERTEX_SHADER = Utils.readTextFileFromResource(this, R.raw.blur_vert);
        FRAGMENT_SHADER = Utils.readTextFileFromResource(this, R.raw.blur_frag);

        mResources = getResources();
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
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_blur);
        final TextView mRadiusTextView = (TextView)findViewById(R.id.native_radius_textview);
        mRadiusTextView.setText(nativeHelloString("java args"));

        mGlSurfaceView = (GLSurfaceView) findViewById(R.id.native_GLSurfaceView);
        mGlSurfaceView.setEGLContextClientVersion(2);

        final SeekBar mRadiusSeeker = (SeekBar)findViewById(R.id.native_radius_seeker);
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

        init();

        mRenderer = new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {

            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
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

                glGenTextures(1, mTextures);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(0));
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, mWidth, mHeight,
                        0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5, null);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            }

            @Override
            public void onDrawFrame(GL10 gl) {
 /*
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
*/
                nativeBlurTexture((int)RADIUS, mTexNames.get(0), mWidth, mHeight, mTextures.get(0));

                GLES20.glUseProgram(mProgram);

                GLES20.glEnableVertexAttribArray(mPositionHandle);
                GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0,
                        mVertexBuffer);

                GLES20.glEnableVertexAttribArray(mTexCoordHandle);
                GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0,
                        mUvTexVertexBuffer);

                glUniform1i(mTexSamplerHandle, 0);
                glUniform2f(mScaleUniformHandle, 0, 0);

                glBindFramebuffer(GL_FRAMEBUFFER, 0);
                GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMVPMatrix, 0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(0));
                glDrawElements(GLES20.GL_TRIANGLES, VERTEX_INDEX.length,
                        GLES20.GL_UNSIGNED_SHORT, mVertexIndexBuffer);
            }
        };
        mGlSurfaceView.setRenderer(mRenderer);
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

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String nativeHelloString(String str);


    public static native int nativeBlurTexture(int level, int inId, int inWidth, int inHeight, int outId);

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
}
