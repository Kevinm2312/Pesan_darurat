package com.example.aplikasi;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class depan extends AppCompatActivity {
    private ImageView imageView;
    private Button buttonChangeImage;

    private int currentImageIndex = 1;
    private int maxImageIndex = 3;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int ADMIN_PERMISSION_REQUEST_CODE = 2;

    private ComponentName mDeviceAdmin;
    private DevicePolicyManager mDevicePolicyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.depan);

        mDeviceAdmin = new ComponentName(this, MyDeviceAdminReceiver.class);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Meminta izin administrator
        requestAdminPermission();

        imageView = findViewById(R.id.gambar);
        buttonChangeImage = findViewById(R.id.tombol);

        // Cek apakah izin lokasi sudah diberikan
        if (checkLocationPermission()) {
            // Izin sudah diberikan, lanjutkan dengan setup tampilan
            setupView();
        } else {
            // Izin belum diberikan, minta izin
            requestLocationPermission();
        }
    }

    private void setupView() {
        buttonChangeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentImageIndex++;
                if (currentImageIndex > maxImageIndex) {
                    Intent intent = new Intent(depan.this, LoginActivity.class);
                    startActivity(intent);
                    // Pindah ke halaman login jika semua gambar sudah ditampilkan
                    // Ganti aktivitas yang sesuai dengan halaman login Anda
                } else {
                    int imageResource = getResources().getIdentifier("gambardpn" + currentImageIndex, "drawable", getPackageName());
                    imageView.setImageResource(imageResource);
                }

                // Ubah teks tombol menjadi "Masuk" jika tampilan terakhir
                if (currentImageIndex == maxImageIndex) {
                    buttonChangeImage.setText("Masuk");
                }
            }
        });
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    private void requestAdminPermission() {
        if (!mDevicePolicyManager.isAdminActive(mDeviceAdmin)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Activate device administrator");
            startActivityForResult(intent, ADMIN_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Izin diberikan, lanjutkan dengan setup tampilan
                setupView();
            } else {
                // Izin tidak diberikan, mungkin berikan pesan atau tindakan yang sesuai
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADMIN_PERMISSION_REQUEST_CODE) {
            // Cek apakah izin administrator telah diberikan
            if (mDevicePolicyManager.isAdminActive(mDeviceAdmin)) {
                // Izin administrator diberikan
            } else {
                // Izin administrator tidak diberikan, mungkin berikan pesan atau tindakan yang sesuai
            }
        }
    }
}
