package com.example.universitymap;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SigninPage extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextInputEditText nameField, emailField, passwordField, confirmPasswordField;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin_page);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            goToMainMap();
            return;
        }

        nameField = findViewById(R.id.editTextName);
        emailField = findViewById(R.id.editTextEmail);
        passwordField = findViewById(R.id.editTextPassword);
        confirmPasswordField = findViewById(R.id.editTextConfirmPassword);
        progressBar = findViewById(R.id.progressBar);

        MaterialButton signUpButton = findViewById(R.id.button2);
        MaterialButton loginButton = findViewById(R.id.login);

        signUpButton.setOnClickListener(view -> signUpUser());
        loginButton.setOnClickListener(view -> goToLogin());
    }

    private void signUpUser() {
        String name = nameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();

        if (!validateInputs(name, email, password, confirmPassword)) return;

        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserToFirestore(name, email);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        String errorMsg = task.getException() != null ?
                                task.getException().getMessage() : "Account creation failed";
                        showToast(errorMsg);
                    }
                });
    }

    private boolean validateInputs(String name, String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(name)) {
            nameField.setError("Name is required");
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            emailField.setError("Email is required");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            passwordField.setError("Password is required");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordField.setError("Passwords do not match");
            return false;
        }
        if (password.length() < 6) {
            passwordField.setError("Password must be at least 6 characters");
            return false;
        }
        return true;
    }

    private void saveUserToFirestore(String name, String email) {
        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("phone", "");
        user.put("address", "");
        user.put("bio", "");

        db.collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    showToast("Account created successfully!");
                    goToMainMap();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    showToast("Failed to save details: " + e.getMessage());
                });
    }

    private void goToLogin() {
        Intent loginIntent = new Intent(SigninPage.this, LoginPage.class);
        startActivity(loginIntent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        finish();
    }

    private void goToMainMap() {
        Intent mainMapIntent = new Intent(SigninPage.this, MainActivity.class);
        mainMapIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainMapIntent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(SigninPage.this, message, Toast.LENGTH_LONG).show();
    }
}
