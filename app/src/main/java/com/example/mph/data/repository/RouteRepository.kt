package com.example.mph.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.mph.data.model.LocationPoint
import com.example.mph.data.model.Route
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class ActiveTrackingState(
    val originPoint: LocationPoint,
    val points: List<LocationPoint>,
    val startTime: Long,
    val totalDistanceMeters: Double
)

interface RouteRepository {
    fun getSavedRoutes(): Flow<List<Route>>
    suspend fun saveRoute(route: Route): Result<Unit>
    suspend fun deleteRoute(routeId: String): Result<Unit>
    
    // Métodos para persistencia contra cierre accidental de la app
    fun saveActiveTracking(state: ActiveTrackingState)
    fun getActiveTracking(): ActiveTrackingState?
    fun clearActiveTracking()
}

/**
 * Repositorio persistente usando SharedPreferences para resistir cierres accidentales y reinicios.
 */
class PersistentRouteRepository(context: Context) : RouteRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("mph_routes_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val _routesFlow = MutableStateFlow<List<Route>>(emptyList())

    init {
        _routesFlow.value = loadRoutesFromDisk()
    }

    override fun getSavedRoutes(): Flow<List<Route>> = _routesFlow.asStateFlow()

    override suspend fun saveRoute(route: Route): Result<Unit> {
        val current = loadRoutesFromDisk().toMutableList()
        val newRoute = if (route.id.isEmpty()) route.copy(id = UUID.randomUUID().toString()) else route
        current.add(newRoute)
        
        prefs.edit().putString(KEY_SAVED_ROUTES, gson.toJson(current)).apply()
        _routesFlow.value = current
        clearActiveTracking() // Limpiar ruta activa al guardar
        return Result.success(Unit)
    }

    override suspend fun deleteRoute(routeId: String): Result<Unit> {
        val updated = loadRoutesFromDisk().filterNot { it.id == routeId }
        prefs.edit().putString(KEY_SAVED_ROUTES, gson.toJson(updated)).apply()
        _routesFlow.value = updated
        return Result.success(Unit)
    }

    override fun saveActiveTracking(state: ActiveTrackingState) {
        prefs.edit().putString(KEY_ACTIVE_TRACKING, gson.toJson(state)).apply()
    }

    override fun getActiveTracking(): ActiveTrackingState? {
        val json = prefs.getString(KEY_ACTIVE_TRACKING, null) ?: return null
        return try {
            gson.fromJson(json, ActiveTrackingState::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun clearActiveTracking() {
        prefs.edit().remove(KEY_ACTIVE_TRACKING).apply()
    }

    private fun loadRoutesFromDisk(): List<Route> {
        val json = prefs.getString(KEY_SAVED_ROUTES, null) ?: return emptyList()
        val type = object : TypeToken<List<Route>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY_SAVED_ROUTES = "key_saved_routes"
        private const val KEY_ACTIVE_TRACKING = "key_active_tracking"
    }
}
