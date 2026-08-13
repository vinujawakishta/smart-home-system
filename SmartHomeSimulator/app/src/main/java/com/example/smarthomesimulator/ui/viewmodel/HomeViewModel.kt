package com.example.smarthomesimulator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthomesimulator.domain.model.Device
import com.example.smarthomesimulator.domain.repository.SmartHomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SmartHomeRepository
) : ViewModel() {

    private val _devices = repository.getDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val devices: StateFlow<List<Device>> = _devices

    private val _initialLoadComplete = MutableStateFlow(false)
    val initialLoadComplete: StateFlow<Boolean> = _initialLoadComplete.asStateFlow()

    init {
        viewModelScope.launch {
            _devices.collect {
                _initialLoadComplete.value = true
            }
        }
    }

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

    fun addDevice(device: Device) {
        viewModelScope.launch {
            repository.addDevice(device)
        }
    }

    fun removeDevice(deviceId: String) {
        viewModelScope.launch {
            repository.removeDevice(deviceId)
        }
    }
}
