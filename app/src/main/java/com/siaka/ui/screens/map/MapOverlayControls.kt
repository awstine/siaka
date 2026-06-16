package com.siaka.ui.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siaka.data.MapUiState
import com.siaka.ui.theme.Primary
import com.siaka.ui.theme.Secondary

@Composable
fun MapOverlayControls(
    uiState: MapUiState,
    onDistanceChange: (String) -> Unit,
    onGenerateClick: () -> Unit,
    onStartNavigation: () -> Unit,
    onStopNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isNavigating) {
        NavigationControls(uiState, onStopNavigation, modifier)
    } else if (uiState.generatedRoutePoints.isNotEmpty()) {
        RouteInfoCard(uiState, onStartNavigation, modifier)
    } else {
        GenerationCard(uiState, onDistanceChange, onGenerateClick, modifier)
    }
}

@Composable
fun RouteInfoCard(
    uiState: MapUiState,
    onStartNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = Color(0xFFC8E6C9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Fastest Route",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${uiState.remainingDistanceKm} km",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "/ ${uiState.estimatedTimeMinutes} mins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                Text(
                    text = uiState.routeDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Surface(
                onClick = onStartNavigation,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = Primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun GenerationCard(
    uiState: MapUiState,
    onDistanceChange: (String) -> Unit,
    onGenerateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Where are we riding today?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Primary
            )

            OutlinedTextField(
                value = uiState.distanceInput,
                onValueChange = onDistanceChange,
                label = { Text("Distance (km)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = onGenerateClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState.distanceInput.isNotEmpty() && !uiState.isLoadingRoute,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (uiState.isLoadingRoute) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Generate Route")
                }
            }
        }
    }
}

@Composable
fun NavigationControls(
    uiState: MapUiState,
    onStopNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            LinearProgressIndicator(
                progress = { 0.4f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Primary,
                trackColor = Color.LightGray
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ETA", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(uiState.eta, fontWeight = FontWeight.Bold, color = Primary)
                }
                
                Column {
                    Text("REMAINING", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${uiState.estimatedTimeMinutes} min", fontWeight = FontWeight.Bold, color = Color.Black)
                }
                
                Column {
                    Text("DISTANCE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${uiState.remainingDistanceKm} km", fontWeight = FontWeight.Bold, color = Color.Black)
                }
                
                Button(
                    onClick = onStopNavigation,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("End")
                }
            }
        }
    }
}
