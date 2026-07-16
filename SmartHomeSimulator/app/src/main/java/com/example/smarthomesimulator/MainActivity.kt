package com.example.smarthomesimulator

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smarthomesimulator.ui.theme.SmartHomeSimulatorTheme
import com.google.firebase.database.*

/* =========================================================================
   DATA MODEL
   -------------------------------------------------------------------------
   Mirrors the schema your web simulator writes. Keep this in sync with
   Kulakshi & Warusha's understanding of the schema — this IS the contract.
   ========================================================================= */

data class Device(
    val id: String = "",
    val type: String = "",
    val name: String = "",
    val floorId: String = "",
    val roomId: String = "",
    val state: String = "off",
    val details: List<String> = emptyList(),
    val channels: List<Boolean>? = null
)

data class Room(val id: String, val label: String)
data class Floor(val id: String, val label: String, val rooms: List<Room>)

// Static floor/room structure — mirrors the simulator's FLOOR_PLAN.
// This does not live in Firebase; only device *state* is synced.
val FLOOR_PLAN = listOf(
    Floor(
        id = "floor1", label = "1st Floor", rooms = listOf(
            Room("entrance", "Entrance"),
            Room("living", "Living Room"),
            Room("kitchen", "Kitchen"),
            Room("bath1", "Bathroom"),
            Room("stairs", "Staircase"),
        )
    ),
    Floor(
        id = "floor2", label = "2nd Floor", rooms = listOf(
            Room("master", "Master Bedroom"),
            Room("bed2", "Bedroom 2"),
            Room("study", "Study Room"),
            Room("bath2", "Bathroom"),
            Room("balcony", "Balcony"),
        )
    ),
)

fun findFloor(floorId: String) = FLOOR_PLAN.find { it.id == floorId }
fun findRoomLabel(floorId: String, roomId: String) =
    findFloor(floorId)?.rooms?.find { it.id == roomId }?.label ?: roomId

/* =========================================================================
   ACTIVITY
   ========================================================================= */

class MainActivity : ComponentActivity() {

    private val deviceList = mutableStateListOf<Device>()
    private lateinit var devicesRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = FirebaseDatabase.getInstance(
            "https://smart-home-system-d7702-default-rtdb.asia-southeast1.firebasedatabase.app"
        )
        devicesRef = database.getReference("devices")

        devicesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updated = snapshot.children.mapNotNull { child ->
                    child.getValue(Device::class.java)?.copy(id = child.key ?: "")
                }
                deviceList.clear()
                deviceList.addAll(updated)
                Log.d("FirebaseTest", "Devices count: ${deviceList.size}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseTest", "Failed to read devices", error.toException())
            }
        })

        enableEdgeToEdge()
        setContent {
            SmartHomeSimulatorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        devices = deviceList,
                        modifier = Modifier.padding(innerPadding),
                        onToggle = { device -> toggleDevice(device) },
                        onToggleChannel = { device, index -> toggleChannel(device, index) }
                    )
                }
            }
        }
    }

    // Writes go straight to Firebase — the listener above fires automatically
    // and updates deviceList, which recomposes the UI. Same pattern as the
    // web simulator: no local state faking.
    private fun toggleDevice(device: Device) {
        val newState = if (device.state == "on") "off" else "on"
        devicesRef.child(device.id).child("state").setValue(newState)
    }

    private fun toggleChannel(device: Device, index: Int) {
        val channels = device.channels ?: return
        val updated = channels.toMutableList()
        updated[index] = !updated[index]
        val anyOn = updated.any { it }
        devicesRef.child(device.id).child("channels").setValue(updated)
        devicesRef.child(device.id).child("state").setValue(if (anyOn) "on" else "off")
    }
}

/* =========================================================================
   SCREEN: floor tabs + room filter + grouped device list
   ========================================================================= */

