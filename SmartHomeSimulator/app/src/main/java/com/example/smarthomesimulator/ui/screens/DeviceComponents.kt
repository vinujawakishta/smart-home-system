package com.example.smarthomesimulator.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomesimulator.domain.model.Device
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun DeviceCard(
    device: Device,
    onToggle: () -> Unit,
    onToggleChannel: (Int) -> Unit,
    onIronOverdue: () -> Unit,
    onDeviceClick: () -> Unit,
    onRemove: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDeviceClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = device.type.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(state = device.state)
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                when (device.type) {
                    "iron" -> IronBody(device, onToggle, onIronOverdue)
                    "camera" -> CameraBody(device, onToggle)
                    "multiswitch" -> MultiSwitchBody(device, onToggleChannel)
                    else -> SimpleToggleBody(device, onToggle)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Device?") },
            text = { Text("Are you sure you want to remove ${device.name} from your system?") },
            confirmButton = {
                TextButton(onClick = {
                    onRemove()
                    showDeleteConfirm = false
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatusPill(state: String) {
    val isOn = state == "on"
    val color = if (isOn) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
    
    Surface(
        color = if (isOn) color.copy(alpha = 0.15f) else Color.Transparent,
        shape = CircleShape,
        border = if (!isOn) androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isOn) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
            }
            Text(
                text = if (isOn) "ACTIVE" else "OFF",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isOn) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SimpleToggleBody(device: Device, onToggle: () -> Unit) {
    Button(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (device.state == "on") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (device.state == "on") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            if (device.state == "on") "Power Off" else "Power On",
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
fun IronBody(device: Device, onToggle: () -> Unit, onIronOverdue: () -> Unit) {
    val isOn = device.state == "on"
    var timeLeft by remember(device.turnedOnAt, device.state) {
        mutableLongStateOf(
            if (isOn && device.turnedOnAt != null && device.maxOnDuration != null) {
                val elapsed = (System.currentTimeMillis() - device.turnedOnAt) / 1000
                (device.maxOnDuration - elapsed).coerceAtLeast(0L)
            } else 0L
        )
    }

    LaunchedEffect(isOn, device.turnedOnAt) {
        if (isOn && device.turnedOnAt != null) {
            while (timeLeft > 0) {
                delay(1.seconds)
                val elapsed = (System.currentTimeMillis() - device.turnedOnAt) / 1000
                timeLeft = (device.maxOnDuration!! - elapsed).coerceAtLeast(0L)
                if (timeLeft <= 0L) {
                    onIronOverdue()
                }
            }
        }
    }

    Column {
        if (isOn) {
            LinearProgressIndicator(
                progress = {
                    if (device.maxOnDuration != null && device.maxOnDuration > 0) {
                        (timeLeft.toFloat() / device.maxOnDuration.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = if (timeLeft < 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Warning,
                    null,
                    tint = if (timeLeft < 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Safety cutoff in ${timeLeft}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (timeLeft < 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        SimpleToggleBody(device, onToggle)
    }
}

@Composable
fun CameraBody(device: Device, onToggle: () -> Unit) {
    val isOn = device.state == "on"
    Column {
        if (isOn) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "live")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha"
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color.Red.copy(alpha = alpha)))
                    Spacer(Modifier.width(10.dp))
                    Text("SECURE LIVE FEED", color = Color.White, style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        SimpleToggleBody(device, onToggle)
    }
}

@Composable
fun MultiSwitchBody(device: Device, onToggleChannel: (Int) -> Unit) {
    val channels = device.channels ?: listOf(false, false, false)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        channels.forEachIndexed { index, active ->
            val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .clickable { onToggleChannel(index) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CH ${index + 1}", style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
                    Text(if (active) "ON" else "OFF", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = contentColor)
                }
            }
        }
    }
}
