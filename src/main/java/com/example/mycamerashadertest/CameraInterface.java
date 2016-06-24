package com.example.mycamerashadertest;

/*
 * Copyright Liu Yuecheng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.util.Size;

import java.io.IOException;
import java.util.List;

public class CameraInterface {

    private static CameraInterface mCameraInterface;
    public Camera gCamera;
    private Camera.Parameters parameters;
    public static CameraInterface getInstance()
    {
        if(mCameraInterface == null)
            mCameraInterface = new CameraInterface();

        return mCameraInterface;
    }

    public void doOpenCamera(SurfaceTexture gSurfaceTexture)
    {
        gCamera = Camera.open();
        try {
            gCamera.setPreviewTexture(gSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doStartPreview(int width,int height)
    {
        gCamera.startPreview();
    }
    public void doStop()
    {
        Log.e("sss,","111111111111111111111111111111111");
        gCamera.stopPreview();
        try {
            gCamera.setPreviewTexture(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        gCamera.release();
    }

}
