package com.siaka.data

import android.util.Log
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class SimpleRouteGenerator @Inject constructor() {
    
    companion object {
        private const val TAG = "SimpleRouteGenerator"
        // Approximate km per degree of latitude/longitude
        private const val KM_PER_DEGREE_LAT = 111.32
    }
    
    fun generateLoopRoute(
        centerPoint: LocationPoint,
        targetDistanceKm: Double
    ): List<LocationPoint> {
        Log.d(TAG, "Generating loop route: center=${centerPoint.latitude},${centerPoint.longitude}, distance=${targetDistanceKm}km")
        
        val route = mutableListOf<LocationPoint>()
        val numWaypoints = 12 // More waypoints = smoother loop
        
        // Calculate radius in degrees
        val radiusKm = targetDistanceKm / (2 * Math.PI) // radius of circle
        val radiusLatDeg = radiusKm / KM_PER_DEGREE_LAT // radius in latitude degrees
        val radiusLonDeg = radiusKm / (KM_PER_DEGREE_LAT * cos(Math.toRadians(centerPoint.latitude))) // radius in longitude degrees (adjusted for latitude)
        
        // Create an irregular but road-like loop
        val random = Random(centerPoint.latitude.toBits()) // Deterministic seed based on location
        
        // Add start point
        route.add(centerPoint)
        
        // Generate waypoints that approximate the desired distance
        for (i in 1 until numWaypoints) {
            val angle = 2 * Math.PI * i / numWaypoints
            
            // Add some randomness to make it look like a real route
            val randomOffsetLat = (random.nextDouble() - 0.5) * radiusLatDeg * 0.3
            val randomOffsetLon = (random.nextDouble() - 0.5) * radiusLonDeg * 0.3
            
            val lat = centerPoint.latitude + (radiusLatDeg * sin(angle)) + randomOffsetLat
            val lon = centerPoint.longitude + (radiusLonDeg * cos(angle)) + randomOffsetLon
            
            route.add(LocationPoint(lat, lon))
        }
        
        // Close the loop by adding start point again
        route.add(centerPoint)
        
        Log.d(TAG, "Generated route with ${route.size} points")
        return route
    }
}
