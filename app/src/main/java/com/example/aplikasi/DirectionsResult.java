// DirectionsResult.java
package com.example.aplikasi;

import com.google.gson.annotations.SerializedName;

public class DirectionsResult {

    @SerializedName("routes")
    public DirectionsRoute[] routes;

}
