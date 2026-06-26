package com.example.screenshare.gl;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class AnimatedGLSurfaceView extends GLSurfaceView {
    public AnimatedGLSurfaceView(Context context, int mode) {
        super(context);
        setEGLContextClientVersion(2);
        setRenderer(new AnimatedRenderer(mode));
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }
}
