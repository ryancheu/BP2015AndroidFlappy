package com.example.flappybird;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This code just makes the app fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // We're manually constructing a view here to avoid explaining XML
        gameView = new GameView(this);
        FrameLayout fl = new FrameLayout(this);
        fl.setLayoutParams(new LayoutParams());
        fl.addView(gameView);
        setContentView(fl);
    }
}
