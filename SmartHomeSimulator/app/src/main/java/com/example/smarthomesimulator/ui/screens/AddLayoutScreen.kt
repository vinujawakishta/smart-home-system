package com.example.smarthomesimulator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomesimulator.domain.model.*

data class RoomCategory(val name: String, val color: Color)

val CATEGORIES = listOf(
    RoomCategory("Living Room", Color(0xFFBBDEFB)), // Light Blue
    RoomCategory("Kitchen", Color(0xFFC8E6C9)),     // Light Green
    RoomCategory("Bedroom", Color(0xFFFFE0B2)),     // Light Orange
    RoomCategory("Bathroom", Color(0xFFE1BEE7)),    // Light Purple
    RoomCategory("Dining", Color(0xFFFFF9C4)),      // Light Yellow
    RoomCategory("Hallway", Color(0xFFD7CCC8)),     // Light Brown
    RoomCategory("Balcony", Color(0xFFB2EBF2)),     // Cyan
    RoomCategory("None", Color.Transparent)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddLayoutScreen(
    floorToEditId: String? = null,
    onLayoutAdded: (Floor, List<RoomLayout>) -> Unit,
    onBack: () -> Unit
) {
    val existingFloor = floorToEditId?.let { id -> dynamicFloors.find { it.id == id } }
    val existingLayout = floorToEditId?.let { id -> dynamicLayouts[id] }

    var floorName by remember { mutableStateOf(existingFloor?.label ?: "") }
    var rows by remember { mutableIntStateOf(existingFloor?.gridRows ?: 4) }
    var cols by remember { mutableIntStateOf(existingFloor?.gridCols ?: 6) }
    
    // Grid state: maps (col, row) to a category
    val gridState = remember { 
        val state = mutableStateMapOf<Pair<Int, Int>, RoomCategory>()
        if (existingFloor != null && existingLayout != null) {
            existingLayout.forEach { layout ->
                val room = existingFloor.rooms.find { it.id == layout.roomId }
                val category = CATEGORIES.find { it.name == room?.label } ?: CATEGORIES.last()
                state[Pair(layout.colStart, layout.rowStart)] = category
            }
        }
        state
    }
    
    var selectedCategory by remember { mutableStateOf(CATEGORIES[0]) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (floorToEditId == null) "Design Floor Grid" else "Edit Floor Grid") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = floorName,
                onValueChange = { floorName = it },
                label = { Text("Floor Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Columns: ${cols}", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = cols.toFloat(),
                        onValueChange = { cols = it.toInt() },
                        valueRange = 2f..10f,
                        steps = 7
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Rows: ${rows}", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = rows.toFloat(),
                        onValueChange = { rows = it.toInt() },
                        valueRange = 2f..10f,
                        steps = 7
                    )
                }
            }

            Text("Pick a Room Category:", fontWeight = FontWeight.Bold)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CATEGORIES.forEach { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (category.color == Color.Transparent) MaterialTheme.colorScheme.primaryContainer else category.color
                        )
                    )
                }
            }

            Text("Tap cells to assign rooms:", fontWeight = FontWeight.Bold)
            
            // The Grid
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(cols.toFloat() / rows.toFloat())
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                val cellWidth = maxWidth / cols
                val cellHeight = maxHeight / rows

                Column {
                    for (r in 0 until rows) {
                        Row {
                            for (c in 0 until cols) {
                                val category = gridState[Pair(c, r)]
                                Box(
                                    modifier = Modifier
                                        .size(cellWidth, cellHeight)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                        .background(category?.color ?: Color.Transparent)
                                        .clickable {
                                            if (selectedCategory.name == "None") {
                                                gridState.remove(Pair(c, r))
                                            } else {
                                                gridState[Pair(c, r)] = selectedCategory
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (category != null && category.color != Color.Transparent) {
                                        Text(
                                            category.name.take(1),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (floorName.isBlank()) {
                        errorMessage = "Floor name is required"
                        return@Button
                    }
                    if (gridState.isEmpty()) {
                        errorMessage = "Please assign at least one room cell"
                        return@Button
                    }
                    
                    val newFloorId = floorToEditId ?: "custom_${System.currentTimeMillis()}"
                    
                    val uniqueCategories = gridState.values.distinct()
                    val rooms = uniqueCategories.map { cat ->
                        Room(
                            id = "${newFloorId}_${cat.name.lowercase().replace(" ", "_")}",
                            label = cat.name,
                            color = cat.color.toArgb().toLong()
                        )
                    }
                    
                    val newFloor = Floor(newFloorId, floorName, rooms, gridCols = cols, gridRows = rows)
                    
                    val layouts = gridState.map { (pos, cat) ->
                        val roomId = "${newFloorId}_${cat.name.lowercase().replace(" ", "_")}"
                        RoomLayout(roomId, pos.first, pos.second, 1, 1)
                    }
                    
                    onLayoutAdded(newFloor, layouts)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(if (floorToEditId == null) "Create Floor Layout" else "Update Floor Layout")
            }
        }
    }
}
