package com.example.smarthomesimulator.domain.model

data class Device(
    val id: String = "",
    val type: String = "",
    val name: String = "",
    val floorId: String = "",
    val roomId: String = "",
    val state: String = "off",
    val details: List<String> = emptyList(),
    val channels: List<Boolean>? = null,
    val maxOnDuration: Long? = null, // seconds
    val turnedOnAt: Long? = null     // epoch millis
)

data class Alert(
    val id: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

data class DeviceEvent(
    val id: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val toState: String = "",
    val timestamp: Long = 0L
)

data class Room(val id: String, val label: String, val color: Long? = null)
data class Floor(
    val id: String,
    val label: String,
    val rooms: List<Room>,
    val gridCols: Int = 6,
    val gridRows: Int = 4
)

data class RoomLayout(val roomId: String, val colStart: Int, val rowStart: Int, val colSpan: Int, val rowSpan: Int)
