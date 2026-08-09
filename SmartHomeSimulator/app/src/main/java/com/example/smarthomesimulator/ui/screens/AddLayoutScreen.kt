package com.example.smarthomesimulator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smarthomesimulator.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLayoutScreen(
    onLayoutAdded: (Floor, List<RoomLayout>) -> Unit,
    onBack: () -> Unit
) {
    var floorName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Custom Layout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Define a new floor and its room grid mapping.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = floorName,
                onValueChange = { floorName = it },
                label = { Text("Floor Name (e.g. Attic)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Room Configuration (Experimental)", style = MaterialTheme.typography.titleSmall)
            Text(
                "For now, adding a static 3-room layout for simplicity in this demo UI.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (floorName.isBlank()) {
                        errorMessage = "Floor name is required"
                        return@Button
                    }
                    
                    val newFloorId = "custom_${System.currentTimeMillis()}"
                    val rooms = listOf(
                        Room("custom1", "Room A"),
                        Room("custom2", "Room B"),
                        Room("custom3", "Room C")
                    )
                    val newFloor = Floor(newFloorId, floorName, rooms)
                    
                    val layouts = listOf(
                        RoomLayout("custom1", 0, 0, 3, 2),
                        RoomLayout("custom2", 3, 0, 3, 2),
                        RoomLayout("custom3", 0, 2, 6, 2)
                    )
                    
                    onLayoutAdded(newFloor, layouts)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Create Floor Layout")
            }
        }
    }
}
