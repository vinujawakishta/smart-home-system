package com.example.smarthomesimulator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomesimulator.domain.model.*
import com.example.smarthomesimulator.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDeviceClick: (Device) -> Unit,
    onAddLayout: () -> Unit
) {
    val devices by viewModel.devices.collectAsState()
    val initialLoadComplete by viewModel.initialLoadComplete.collectAsState()

    var activeFloor by remember { mutableStateOf(dynamicFloors.firstOrNull()?.id ?: "") }
    var activeRoom by remember { mutableStateOf("all") }
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var floorToDelete by remember { mutableStateOf<String?>(null) }

    // Ensure we have an active floor if floors were added/removed
    LaunchedEffect(dynamicFloors.size) {
        if (activeFloor.isEmpty() && dynamicFloors.isNotEmpty()) {
            activeFloor = dynamicFloors.first().id
        }
    }

    // Reset room filter if it doesn't belong to the newly selected floor
    LaunchedEffect(activeFloor) {
        val valid = findFloor(activeFloor)?.rooms?.any { it.id == activeRoom } ?: false
        if (!valid) activeRoom = "all"
    }

    val floor = findFloor(activeFloor) ?: dynamicFloors.firstOrNull() ?: Floor("", "", emptyList())
    val floorDevices = devices.filter { it.floorId == activeFloor }
    val visibleDevices = floorDevices.filter { activeRoom == "all" || it.roomId == activeRoom }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDeviceDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Device")
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            
            // Modern Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Smart Dashboard",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Manage your premium home environment",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dynamicFloors.size > 1) {
                        IconButton(
                            onClick = { floorToDelete = activeFloor },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.DeleteSweep, "Delete Floor")
                        }
                    }
                    
                    IconButton(
                        onClick = onAddLayout,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Filled.Layers, "Add Floor")
                    }
                }
            }

            // Floor Tabs
            PrimaryScrollableTabRow(
                selectedTabIndex = dynamicFloors.indexOfFirst { it.id == activeFloor }.coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                dynamicFloors.forEach { f ->
                    Tab(
                        selected = activeFloor == f.id,
                        onClick = { activeFloor = f.id },
                        text = {
                            Text(
                                f.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (activeFloor == f.id) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Room Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    FilterChip(
                        selected = activeRoom == "all",
                        onClick = { activeRoom = "all" },
                        label = { Text("All Rooms") },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                items(floor.rooms) { room ->
                    FilterChip(
                        selected = activeRoom == room.id,
                        onClick = { activeRoom = room.id },
                        label = { Text(room.label) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            SummaryRow(floorDevices = floorDevices, visibleDevices = visibleDevices)

            if (!initialLoadComplete && devices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (visibleDevices.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No devices in this area",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(visibleDevices, key = { it.id }) { device ->
                        DeviceCard(
                            device = device,
                            onToggle = { viewModel.toggleDevice(device) },
                            onToggleChannel = { idx -> viewModel.toggleChannel(device, idx) },
                            onIronOverdue = { viewModel.forceOffIron(device) },
                            onDeviceClick = { onDeviceClick(device) },
                            onRemove = { viewModel.removeDevice(device.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }

    if (showAddDeviceDialog) {
        AddDeviceDialog(
            currentFloor = activeFloor,
            rooms = floor.rooms,
            onDismiss = { showAddDeviceDialog = false },
            onAdd = { newDevice ->
                viewModel.addDevice(newDevice)
                showAddDeviceDialog = false
            }
        )
    }

    if (floorToDelete != null) {
        AlertDialog(
            onDismissRequest = { floorToDelete = null },
            title = { Text("Delete Floor") },
            text = { Text("Are you sure you want to delete this floor and all its room mappings? This will not delete the actual devices.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = floorToDelete!!
                        val nextIndex = dynamicFloors.indexOfFirst { it.id == id }
                        val newActive = if (nextIndex <= 0) {
                             if (dynamicFloors.size > 1) dynamicFloors[1].id else ""
                        } else {
                             dynamicFloors[0].id
                        }
                        activeFloor = newActive
                        dynamicFloors.removeIf { it.id == id }
                        dynamicLayouts.remove(id)
                        floorToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { floorToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SummaryRow(floorDevices: List<Device>, visibleDevices: List<Device>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val totalOn = floorDevices.count { it.state == "on" }
        val visibleCount = visibleDevices.size

        MetricChip(label = "DEVICES", value = visibleCount)
        MetricChip(label = "ACTIVE", value = totalOn)
    }
}

@Composable
fun MetricChip(label: String, value: Int) {
    Surface(
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun AddDeviceDialog(
    currentFloor: String,
    rooms: List<Room>,
    onDismiss: () -> Unit,
    onAdd: (Device) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedRoom by remember { mutableStateOf(rooms.firstOrNull()?.id ?: "") }
    var type by remember { mutableStateOf("light") }

    val deviceTypes = listOf("light", "iron", "camera", "multiswitch", "fan", "ac")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Device", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name") },
                    placeholder = { Text("e.g. Living Room Light") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("Select Room", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(rooms) { room ->
                            FilterChip(
                                selected = selectedRoom == room.id,
                                onClick = { selectedRoom = room.id },
                                label = { Text(room.label) }
                            )
                        }
                    }
                }

                Column {
                    Text("Device Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(deviceTypes) { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val newDevice = Device(
                            name = name,
                            type = type,
                            floorId = currentFloor,
                            roomId = selectedRoom,
                            state = "off",
                            channels = if (type == "multiswitch") listOf(false, false, false) else null,
                            maxOnDuration = if (type == "iron") 600 else null
                        )
                        onAdd(newDevice)
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Device")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
