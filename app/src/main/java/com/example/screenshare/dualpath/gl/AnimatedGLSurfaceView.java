package com.example.screenshare.dualpath.gl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.view.Choreographer;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;

public class AnimatedGLSurfaceView extends GLSurfaceView implements Choreographer.FrameCallback {
    private static final float TARGET_FPS = 60.0f;

    private volatile boolean attachedToWindow;
    private volatile boolean paused;
    private volatile boolean frameCallbackPosted;
    private int vsyncsPerRender = 1;
    private int vsyncCounter;
    private HandlerThread frameSchedulerThread;
    private Handler frameSchedulerHandler;
    private Choreographer frameSchedulerChoreographer;

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
        startFrameScheduler();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopFrameScheduler();
        attachedToWindow = false;
        super.onDetachedFromWindow();
    }

    @Override
    public void onResume() {
        paused = false;
        super.onResume();
        updateFrameCadenceFromDisplay();
        startFrameScheduler();
    }

    @Override
    public void onPause() {
        paused = true;
        stopFrameScheduler();
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
        postFrameCallbackOnScheduler();
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

    private void startFrameScheduler() {
        if (!attachedToWindow || paused || frameSchedulerThread != null) {
            return;
        }
        frameSchedulerThread = new HandlerThread("ScreenShareGlFrameScheduler",
                Process.THREAD_PRIORITY_DISPLAY);
        frameSchedulerThread.start();
        frameSchedulerHandler = new Handler(frameSchedulerThread.getLooper());
        frameSchedulerHandler.post(() -> {
            frameSchedulerChoreographer = Choreographer.getInstance();
            postFrameCallbackOnScheduler();
        });
    }

    private void stopFrameScheduler() {
        Handler handler = frameSchedulerHandler;
        HandlerThread thread = frameSchedulerThread;
        if (handler != null) {
            handler.post(() -> {
                if (frameSchedulerChoreographer != null && frameCallbackPosted) {
                    frameSchedulerChoreographer.removeFrameCallback(this);
                    frameCallbackPosted = false;
                }
                frameSchedulerChoreographer = null;
            });
        }
        frameSchedulerHandler = null;
        frameSchedulerThread = null;
        if (thread != null) {
            thread.quitSafely();
        }
    }

    private void postFrameCallbackOnScheduler() {
        if (!attachedToWindow || paused || frameCallbackPosted || frameSchedulerChoreographer == null) {
            return;
        }
        frameCallbackPosted = true;
        frameSchedulerChoreographer.postFrameCallback(this);
    }
}
