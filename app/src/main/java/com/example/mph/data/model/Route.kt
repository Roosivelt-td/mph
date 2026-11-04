package com.example.mph.data.model

import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

/**
 * Modelo de datos que representa una ruta registrada compuesta por una lista de puntos de ubicación.
 */
data class Route(
    val id: String = "",
    val name: String = "",
    val points: List<LocationPoint> = emptyList(),
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Retorna la lista de coordenadas LatLng para dibujarse como Polylines en el Mapa.
     */
    fun toLatLngList(): List<LatLng> {
        return points.map { it.toLatLng() }
    }

    /**
     * Calcula la distancia acumulada en kilómetros.
     */
    val distanceKm: Double
        get() = String.format("%.2f", distanceMeters / 1000.0).toDouble()

    companion object {
        /**
         * Calcula la distancia entre dos puntos geográficos usando la fórmula de Haversine.
         */
        fun calculateDistanceMeters(start: LocationPoint, end: LocationPoint): Double {
            val earthRadius = 6371000.0 // metros
            val dLat = Math.toRadians(end.latitude - start.latitude)
            val dLng = Math.toRadians(end.longitude - start.longitude)
            val a = sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) *
                    sin(dLng / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadius * c
        }
    }
}
