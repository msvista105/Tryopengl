package com.example.prife.tryopengl;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class NativeBlurActivity extends AppCompatActivity {
    GLSurfaceView mGlSurfaceView;
    GLSurfaceView.Renderer mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_blur);

        final TextView mRadiusTextView = (TextView)findViewById(R.id.native_radius_textview);
        mRadiusTextView.setText(nativeHelloString("java args"));

        mGlSurfaceView = (GLSurfaceView) findViewById(R.id.native_GLSurfaceView);
        mGlSurfaceView.setEGLContextClientVersion(2);
        mRenderer = new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {

            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {

            }

            @Override
            public void onDrawFrame(GL10 gl) {

            }
        };
        mGlSurfaceView.setRenderer(mRenderer);
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String nativeHelloString(String str);

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
}
