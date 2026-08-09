package com.example.smarthomesimulator.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
    var activeFloorId by remember { mutableStateOf(dynamicFloors.first().id) }

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
            IconButton(
                onClick = onAddLayout,
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Filled.Add, "Add Layout", tint = MaterialTheme.colorScheme.primary)
            }
        }

        ScrollableTabRow(
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
            val currentLayout = layoutForFloor(activeFloorId)
            val floorDevices = devices.filter { it.floorId == activeFloorId }

            FloorCanvas(
                layout = currentLayout,
                devices = floorDevices,
                onDeviceClick = onDeviceClick
            )
        }

        // Luxury Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LuxuryLegendDot(Color(0xFF00A86B), "ONLINE")
            LuxuryLegendDot(Color(0xFFFF4842), "OFFLINE")
            LuxuryLegendDot(MaterialTheme.colorScheme.primary, "SYSTEM")
        }
    }
}

@Composable
fun FloorCanvas(
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

        val cellW = canvasWidth / GRID_COLS
        val cellH = canvasHeight / GRID_ROWS

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw Rooms
            layout.forEach { room ->
                val left = room.colStart * cellW
                val top = room.rowStart * cellH
                val width = room.colSpan * cellW
                val height = room.rowSpan * cellH

                drawRect(
                    color = strokeColor.copy(alpha = 0.05f),
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
                    room.roomId.uppercase(),
                    left + 12.dp.toPx(),
                    top + labelSize + 8.dp.toPx(),
                    AndroidPaint().apply {
                        color = AndroidColor.LTGRAY
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
