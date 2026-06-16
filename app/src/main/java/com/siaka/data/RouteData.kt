package com.siaka.data

data class RouteStep(
    val instruction: String,
    val distanceMeters: Double,
    val location: LocationPoint,
    val roadName: String? = null
)

data class RouteData(
    val points: List<LocationPoint>,
    val steps: List<RouteStep>,
    val totalDistanceKm: Double,
    val totalDurationMinutes: Int
)
