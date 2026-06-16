package com.siaka.ui.screens.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.siaka.data.MapUiState
import com.siaka.ui.theme.DangerRed
import com.siaka.ui.theme.DarkNavy
import com.siaka.ui.theme.NavigationGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(fineLocationGranted || coarseLocationGranted)
    }

    LaunchedEffect(Unit) {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation) {
            viewModel.onPermissionResult(true)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Handle back-button to exit navigation safely
    BackHandler(enabled = uiState.isNavigating || uiState.isRouteGenerated) {
        if (uiState.isNavigating) {
            viewModel.stopNavigation()
        } else if (uiState.isRouteGenerated) {
            viewModel.clearRoute()
        }
    }

    MapScreenContent(
        uiState = uiState,
        onPermissionResult = viewModel::onPermissionResult,
        onMapCentered = viewModel::onMapCentered,
        onCenterOnLocationRequested = viewModel::onCenterOnLocationRequested,
        onStopNavigation = viewModel::stopNavigation,
        onClearRoute = viewModel::clearRoute,
        onGenerateRoute = viewModel::generateRoute,
        onStartNavigation = viewModel::startNavigation,
        onSaveRoute = viewModel::saveRoute,
        onDistanceInputChange = viewModel::onDistanceInputChange,
        onShowDistanceDialog = viewModel::onShowDistanceDialog,
        onDismissDistanceDialog = viewModel::onDismissDistanceDialog
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreenContent(
    uiState: MapUiState,
    onPermissionResult: (Boolean) -> Unit,
    onMapCentered: () -> Unit,
    onCenterOnLocationRequested: () -> Unit,
    onStopNavigation: () -> Unit,
    onClearRoute: () -> Unit,
    onGenerateRoute: () -> Unit,
    onStartNavigation: () -> Unit,
    onSaveRoute: () -> Unit,
    onDistanceInputChange: (String) -> Unit,
    onShowDistanceDialog: () -> Unit,
    onDismissDistanceDialog: () -> Unit
) {
    val viewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(12.0)
            center(Point.fromLngLat(36.817223, -1.286389))
        }
    }

    LaunchedEffect(uiState.userLocation, uiState.shouldCenterOnLocation, uiState.isNavigating) {
        uiState.userLocation?.let { location ->
            val point = Point.fromLngLat(location.longitude, location.latitude)
            if (uiState.shouldCenterOnLocation || uiState.isNavigating) {
                viewportState.flyTo(
                    com.mapbox.maps.CameraOptions.Builder()
                        .center(point)
                        // Tilt and zoom more when navigating for 3D effect
                        .zoom(if (uiState.isNavigating) 18.0 else 15.0)
                        .pitch(if (uiState.isNavigating) 45.0 else 0.0)
                        .build()
                )
                if (uiState.shouldCenterOnLocation) {
                    onMapCentered()
                }
            }
        }
    }

    LaunchedEffect(uiState.generatedRoutePoints) {
        if (uiState.generatedRoutePoints.isNotEmpty() && !uiState.isNavigating) {
            val points = uiState.generatedRoutePoints.map { Point.fromLngLat(it.longitude, it.latitude) }
            val cameraOptions = viewportState.cameraForCoordinates(
                points,
                coordinatesPadding = EdgeInsets(100.0, 100.0, 600.0, 100.0) // Extra padding at bottom for UI
            )
            viewportState.flyTo(cameraOptions)
        }
    }

    Scaffold(
        topBar = {
            if (!uiState.isNavigating) {
                TopAppBar(
                    title = {
                        Text(
                            "Siaka",
                            color = DarkNavy,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 1. Map Layer
            if (uiState.isLocationPermissionGranted) {
                MapboxMap(
                    modifier = Modifier.fillMaxSize(),
                    mapViewportState = viewportState
                ) {
                    // Draw Polyline Route
                    if (uiState.generatedRoutePoints.isNotEmpty()) {
                        PolylineAnnotation(
                            points = uiState.generatedRoutePoints.map { Point.fromLngLat(it.longitude, it.latitude) }
                        ) {
                            lineColor = Color(0xFF2196F3) // Bright blue
                            lineWidth = 6.0
                            lineBorderColor = Color(0xFF0D47A1)
                            lineBorderWidth = 2.0
                        }
                    }

                    // Draw User Location as Compose UI (ViewAnnotation)
                    uiState.userLocation?.let { location ->
                        ViewAnnotation(
                            options = viewAnnotationOptions {
                                geometry(Point.fromLngLat(location.longitude, location.latitude))
                                allowOverlap(true)
                            }
                        ) {
                            UserLocationMarker(
                                isNavigating = uiState.isNavigating,
                                bearing = location.bearing ?: 0f // Assumes ViewModel provides device bearing
                            )
                        }
                    }
                }
            }

            // 2. Top Navigation Banner (Turn-by-Turn)
            AnimatedVisibility(
                visible = uiState.isNavigating,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                NavigationTopBanner(
                    instruction = uiState.nextInstruction,
                    distance = uiState.distanceToNextInstructionMeters
                )
            }

            // 3. Search Bar (When Idle)
            AnimatedVisibility(
                visible = !uiState.isNavigating && !uiState.isRouteGenerated,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
            ) {
                SearchBarOverlay(onClick = onShowDistanceDialog)
            }

            // 4. Map Action Buttons (Right side)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp, bottom = if (uiState.isNavigating) 140.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MapControlCircleButton(Icons.Default.Add) {;
                    viewportState.cameraState?.let {
                        viewportState.flyTo(
                            com.mapbox.maps.CameraOptions.Builder()
                                .zoom(it.zoom + 1.0)
                                .build()
                        )
                    }
                }
                MapControlCircleButton(Icons.Default.Remove) {
                    viewportState.cameraState?.let {
                        viewportState.flyTo(
                            com.mapbox.maps.CameraOptions.Builder()
                                .zoom(it.zoom - 1.0)
                                .build()
                        )
                    }
                }
                MapControlCircleButton(Icons.Default.MyLocation) { onCenterOnLocationRequested() }
            }

            // 5. Bottom Controls (Route Actions OR Navigation Details)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // State: Route has been generated but not started
                AnimatedVisibility(
                    visible = uiState.isRouteGenerated && !uiState.isNavigating,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    RouteActionButtons(
                        onRegenerate = onShowDistanceDialog,
                        onStart = onStartNavigation,
                        onSave = onSaveRoute
                    )
                }

                // State: Actively Navigating
                AnimatedVisibility(
                    visible = uiState.isNavigating,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = onStopNavigation,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            modifier = Modifier
                                .height(56.dp)
                                .shadow(8.dp, RoundedCornerShape(50)),
                            contentPadding = PaddingValues(horizontal = 32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "End Navigation",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        if (uiState.showDistanceDialog) {
            DistanceInputDialog(
                distance = uiState.distanceInput,
                onDistanceChange = onDistanceInputChange,
                onConfirm = onGenerateRoute,
                onDismiss = onDismissDistanceDialog
            )
        }
    }
}

// --- COMPOSE COMPONENTS ---

@Composable
fun UserLocationMarker(isNavigating: Boolean, bearing: Float = 0f) {
    if (isNavigating) {
        // Navigation Chevron Match
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(3.dp, DarkNavy, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = "User Location",
                tint = DarkNavy,
                modifier = Modifier
                    .size(28.dp)
                    .rotate(bearing) // Rotates arrow based on direction
            )
        }
    } else {
        // Standard Blue Dot with white-border
        Box(
            modifier = Modifier
                .size(24.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF2196F3))
                .border(3.dp, Color.White, CircleShape)
        )
    }
}

@Composable
fun SearchBarOverlay(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Enter loop distance (km)...", color = Color.Gray, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun NavigationTopBanner(instruction: String, distance: Int) {
    Card(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavigationGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Turn Icon Box
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.25f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.TurnRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = instruction.ifEmpty { "Proceed on route" },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$distance",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "M",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RouteActionButtons(
    onRegenerate: () -> Unit,
    onStart: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color.White,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Start Navigation (Primary Focus)
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkNavy)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Navigation", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onRegenerate,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = DarkNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Regenerate", color = DarkNavy, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(16.dp))

                OutlinedButton(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = DarkNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Route", color = DarkNavy, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun MapControlCircleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = DarkNavy)
        }
    }
}

@Composable
fun DistanceInputDialog(
    distance: String,
    onDistanceChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Set Loop Distance",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkNavy
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "How many kilometers would you like to run today?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                androidx.compose.material3.OutlinedTextField(
                    value = distance,
                    onValueChange = onDistanceChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Distance in km") },
                    suffix = { Text("km") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkNavy)
                    ) {
                        Text("Generate", color = Color.White)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    com.siaka.ui.theme.SiakaTheme {
        MapScreenContent(
            uiState = com.siaka.data.MapUiState(
                isLocationPermissionGranted = true,
                userLocation = com.siaka.data.LocationPoint(-1.286389, 36.817223)
            ),
            onPermissionResult = {},
            onMapCentered = {},
            onCenterOnLocationRequested = {},
            onStopNavigation = {},
            onClearRoute = {},
            onGenerateRoute = {},
            onStartNavigation = {},
            onSaveRoute = {},
            onDistanceInputChange = {},
            onShowDistanceDialog = {},
            onDismissDistanceDialog = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenNavigationPreview() {
    com.siaka.ui.theme.SiakaTheme {
        MapScreenContent(
            uiState = com.siaka.data.MapUiState(
                isLocationPermissionGranted = true,
                userLocation = com.siaka.data.LocationPoint(-1.286389, 36.817223),
                isNavigating = true,
                nextInstruction = "Head North on Naivasha Road",
                distanceToNextInstructionMeters = 297
            ),
            onPermissionResult = {},
            onMapCentered = {},
            onCenterOnLocationRequested = {},
            onStopNavigation = {},
            onClearRoute = {},
            onGenerateRoute = {},
            onStartNavigation = {},
            onSaveRoute = {},
            onDistanceInputChange = {},
            onShowDistanceDialog = {},
            onDismissDistanceDialog = {}
        )
    }
}
