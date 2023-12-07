package com.example.aplikasi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class daftar extends AppCompatActivity {

    private EditText nameEditText;
    private EditText emailEditText;
    private EditText addressEditText;
    private EditText phoneEditText;
    private EditText passwordEditText;
    private Button registerButton;
    private EditText confirmPasswordEditText;
    private CheckBox termsCheckbox;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.daftar);

        nameEditText = findViewById(R.id.nama);
        emailEditText = findViewById(R.id.email);
        addressEditText = findViewById(R.id.alamat);
        phoneEditText = findViewById(R.id.nohp);
        passwordEditText = findViewById(R.id.password);
        registerButton = findViewById(R.id.daftarbtn);
        termsCheckbox = findViewById(R.id.ckcketentuan);
        confirmPasswordEditText = findViewById(R.id.passwordrepet);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name = nameEditText.getText().toString();
                final String email = emailEditText.getText().toString();
                final String address = addressEditText.getText().toString();
                final String phone = phoneEditText.getText().toString();
                final String password = passwordEditText.getText().toString();
                final String confirmPassword = confirmPasswordEditText.getText().toString();

                if (!termsCheckbox.isChecked()) {
                    showToast("Silakan setujui syarat dan ketentuan.");
                } else if (!password.equals(confirmPassword)) {
                    showToast("Password dan konfirmasi password tidak cocok.");
                } else {
                    registerUser(email, password, name, address, phone);
                }
            }
        });
    }

    private void registerUser(final String email, final String password, final String name, final String address, final String phone) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            saveUserDataToFirestore(user.getUid(), name, email, address, phone);
                            showToast("Pendaftaran berhasil.");
                            goToLoginActivity();
                        } else {
                            showToast("Pendaftaran gagal. " + task.getException().getMessage());
                        }
                    }
                });
    }

    private void saveUserDataToFirestore(String userId, String name, String email, String address, String phone) {
        CollectionReference usersRef = db.collection("USERS");
        Map<String, Object> user = new HashMap<>();
        user.put("nama", name);
        user.put("email", email);
        user.put("alamat", address);
        user.put("phoneNumber", phone);

        usersRef.document(userId)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Data pengguna berhasil disimpan
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showToast("Gagal menyimpan data pengguna: " + e.getMessage());
                    }
                });
    }

    private void goToLoginActivity() {
        Intent loginIntent = new Intent(daftar.this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}