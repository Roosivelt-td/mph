package com.example.mph.ui.map

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mph.data.model.Route
import com.example.mph.location.LocationService
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onRequestLocationPermissions: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showSaveDialog by remember { mutableStateOf(false) }
    var showRoutesBottomSheet by remember { mutableStateOf(false) }
    var routeNameInput by remember { mutableStateOf("") }

    // Posición por defecto (ejemplo: Lima, Perú o la ubicación actual)
    val defaultLocation = LatLng(-12.046374, -77.042793)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(uiState.currentLocation ?: defaultLocation, 15f)
    }

    // Actualizar cámara cuando cambie la ubicación del usuario
    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let { location ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(location, 16f)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapas y Rutas Registradas", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showRoutesBottomSheet = true }) {
                        Icon(Icons.Default.Route, contentDescription = "Ver Rutas Registradas")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Google Maps View
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = true
                ),
                properties = MapProperties(
                    isMyLocationEnabled = uiState.isPermissionGranted
                )
            ) {
                // Dibujar línea de la ruta actual en tiempo real
                if (uiState.currentRoutePoints.size > 1) {
                    Polyline(
                        points = uiState.currentRoutePoints.map { it.toLatLng() },
                        color = Color(0xFF1E88E5),
                        width = 12f
                    )
                }

                // Marcador para el LUGAR DE ORIGEN activo
                if (uiState.isTracking && uiState.currentRoutePoints.isNotEmpty()) {
                    val originLatLng = uiState.currentRoutePoints.first().toLatLng()
                    Marker(
                        state = MarkerState(position = originLatLng),
                        title = "📍 LUGAR DE ORIGEN",
                        snippet = "Punto inicial marcado al iniciar el trayecto"
                    )
                }

                // Dibujar ruta guardada seleccionada
                uiState.selectedRoute?.let { route ->
                    val points = route.toLatLngList()
                    if (points.isNotEmpty()) {
                        Polyline(
                            points = points,
                            color = Color(0xFF4CAF50),
                            width = 10f
                        )
                        Marker(
                            state = MarkerState(position = points.first()),
                            title = "Inicio: ${route.name}",
                            snippet = "Punto de partida"
                        )
                        Marker(
                            state = MarkerState(position = points.last()),
                            title = "Fin: ${route.name}",
                            snippet = "Distancia: ${route.distanceKm} km"
                        )
                    }
                }

                // Marcador de ubicación actual
                uiState.currentLocation?.let { current ->
                    Marker(
                        state = MarkerState(position = current),
                        title = "Mi Ubicación Actual"
                    )
                }
            }

            // 2. Tarjeta de Control y Estadísticas en vivo
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.isTracking) {
                        Text(
                            text = "🔴 RASTREANDO RUTA...",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Distancia", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    text = "${String.format("%.2f", uiState.totalDistanceMeters / 1000.0)} km",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Puntos", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    text = "${uiState.currentRoutePoints.size}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showSaveDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detener y Guardar Ruta")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (uiState.isPermissionGranted) {
                                    startLocationService(context)
                                    viewModel.startTracking()
                                } else {
                                    onRequestLocationPermissions()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Iniciar Registro de Ruta")
                        }
                    }

                    if (uiState.selectedRoute != null && !uiState.isTracking) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.selectRoute(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Limpiar Ruta Seleccionada (${uiState.selectedRoute?.name})")
                        }
                    }
                }
            }
        }
    }

    // Diálogo para nombrar y guardar la ruta
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Guardar Ruta Registrada") },
            text = {
                Column {
                    Text("Distancia total: ${String.format("%.2f", uiState.totalDistanceMeters / 1000.0)} km")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = routeNameInput,
                        onValueChange = { routeNameInput = it },
                        label = { Text("Nombre de la ruta (Ej: Camina al parque)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        stopLocationService(context)
                        viewModel.stopAndSaveTracking(routeNameInput)
                        routeNameInput = ""
                        showSaveDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        stopLocationService(context)
                        viewModel.cancelTracking()
                        showSaveDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // BottomSheet para ver el historial de Rutas Registradas
    if (showRoutesBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRoutesBottomSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Rutas Registradas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.savedRoutes.isEmpty()) {
                    Text(
                        text = "Aún no has guardado ninguna ruta. Presiona 'Iniciar Registro de Ruta' para empezar.",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.savedRoutes) { route ->
                            Card(
                                onClick = {
                                    viewModel.selectRoute(route)
                                    showRoutesBottomSheet = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(route.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("${route.distanceKm} km • ${route.points.size} puntos", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun startLocationService(context: Context) {
    val intent = Intent(context, LocationService::class.java).apply {
        action = LocationService.ACTION_START
    }
    context.startService(intent)
}

private fun stopLocationService(context: Context) {
    val intent = Intent(context, LocationService::class.java).apply {
        action = LocationService.ACTION_STOP
    }
    context.startService(intent)
}
