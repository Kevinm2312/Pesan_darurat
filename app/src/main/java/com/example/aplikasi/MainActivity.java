package com.example.aplikasi;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Date;

public class MainActivity extends FragmentActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;

    private static final int LOCATION_UPDATE_INTERVAL = 1000; // Update every second
    private static final String USER_LOCATIONS_COLLECTION = "lokasi_pengguna";

    private Handler locationUpdateHandler = new Handler();
    private LocationCallback locationCallback;
    private boolean isLocationUpdatesActive = false;
    private String userEmail; // Variable to store the user email
    private ImageButton openMapButton;

    private ComponentName mDeviceAdmin;
    private DevicePolicyManager mDevicePolicyManager;
    private boolean isToggleLocationUpdatesInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.koordinat);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();

        // Ambil email pengguna dari intent
        userEmail = getIntent().getStringExtra("userEmail");

        openMapButton = findViewById(R.id.btn_mulai);

        // Setel keadaan awal dan sumber gambar
        isLocationUpdatesActive = false;
        openMapButton.setImageResource(R.drawable.button_on); // Mengasumsikan 'button_off' adalah gambar awal

        openMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleLocationUpdates();
            }
        });

        mDeviceAdmin = new ComponentName(this, MyDeviceAdminReceiver.class);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Periksa apakah administrator perangkat aktif
        if (!mDevicePolicyManager.isAdminActive(mDeviceAdmin)) {
            // Jika tidak aktif, munculkan pesan untuk mengaktifkannya
            showToast("Silakan aktifkan administrator perangkat");
        }
    }


    private void toggleLocationUpdates() {
        Log.d("ToggleLocation", "Before toggle, isLocationUpdatesActive: " + isLocationUpdatesActive);

        if (!isToggleLocationUpdatesInProgress) {
            isToggleLocationUpdatesInProgress = true;

            if (isLocationUpdatesActive) {
                stopLocationUpdate();

                openMapButton.setImageResource(R.drawable.button_on); // Ganti dengan gambar "location off"
            } else {
                startLocationUpdates();
                lockScreen();
                openMapButton.setImageResource(R.drawable.button_off); // Ganti dengan gambar "location on"
            }

            // Tambahkan log untuk memeriksa nilai isLocationUpdatesActive setelah beberapa saat
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d("ToggleLocation", "After delay, isLocationUpdatesActive: " + isLocationUpdatesActive);
                    isToggleLocationUpdatesInProgress = false;
                }
            }, 1000); // Ganti dengan waktu yang sesuai
        }
    }

    private void startLocationUpdates() {
        Log.d("ToggleLocation", "startLocationUpdates called");

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(LOCATION_UPDATE_INTERVAL);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d("ToggleLocation", "onLocationResult called");
                if (locationResult != null) {
                    onLocationChanged(locationResult.getLastLocation());
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            isLocationUpdatesActive = true;
            Log.d("ToggleLocation", "Location updates started. isLocationUpdatesActive: true");
        } catch (SecurityException e) {
            e.printStackTrace();
            Log.e("ToggleLocation", "SecurityException: " + e.getMessage());
        }
    }




    private void stopLocationUpdate() {
        try {
            // Use the same locationCallback instance for removal
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isLocationUpdatesActive = false;
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        locationUpdateHandler.removeCallbacksAndMessages(null);
    }

    private void onLocationChanged(Location location) {
        Log.d("ToggleLocation", "onLocationChanged called");
        if (location != null) {
            // Use userEmail to fetch the corresponding user ID
            fetchUserIdFromFirestore(userEmail, new UserIdCallback() {
                @Override
                public void onUserIdFetched(String userId) {
                    // Now you have the userId, and you can use it as needed
                    String documentId = "user_location_" + userId;
                    GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    LocationData locationData = new LocationData(geoPoint, new Date());

                    CollectionReference locationsRef = db.collection(USER_LOCATIONS_COLLECTION);
                    locationsRef.document(documentId).set(locationData);

                    // Remove old locations (customize this logic as needed)
                    removeOldLocations(locationsRef, documentId);
                }

                @Override
                public void onError(String errorMessage) {
                    // Handle the error
                }
            });
        }
    }

    private void fetchUserIdFromFirestore(String userEmail, UserIdCallback callback) {
        CollectionReference usersRef = db.collection("USERS");

        usersRef.whereEqualTo("email", userEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String userId = document.getString("userId");
                            callback.onUserIdFetched(userId);
                            return; // Assuming there is only one user with the given email
                        }
                        callback.onError("User not found");
                    } else {
                        callback.onError("Error fetching user data");
                    }
                });
    }

    private void removeOldLocations(CollectionReference locationsRef, String documentId) {
        Date satuJamYangLalu = new Date(System.currentTimeMillis() - 3600 * 1000);
        Query oldLocationsQuery = locationsRef
                .document(documentId)
                .collection("lokasi")  // Change to the appropriate sub-collection name
                .whereLessThan("timestamp", satuJamYangLalu);

        oldLocationsQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    document.getReference().delete();
                }
            } else {
                // Handle errors
            }
        });
    }

    private void lockScreen() {
        if (mDevicePolicyManager.isAdminActive(mDeviceAdmin)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mDevicePolicyManager.lockNow();
            } else {
                mDevicePolicyManager.lockNow();
            }
        } else {
            showToast("Please activate device administrator");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Interface for the callback when user ID is fetched
    interface UserIdCallback {
        void onUserIdFetched(String userId);

        void onError(String errorMessage);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }
}
