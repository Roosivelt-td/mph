package com.example.mph.location

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    /**
     * Emite actualizaciones de ubicación continuas en tiempo real.
     * @param intervalMs Intervalo de actualización en milisegundos.
     */
    fun getLocationUpdates(intervalMs: Long): Flow<Location>

    class LocationException(message: String) : Exception(message)
}
