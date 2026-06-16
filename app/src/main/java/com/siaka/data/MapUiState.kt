package com.siaka.data

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val bearing: Float? = null
)

data class MapUiState(
    val userLocation: LocationPoint? = null,
    val isLocationPermissionGranted: Boolean = false,
    val generatedRoutePoints: List<LocationPoint> = emptyList(),
    val routeSteps: List<RouteStep> = emptyList(),
    val isLoadingRoute: Boolean = false,
    val isMyLocationEnabled: Boolean = false,
    val distanceInput: String = "1.4",
    val showDistanceDialog: Boolean = false,
    val shouldCenterOnLocation: Boolean = false,
    val isNavigating: Boolean = false,
    val isRouteGenerated: Boolean = false,
    val error: String? = null,
    
    // Navigation details
    val estimatedTimeMinutes: Int = 45,
    val remainingMinutes: Int = 45,
    val remainingDistanceKm: Double = 12.4,
    val eta: String = "4:25 PM",
    val etaTime: String = "4:25 PM",
    val routeDescription: String = "Via Skyline Drive and Greenway Loop",
    val nextInstruction: String = "Turn Right on Pine St",
    val distanceToNextInstructionMeters: Int = 200
)
