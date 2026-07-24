package com.example.screenshare.dualpath.gl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.view.Choreographer;
import android.view.Display;

public class AnimatedGLSurfaceView extends GLSurfaceView implements Choreographer.FrameCallback {
    private static final int MODE_ACTIVITY_TWO = 2;
    private static final float ACTIVITY_ONE_FPS = 60.0f;
    private static final float ACTIVITY_TWO_FPS = 30.0f;

    private final float targetFps;
    private volatile boolean attachedToWindow;
    private volatile boolean paused;
    private volatile boolean frameCallbackPosted;
    private float displayRefreshRate = ACTIVITY_ONE_FPS;
    private float effectiveTargetFps;
    private float frameAccumulator;
    private HandlerThread frameSchedulerThread;
    private Handler frameSchedulerHandler;
    private Choreographer frameSchedulerChoreographer;

    public AnimatedGLSurfaceView(Context context, int mode) {
        super(context);
        targetFps = mode == MODE_ACTIVITY_TWO ? ACTIVITY_TWO_FPS : ACTIVITY_ONE_FPS;
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setPreserveEGLContextOnPause(true);
        setRenderer(new AnimatedRenderer(mode));
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
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
        attachedToWindow = false;
        stopFrameScheduler();
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
        pauseFrameScheduler();
        super.onPause();
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        frameCallbackPosted = false;
        if (!attachedToWindow || paused) {
            return;
        }

        frameAccumulator += effectiveTargetFps;
        if (frameAccumulator >= displayRefreshRate) {
            frameAccumulator -= displayRefreshRate;
            requestRender();
        }
        postFrameCallbackOnScheduler();
    }

    private void updateFrameCadenceFromDisplay() {
        Display display = getDisplay();
        displayRefreshRate = display == null ? ACTIVITY_ONE_FPS : display.getRefreshRate();
        effectiveTargetFps = Math.min(targetFps, displayRefreshRate);
        frameAccumulator = displayRefreshRate - effectiveTargetFps;
    }

    private void startFrameScheduler() {
        if (!attachedToWindow || paused) {
            return;
        }
        if (frameSchedulerThread != null && frameSchedulerHandler != null) {
            frameSchedulerHandler.post(this::postFrameCallbackOnScheduler);
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

    private void pauseFrameScheduler() {
        Handler handler = frameSchedulerHandler;
        if (handler == null) {
            return;
        }
        handler.post(() -> {
            if (frameSchedulerChoreographer != null && frameCallbackPosted) {
                frameSchedulerChoreographer.removeFrameCallback(this);
            }
            frameCallbackPosted = false;
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
