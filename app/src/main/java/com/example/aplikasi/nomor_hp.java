package com.example.aplikasi;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class nomor_hp extends AppCompatActivity {
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private DataAdapter adapter;
    private List<DataModel> dataModels;
    private String userEmail; // Simpan email pengguna di sini
    private static final int PICK_CONTACT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nomorhp);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dataModels = new ArrayList<>();
        adapter = new DataAdapter(dataModels);
        recyclerView.setAdapter(adapter);

        // Ambil email pengguna dari Intent (pastikan Anda mengirimkannya dari Activity sebelumnya)
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userEmail = extras.getString("userEmail");
        }

        FloatingActionButton fabAddData = findViewById(R.id.fabAddData);
        fabAddData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openContactPicker();
            }
        });

        ImageButton menuButton = findViewById(R.id.toolbarButton);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopupMenu(view);
            }
        });

        // Load data from Firestore when the activity starts
        loadUserContactsFromFirestore(userEmail);
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.menu_main, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_edit) {
                showEditDataDialog();
            } else if (itemId == R.id.action_delete) {
                showDeleteDataDialog();
            } else {
                // Handle other menu items here
            }

            return true;
        });

        popupMenu.show();
    }

    private void showDeleteDataDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hapus Kontak");

        // Buat array String untuk menampilkan daftar nama kontak
        String[] contactNames = new String[dataModels.size()];
        for (int i = 0; i < dataModels.size(); i++) {
            contactNames[i] = dataModels.get(i).getName();
        }

        builder.setItems(contactNames, (dialog, which) -> {
            // Dapatkan indeks item yang dipilih
            DataModel selectedData = dataModels.get(which);

            // Tampilkan dialog konfirmasi untuk menghapus data
            showDeleteConfirmationDialog(selectedData);
        });

        builder.setNegativeButton("Batal", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void showDeleteConfirmationDialog(DataModel dataModel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Konfirmasi Hapus");
        builder.setMessage("Apakah Anda yakin ingin menghapus kontak ini?");

        builder.setPositiveButton("Ya", (dialog, which) -> {
            // Dapatkan dokumen ID dari DataModel
            String documentId = dataModel.getDocumentId();

            // Dapatkan koleksi yang sesuai
            CollectionReference contactsRef = db.collection("contacts");

            // Hapus dokumen berdasarkan ID
            contactsRef.document(documentId).delete()
                    .addOnSuccessListener(aVoid -> {
                        // Data berhasil dihapus dari Firestore
                        Toast.makeText(nomor_hp.this, "Kontak Telah dihapus", Toast.LENGTH_SHORT).show();

                        // Setelah menghapus data, Anda dapat memperbarui RecyclerView
                        dataModels.remove(dataModel);
                        adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        // Tangani kesalahan jika data tidak dapat dihapus dari Firestore
                        Toast.makeText(nomor_hp.this, "Gagal menghapus kontak: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });


        builder.setNegativeButton("Tidak", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void showEditDataDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Kontak");

        // Create a String array to display the list of contact names
        String[] contactNames = new String[dataModels.size()];
        for (int i = 0; i < dataModels.size(); i++) {
            contactNames[i] = dataModels.get(i).getName();
        }

        builder.setItems(contactNames, (dialog, which) -> {
            // Get the index of the selected item
            DataModel selectedData = dataModels.get(which);

            // Show the edit dialog with the selected data
            showEditDialog(selectedData);
        });

        builder.setNegativeButton("Batal", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void showEditDialog(DataModel dataModel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Kontak");
        View view = getLayoutInflater().inflate(R.layout.dialog_add_data, null);
        builder.setView(view);

        final TextView nameEditText = view.findViewById(R.id.editTextName);
        final TextView phoneEditText = view.findViewById(R.id.editTextPhone);

        nameEditText.setText(dataModel.getName());
        phoneEditText.setText(dataModel.getPhoneNumber());

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            // Ambil data yang diubah dari dialog edit
            String updatedName = nameEditText.getText().toString();
            String updatedPhone = phoneEditText.getText().toString();

            // Update data di Firestore
            updateDataInFirestore(dataModel, updatedName, updatedPhone);

            dialog.dismiss(); // Tutup dialog setelah data diperbarui
        });
        builder.setNegativeButton("Batal", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void updateDataInFirestore(DataModel dataModel, String updatedName, String updatedPhone) {
        CollectionReference contactsRef = db.collection("contacts");

        // Perbarui nilai "name" dan "phoneNumber" dalam dokumen yang ada
        contactsRef.document(dataModel.getDocumentId())
                .update("name", updatedName, "phoneNumber", updatedPhone)
                .addOnSuccessListener(aVoid -> {
                    // Data berhasil diperbarui di Firestore.
                    Toast.makeText(nomor_hp.this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show();

                    // Perbarui data di RecyclerView
                    int position = dataModels.indexOf(dataModel);
                    if (position != -1) {
                        dataModel.setName(updatedName); // Update name in DataModel
                        dataModel.setPhoneNumber(updatedPhone); // Update phoneNumber in DataModel
                        adapter.notifyItemChanged(position);
                    }
                })
                .addOnFailureListener(e -> {
                    // Menangani kesalahan jika data tidak dapat diperbarui di Firestore.
                    Toast.makeText(nomor_hp.this, "Gagal memperbarui data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONTACT && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            String[] projection = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
            Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int nameColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                String name = cursor.getString(nameColumn);
                String phoneNumber = cursor.getString(numberColumn);

                cursor.close();

                // Simpan data ke Firestore
                if (name != null && phoneNumber != null) {
                    saveDataToFirestore(name, phoneNumber);
                }
            }
        }
    }

    private String getContactNumber(Uri contactUri) {
        String phoneNumber = null;
        String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int numberColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            phoneNumber = cursor.getString(numberColumn);
            cursor.close();
        }

        return phoneNumber;
    }

    private void saveDataToFirestore(String name, String phoneNumber) {
        CollectionReference contactsRef = db.collection("contacts");

        DataModel dataModel = new DataModel(name, phoneNumber, userEmail, null);

        // Simpan data ke Firestore
        contactsRef.add(dataModel)
                .addOnSuccessListener(documentReference -> {
                    // Setelah berhasil ditambahkan ke Firestore, kita akan memperbarui documentId
                    String documentId = documentReference.getId();

                    // Mengambil DataModel yang sesuai dengan documentReference
                    DataModel updatedDataModel = new DataModel(name, phoneNumber, userEmail, documentId);

                    // Tambahkan data yang telah diperbarui ke dataModels dan perbarui RecyclerView
                    dataModels.add(updatedDataModel);
                    adapter.notifyDataSetChanged();

                    Toast.makeText(nomor_hp.this, "Kontak ditambahkan", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(nomor_hp.this, "Gagal menambahkan kontak: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void loadUserContactsFromFirestore(String userEmail) {
        CollectionReference contactsRef = db.collection("contacts");

        Query query = contactsRef.whereEqualTo("email", userEmail);

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    dataModels.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String documentId = document.getId();
                        String name = document.getString("name");
                        String phoneNumber = document.getString("phoneNumber");
                        DataModel dataModel = new DataModel(name, phoneNumber, userEmail, documentId);
                        dataModels.add(dataModel);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(nomor_hp.this, "Gagal mengambil data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


}
