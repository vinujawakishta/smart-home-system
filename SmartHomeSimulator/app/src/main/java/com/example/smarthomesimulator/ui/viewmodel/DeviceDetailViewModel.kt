package com.example.smarthomesimulator.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthomesimulator.domain.model.Alert
import com.example.smarthomesimulator.domain.model.Device
import com.example.smarthomesimulator.domain.repository.SmartHomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    private val repository: SmartHomeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deviceId: String = savedStateHandle.get<String>("deviceId") ?: ""

    val device: StateFlow<Device?> = repository.getDevices()
        .map { devices -> devices.find { it.id == deviceId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val alerts: StateFlow<List<Alert>> = repository.getAlerts()
        .map { alerts -> alerts.filter { it.deviceId == deviceId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleDevice(device: Device) {
        viewModelScope.launch {
            repository.toggleDevice(device)
        }
    }

    fun toggleChannel(device: Device, index: Int) {
        viewModelScope.launch {
            repository.toggleChannel(device, index)
        }
    }

    fun forceOffIron(device: Device) {
        viewModelScope.launch {
            repository.forceOffIron(device)
        }
    }

    fun saveMaxDuration(deviceId: String, maxDuration: Long) {
        viewModelScope.launch {
            repository.saveMaxDuration(deviceId, maxDuration)
        }
    }
}
