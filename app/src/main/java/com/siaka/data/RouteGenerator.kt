package com.siaka.data

import com.siaka.BuildConfig
import com.siaka.data.api.OpenRouteServiceApi
import com.siaka.data.api.RouteRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteGenerator @Inject constructor() {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openrouteservice.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(OpenRouteServiceApi::class.java)
    
    private val apiKey = BuildConfig.ORS_API_KEY

    suspend fun generateLoopRoute(
        centerPoint: LocationPoint,
        targetDistanceKm: Double
    ): List<LocationPoint> {
        // Strategy: Create waypoints around the center to form a loop
        // OpenRouteService will snap these to actual roads
        
        val radius = (targetDistanceKm / (2 * Math.PI)) * 0.009 // Convert to degrees
        val waypoints = mutableListOf<List<Double>>()
        val numPoints = 8 // Number of points to shape the route
        
        // Generate waypoints in a circle
        for (i in 0 until numPoints) {
            val angle = 2 * Math.PI * i / numPoints
            val lat = centerPoint.latitude + (radius * Math.sin(angle))
            val lon = centerPoint.longitude + (radius * Math.cos(angle))
            waypoints.add(listOf(lon, lat)) // Note: ORS uses [lon, lat]
        }
        
        // Close the loop
        waypoints.add(waypoints[0])
        
        return try {
            val response = api.getRoute(
                apiKey = apiKey,
                request = RouteRequest(
                    coordinates = waypoints
                )
            )
            
            if (response.isSuccessful) {
                response.body()?.features?.firstOrNull()?.geometry?.coordinates?.map { coord ->
                    LocationPoint(
                        latitude = coord[1],  // lat is second element
                        longitude = coord[0]  // lon is first element
                    )
                } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
