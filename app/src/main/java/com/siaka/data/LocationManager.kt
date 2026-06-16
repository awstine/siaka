package com.siaka.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMillis: Long = 10000): Flow<Location> = callbackFlow {
        // First, try to get the last known location immediately
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let { trySend(it) }
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMillis
        ).apply {
            setMinUpdateIntervalMillis(intervalMillis / 2)
            setWaitForAccurateLocation(true) // Wait for a more precise fix
        }.build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    trySend(location) // Safely emit location into the Flow stream
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            callback,
            Looper.getMainLooper()
        )

        // Clean up and stop receiving location updates when the Flow collector drops out
        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}