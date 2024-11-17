package com.example.a06_frunigame;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {
    Handler updateHandler;
    private Game game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        game = new Game(this);
        setContentView(game);

        StartAnimation();
    }

    private void StartAnimation() {
        updateHandler = new Handler();
        updateHandler.postDelayed( new Runnable() {
            @Override
            public void run() {
                game.update();
                game.invalidate();
                updateHandler.postDelayed(this, 30);
            }
        }, 30);
    }

}