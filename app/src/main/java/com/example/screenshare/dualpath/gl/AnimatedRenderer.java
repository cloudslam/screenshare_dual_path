package com.example.screenshare.dualpath.gl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class AnimatedRenderer implements GLSurfaceView.Renderer {
    private static final String VERTEX_SHADER =
            "attribute vec4 aPosition;" +
            "uniform float uAngle;" +
            "uniform float uScale;" +
            "void main() {" +
            "  float s = sin(uAngle);" +
            "  float c = cos(uAngle);" +
            "  vec2 p = aPosition.xy * uScale;" +
            "  gl_Position = vec4(c * p.x - s * p.y, s * p.x + c * p.y, 0.0, 1.0);" +
            "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
            "uniform vec4 uColor;" +
            "void main() {" +
            "  gl_FragColor = uColor;" +
            "}";

    private static final float[] TRIANGLE = {
            0.0f, 0.65f,
            -0.65f, -0.55f,
            0.65f, -0.55f
    };

    private static final float[] SQUARE = {
            -0.55f, 0.55f,
            -0.55f, -0.55f,
            0.55f, 0.55f,
            0.55f, -0.55f
    };

    private final int mode;
    private FloatBuffer triangleBuffer;
    private FloatBuffer squareBuffer;
    private int program;
    private int positionHandle;
    private int angleHandle;
    private int scaleHandle;
    private int colorHandle;
    private int drawMode;
    private int vertexCount;
    private FloatBuffer activeBuffer;
    private long startTimeNs;

    public AnimatedRenderer(int mode) {
        this.mode = mode;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        triangleBuffer = createBuffer(TRIANGLE);
        squareBuffer = createBuffer(SQUARE);
        program = ShaderUtils.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        angleHandle = GLES20.glGetUniformLocation(program, "uAngle");
        scaleHandle = GLES20.glGetUniformLocation(program, "uScale");
        colorHandle = GLES20.glGetUniformLocation(program, "uColor");
        if (mode == 1) {
            activeBuffer = triangleBuffer;
            drawMode = GLES20.GL_TRIANGLES;
            vertexCount = 3;
            GLES20.glClearColor(0.02f, 0.02f, 0.08f, 1.0f);
        } else {
            activeBuffer = squareBuffer;
            drawMode = GLES20.GL_TRIANGLE_STRIP;
            vertexCount = 4;
            GLES20.glClearColor(0.08f, 0.02f, 0.02f, 1.0f);
        }
        GLES20.glDisable(GLES20.GL_DITHER);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glUseProgram(program);
        GLES20.glEnableVertexAttribArray(positionHandle);
        startTimeNs = System.nanoTime();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        float t = (System.nanoTime() - startTimeNs) / 1_000_000_000.0f;
        if (mode == 1) {
            renderTriangle(t);
        } else {
            renderSquare(t);
        }
    }

    private void renderTriangle(float t) {
        float red = 0.5f + 0.5f * (float) Math.sin(t * 2.0f);
        float green = 0.5f + 0.5f * (float) Math.sin(t * 2.0f + 2.094f);
        float blue = 0.5f + 0.5f * (float) Math.sin(t * 2.0f + 4.188f);
        drawShape(t * 1.8f, 0.9f, red, green, blue);
    }

    private void renderSquare(float t) {
        float pulse = 0.75f + 0.2f * (float) Math.sin(t * 4.0f);
        drawShape(-t * 1.2f, pulse, 0.1f, 0.8f, 1.0f);
    }

    private void drawShape(float angle, float scale, float red, float green, float blue) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, activeBuffer);
        GLES20.glUniform1f(angleHandle, angle);
        GLES20.glUniform1f(scaleHandle, scale);
        GLES20.glUniform4f(colorHandle, red, green, blue, 1.0f);
        GLES20.glDrawArrays(drawMode, 0, vertexCount);
    }

    private FloatBuffer createBuffer(float[] coordinates) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(coordinates.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(coordinates);
        floatBuffer.position(0);
        return floatBuffer;
    }
}
