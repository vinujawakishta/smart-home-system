package com.example.smarthomesimulator.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthomesimulator.domain.model.Alert
import com.example.smarthomesimulator.domain.repository.SmartHomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val repository: SmartHomeRepository
) : ViewModel() {

    val alerts: StateFlow<List<Alert>> = repository.getAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
