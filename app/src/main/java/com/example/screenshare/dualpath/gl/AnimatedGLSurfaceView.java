package com.example.screenshare.dualpath.gl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;

public class AnimatedGLSurfaceView extends GLSurfaceView {
    private static final float TARGET_FPS = 60.0f;

    public AnimatedGLSurfaceView(Context context, int mode) {
        super(context);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setPreserveEGLContextOnPause(true);
        setRenderer(new AnimatedRenderer(mode));
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        requestFixedSourceFrameRate(holder);
        super.surfaceCreated(holder);
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
}
