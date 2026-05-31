package com.example.universitymap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    private CheckBox campusEventsCheckBox;
    private CheckBox buildingUpdatesCheckBox;
    private CheckBox emergencyAlertsCheckBox;
    private MaterialSwitch locationSharingSwitch;
    private MaterialSwitch dataUsageSwitch;
    private TextView appVersionTextView;
    private MaterialButton clearCacheButton;

    public SettingsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        campusEventsCheckBox = view.findViewById(R.id.campusEvents);
        buildingUpdatesCheckBox = view.findViewById(R.id.buildingUpdates);
        emergencyAlertsCheckBox = view.findViewById(R.id.emergencyAlerts);
        locationSharingSwitch = view.findViewById(R.id.locationSharing);
        dataUsageSwitch = view.findViewById(R.id.dataUsage);
        appVersionTextView = view.findViewById(R.id.appVersion);
        clearCacheButton = view.findViewById(R.id.clearCacheButton);

        appVersionTextView.setText(getString(R.string.app_version_1_0_0));

        campusEventsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(getContext(), isChecked ? "Campus Events Notifications Enabled" : "Campus Events Notifications Disabled", Toast.LENGTH_SHORT).show());

        buildingUpdatesCheckBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(getContext(), isChecked ? "Building Updates Notifications Enabled" : "Building Updates Notifications Disabled", Toast.LENGTH_SHORT).show());

        emergencyAlertsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(getContext(), isChecked ? "Emergency Alerts Enabled" : "Emergency Alerts Disabled", Toast.LENGTH_SHORT).show());

        locationSharingSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(getContext(), isChecked ? "Location Sharing Enabled" : "Location Sharing Disabled", Toast.LENGTH_SHORT).show());

        dataUsageSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(getContext(), isChecked ? "Data Usage Saving Enabled" : "Data Usage Saving Disabled", Toast.LENGTH_SHORT).show());

        clearCacheButton.setOnClickListener(v ->
                Toast.makeText(getContext(), "Cache Cleared Successfully", Toast.LENGTH_SHORT).show());

        return view;
    }
}
