package com.example.universitymap;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextInputEditText etName, etEmail, etPhone, etAddress, etBio;
    private ImageButton imgEditPhone, imgEditAddress, imgEditBio;
    private MaterialButton btnSignOut, btnSave;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String userId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);
        etAddress = view.findViewById(R.id.etAddress);
        etBio = view.findViewById(R.id.etBio);
        imgEditPhone = view.findViewById(R.id.imgEditPhone);
        imgEditAddress = view.findViewById(R.id.imgEditAddress);
        imgEditBio = view.findViewById(R.id.imgEditBio);
        btnSignOut = view.findViewById(R.id.btnSignOut);
        btnSave = view.findViewById(R.id.btnSave);

        if (currentUser != null) {
            etEmail.setText(currentUser.getEmail());
        }

        fetchUserData();

        imgEditPhone.setOnClickListener(v -> enableEditing(etPhone));
        imgEditAddress.setOnClickListener(v -> enableEditing(etAddress));
        imgEditBio.setOnClickListener(v -> enableEditing(etBio));

        btnSave.setOnClickListener(v -> saveUpdatedDetails());

        btnSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getContext(), LoginPage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }

    private void fetchUserData() {
        if (userId == null) return;

        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot snapshot = task.getResult();
                        String name = snapshot.getString("name");
                        String phone = snapshot.getString("phone");
                        String address = snapshot.getString("address");
                        String bio = snapshot.getString("bio");

                        if (name != null) etName.setText(name);
                        if (phone != null) etPhone.setText(phone);
                        if (address != null) etAddress.setText(address);
                        if (bio != null) etBio.setText(bio);
                    }
                });
    }

    private void enableEditing(TextInputEditText editText) {
        editText.setFocusableInTouchMode(true);
        editText.setFocusable(true);
        editText.requestFocus();
        Toast.makeText(getContext(), "You can now edit this field", Toast.LENGTH_SHORT).show();
    }

    private void saveUpdatedDetails() {
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(address) || TextUtils.isEmpty(bio)) {
            Toast.makeText(getContext(), "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("phone", phone);
        updates.put("address", address);
        updates.put("bio", bio);

        if (userId != null) {
            db.collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        disableEditing(etPhone);
                        disableEditing(etAddress);
                        disableEditing(etBio);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show());
        }
    }

    private void disableEditing(TextInputEditText editText) {
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
    }
}
