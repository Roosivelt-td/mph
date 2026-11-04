package com.example.mph.ui.map

import com.example.mph.data.model.LocationPoint
import com.example.mph.data.model.Route
import com.google.android.gms.maps.model.LatLng

/**
 * Estado UI de la pantalla de Mapas y Registro de Rutas.
 */
data class MapState(
    val isTracking: Boolean = false,
    val currentLocation: LatLng? = null,
    val currentRoutePoints: List<LocationPoint> = emptyList(),
    val totalDistanceMeters: Double = 0.0,
    val savedRoutes: List<Route> = emptyList(),
    val selectedRoute: Route? = null,
    val isPermissionGranted: Boolean = false,
    val trackingStartTime: Long? = null,
    val errorMessage: String? = null
)
