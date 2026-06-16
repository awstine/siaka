package com.siaka.data

import android.content.Context
import android.util.Log
import com.siaka.BuildConfig
import com.siaka.data.api.MapboxRoutingService
import com.siaka.data.api.MapboxStep
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class MapboxRouteGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MapboxRouteGenerator"
        private const val BASE_URL = "https://api.mapbox.com/"
        private const val KM_PER_DEGREE_LAT = 111.32
    }

    private val accessToken: String by lazy {
        BuildConfig.MAPBOX_ACCESS_TOKEN
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val routingService = retrofit.create(MapboxRoutingService::class.java)

    suspend fun generateLoopRoute(
        centerPoint: LocationPoint,
        targetDistanceKm: Double
    ): RouteData? = withContext(Dispatchers.IO) {
        try {
            val waypoints = generateCircularWaypoints(centerPoint, targetDistanceKm)
            val coordinatesString = waypoints.joinToString(";") { "${it.longitude},${it.latitude}" }

            val response = routingService.getCyclingRoute(
                coordinates = coordinatesString,
                accessToken = accessToken
            )

            if (response.isSuccessful && response.body()?.code == "Ok") {
                val route = response.body()?.routes?.firstOrNull()
                if (route != null) {
                    val points = decodePolyline(route.geometry, 6)
                    val steps = route.legs?.flatMap { leg ->
                        leg.steps?.map { step ->
                            mapToRouteStep(step)
                        } ?: emptyList()
                    } ?: emptyList()
                    
                    return@withContext RouteData(
                        points = points,
                        steps = steps,
                        totalDistanceKm = route.distance / 1000.0,
                        totalDurationMinutes = (route.duration / 60.0).toInt()
                    )
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Mapbox Route generation failed", e)
            null
        }
    }

    private fun mapToRouteStep(step: MapboxStep): RouteStep {
        val maneuver = step.maneuver
        val hasRoadName = step.name.isNotEmpty()
        
        val modifier = maneuver.modifier?.replace("-", " ")?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: ""
        
        val instruction = if (hasRoadName) {
            if (maneuver.type == "turn" || maneuver.type == "on ramp" || maneuver.type == "merge") {
                "Turn $modifier on ${step.name}"
            } else if (maneuver.type == "depart") {
                "Head $modifier on ${step.name}"
            } else {
                maneuver.instruction
            }
        } else {
            if (maneuver.type == "turn") {
                "Turn $modifier".trim()
            } else if (maneuver.type == "depart") {
                "Head $modifier".trim()
            } else {
                maneuver.instruction
            }
        }

        return RouteStep(
            instruction = instruction,
            distanceMeters = step.distance,
            location = decodePolyline(step.geometry, 6).firstOrNull() ?: LocationPoint(0.0, 0.0),
            roadName = step.name.takeIf { it.isNotEmpty() }
        )
    }

    private fun generateCircularWaypoints(center: LocationPoint, targetDistanceKm: Double): List<LocationPoint> {
        val circuityFactor = 2.2
        val adjustedDistance = targetDistanceKm / circuityFactor
        val radiusKm = adjustedDistance / (2 * Math.PI)
        
        val radiusLatDeg = radiusKm / KM_PER_DEGREE_LAT
        val radiusLonDeg = radiusKm / (KM_PER_DEGREE_LAT * cos(Math.toRadians(center.latitude)))
        
        val randomRotation = Math.random() * 2 * Math.PI
        val waypoints = mutableListOf<LocationPoint>()
        
        val numPoints = 5
        for (i in 0 until numPoints) {
            val angle = (2 * Math.PI * i / numPoints) + randomRotation
            val lat = center.latitude + (radiusLatDeg * sin(angle))
            val lon = center.longitude + (radiusLonDeg * cos(angle))
            waypoints.add(LocationPoint(lat, lon))
        }
        waypoints.add(waypoints.first()) // Close the loop
        return waypoints
    }

    private fun decodePolyline(encoded: String, precision: Int): List<LocationPoint> {
        val points = mutableListOf<LocationPoint>()
        var index = 0
        var lat = 0
        var lon = 0

        while (index < encoded.length) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlon = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lon += dlon

            val factor = Math.pow(10.0, precision.toDouble())
            points.add(LocationPoint(lat.toDouble() / factor, lon.toDouble() / factor))
        }
        return points
    }
}
