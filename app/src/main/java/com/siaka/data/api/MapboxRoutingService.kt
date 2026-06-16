package com.siaka.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class MapboxRouteResponse(
    val code: String,
    val routes: List<MapboxRoute>?,
    val waypoints: List<MapboxWaypoint>?
)

data class MapboxRoute(
    val distance: Double,
    val duration: Double,
    val geometry: String, // Encoded polyline
    val legs: List<MapboxLeg>?
)

data class MapboxLeg(
    val distance: Double,
    val duration: Double,
    val summary: String,
    val steps: List<MapboxStep>?
)

data class MapboxStep(
    val distance: Double,
    val duration: Double,
    val geometry: String,
    val name: String,
    @SerializedName("maneuver") val maneuver: MapboxManeuver
)

data class MapboxManeuver(
    val instruction: String,
    val type: String,
    val modifier: String?
)

data class MapboxWaypoint(
    val location: List<Double>,
    val name: String
)

interface MapboxRoutingService {
    @GET("directions/v5/mapbox/cycling/{coordinates}")
    suspend fun getCyclingRoute(
        @Path("coordinates") coordinates: String,
        @Query("access_token") accessToken: String,
        @Query("geometries") geometries: String = "polyline6",
        @Query("overview") overview: String = "full",
        @Query("steps") steps: Boolean = true
    ): Response<MapboxRouteResponse>
}
