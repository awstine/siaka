package com.siaka.data.api

import retrofit2.Response
import retrofit2.http.*

data class RouteRequest(
    val coordinates: List<List<Double>>,
    val instructions: Boolean = false,
    val preference: String = "recommended"
)

data class RouteResponse(
    val features: List<Feature>,
    val metadata: Metadata
)

data class Feature(
    val geometry: Geometry,
    val properties: Properties
)

data class Geometry(
    val coordinates: List<List<Double>>
)

data class Properties(
    val summary: Summary
)

data class Summary(
    val distance: Double,
    val duration: Double
)

data class Metadata(
    val query: Query
)

data class Query(
    val coordinates: List<List<Double>>
)

interface OpenRouteServiceApi {
    @Headers("Accept: application/json")
    @POST("/v2/directions/cycling-road")
    suspend fun getRoute(
        @Header("Authorization") apiKey: String,
        @Body request: RouteRequest
    ): Response<RouteResponse>
}
