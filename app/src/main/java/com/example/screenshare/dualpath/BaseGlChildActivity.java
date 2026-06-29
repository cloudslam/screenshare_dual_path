package com.example.screenshare.dualpath;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.example.screenshare.dualpath.gl.AnimatedGLSurfaceView;

public abstract class BaseGlChildActivity extends Activity {
    private AnimatedGLSurfaceView glSurfaceView;
    private int projectionDisplayId = ProjectionControl.DISPLAY_ID_NONE;
    private String projectionActivityClass;
    private final BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ProjectionControl.ACTION_STOP_PROJECTION.equals(intent.getAction())
                    && shouldFinishForStopIntent(intent)) {
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        projectionDisplayId = getIntent().getIntExtra(ProjectionControl.EXTRA_DISPLAY_ID,
                ProjectionControl.DISPLAY_ID_NONE);
        projectionActivityClass = getIntent().getStringExtra(ProjectionControl.EXTRA_ACTIVITY_CLASS);
        if (projectionActivityClass == null) {
            projectionActivityClass = getClass().getName();
        }
        glSurfaceView = new AnimatedGLSurfaceView(this, getAnimationMode());
        setContentView(glSurfaceView);
        IntentFilter filter = new IntentFilter(ProjectionControl.ACTION_STOP_PROJECTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stopReceiver, filter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        glSurfaceView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(stopReceiver);
        super.onDestroy();
    }

    private boolean shouldFinishForStopIntent(Intent intent) {
        int targetDisplayId = intent.getIntExtra(ProjectionControl.EXTRA_DISPLAY_ID,
                ProjectionControl.DISPLAY_ID_NONE);
        String targetActivityClass = intent.getStringExtra(ProjectionControl.EXTRA_ACTIVITY_CLASS);
        boolean displayMatches = targetDisplayId == ProjectionControl.DISPLAY_ID_NONE
                || targetDisplayId == projectionDisplayId;
        boolean activityMatches = targetActivityClass == null
                || targetActivityClass.equals(projectionActivityClass);
        return displayMatches && activityMatches;
    }

    protected abstract int getAnimationMode();
}
