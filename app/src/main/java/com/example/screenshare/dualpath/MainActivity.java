package com.example.screenshare.dualpath;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int COLOR_BACKGROUND_TOP = Color.rgb(10, 16, 32);
    private static final int COLOR_BACKGROUND_BOTTOM = Color.rgb(25, 37, 67);
    private static final int COLOR_CARD = Color.rgb(30, 42, 73);
    private static final int COLOR_CARD_STROKE = Color.rgb(70, 92, 140);
    private static final int COLOR_TEXT_PRIMARY = Color.WHITE;
    private static final int COLOR_TEXT_SECONDARY = Color.rgb(190, 204, 230);
    private static final int COLOR_ACCENT = Color.rgb(78, 163, 255);
    private static final int COLOR_DANGER = Color.rgb(255, 105, 105);
    private static final int COLOR_SUCCESS = Color.rgb(74, 222, 128);
    private static final int COLOR_INPUT = Color.rgb(244, 248, 255);
    private static final int PROJECTION_SWITCH_DELAY_MS = 700;

    private final Map<Integer, Class<?>> activeActivityByDisplayId = new HashMap<>();
    private final Map<Class<?>, Integer> activeDisplayIdByActivity = new HashMap<>();
    private EditText displayIdEditText;
    private Spinner activitySpinner;
    private TextView statusTextView;
    private Runnable pendingLaunchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int padding = dp(24);
        root.setPadding(padding, padding, padding, padding);
        root.setBackground(makeBackground());

        TextView title = new TextView(this);
        title.setText("ScreenShare Dual Path");
        title.setTextColor(COLOR_TEXT_PRIMARY);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrapParams());

        TextView subtitle = new TextView(this);
        subtitle.setText("输入 display_id，选择 OpenGL 子界面，然后启动到目标屏幕");
        subtitle.setTextColor(COLOR_TEXT_SECONDARY);
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, matchWrapParams(dp(8), dp(0), dp(0), dp(20)));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(makeRoundedDrawable(COLOR_CARD, COLOR_CARD_STROKE, dp(18)));
        root.addView(card, matchWrapParams());

        card.addView(makeLabel("目标 display_id（必填）"));
        displayIdEditText = new EditText(this);
        displayIdEditText.setHint("例如：0、1、2 或 private display id");
        displayIdEditText.setSingleLine(true);
        displayIdEditText.setTextColor(Color.rgb(20, 30, 48));
        displayIdEditText.setHintTextColor(Color.rgb(105, 119, 145));
        displayIdEditText.setTextSize(18);
        displayIdEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        displayIdEditText.setBackground(makeRoundedDrawable(COLOR_INPUT, COLOR_ACCENT, dp(12)));
        displayIdEditText.setPadding(dp(14), dp(10), dp(14), dp(10));
        card.addView(displayIdEditText, matchWrapParams(dp(0), dp(8), dp(0), dp(18)));

        card.addView(makeLabel("选择子 Activity"));
        activitySpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"OpenGL 子 Activity 1  ·  旋转三角形", "OpenGL 子 Activity 2  ·  脉冲方形"}) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                styleSpinnerText(view, false);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                styleSpinnerText(view, true);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activitySpinner.setAdapter(adapter);
        activitySpinner.setBackground(makeRoundedDrawable(Color.rgb(242, 247, 255), COLOR_ACCENT, dp(12)));
        activitySpinner.setPadding(dp(8), dp(4), dp(8), dp(4));
        card.addView(activitySpinner, matchWrapParams(dp(0), dp(8), dp(0), dp(22)));

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);
        card.addView(buttonRow, matchWrapParams());

        Button startButton = makeActionButton("启动投屏", COLOR_ACCENT);
        startButton.setOnClickListener(v -> runWithButtonPressEffect(startButton, this::startProjection));
        buttonRow.addView(startButton, weightedButtonParams(dp(0), dp(0), dp(8), dp(0)));

        Button cancelButton = makeActionButton("取消投屏", COLOR_DANGER);
        cancelButton.setOnClickListener(v -> runWithButtonPressEffect(cancelButton, this::cancelProjection));
        buttonRow.addView(cancelButton, weightedButtonParams(dp(8), dp(0), dp(0), dp(0)));

        statusTextView = new TextView(this);
        statusTextView.setText("请输入 display_id 后启动投屏");
        statusTextView.setTextColor(COLOR_TEXT_SECONDARY);
        statusTextView.setTextSize(15);
        statusTextView.setGravity(Gravity.CENTER);
        root.addView(statusTextView, matchWrapParams(dp(0), dp(18), dp(0), dp(0)));

        setContentView(root);
    }

    private void startProjection() {
        String displayIdValue = displayIdEditText.getText().toString().trim();
        if (displayIdValue.isEmpty()) {
            displayIdEditText.setError("启动投屏前必须输入 display_id");
            displayIdEditText.requestFocus();
            statusTextView.setTextColor(COLOR_DANGER);
            statusTextView.setText("启动投屏前必须输入 display_id");
            return;
        }

        final int displayId;
        try {
            displayId = Integer.parseInt(displayIdValue);
        } catch (NumberFormatException e) {
            displayIdEditText.setError("display_id 必须是整数");
            displayIdEditText.requestFocus();
            statusTextView.setTextColor(COLOR_DANGER);
            statusTextView.setText("display_id 必须是整数");
            return;
        }

        displayIdEditText.setError(null);
        Class<?> targetActivity = getSelectedActivityClass();
        int switchDelayMs = prepareProjectionSlot(displayId, targetActivity);
        if (switchDelayMs < 0) {
            return;
        }

        if (switchDelayMs > 0) {
            statusTextView.setTextColor(COLOR_TEXT_SECONDARY);
            statusTextView.setText("正在切换投屏，请稍候...");
            clearPendingLaunch();
            pendingLaunchRunnable = () -> {
                pendingLaunchRunnable = null;
                launchProjection(displayId, targetActivity);
            };
            displayIdEditText.postDelayed(pendingLaunchRunnable, switchDelayMs);
            return;
        }

        clearPendingLaunch();
        launchProjection(displayId, targetActivity);
    }

    private void launchProjection(int displayId, Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ProjectionControl.EXTRA_DISPLAY_ID, displayId);
        intent.putExtra(ProjectionControl.EXTRA_ACTIVITY_CLASS, targetActivity.getName());

        try {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(displayId);
            startActivity(intent, options.toBundle());
            rememberProjection(displayId, targetActivity);
            showSuccess("已启动" + getActivityLabel(targetActivity) + "到 display_id=" + displayId);
        } catch (RuntimeException e) {
            statusTextView.setTextColor(COLOR_DANGER);
            statusTextView.setText("启动投屏失败: " + e.getMessage());
        }
    }

    private void cancelProjection() {
        clearPendingLaunch();
        Integer displayId = readDisplayIdForCancel();
        if (displayId == null) {
            return;
        }

        Intent intent = new Intent(ProjectionControl.ACTION_STOP_PROJECTION);
        intent.setPackage(getPackageName());
        intent.putExtra(ProjectionControl.EXTRA_DISPLAY_ID, displayId);
        sendBroadcast(intent);

        Class<?> stoppedActivity = activeActivityByDisplayId.remove(displayId);
        if (stoppedActivity != null) {
            activeDisplayIdByActivity.remove(stoppedActivity);
        }
        showSuccess("已发送取消 display_id=" + displayId + " 的投屏指令");
    }

    private Integer readDisplayIdForCancel() {
        String displayIdValue = displayIdEditText.getText().toString().trim();
        if (displayIdValue.isEmpty()) {
            displayIdEditText.setError("取消投屏前必须输入 display_id");
            displayIdEditText.requestFocus();
            statusTextView.setTextColor(COLOR_DANGER);
            statusTextView.setText("取消投屏前必须输入 display_id");
            return null;
        }

        try {
            displayIdEditText.setError(null);
            return Integer.parseInt(displayIdValue);
        } catch (NumberFormatException e) {
            displayIdEditText.setError("display_id 必须是整数");
            displayIdEditText.requestFocus();
            statusTextView.setTextColor(COLOR_DANGER);
            statusTextView.setText("display_id 必须是整数");
            return null;
        }
    }

    private int prepareProjectionSlot(int displayId, Class<?> targetActivity) {
        Class<?> activeActivityOnDisplay = activeActivityByDisplayId.get(displayId);
        Integer activeDisplayForActivity = activeDisplayIdByActivity.get(targetActivity);
        boolean stoppedExistingProjection = false;

        if (activeActivityOnDisplay == targetActivity && activeDisplayForActivity != null
                && activeDisplayForActivity == displayId) {
            statusTextView.setTextColor(COLOR_TEXT_SECONDARY);
            statusTextView.setText("已经启动投屏");
            return -1;
        }

        if (activeDisplayForActivity != null) {
            sendStopProjection(activeDisplayForActivity, targetActivity);
            activeActivityByDisplayId.remove(activeDisplayForActivity);
            activeDisplayIdByActivity.remove(targetActivity);
            stoppedExistingProjection = true;
        }

        if (activeActivityOnDisplay != null && activeActivityOnDisplay != targetActivity) {
            sendStopProjection(displayId, activeActivityOnDisplay);
            activeActivityByDisplayId.remove(displayId);
            activeDisplayIdByActivity.remove(activeActivityOnDisplay);
            stoppedExistingProjection = true;
        }

        return stoppedExistingProjection ? PROJECTION_SWITCH_DELAY_MS : 0;
    }

    private void clearPendingLaunch() {
        if (pendingLaunchRunnable == null || displayIdEditText == null) {
            return;
        }
        displayIdEditText.removeCallbacks(pendingLaunchRunnable);
        pendingLaunchRunnable = null;
    }

    private void rememberProjection(int displayId, Class<?> targetActivity) {
        activeActivityByDisplayId.put(displayId, targetActivity);
        activeDisplayIdByActivity.put(targetActivity, displayId);
    }

    private void sendStopProjection(int displayId, Class<?> targetActivity) {
        Intent intent = new Intent(ProjectionControl.ACTION_STOP_PROJECTION);
        intent.setPackage(getPackageName());
        intent.putExtra(ProjectionControl.EXTRA_DISPLAY_ID, displayId);
        intent.putExtra(ProjectionControl.EXTRA_ACTIVITY_CLASS, targetActivity.getName());
        sendBroadcast(intent);
    }

    private Class<?> getSelectedActivityClass() {
        return activitySpinner.getSelectedItemPosition() == 0
                ? GlChildActivityOne.class
                : GlChildActivityTwo.class;
    }

    private String getActivityLabel(Class<?> activityClass) {
        return activityClass == GlChildActivityOne.class
                ? "OpenGL 子 Activity 1"
                : "OpenGL 子 Activity 2";
    }

    private void showSuccess(String message) {
        statusTextView.setTextColor(COLOR_SUCCESS);
        statusTextView.setText(message + " · 操作成功");
        Toast.makeText(this, "操作成功", Toast.LENGTH_SHORT).show();
    }

    private void runWithButtonPressEffect(Button button, Runnable action) {
        button.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(70)
                .withEndAction(() -> button.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(90)
                        .start())
                .start();
        action.run();
    }

    private TextView makeLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(COLOR_TEXT_PRIMARY);
        label.setTextSize(15);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        return label;
    }

    private Button makeActionButton(String text, int color) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setBackground(makeButtonBackground(color));
        button.setPadding(dp(8), dp(10), dp(8), dp(10));
        return button;
    }

    private void styleSpinnerText(TextView view, boolean dropdown) {
        view.setTextColor(Color.rgb(18, 30, 52));
        view.setTextSize(dropdown ? 18 : 17);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(dp(14), dp(12), dp(14), dp(12));
        if (dropdown) {
            view.setBackgroundColor(Color.rgb(232, 241, 255));
        }
    }

    private StateListDrawable makeButtonBackground(int color) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed},
                makeRoundedDrawable(darken(color), Color.WHITE, dp(12)));
        states.addState(new int[]{android.R.attr.state_focused},
                makeRoundedDrawable(color, Color.WHITE, dp(12)));
        states.addState(new int[]{}, makeRoundedDrawable(color, color, dp(12)));
        return states;
    }

    private int darken(int color) {
        return Color.rgb(
                Math.round(Color.red(color) * 0.72f),
                Math.round(Color.green(color) * 0.72f),
                Math.round(Color.blue(color) * 0.72f));
    }

    private GradientDrawable makeBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{COLOR_BACKGROUND_TOP, COLOR_BACKGROUND_BOTTOM});
        return drawable;
    }

    private GradientDrawable makeRoundedDrawable(int fillColor, int strokeColor, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrapParams() {
        return matchWrapParams(0, 0, 0, 0);
    }

    private LinearLayout.LayoutParams matchWrapParams(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(left, top, right, bottom);
        return params;
    }

    private LinearLayout.LayoutParams weightedButtonParams(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f);
        params.setMargins(left, top, right, bottom);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
