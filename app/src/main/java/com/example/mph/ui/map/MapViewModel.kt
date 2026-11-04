package com.example.mph.ui.map

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mph.data.model.LocationPoint
import com.example.mph.data.model.Route
import com.example.mph.data.repository.PersistentRouteRepository
import com.example.mph.data.repository.RouteRepository
import com.example.mph.location.LocationService
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RouteRepository = PersistentRouteRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(MapState())
    val uiState: StateFlow<MapState> = _uiState.asStateFlow()

    init {
        restoreActiveTrackingState()
        observeLocationUpdates()
        observeSavedRoutes()
    }

    /**
     * Restaura el estado de rastreo activo si la aplicación fue cerrada accidentalmente por el usuario.
     */
    private fun restoreActiveTrackingState() {
        val activeState = repository.getActiveTracking() ?: return
        val originPoint = activeState.originPoint
        val points = activeState.points

        _uiState.update {
            it.copy(
                isTracking = true,
                currentRoutePoints = points,
                totalDistanceMeters = activeState.totalDistanceMeters,
                trackingStartTime = activeState.startTime,
                currentLocation = points.lastOrNull()?.toLatLng() ?: originPoint.toLatLng()
            )
        }
    }

    private fun observeLocationUpdates() {
        viewModelScope.launch {
            LocationService.locationUpdates.collect { location ->
                onNewLocation(location)
            }
        }
    }

    private fun observeSavedRoutes() {
        viewModelScope.launch {
            repository.getSavedRoutes().collect { routes ->
                _uiState.update { it.copy(savedRoutes = routes) }
            }
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(isPermissionGranted = isGranted) }
    }

    private fun onNewLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        val newPoint = LocationPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            speed = location.speed,
            altitude = location.altitude
        )

        _uiState.update { currentState ->
            val updatedPoints = if (currentState.isTracking) {
                currentState.currentRoutePoints + newPoint
            } else {
                currentState.currentRoutePoints
            }

            val addedDistance = if (currentState.isTracking && currentState.currentRoutePoints.isNotEmpty()) {
                Route.calculateDistanceMeters(currentState.currentRoutePoints.last(), newPoint)
            } else 0.0

            currentState.copy(
                currentLocation = latLng,
                currentRoutePoints = updatedPoints,
                totalDistanceMeters = currentState.totalDistanceMeters + addedDistance
            )
        }
    }

    fun startTracking() {
        _uiState.update {
            it.copy(
                isTracking = true,
                currentRoutePoints = emptyList(),
                totalDistanceMeters = 0.0,
                selectedRoute = null,
                trackingStartTime = System.currentTimeMillis()
            )
        }
    }

    fun stopAndSaveTracking(routeName: String) {
        val currentState = _uiState.value
        if (!currentState.isTracking) return

        val startTime = currentState.trackingStartTime ?: System.currentTimeMillis()
        val duration = (System.currentTimeMillis() - startTime) / 1000

        val newRoute = Route(
            name = routeName.ifBlank { "Ruta ${currentState.savedRoutes.size + 1}" },
            points = currentState.currentRoutePoints,
            distanceMeters = currentState.totalDistanceMeters,
            durationSeconds = duration
        )

        viewModelScope.launch {
            repository.saveRoute(newRoute)
            _uiState.update {
                it.copy(
                    isTracking = false,
                    trackingStartTime = null,
                    selectedRoute = newRoute
                )
            }
        }
    }

    fun cancelTracking() {
        repository.clearActiveTracking()
        _uiState.update {
            it.copy(
                isTracking = false,
                currentRoutePoints = emptyList(),
                totalDistanceMeters = 0.0,
                trackingStartTime = null
            )
        }
    }

    fun selectRoute(route: Route?) {
        _uiState.update { it.copy(selectedRoute = route) }
    }
}
