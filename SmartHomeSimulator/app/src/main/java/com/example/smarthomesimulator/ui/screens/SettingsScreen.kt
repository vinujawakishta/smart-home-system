package com.example.smarthomesimulator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smarthomesimulator.domain.model.dynamicFloors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onEditFloor: (String) -> Unit,
    onEditDevice: (String) -> Unit
) {
    var userName by remember { mutableStateOf("User") }
    var userEmail by remember { mutableStateOf("user@example.com") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Profile Customization", style = MaterialTheme.typography.titleLarge)
                    }
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = userEmail,
                        onValueChange = { userEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { /* Save profile logic */ }) {
                        Text("Save Profile")
                    }
                }
            }

            // Edit Floors Section
            Text("Manage Floors", style = MaterialTheme.typography.titleMedium)
            dynamicFloors.forEach { floor ->
                OutlinedCard(
                    onClick = { onEditFloor(floor.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(floor.label)
                        Text("Edit", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Text("For Device Editing, please go to the Home screen and click on a device to view/edit details.", 
                 style = MaterialTheme.typography.bodySmall, 
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
