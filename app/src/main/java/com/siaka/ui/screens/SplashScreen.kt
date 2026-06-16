package com.siaka.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.siaka.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000) // Display splash screen for 2 seconds
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Siaka Logo",
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                color = Color(0xFF1A237E)
            )
        }
    }
}
