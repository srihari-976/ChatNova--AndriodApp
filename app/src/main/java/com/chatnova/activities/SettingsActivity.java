package com.chatnova.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chatnova.databinding.ActivitySettingsBinding;
import com.chatnova.utilities.Constants;
import com.chatnova.utilities.PermissionHelper;
import com.chatnova.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
        loadUserInfo();
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> finish());
        binding.layoutPermissions.setOnClickListener(v -> PermissionHelper.openAppSettings(this));
        binding.layoutPrivacy.setOnClickListener(v -> Toast.makeText(this, "Privacy settings", Toast.LENGTH_SHORT).show());
        binding.layoutNotifications.setOnClickListener(v -> PermissionHelper.openAppSettings(this));
        binding.layoutDataUsage.setOnClickListener(v -> Toast.makeText(this, "Data usage settings", Toast.LENGTH_SHORT).show());
        binding.layoutAbout.setOnClickListener(v -> Toast.makeText(this, "ChatNova v1.0", Toast.LENGTH_SHORT).show());
        binding.layoutHelp.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://chatnova.app/help"));
            startActivity(intent);
        });
    }

    private void loadUserInfo() {
        String name = preferenceManager.getString(Constants.KEY_NAME);
        String email = preferenceManager.getString(Constants.KEY_EMAIL);
        binding.textName.setText(name != null ? name : "User");
        binding.textEmail.setText(email != null ? email : "No email");
    }
}
