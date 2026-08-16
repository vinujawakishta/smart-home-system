package com.example.smarthomesimulator.data.repository

import com.example.smarthomesimulator.domain.model.Alert
import com.example.smarthomesimulator.domain.model.Device
import com.example.smarthomesimulator.domain.model.DeviceEvent
import com.example.smarthomesimulator.domain.repository.SmartHomeRepository
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class SmartHomeRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : SmartHomeRepository {

    private val devicesRef = database.getReference("devices")
    private val alertsRef = database.getReference("alerts")
    private val eventsRef = database.getReference("deviceEvents")

    override fun getDevices(): Flow<List<Device>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updated = snapshot.children.mapNotNull { child ->
                    child.getValue(Device::class.java)?.copy(id = child.key ?: "")
                }
                trySend(updated)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        devicesRef.addValueEventListener(listener)
        awaitClose { devicesRef.removeEventListener(listener) }
    }

    override fun getAlerts(): Flow<List<Alert>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updated = snapshot.children
                    .mapNotNull { child -> child.getValue(Alert::class.java)?.copy(id = child.key ?: "") }
                    .sortedByDescending { it.timestamp }
                trySend(updated)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        alertsRef.addValueEventListener(listener)
        awaitClose { alertsRef.removeEventListener(listener) }
    }

    override fun getDeviceEvents(): Flow<List<DeviceEvent>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updated = snapshot.children
                    .mapNotNull { child -> child.getValue(DeviceEvent::class.java)?.copy(id = child.key ?: "") }
                    .sortedBy { it.timestamp }
                trySend(updated)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        eventsRef.addValueEventListener(listener)
        awaitClose { eventsRef.removeEventListener(listener) }
    }

    override suspend fun toggleDevice(device: Device) {
        val turningOn = device.state != "on"
        val newState = if (turningOn) "on" else "off"
        devicesRef.child(device.id).child("state").setValue(newState)
        if (device.type == "iron") {
            devicesRef.child(device.id).child("turnedOnAt")
                .setValue(if (turningOn) System.currentTimeMillis() else null)
        }
        logDeviceEvent(device.id, device.name, newState)
    }

    override suspend fun toggleChannel(device: Device, channelIndex: Int) {
        val channels = device.channels ?: return
        val updated = channels.toMutableList()
        updated[channelIndex] = !updated[channelIndex]
        val anyOn = updated.any { it }
        val newState = if (anyOn) "on" else "off"
        devicesRef.child(device.id).child("channels").setValue(updated)
        devicesRef.child(device.id).child("state").setValue(newState)
        if (newState != device.state) {
            logDeviceEvent(device.id, device.name, newState)
        }
    }

    override suspend fun forceOffIron(device: Device) {
        devicesRef.child(device.id).child("state").setValue("off")
        devicesRef.child(device.id).child("turnedOnAt").setValue(null)
        logDeviceEvent(device.id, device.name, "off")
        val alertRef = alertsRef.push()
        alertRef.setValue(
            mapOf(
                "deviceId" to device.id,
                "deviceName" to device.name,
                "message" to "Safety cutoff (client-side) — exceeded ${device.maxOnDuration}s max on-duration",
                "timestamp" to System.currentTimeMillis()
            )
        )
    }

    override suspend fun logDeviceEvent(deviceId: String, deviceName: String, toState: String) {
        val eventRef = eventsRef.push()
        eventRef.setValue(
            mapOf(
                "deviceId" to deviceId,
                "deviceName" to deviceName,
                "toState" to toState,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }

    override suspend fun saveMaxDuration(deviceId: String, maxDuration: Long) {
        devicesRef.child(deviceId).child("maxOnDuration").setValue(maxDuration)
    }

    override suspend fun addDevice(device: Device) {
        val newRef = devicesRef.push()
        val deviceWithId = device.copy(id = newRef.key ?: "")
        newRef.setValue(deviceWithId)
    }

    override suspend fun removeDevice(deviceId: String) {
        devicesRef.child(deviceId).removeValue()
    }

    override suspend fun updateDevice(device: Device) {
        devicesRef.child(device.id).setValue(device)
    }
}
