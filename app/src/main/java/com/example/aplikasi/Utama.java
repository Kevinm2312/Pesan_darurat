package com.example.aplikasi;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class Utama extends AppCompatActivity {
    private ImageButton sendEmailButton;
    private Spinner spinnerPilihan;
    private String userEmail;
    private String actualMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.utama);

        sendEmailButton = findViewById(R.id.tomboldarurat);
        spinnerPilihan = findViewById(R.id.spinnerPilihan);

        Intent intent = getIntent();
        userEmail = intent.getStringExtra("userEmail");

        if (userEmail == null || userEmail.isEmpty()) {
            showErrorToast("Tidak dapat menerima data pengguna");
            return;
        }

        setupSpinner();
        setupSpinnerListener();

        sendEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actualMessage != null && !actualMessage.isEmpty()) {
                    Log.d("UtamaActivity", "Actual Message: " + actualMessage);
                    fetchRecipientWhatsAppNumbers(userEmail);
                } else {
                    Log.d("UtamaActivity", "Actual Message is empty");
                    showErrorToast("Pilih jenis pesan terlebih dahulu");
                }
            }
        });
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.pilihan_pesan, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPilihan.setAdapter(adapter);
    }

    private void setupSpinnerListener() {
        spinnerPilihan.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedItem = parentView.getItemAtPosition(position).toString();
                handleSpinnerSelection(selectedItem);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing here
            }
        });
    }

    private void handleSpinnerSelection(String selectedItem) {
        Log.d("UtamaActivity", "Selected Item: " + selectedItem);

        if (userEmail == null || userEmail.isEmpty()) {
            showErrorToast("Alamat email pengirim tidak tersedia");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query userIdQuery = db.collection("USERS").whereEqualTo("email", userEmail);

        userIdQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String userId = document.getString("userId");
                    if (userId != null) {
                        handleSpinnerItem(selectedItem, userId);
                        return; // Hentikan iterasi setelah menemukan userId
                    } else {
                        showErrorToast("UserID tidak tersedia");
                    }
                }
            } else {
                showErrorToast("Gagal mengambil data pengguna");
            }
        });
    }
    private void handleSpinnerItem(String selectedItem, String userId) {
        switch (selectedItem) {
            case "Pilihan Pencurian":
                actualMessage = "UserID: " + userId + "\nIni adalah situasi darurat! Seseorang mencoba melakukan pencurian. Mohon bantu saya dengan segera.\n" +
                        "Harap tetap tenang dan amankan diri Anda. Jangan panik, segera hubungi pihak berwajib atau keamanan setempat.";
                break;
            case "Pilihan Kebakaran":
                actualMessage = "UserID: " + userId + "\nIni adalah situasi darurat! Ada kebakaran. Mohon bantu saya dengan segera.\n" +
                        "Segera keluar dari bangunan, hindari lift, dan hubungi pemadam kebakaran serta layanan darurat setempat.";
                break;
            case "Pilihan Kecelakaan":
                actualMessage = "UserID: " + userId + "\nSaya terlibat dalam kecelakaan! Mohon bantu saya dengan segera.\n" +
                        "Pastikan keselamatan Anda terlebih dahulu, beri tahu pihak berwenang, dan segera hubungi layanan darurat jika diperlukan.";
                break;
            case "Pilihan Kriminalisasi":
                actualMessage = "UserID: " + userId + "\nIni adalah pesan pencurian yang sebenarnya.\n" +
                        "Jika Anda melihat atau mencurigai aktivitas kriminal, amankan diri Anda dan segera hubungi pihak berwajib atau keamanan setempat.";
                break;
            default:
                actualMessage = ""; // Atur pesan ke kosong jika pilihan tidak dikenali
                break;
        }
    }
    private void fetchRecipientWhatsAppNumbers(final String senderEmail) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query numberQuery = db.collection("contacts").whereEqualTo("email", senderEmail);

        numberQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ArrayList<String> recipientWhatsAppNumbers = new ArrayList<>();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String whatsappNumber = document.getString("phoneNumber");

                    if (whatsappNumber != null) {
                        recipientWhatsAppNumbers.add(whatsappNumber);
                    }
                }
                composeWhatsAppMessage(recipientWhatsAppNumbers);
            } else {
                showErrorToast("Gagal mengambil nomor telepon penerima WhatsApp");
            }
        });
    }

    private void composeWhatsAppMessage(ArrayList<String> recipientNumbers) {
        if (recipientNumbers == null || recipientNumbers.isEmpty()) {
            showErrorToast("Tidak ada nomor penerima WhatsApp");
            return;
        }

        String message = actualMessage;

        try {
            for (String recipientNumber : recipientNumbers) {
                String url = "https://api.whatsapp.com/send?phone=" + recipientNumber + "&text=" + Uri.encode(message);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }


        } catch (android.content.ActivityNotFoundException ex) {
            showErrorToast("WhatsApp tidak terinstal");
        }

        Intent mainActivityIntent = new Intent(Utama.this, MainActivity.class);
        mainActivityIntent.putExtra("userEmail", userEmail);
        startActivity(mainActivityIntent);
    }

    private void showErrorToast(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    public void openInformasiActivity(View view) {
        Intent intent = new Intent(this, informasi.class);
        Intent emailIntent = getIntent();
        String userEmail = emailIntent.getStringExtra("userEmail");

        // Kirim email ke aktivitas "Info Akun"
        intent.putExtra("userEmail", userEmail);
        startActivity(intent);
    }

    public void openHistoriActivity(View view) {
        Intent intent = new Intent(this, history.class);
        startActivity(intent);
    }

    public void openGPSActivity(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    public void openNomorHpActivity(View view) {
        Intent intent = new Intent(this, nomor_hp.class);
        Intent emailIntent = getIntent();
        String userEmail = emailIntent.getStringExtra("userEmail");

        // Kirim email ke aktivitas "Info Akun"
        intent.putExtra("userEmail", userEmail);
        startActivity(intent);
    }
}
