package com.example.stopwatchapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StopwatchScreen(
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onExit: () -> Unit,
    time: String // The time passed as a parameter from MainActivity
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display the stopwatch time
        Text(
            text = time,
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Buttons for Start, Stop, Reset, Exit
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStart) { Text("Start") }
            Button(onClick = onStop) { Text("Stop") }
            Button(onClick = onReset) { Text("Reset") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Exit button
        Button(onClick = onExit) { Text("Exit") }
    }
}
