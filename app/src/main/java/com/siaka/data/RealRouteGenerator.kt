package com.siaka.data

import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.siaka.data.api.RoutingService
import com.siaka.data.api.OSRMResponse
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class RealRouteGenerator @Inject constructor() {
    
    companion object {
        private const val TAG = "RealRouteGenerator"
        // Using free public OSRM server (demo only - get your own for production)
        private const val OSRM_BASE_URL = "https://router.project-osrm.org/"
        private const val KM_PER_DEGREE_LAT = 111.32
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(OSRM_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val routingService = retrofit.create(RoutingService::class.java)
    
    suspend fun generateLoopRoute(
        centerPoint: LocationPoint,
        targetDistanceKm: Double
    ): List<LocationPoint> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating loop route: center=${centerPoint.latitude},${centerPoint.longitude}, distance=${targetDistanceKm}km")
            
            // Strategy: Create waypoints around the center to form a loop
            // OSRM will snap these to actual roads
            
            val waypoints = generateWaypoints(centerPoint, targetDistanceKm)
            val coordinatesString = waypoints.joinToString(";") { "${it.longitude},${it.latitude}" }
            
            Log.d(TAG, "Requesting route with coordinates: $coordinatesString")
            
            val response = routingService.getBikeRoute(
                coordinates = coordinatesString
            )
            
            if (response.isSuccessful && response.body()?.code == "Ok") {
                val route = response.body()?.routes?.firstOrNull()
                if (route != null) {
                    val points = decodePolyline(route.geometry, 5)
                    val actualDistance = route.distance / 1000.0 // Convert to km
                    
                    Log.d(TAG, "Route found: ${points.size} points, distance: ${"%.2f".format(actualDistance)}km")
                    
                    // VERY lenient check: Allow up to 200% mismatch to prioritize road-based routes
                    // over geometric fallbacks. We want roads even if distance isn't perfect.
                    if (abs(actualDistance - targetDistanceKm) / targetDistanceKm < 2.0) {
                        return@withContext points
                    } else {
                        Log.d(TAG, "Distance mismatch too high: got ${"%.2f".format(actualDistance)}km, wanted ${targetDistanceKm}km")
                    }
                }
            } else {
                Log.e(TAG, "OSRM Error: ${response.code()} - ${response.message()}")
            }
            
            // Fallback: generate approximate route if OSRM fails
            generateApproximateLoopRoute(centerPoint, targetDistanceKm)
            
        } catch (e: Exception) {
            Log.e(TAG, "Route generation failed", e)
            // Fallback to approximate route
            generateApproximateLoopRoute(centerPoint, targetDistanceKm)
        }
    }
    
    private fun generateWaypoints(center: LocationPoint, targetDistanceKm: Double): List<LocationPoint> {
        // Reduced number of waypoints (5 instead of 8) makes it easier for OSRM to find a shorter route
        val numWaypoints = 5
        
        // Circuity factor: road distance is typically ~1.5-2.0x straight line distance for loops.
        // We reduce the radius so the road-mapped route ends up closer to targetDistanceKm.
        val circuityFactor = 2.2
        val adjustedTargetDistance = targetDistanceKm / circuityFactor
        
        val radiusKm = adjustedTargetDistance / (2 * Math.PI)
        val radiusLatDeg = radiusKm / KM_PER_DEGREE_LAT
        val radiusLonDeg = radiusKm / (KM_PER_DEGREE_LAT * cos(Math.toRadians(center.latitude)))
        
        val waypoints = mutableListOf<LocationPoint>()
        
        // Add a random rotation to the starting angle so the loop direction changes every time
        val randomRotation = Math.random() * 2 * Math.PI
        
        // Generate waypoints in a circle around the center
        for (i in 0 until numWaypoints) {
            val angle = (2 * Math.PI * i / numWaypoints) + randomRotation
            
            // Add a small random jitter to each waypoint distance to explore different road combinations
            val distanceJitter = 0.9 + (Math.random() * 0.2) // Jitter between 0.9 and 1.1
            
            val lat = center.latitude + (radiusLatDeg * sin(angle) * distanceJitter)
            val lon = center.longitude + (radiusLonDeg * cos(angle) * distanceJitter)
            
            waypoints.add(LocationPoint(lat, lon))
        }
        
        // Close the loop by adding the first waypoint
        waypoints.add(waypoints.first())
        
        return waypoints
    }
    
    private fun generateApproximateLoopRoute(
        centerPoint: LocationPoint,
        targetDistanceKm: Double
    ): List<LocationPoint> {
        Log.d(TAG, "Using approximate route generation")
        
        val route = mutableListOf<LocationPoint>()
        val numWaypoints = 20 // More points for smoother route
        
        val radiusKm = targetDistanceKm / (2 * Math.PI)
        val radiusLatDeg = radiusKm / KM_PER_DEGREE_LAT
        val radiusLonDeg = radiusKm / (KM_PER_DEGREE_LAT * cos(Math.toRadians(centerPoint.latitude)))
        
        val randomRotation = Math.random() * 2 * Math.PI
        
        // Create a route that follows a rough circular pattern with realistic variations
        for (i in 0..numWaypoints) {
            val angle = (2 * Math.PI * i / numWaypoints) + randomRotation
            
            // Add sinusoidal variations to simulate road curves
            val variation = sin(angle * 3) * radiusLatDeg * 0.2
            
            val lat = centerPoint.latitude + (radiusLatDeg * sin(angle)) + variation
            val lon = centerPoint.longitude + (radiusLonDeg * cos(angle)) + variation
            
            route.add(LocationPoint(lat, lon))
        }
        
        return route
    }
    
    // Decode OSRM polyline encoding
    private fun decodePolyline(encoded: String, precision: Int = 5): List<LocationPoint> {
        val points = mutableListOf<LocationPoint>()
        var index = 0
        var lat = 0.0
        var lon = 0.0
        
        while (index < encoded.length) {
            // Decode latitude
            var result = 0
            var shift = 0
            var byte: Int
            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)
            val deltaLat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += deltaLat
            
            // Decode longitude
            result = 0
            shift = 0
            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)
            val deltaLon = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lon += deltaLon
            
            points.add(LocationPoint(lat / Math.pow(10.0, precision.toDouble()), lon / Math.pow(10.0, precision.toDouble())))
        }
        
        return points
    }
}
