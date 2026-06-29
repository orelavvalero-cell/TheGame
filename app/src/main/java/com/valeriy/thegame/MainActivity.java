package com.valeriy.thegame;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private GameView gameView;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.NEARBY_WIFI_DEVICES}, 7);
        }

        FrameLayout root = new FrameLayout(this);
        gameView = new GameView(this);
        root.addView(gameView, new FrameLayout.LayoutParams(-1, -1));
        root.addView(buildHud(), new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));
        setContentView(root);
        hideSystemUi();
    }

    private View buildHud() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(14, 10, 14, 8);
        bar.setBackgroundColor(0x77000000);

        Button solo = makeButton("SOLO");
        Button host = makeButton("HOST");
        Button join = makeButton("FIND");
        Button reset = makeButton("RESET");
        Button help = makeButton("GET HELP");
        Button stop = makeButton("STOP");
        status = new TextView(this);
        status.setTextColor(0xffffffff);
        status.setTextSize(14);
        status.setText("SOLO: bots active");
        status.setPadding(16, 0, 0, 0);

        solo.setOnClickListener(v -> gameView.startSolo());
        host.setOnClickListener(v -> gameView.startHost());
        join.setOnClickListener(v -> gameView.findAndJoinHost());
        reset.setOnClickListener(v -> gameView.restartRound());
        help.setOnClickListener(v -> help.setText(gameView.toggleHelp() ? "STOP HELP" : "GET HELP"));
        stop.setOnClickListener(v -> gameView.stopNetwork());
        gameView.setStatusSink(text -> runOnUiThread(() -> status.setText(text)));

        bar.addView(solo);
        bar.addView(host);
        bar.addView(join);
        bar.addView(reset);
        bar.addView(help);
        bar.addView(stop);
        bar.addView(status, new LinearLayout.LayoutParams(0, -2, 1f));
        return bar;
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(12);
        button.setTextColor(0xffffffff);
        button.setBackgroundColor(0xff293241);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(78), dp(42));
        params.setMargins(0, 0, 8, 0);
        button.setLayoutParams(params);
        return button;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void hideSystemUi() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        gameView.resume();
    }

    @Override
    protected void onPause() {
        gameView.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        gameView.shutdown();
        super.onDestroy();
    }
}
