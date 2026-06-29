package com.example.screenshare.dualpath.gl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.Choreographer;

public class AnimatedGLSurfaceView extends GLSurfaceView implements Choreographer.FrameCallback {
    private static final long MIN_FRAME_INTERVAL_NS = 15_500_000L;

    private boolean attachedToWindow;
    private boolean paused;
    private boolean frameCallbackPosted;
    private long lastRenderFrameTimeNs;

    public AnimatedGLSurfaceView(Context context, int mode) {
        super(context);
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

        if (lastRenderFrameTimeNs == 0L
                || frameTimeNanos - lastRenderFrameTimeNs >= MIN_FRAME_INTERVAL_NS) {
            lastRenderFrameTimeNs = frameTimeNanos;
            requestRender();
        }
        startFrameCallback();
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
