package com.example.aplikasi; // Ganti ini dengan nama paket sesuai proyek Anda

import com.google.firebase.firestore.GeoPoint;
import java.util.Date;

public class LocationData {
    private GeoPoint geoPoint;
    private Date timestamp;

    public LocationData() {
        // Diperlukan oleh Firestore untuk deserialisasi objek
    }

    public LocationData(GeoPoint geoPoint, Date timestamp) {
        this.geoPoint = geoPoint;
        this.timestamp = timestamp;
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public void setGeoPoint(GeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
