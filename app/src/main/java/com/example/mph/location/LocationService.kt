package com.example.mph.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.mph.MainActivity
import com.example.mph.R
import com.example.mph.data.model.LocationPoint
import com.example.mph.data.model.Route
import com.example.mph.data.repository.ActiveTrackingState
import com.example.mph.data.repository.PersistentRouteRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    private lateinit var repository: PersistentRouteRepository

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repository = PersistentRouteRepository(applicationContext)
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        // START_STICKY garantiza que si el sistema mata el servicio por falta de memoria, lo reinicie automáticamente
        return START_STICKY
    }

    private fun start() {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📍 Rastreo de Ruta Activo")
            .setContentText("El origen y los movimientos siguen registrándose en segundo plano.")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Recuperar o inicializar estado activo
        var currentState = repository.getActiveTracking()
        val startTime = currentState?.startTime ?: System.currentTimeMillis()
        val points = currentState?.points?.toMutableList() ?: mutableListOf()
        var totalDistance = currentState?.totalDistanceMeters ?: 0.0

        locationClient.getLocationUpdates(3000L)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                val newPoint = LocationPoint(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    speed = location.speed,
                    altitude = location.altitude
                )

                // Si es el primer punto, se marca como el LUGAR DE ORIGEN
                val originPoint = points.firstOrNull() ?: newPoint

                if (points.isNotEmpty()) {
                    totalDistance += Route.calculateDistanceMeters(points.last(), newPoint)
                }
                points.add(newPoint)

                // Persistir inmediatamente en disco para resistir cualquier cierre accidental
                val activeState = ActiveTrackingState(
                    originPoint = originPoint,
                    points = points,
                    startTime = startTime,
                    totalDistanceMeters = totalDistance
                )
                repository.saveActiveTracking(activeState)

                // Emitir a la UI si la app está abierta
                _locationUpdates.emit(location)
            }
            .launchIn(serviceScope)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Cuando el usuario desliza y cierra la app de la lista de tareas recientes:
        // El servicio en primer plano PERMANECE ACTIVO y la notificación avisa al usuario.
    }

    private fun stop() {
        repository.clearActiveTracking()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rastreo de Rutas Activo",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val CHANNEL_ID = "location_service_channel"
        private const val NOTIFICATION_ID = 101

        private val _locationUpdates = MutableSharedFlow<android.location.Location>(extraBufferCapacity = 64)
        val locationUpdates: SharedFlow<android.location.Location> = _locationUpdates.asSharedFlow()
    }
}
