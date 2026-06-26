package com.example.screenshare.dualpath;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {
    private EditText displayIdEditText;
    private Spinner activitySpinner;
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int padding = dp(24);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("ScreenShare Dual Path");
        title.setTextSize(24);
        root.addView(title);

        displayIdEditText = new EditText(this);
        displayIdEditText.setHint("输入 display_id");
        displayIdEditText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        root.addView(displayIdEditText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        activitySpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"OpenGL 子 Activity 1", "OpenGL 子 Activity 2"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activitySpinner.setAdapter(adapter);
        root.addView(activitySpinner, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Button startButton = new Button(this);
        startButton.setText("启动投屏");
        startButton.setOnClickListener(v -> startProjection());
        root.addView(startButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Button cancelButton = new Button(this);
        cancelButton.setText("取消投屏");
        cancelButton.setOnClickListener(v -> cancelProjection());
        root.addView(cancelButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        statusTextView = new TextView(this);
        statusTextView.setText("请输入 display_id 后启动投屏");
        root.addView(statusTextView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private void startProjection() {
        String displayIdValue = displayIdEditText.getText().toString().trim();
        if (displayIdValue.isEmpty()) {
            statusTextView.setText("请输入 display_id");
            return;
        }

        final int displayId;
        try {
            displayId = Integer.parseInt(displayIdValue);
        } catch (NumberFormatException e) {
            statusTextView.setText("display_id 必须是整数");
            return;
        }

        Class<?> targetActivity = activitySpinner.getSelectedItemPosition() == 0
                ? GlChildActivityOne.class
                : GlChildActivityTwo.class;
        Intent intent = new Intent(this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(displayId);
            startActivity(intent, options.toBundle());
            statusTextView.setText("已启动投屏到 display_id=" + displayId);
        } catch (RuntimeException e) {
            statusTextView.setText("启动投屏失败: " + e.getMessage());
        }
    }

    private void cancelProjection() {
        Intent intent = new Intent(ProjectionControl.ACTION_STOP_PROJECTION);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
        statusTextView.setText("已发送取消投屏指令");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
