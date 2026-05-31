package com.example.universitymap;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    private int selectedTab = 1;

    private ImageView homeImage, profileImage, mapImage, settingsImage;
    private TextView homeTxt, profileTxt, mapTxt, settingsTxt;
    private LinearLayout homeLayout, profileLayout, mapLayout, settingsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        homeLayout = findViewById(R.id.homeLayout);
        profileLayout = findViewById(R.id.profileLayout);
        mapLayout = findViewById(R.id.mapLayout);
        settingsLayout = findViewById(R.id.settingsLayout);

        homeImage = findViewById(R.id.homeImage);
        profileImage = findViewById(R.id.profileImage);
        mapImage = findViewById(R.id.mapImage);
        settingsImage = findViewById(R.id.settingsImage);

        homeTxt = findViewById(R.id.homeTxt);
        profileTxt = findViewById(R.id.profileTxt);
        mapTxt = findViewById(R.id.mapTxt);
        settingsTxt = findViewById(R.id.settingsTxt);

        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainer, HomeFragment.class, null)
                .commit();

        homeLayout.setOnClickListener(v -> selectTab(1));
        profileLayout.setOnClickListener(v -> selectTab(2));
        mapLayout.setOnClickListener(v -> selectTab(3));
        settingsLayout.setOnClickListener(v -> selectTab(4));
    }

    private void selectTab(int tabIndex) {
        if (selectedTab == tabIndex) return;

        resetAllTabs();

        switch (tabIndex) {
            case 1:
                replaceFragment(new HomeFragment());
                homeTxt.setVisibility(View.VISIBLE);
                homeImage.setImageResource(R.drawable.home_selected_icon);
                animateTab(homeLayout);
                selectedTab = 1;
                break;
            case 2:
                replaceFragment(new ProfileFragment());
                profileTxt.setVisibility(View.VISIBLE);
                profileImage.setImageResource(R.drawable.profile_selected_icon);
                animateTab(profileLayout);
                selectedTab = 2;
                break;
            case 3:
                replaceFragment(new MapFragment());
                mapTxt.setVisibility(View.VISIBLE);
                mapImage.setImageResource(R.drawable.map_selected_icon);
                animateTab(mapLayout);
                selectedTab = 3;
                break;
            case 4:
                replaceFragment(new SettingsFragment());
                settingsTxt.setVisibility(View.VISIBLE);
                settingsImage.setImageResource(R.drawable.settings_selected_icon);
                animateTab(settingsLayout);
                selectedTab = 4;
                break;
        }
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainer, fragment, null)
                .commit();
    }

    private void resetAllTabs() {
        homeImage.setImageResource(R.drawable.home_icon);
        profileImage.setImageResource(R.drawable.profile_icon);
        mapImage.setImageResource(R.drawable.map_icon);
        settingsImage.setImageResource(R.drawable.settings_icon);

        homeTxt.setVisibility(View.GONE);
        profileTxt.setVisibility(View.GONE);
        mapTxt.setVisibility(View.GONE);
        settingsTxt.setVisibility(View.GONE);
    }

    private void animateTab(View tabView) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0.85f, 1.0f, 0.85f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(200);
        scaleAnimation.setFillAfter(true);
        tabView.startAnimation(scaleAnimation);
    }
}
