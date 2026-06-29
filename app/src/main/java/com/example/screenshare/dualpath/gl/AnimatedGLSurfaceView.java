package com.example.screenshare.dualpath.gl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.view.Choreographer;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;

public class AnimatedGLSurfaceView extends GLSurfaceView implements Choreographer.FrameCallback {
    private static final float TARGET_FPS = 60.0f;

    private boolean attachedToWindow;
    private boolean paused;
    private boolean frameCallbackPosted;
    private int vsyncsPerRender = 1;
    private int vsyncCounter;

    public AnimatedGLSurfaceView(Context context, int mode) {
        super(context);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setPreserveEGLContextOnPause(true);
        setRenderer(new AnimatedRenderer(mode));
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        requestFixedSourceFrameRate(holder);
        super.surfaceCreated(holder);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attachedToWindow = true;
        updateFrameCadenceFromDisplay();
        startFrameCallback();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopFrameCallback();
        attachedToWindow = false;
        super.onDetachedFromWindow();
    }

    @Override
    public void onResume() {
        paused = false;
        super.onResume();
        updateFrameCadenceFromDisplay();
        startFrameCallback();
    }

    @Override
    public void onPause() {
        paused = true;
        stopFrameCallback();
        super.onPause();
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        frameCallbackPosted = false;
        if (!attachedToWindow || paused) {
            return;
        }

        if (vsyncCounter == 0) {
            requestRender();
        }
        vsyncCounter = (vsyncCounter + 1) % vsyncsPerRender;
        startFrameCallback();
    }

    private void updateFrameCadenceFromDisplay() {
        Display display = getDisplay();
        float refreshRate = display == null ? TARGET_FPS : display.getRefreshRate();
        vsyncsPerRender = Math.max(1, Math.round(refreshRate / TARGET_FPS));
        vsyncCounter = 0;
    }

    private void requestFixedSourceFrameRate(SurfaceHolder holder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || holder == null) {
            return;
        }
        Surface surface = holder.getSurface();
        if (surface == null || !surface.isValid()) {
            return;
        }
        surface.setFrameRate(TARGET_FPS, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
    }

    private void startFrameCallback() {
        if (!attachedToWindow || paused || frameCallbackPosted) {
            return;
        }
        frameCallbackPosted = true;
        Choreographer.getInstance().postFrameCallback(this);
    }

    private void stopFrameCallback() {
        if (!frameCallbackPosted) {
            return;
        }
        Choreographer.getInstance().removeFrameCallback(this);
        frameCallbackPosted = false;
    }
}
