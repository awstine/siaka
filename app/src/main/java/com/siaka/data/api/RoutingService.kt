package com.siaka.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

data class OSRMResponse(
    val code: String,
    val routes: List<OSRMRoute>?,
    val waypoints: List<OSRMWaypoint>?
)

data class OSRMRoute(
    val distance: Double,
    val duration: Double,
    val geometry: String  // Encoded polyline
)

data class OSRMWaypoint(
    val location: List<Double>,
    val name: String
)

interface RoutingService {
    @GET("route/v1/bike/{coordinates}")
    suspend fun getBikeRoute(
        @Path("coordinates") coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline",
        @Query("alternatives") alternatives: Boolean = false,
        @Query("steps") steps: Boolean = false
    ): Response<OSRMResponse>
}
