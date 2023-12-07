package com.example.aplikasi;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class informasi extends AppCompatActivity {

    private TextView textViewNama;
    private TextView textViewAlamat;
    private TextView textViewNomorHP;
    private ImageView imageViewProfil;
    private Uri selectedImageUri;
    private String userEmail;
    private String imageCode = ""; // Declare imageCode at the class level
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private StorageReference storageRef = FirebaseStorage.getInstance().getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.informasi);

        textViewNama = findViewById(R.id.textViewakun);
        textViewAlamat = findViewById(R.id.textViewalamat);
        textViewNomorHP = findViewById(R.id.textViewnohp);
        imageViewProfil = findViewById(R.id.imageviewprofill);

        imageViewProfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Buat Intent untuk memilih gambar dari galeri
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });

        // Dapatkan email dari Intent Extra
        userEmail = getIntent().getStringExtra("userEmail");

        // Pastikan email tidak kosong sebelum mencoba mengambil data
        if (userEmail != null && !userEmail.isEmpty()) {
            // Cari dokumen berdasarkan email
            findDocumentByEmail(userEmail);
            // Muat gambar ketika aktivitas dibuat
            loadImageFromFirestore(imageCode);
        } else {
            // Handle jika email kosong
            Log.e("InformasiActivity", "Email kosong atau tidak valid");
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            // Upload URI gambar ke Firestore
            saveImageUriToFirestore(selectedImageUri, userEmail);
        }
    }

    private void saveImageUriToFirestore(Uri imageUri, final String email) {
        String imagePath = "images/" + email + "_profile.jpg";
        final StorageReference imageRef = storageRef.child(imagePath);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Gambar berhasil diunggah
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String imageUrl = uri.toString();
                                // Simpan URL gambar ke Firestore bersama dengan kode gambar
                                saveImageUrlAndCodeToFirestore(imageUrl, email);
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle kesalahan saat mengunggah gambar
                        Log.e("InformasiActivity", "Gagal mengunggah gambar: " + e.getMessage());
                    }
                });
    }

    private void saveImageUrlAndCodeToFirestore(final String imageUrl, final String email) {
        // Menghasilkan kode gambar unik (bisa menggunakan timestamp atau UUID)
        final String imageCode = generateUniqueImageCode(); // Gunakan variabel imageCode yang dideklarasikan di level kelas

        // Menyimpan URL gambar dan kode gambar ke Firestore
        db.collection("USERS")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);

                            // Dapatkan referensi dokumen dan simpan URL gambar
                            document.getReference()
                                    .update("profileImageUrl", imageUrl, "imageCode", imageCode)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // URL gambar dan kode gambar berhasil disimpan
                                            // Anda dapat menambahkan tindakan lain yang diperlukan di sini
                                            // Setelah menyimpan, tampilkan gambar profil yang baru diunggah
                                            loadImageFromFirestore(imageCode);
                                        }
                                    });
                        }
                    }
                });
    }


    private void findDocumentByEmail(String email) {
        db.collection("USERS")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);

                            // Mendapatkan data dari Firestore
                            String nama = document.getString("nama");
                            String alamat = document.getString("alamat");
                            String nomorHP = document.getString("phoneNumber");
                            imageCode = document.getString("imageCode"); // Mengambil kode gambar

                            // Menampilkan data di TextView
                            textViewNama.setText(nama);
                            textViewAlamat.setText(alamat);
                            textViewNomorHP.setText(nomorHP);

                            // Memuat gambar profil
                            loadImageFromFirestore(imageCode);
                        } else {
                            Log.e("InformasiActivity", "Dokumen tidak ditemukan");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        // Handle kesalahan saat mengambil data
                        Log.e("InformasiActivity", "Gagal mengambil data dari Firestore: " + e.getMessage());
                    }
                });
    }

    private void loadImageFromFirestore(String imageCode) {
        // Mendapatkan URL gambar dari Firestore berdasarkan kode gambar
        CollectionReference imagesRef = db.collection("USERS");

        imagesRef.whereEqualTo("imageCode", imageCode)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                            String imageUrl = document.getString("profileImageUrl");

                            // Load gambar dari URL ke ImageView
                            Glide.with(getApplicationContext())
                                    .load(imageUrl)
                                    .circleCrop()
                                    .into(imageViewProfil);
                        } else {
                            Log.e("InformasiActivity", "Gambar tidak ditemukan");
                        }
                    }
                });
    }

    private String generateUniqueImageCode() {
        // Buat kode gambar unik, misalnya menggunakan timestamp atau UUID
        // Contoh penggunaan timestamp:
        return String.valueOf(System.currentTimeMillis());
    }
}
