package com.example.aplikasi;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private MapView mapView;
    private FirebaseFirestore db;
    private DirectionsApiService apiService;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int LOCATION_UPDATE_INTERVAL = 1000; // Update every second
    private static final String USER_LOCATIONS_COLLECTION = "lokasi_pengguna";
    private static final String API_KEY = "AIzaSyCvZxUffYynPKPjAAu_B8uAP0Ia4hrzTpk";

    private Polyline userPathPolyline;
    private List<LatLng> userPathPoints = new ArrayList<>();
    private Marker myLocationMarker;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        db = FirebaseFirestore.getInstance();
        apiService = RetrofitClient.getClient().create(DirectionsApiService.class);

        tampilkanDialogInputUserId();
    }

    private void tampilkanDialogInputUserId() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Masukkan ID Pengguna");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                userId = input.getText().toString();
                if (!TextUtils.isEmpty(userId)) {
                    inisialisasiMapDanLokasi(userId);
                } else {
                    tampilkanDialogInputUserId();
                }
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void inisialisasiMapDanLokasi(String userId) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(null);
        mapView.onResume();
        mapView.getMapAsync(this);

        tampilkanLokasiPengguna(userId);
        startLocationUpdates();
    }

    private void tampilkanLokasiPengguna(String userId) {
        CollectionReference locationsRef = db.collection(USER_LOCATIONS_COLLECTION);
        DocumentReference userDocumentRef = locationsRef.document("user_location_" + userId);
        userDocumentRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                GeoPoint geoPoint = documentSnapshot.getGeoPoint("geoPoint");
                if (geoPoint != null) {
                    LatLng location = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                    userPathPoints.add(location);
                    updatePolyline();

                    if (userPathPoints.size() == 1) {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
                    }
                }
            }
        });
    }

    private void updatePolyline() {
        if (userPathPolyline != null) {
            userPathPolyline.setPoints(userPathPoints);
        } else {
            userPathPolyline = googleMap.addPolyline(new PolylineOptions()
                    .width(5)
                    .color(Color.BLUE)
                    .addAll(userPathPoints));
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        if (checkLocationPermission()) {
            googleMap.setMyLocationEnabled(true);
        } else {
            requestLocationPermission();
        }

        userPathPolyline = googleMap.addPolyline(new PolylineOptions()
                .width(5)
                .color(Color.BLUE));

        if (!TextUtils.isEmpty(userId)) {
            tampilkanLokasiPengguna(userId);
        } else {
            Toast.makeText(this, "ID Pengguna Kosong", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void startLocationUpdates() {
        if (checkLocationPermission()) {
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(LOCATION_UPDATE_INTERVAL);

            LocationCallback locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null) {
                        onLocationChanged(locationResult.getLastLocation());
                    }
                }
            };

            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private void onLocationChanged(Location location) {
        if (location != null) {
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            userPathPoints.add(currentLocation);
            updatePolyline();

            if (userPathPoints.size() == 1) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        mapView.onDestroy();
    }

    private void stopLocationUpdates() {
        if (checkLocationPermission()) {
            try {
                fusedLocationClient.removeLocationUpdates(new LocationCallback() {
                });
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
