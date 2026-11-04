package com.example.mph.data.model

import com.google.android.gms.maps.model.LatLng

/**
 * Representa un punto de ubicación con coordenadas geográficas y metadatos.
 */
data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val speed: Float = 0f,
    val altitude: Double = 0.0
) {
    /**
     * Convierte la ubicación al formato LatLng requerido por Google Maps.
     */
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}
