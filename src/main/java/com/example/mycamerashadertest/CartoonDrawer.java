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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

public class CartoonDrawer {
    private final String vertexShaderCode =
            "attribute vec4 vPosition;\n" +
                    "attribute vec2 inputTextureCoordinate;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "varying vec2 offset[9];\n"+
                    "uniform float dx;\n"+
                    "uniform float dy;\n"+
                    "void main()\n" +
                    "{\n"+
                    "textureCoordinate = inputTextureCoordinate;\n" +
                    "gl_Position =  vPosition;\n"+
                    "offset[0] = vec2(textureCoordinate.x-1.0*dx,textureCoordinate.y-1.0*dy);\n"+
                    "offset[1] = vec2(textureCoordinate.x,textureCoordinate.y-1.0*dy);\n"+
                    "offset[2] = vec2(textureCoordinate.x+1.0*dx,textureCoordinate.y-1.0*dy);\n"+
                    "offset[3] = vec2(textureCoordinate.x-1.0*dx,textureCoordinate.y);\n"+
                    "offset[4] = vec2(textureCoordinate.x,textureCoordinate.y);\n"+
                    "offset[5] = vec2(textureCoordinate.x+1.0*dx,textureCoordinate.y);\n"+
                    "offset[6] = vec2(textureCoordinate.x-1.0*dx,textureCoordinate.y+1.0*dy);\n"+
                    "offset[7] = vec2(textureCoordinate.x,textureCoordinate.y+1.0*dy);\n"+
                    "offset[8] = vec2(textureCoordinate.x+1.0*dx,textureCoordinate.y+1.0*dy);\n"+
                    "}";

    private final String fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n"+
                    "precision mediump float;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "uniform samplerExternalOES s_texture;\n" +

                    "varying vec2 offset[9];\n"+
                    "void main() {\n"+


                    "vec3 col = vec3(0.0);\n"+
                    "vec3 colX = vec3(0.0);\n"+
                    "vec3 colY = vec3(0.0);\n"+

                    "colX =texture2D(s_texture,offset[5]).rgb - texture2D(s_texture,offset[3]).rgb;\n "+
                    "colX +=texture2D(s_texture,offset[2]).rgb - texture2D(s_texture,offset[0]).rgb;\n "+
                    "colX +=texture2D(s_texture,offset[8]).rgb - texture2D(s_texture,offset[6]).rgb;\n "+
                    "colY =texture2D(s_texture,offset[6]).rgb - texture2D(s_texture,offset[0]).rgb;\n "+
                    "colY +=texture2D(s_texture,offset[7]).rgb - texture2D(s_texture,offset[1]).rgb;\n "+
                    "colY +=texture2D(s_texture,offset[8]).rgb - texture2D(s_texture,offset[2]).rgb;\n "+
                    "col = abs(colX) + abs(colY);\n"+
                    " col = vec3(dot(col, vec3(0.299, 0.587, 0.114)));\n"+
                    " col = vec3(1.0) - col;\n"+

//                    " vec3 col = texture2D(s_texture,textureCoordinate).rgb;\n"+
                    "gl_FragColor = vec4(col, 1.0);\n"+

                    "}";


    private FloatBuffer vertexBuffer, textureVerticesBuffer;
    private ShortBuffer drawListBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mTextureCoordHandle;

    private int guTexture;
    private int guImgWidth;
    private int guImgHeight;

    private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX = 2;

    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    static float squareCoords[] = {
            -1.0f,  1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f,
            1.0f,  1.0f,
    };

    static float textureVertices[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
    };

    private int texture;
    int[] fbotexture = new int[1];

    WaveDrawer mWaveDrawer;
    public CartoonDrawer(int texture)
    {
        this.texture = texture;
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(textureVertices.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textureVerticesBuffer = bb2.asFloatBuffer();
        textureVerticesBuffer.put(textureVertices);
        textureVerticesBuffer.position(0);

        int vertexShader    = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader  = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables

        guTexture = GLES20.glGetUniformLocation(mProgram,"s_texture");
        guImgWidth = GLES20.glGetUniformLocation(mProgram,"dx");
        guImgHeight = GLES20.glGetUniformLocation(mProgram,"dy");

        GLES20.glUseProgram(mProgram);


    }

    int []fbo = new int[1];
    public void setImageSize(int width,int height)
    {
        GLES20.glUniform1f(guImgWidth, 1.0f/(float)width);
        GLES20.glUniform1f(guImgHeight,1.0f/(float)height);

        GLES20.glGenTextures(1, fbotexture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fbotexture[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);


        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,width,height,0, GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,null);

        GLES20.glGenFramebuffers(1,fbo,0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,fbo[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,GLES20.GL_TEXTURE_2D,fbotexture[0],0);

        mWaveDrawer = new WaveDrawer(fbotexture[0]);
        mWaveDrawer.setImageSize(width, height);
    }

    public void draw(float[] mtx)
    {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,fbo[0]);
        GLES20.glUseProgram(mProgram);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(guTexture,0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the <insert shape here> coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);


        GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, textureVerticesBuffer);

//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);

        mWaveDrawer.draw(mtx);
    }

    private  int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        String log = GLES20.glGetShaderInfoLog(shader);
        Log.d("DirectDraw",log);

        return shader;
    }

}
