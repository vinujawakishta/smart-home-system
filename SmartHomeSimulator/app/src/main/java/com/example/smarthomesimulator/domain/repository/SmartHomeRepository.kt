package com.example.smarthomesimulator.domain.repository

import com.example.smarthomesimulator.domain.model.Alert
import com.example.smarthomesimulator.domain.model.Device
import com.example.smarthomesimulator.domain.model.DeviceEvent
import kotlinx.coroutines.flow.Flow

interface SmartHomeRepository {
    fun getDevices(): Flow<List<Device>>
    fun getAlerts(): Flow<List<Alert>>
    fun getDeviceEvents(): Flow<List<DeviceEvent>>
    
    suspend fun toggleDevice(device: Device)
    suspend fun toggleChannel(device: Device, channelIndex: Int)
    suspend fun forceOffIron(device: Device)
    suspend fun logDeviceEvent(deviceId: String, deviceName: String, toState: String)
    suspend fun saveMaxDuration(deviceId: String, maxDuration: Long)
    
    suspend fun addDevice(device: Device)
    suspend fun removeDevice(deviceId: String)
    suspend fun updateDevice(device: Device)
}
