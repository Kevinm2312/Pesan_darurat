package com.example.aplikasi;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DirectionsApiService {
    @GET("maps/api/directions/json")
    Call<DirectionsResult> getDirections(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("key") String apiKey
    );
}

