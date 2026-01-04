package com.example.minimap.model

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices


// File to get Phone's Location in order to retrieve latitude and longitude


class WifiScannerViewModel : ViewModel() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    // Initialize FusedLocationProviderClient
    fun initLocationClient(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    // Retrieve last known position
    fun fetchLastLocation(onSuccess: (Location) -> Unit) {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        currentLocation = it
                        onSuccess(it)
                    }
                }
        } catch (e: SecurityException) {
            Log.e("GPS", "Permission denied: ${e.message}")
        }
    }
}