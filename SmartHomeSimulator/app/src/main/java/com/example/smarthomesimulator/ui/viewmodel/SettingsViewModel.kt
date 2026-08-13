package com.example.smarthomesimulator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthomesimulator.domain.model.Device
import com.example.smarthomesimulator.domain.model.DeviceEvent
import com.example.smarthomesimulator.domain.repository.SmartHomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SmartHomeRepository
) : ViewModel() {

    val devices: StateFlow<List<Device>> = repository.getDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val events: StateFlow<List<DeviceEvent>> = repository.getDeviceEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deviceUsageStats: StateFlow<Map<String, Long>> = events.map { allEvents ->
        val stats = mutableMapOf<String, Long>()
        val lastOnTime = mutableMapOf<String, Long>()

        allEvents.sortedBy { it.timestamp }.forEach { event ->
            if (event.toState == "on") {
                lastOnTime[event.deviceId] = event.timestamp
            } else if (event.toState == "off") {
                val onTime = lastOnTime.remove(event.deviceId)
                if (onTime != null) {
                    val duration = event.timestamp - onTime
                    stats[event.deviceId] = (stats[event.deviceId] ?: 0L) + duration
                }
            }
        }
        
        // Include currently ON devices
        val now = System.currentTimeMillis()
        lastOnTime.forEach { (deviceId, onTime) ->
            val duration = now - onTime
            stats[deviceId] = (stats[deviceId] ?: 0L) + duration
        }
        
        stats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}