@Composable
fun HomeScreen(
    devices: List<Device>,
    modifier: Modifier = Modifier,
    onToggle: (Device) -> Unit,
    onToggleChannel: (Device, Int) -> Unit
) {
    var activeFloor by remember { mutableStateOf(FLOOR_PLAN.first().id) }
    var activeRoom by remember { mutableStateOf("all") }

    // Reset room filter if it doesn't belong to the newly selected floor
    LaunchedEffect(activeFloor) {
        val valid = findFloor(activeFloor)?.rooms?.any { it.id == activeRoom } ?: false
        if (!valid) activeRoom = "all"
    }

    val floor = findFloor(activeFloor) ?: FLOOR_PLAN.first()
    val floorDevices = devices.filter { it.floorId == activeFloor }
    val visibleDevices = floorDevices.filter { activeRoom == "all" || it.roomId == activeRoom }

    Column(modifier = modifier.fillMaxSize()) {

        // --- Floor tabs ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FLOOR_PLAN.forEach { f ->
                FilterChip(
                    selected = f.id == activeFloor,
                    onClick = { activeFloor = f.id },
                    label = { Text(f.label) }
                )
            }
        }

        // --- Room chips ---
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = activeRoom == "all",
                    onClick = { activeRoom = "all" },
                    label = { Text("All areas") }
                )
            }
            items(floor.rooms) { room ->
                FilterChip(
                    selected = activeRoom == room.id,
                    onClick = { activeRoom = room.id },
                    label = { Text(room.label) }
                )
            }
        }

        // --- Summary row ---
        SummaryRow(floorDevices = floorDevices, visibleDevices = visibleDevices)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- Device list, grouped by room ---
        if (devices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading devices…")
            }
            return@Column
        }

        if (visibleDevices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No devices in this area.")
            }
            return@Column
        }

        val grouped = visibleDevices.groupBy { it.roomId }
        val orderedRoomIds = floor.rooms.map { it.id }.filter { grouped.containsKey(it) }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            orderedRoomIds.forEach { roomId ->
                item {
                    Text(
                        text = findRoomLabel(activeFloor, roomId),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(grouped[roomId] ?: emptyList(), key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        onToggle = { onToggle(device) },
                        onToggleChannel = { index -> onToggleChannel(device, index) }
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryRow(floorDevices: List<Device>, visibleDevices: List<Device>) {
    val powered = floorDevices.count { it.state == "on" }
    val alerts = floorDevices.count { it.state == "error" || it.state == "disconnected" }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricChip(label = "On this floor", value = floorDevices.size)
        MetricChip(label = "Visible", value = visibleDevices.size)
        MetricChip(label = "Powered ON", value = powered)
        MetricChip(label = "Alerts", value = alerts)
    }
}

@Composable
fun RowScope.MetricChip(label: String, value: Int) {
    Card(modifier = Modifier.weight(1f)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("$value", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/* =========================================================================
   DEVICE CARD — renders differently per type
   ========================================================================= */

@Composable
fun DeviceCard(device: Device, onToggle: () -> Unit, onToggleChannel: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

            // --- Header: name, type, status pill ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(device.name, fontWeight = FontWeight.Bold)
                    Text(
                        device.type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                StatusPill(state = device.state)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Type-specific body ---
            when (device.type) {
                "multiswitch" -> MultiSwitchBody(device, onToggleChannel)
                "camera" -> CameraBody(device)
                "iron" -> IronBody(device, onToggle)
                else -> SimpleToggleBody(device, onToggle) // outlet, bulb
            }

            if (device.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                device.details.forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun StatusPill(state: String) {
    val (bg, fg) = when (state) {
        "on" -> Color(0xFF1F6E4A) to Color(0xFFB9F3D5)
        "error" -> Color(0xFF6E2323) to Color(0xFFFFD2D2)
        "disconnected" -> Color(0xFF6E5A23) to Color(0xFFFFE7BA)
        else -> Color(0xFF3A3A3A) to Color(0xFFDDDDDD) // off
    }
    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.small) {
        Text(
            text = state.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SimpleToggleBody(device: Device, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Power", style = MaterialTheme.typography.bodyMedium)
        Switch(checked = device.state == "on", onCheckedChange = { onToggle() })
    }
}

@Composable
fun IronBody(device: Device, onToggle: () -> Unit) {
    Column {
        SimpleToggleBody(device, onToggle)
        // maxOnDuration / turnedOnAt aren't in the current seed data yet —
        // once the safety-cutoff Cloud Function is built, those fields get
        // added to the schema and this section shows live time remaining.
        Text(
            "Safety timer info comes from Firebase once the cutoff logic is added.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CameraBody(device: Device) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    if (device.state == "disconnected") "No signal" else "Mock camera feed",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun MultiSwitchBody(device: Device, onToggleChannel: (Int) -> Unit) {
    val channels = device.channels ?: emptyList()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        channels.forEachIndexed { index, isOn ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Switch ${index + 1}", style = MaterialTheme.typography.bodySmall)
                Switch(checked = isOn, onCheckedChange = { onToggleChannel(index) })
            }
        }
    }
}