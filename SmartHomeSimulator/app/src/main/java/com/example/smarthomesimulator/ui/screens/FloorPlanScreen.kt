package com.example.smarthomesimulator.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomesimulator.domain.model.*
import com.example.smarthomesimulator.ui.viewmodel.HomeViewModel
import android.graphics.Paint as AndroidPaint
import android.graphics.Color as AndroidColor

@Composable
fun FloorPlanScreen(
    viewModel: HomeViewModel,
    onDeviceClick: (Device) -> Unit,
    onAddLayout: () -> Unit
) {
    val devices by viewModel.devices.collectAsState()
    var activeFloorId by remember { mutableStateOf(dynamicFloors.firstOrNull()?.id ?: "") }
    var floorToDelete by remember { mutableStateOf<String?>(null) }

    // Ensure we have an active floor if floors were added/removed
    LaunchedEffect(dynamicFloors.size) {
        if (activeFloorId.isEmpty() && dynamicFloors.isNotEmpty()) {
            activeFloorId = dynamicFloors.first().id
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Home Layout", style = MaterialTheme.typography.displayMedium)
                Text("Visual room mapping", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dynamicFloors.size > 1) {
                    IconButton(
                        onClick = { floorToDelete = activeFloorId },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.DeleteSweep, "Delete Floor")
                    }
                }
                IconButton(
                    onClick = onAddLayout,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFEBC351)),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.Add, "Add Layout", tint = Color.Black)
                }
            }
        }

        PrimaryScrollableTabRow(
            selectedTabIndex = dynamicFloors.indexOfFirst { it.id == activeFloorId }.coerceAtLeast(0),
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            dynamicFloors.forEach { f ->
                Tab(
                    selected = activeFloorId == f.id,
                    onClick = { activeFloorId = f.id },
                    text = { Text(f.label) }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val floor = findFloor(activeFloorId)
            val currentLayout = layoutForFloor(activeFloorId)
            val floorDevices = devices.filter { it.floorId == activeFloorId }

            if (floor != null) {
                FloorCanvas(
                    floor = floor,
                    layout = currentLayout,
                    devices = floorDevices,
                    onDeviceClick = onDeviceClick
                )
            }
        }

        // Luxury Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LuxuryLegendDot(MaterialTheme.colorScheme.secondary, "ONLINE")
            LuxuryLegendDot(MaterialTheme.colorScheme.error, "OFFLINE")
            LuxuryLegendDot(MaterialTheme.colorScheme.primary, "SYSTEM")
        }
    }

    if (floorToDelete != null) {
        AlertDialog(
            onDismissRequest = { floorToDelete = null },
            title = { Text("Delete Floor") },
            text = { Text("Are you sure you want to delete this floor and all its room mappings?") },
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
                        activeFloorId = newActive
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
fun FloorCanvas(
    floor: Floor,
    layout: List<RoomLayout>,
    devices: List<Device>,
    onDeviceClick: (Device) -> Unit
) {
    val strokeColor = MaterialTheme.colorScheme.outline
    val density = LocalDensity.current
    val labelSize = with(density) { 10.sp.toPx() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()

        val cellW = canvasWidth / floor.gridCols
        val cellH = canvasHeight / floor.gridRows

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw Rooms
            layout.forEach { roomLayout ->
                val room = floor.rooms.find { it.id == roomLayout.roomId }
                val left = roomLayout.colStart * cellW
                val top = roomLayout.rowStart * cellH
                val width = roomLayout.colSpan * cellW
                val height = roomLayout.rowSpan * cellH

                val roomColor = room?.color?.let { Color(it) } ?: strokeColor.copy(alpha = 0.05f)

                drawRect(
                    color = roomColor,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(width, height)
                )
                drawRect(
                    color = strokeColor,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(width, height),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Room Label
                drawContext.canvas.nativeCanvas.drawText(
                    (room?.label ?: roomLayout.roomId).uppercase(),
                    left + 12.dp.toPx(),
                    top + labelSize + 8.dp.toPx(),
                    AndroidPaint().apply {
                        color = AndroidColor.BLACK // Use black for contrast on colored backgrounds
                        textSize = labelSize
                        isFakeBoldText = true
                        letterSpacing = 0.1f
                    }
                )
            }

            // Draw Device Dots
            layout.forEach { room ->
                val roomDevices = devices.filter { it.roomId == room.roomId }
                val left = room.colStart * cellW
                val top = room.rowStart * cellH
                val width = room.colSpan * cellW
                val height = room.rowSpan * cellH

                roomDevices.forEachIndexed { index, device ->
                    val cols = 2
                    val r = index / cols
                    val c = index % cols
                    
                    val dotX = left + (c + 0.5f) * (width / cols)
                    val dotY = top + (r + 0.8f) * (height / 2f) 

                    val dotColor = if (device.state == "on") Color(0xFF00A86B) else Color(0xFFFF4842)

                    // Outer Glow
                    drawCircle(
                        color = dotColor.copy(alpha = 0.2f),
                        radius = 12.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                    // Core
                    drawCircle(
                        color = dotColor,
                        radius = 4.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }
            }
        }

        // Interactive Overlays
        layout.forEach { room ->
            val roomDevices = devices.filter { it.roomId == room.roomId }
            roomDevices.forEachIndexed { index, device ->
                val cols = 2
                val r = index / cols
                val c = index % cols
                
                val dW = (room.colSpan * cellW / cols) / density.density
                val dH = (room.rowSpan * cellH / 2f) / density.density
                val dX = (room.colStart * cellW + (c * room.colSpan * cellW / cols)) / density.density
                val dY = (room.rowStart * cellH + (r * room.rowSpan * cellH / 2f)) / density.density
                
                Box(
                    modifier = Modifier
                        .offset(x = dX.dp, y = dY.dp)
                        .size(dW.dp, dH.dp)
                        .clickable { onDeviceClick(device) }
                )
            }
        }
    }
}

@Composable
fun LuxuryLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
    }
}
