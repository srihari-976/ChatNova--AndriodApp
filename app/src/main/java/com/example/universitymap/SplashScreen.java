package com.example.universitymap;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        ImageView logoImage = findViewById(R.id.logoImage);
        TextView appNameText = findViewById(R.id.appNameText);
        TextView taglineText = findViewById(R.id.taglineText);

        logoImage.setAlpha(0f);
        appNameText.setAlpha(0f);
        taglineText.setAlpha(0f);

        ObjectAnimator logoAnimator = ObjectAnimator.ofFloat(logoImage, "alpha", 0f, 1f);
        logoAnimator.setDuration(800);
        logoAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        logoAnimator.start();

        ObjectAnimator logoScaleAnimator = ObjectAnimator.ofFloat(logoImage, "scaleX", 0.5f, 1f);
        logoScaleAnimator.setDuration(800);
        logoScaleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        logoScaleAnimator.start();

        ObjectAnimator logoScaleYAnimator = ObjectAnimator.ofFloat(logoImage, "scaleY", 0.5f, 1f);
        logoScaleYAnimator.setDuration(800);
        logoScaleYAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        logoScaleYAnimator.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            appNameText.animate().alpha(1f).setDuration(500).setInterpolator(new AccelerateDecelerateInterpolator()).start();
            taglineText.animate().alpha(1f).setDuration(500).setInterpolator(new AccelerateDecelerateInterpolator()).start();
        }, 400);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashScreen.this, LoginPage.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
    }
}
