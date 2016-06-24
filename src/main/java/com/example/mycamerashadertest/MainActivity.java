package com.example.tte.jp.mycamerashadertest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.camera2.*;
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new MyGLSurfaceView(this));
    }
    @Override
    public void onPause() {
        super.onPause();
        CameraInterface.getInstance().doStop();
    }

}
