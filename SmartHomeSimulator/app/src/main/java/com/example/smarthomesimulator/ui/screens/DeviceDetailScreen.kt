package com.example.smarthomesimulator.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import com.example.smarthomesimulator.domain.model.Device
import com.example.smarthomesimulator.ui.viewmodel.DeviceDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    viewModel: DeviceDetailViewModel,
    onBack: () -> Unit
) {
    val device by viewModel.device.collectAsState()
    val alerts by viewModel.alerts.collectAsState()

    if (device == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val d = device!!
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(d.name, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Device")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
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
            DeviceCard(
                device = d,
                onToggle = { viewModel.toggleDevice(d) },
                onToggleChannel = { idx -> viewModel.toggleChannel(d, idx) },
                onIronOverdue = { viewModel.forceOffIron(d) },
                onDeviceClick = {},
                onRemove = {
                    // In real app, we might want to navigate back after removal
                    // For now keeping it simple as it's likely handled by the parent
                }
            )

            if (d.type == "iron") {
                LuxurySafetyCard(
                    maxDuration = d.maxOnDuration ?: 0,
                    onSave = { newDur -> viewModel.saveMaxDuration(d.id, newDur) }
                )
            }

            if (d.type == "light") {
                SchedulingCard(
                    device = d,
                    onSave = { updated -> viewModel.updateDevice(updated) }
                )
            }

            // Alerts History
            Column {
                Text(
                    "LOGGED INCIDENTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (alerts.isEmpty()) {
                    Text(
                        "No safety incidents reported for this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    alerts.forEach { alert ->
                        DetailAlertItem(alert.message, alert.timestamp)
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditDeviceDialog(
            device = d,
            onDismiss = { showEditDialog = false },
            onUpdate = { updated ->
                viewModel.updateDevice(updated)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun EditDeviceDialog(
    device: Device,
    onDismiss: () -> Unit,
    onUpdate: (Device) -> Unit
) {
    var name by remember { mutableStateOf(device.name) }
    var type by remember { mutableStateOf(device.type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Device Type", style = MaterialTheme.typography.labelMedium)
                Column {
                    com.example.smarthomesimulator.domain.model.dynamicDeviceTypes.forEach { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { type = t }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = type == t, onClick = { type = t })
                            Text(t.replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onUpdate(device.copy(name = name, type = type)) }) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LuxurySafetyCard(maxDuration: Long, onSave: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Safety Protocol",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Automatic cutoff configured for $maxDuration seconds of continuous operation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            var showDialog by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Modify Cutoff Timer")
            }

            if (showDialog) {
                var textValue by remember { mutableStateOf(maxDuration.toString()) }
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Update Safety Cutoff") },
                    text = {
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            label = { Text("Duration (seconds)") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            textValue.toLongOrNull()?.let { onSave(it) }
                            showDialog = false
                        }) {
                            Text("Save")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DetailAlertItem(message: String, timestamp: Long) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        val locale = LocalConfiguration.current.locales[0]
        Column {
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                SimpleDateFormat("MMM dd, HH:mm", locale).format(Date(timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SchedulingCard(device: Device, onSave: (Device) -> Unit) {
    var isEnabled by remember(device.scheduled) { mutableStateOf(device.scheduled) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Automation Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Automatic power cycles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { 
                        isEnabled = it
                        onSave(device.copy(scheduled = it)) 
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ScheduleTimeField(
                    label = "TURN ON",
                    time = device.scheduleOn ?: "00:00",
                    modifier = Modifier.weight(1f),
                    onTimeSelected = { onSave(device.copy(scheduleOn = it)) }
                )
                ScheduleTimeField(
                    label = "TURN OFF",
                    time = device.scheduleOff ?: "00:00",
                    modifier = Modifier.weight(1f),
                    onTimeSelected = { onSave(device.copy(scheduleOff = it)) }
                )
            }

            if (isEnabled) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Device will automatically turn ON at ${device.scheduleOn ?: "00:00"} and OFF at ${device.scheduleOff ?: "00:00"} daily.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTimeField(label: String, time: String, modifier: Modifier = Modifier, onTimeSelected: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Surface(
            onClick = { showDialog = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                time,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    if (showDialog) {
        val initialHour = time.split(":")[0].toIntOrNull() ?: 0
        val initialMinute = time.split(":")[1].toIntOrNull() ?: 0
        val timeState = rememberTimePickerState(initialHour, initialMinute, is24Hour = true)

        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val formatted = String.format(Locale.getDefault(), "%02d:%02d", timeState.hour, timeState.minute)
                    onTimeSelected(formatted)
                    showDialog = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
            title = { Text("Select Time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            }
        )
    }
}
