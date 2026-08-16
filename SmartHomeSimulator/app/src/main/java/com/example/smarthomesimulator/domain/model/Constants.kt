package com.example.smarthomesimulator.domain.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf

val FLOOR_PLAN = listOf(
    Floor(
        id = "floor1", label = "1st Floor", rooms = listOf(
            Room("entrance", "Entrance", 0xFFBBDEFB),
            Room("living", "Living Room", 0xFFC8E6C9),
            Room("kitchen", "Kitchen", 0xFFFFE0B2),
            Room("bath1", "Bathroom", 0xFFE1BEE7),
            Room("stairs", "Staircase", 0xFFD7CCC8),
        )
    ),
    Floor(
        id = "floor2", label = "2nd Floor", rooms = listOf(
            Room("master", "Master Bedroom", 0xFFFFE0B2),
            Room("bed2", "Bedroom 2", 0xFFBBDEFB),
            Room("study", "Study Room", 0xFFC8E6C9),
            Room("bath2", "Bathroom", 0xFFE1BEE7),
            Room("balcony", "Balcony", 0xFFB2EBF2),
        )
    ),
)

// Making floors dynamic so "Add Layout" actually works across the app session
val dynamicFloors = mutableStateListOf<Floor>().apply { addAll(FLOOR_PLAN) }

const val GRID_COLS = 6
const val GRID_ROWS = 4

val FLOOR1_LAYOUT = listOf(
    RoomLayout("entrance", 0, 0, 2, 1),
    RoomLayout("stairs", 0, 1, 2, 1),
    RoomLayout("bath1", 0, 2, 2, 2),
    RoomLayout("living", 2, 0, 2, 4),
    RoomLayout("kitchen", 4, 0, 2, 4),
)

val FLOOR2_LAYOUT = listOf(
    RoomLayout("balcony", 0, 0, 2, 2),
    RoomLayout("bath2", 0, 2, 2, 2),
    RoomLayout("master", 2, 0, 2, 4),
    RoomLayout("bed2", 4, 0, 2, 2),
    RoomLayout("study", 4, 2, 2, 2),
)

val dynamicLayouts = mutableStateMapOf<String, List<RoomLayout>>().apply {
    put("floor1", FLOOR1_LAYOUT)
    put("floor2", FLOOR2_LAYOUT)
}

val dynamicDeviceTypes = mutableStateListOf("light", "iron", "camera", "multiswitch", "fan", "ac")

fun findFloor(floorId: String) = dynamicFloors.find { it.id == floorId }

fun layoutForFloor(floorId: String): List<RoomLayout> = dynamicLayouts[floorId] ?: emptyList()
