package com.example.smarthomesimulator

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smarthomesimulator.ui.theme.SmartHomeSimulatorTheme
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Paint as AndroidPaint
import android.graphics.Color as AndroidColor

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
   ABSTRACT GRID LAYOUT
   -------------------------------------------------------------------------
   Per the spec: "an abstract(simple) grid mapping overlaid onto specific
   floor layouts" — not a real architectural floor plan. Each room occupies
   a rectangle of grid cells; devices in that room are drawn as small dots
   inside it. Both floors use the same 6x4 grid.
   ========================================================================= */

const val GRID_COLS = 6
const val GRID_ROWS = 4

data class RoomLayout(val roomId: String, val colStart: Int, val rowStart: Int, val colSpan: Int, val rowSpan: Int)

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

fun layoutForFloor(floorId: String): List<RoomLayout> = when (floorId) {
    "floor1" -> FLOOR1_LAYOUT
    "floor2" -> FLOOR2_LAYOUT
    else -> emptyList()
}

/* =========================================================================
   ACTIVITY
   ========================================================================= */

class MainActivity : ComponentActivity() {

    private val deviceList = mutableStateListOf<Device>()
    private val alertList = mutableStateListOf<Alert>()
    private val eventList = mutableStateListOf<DeviceEvent>()
    private lateinit var devicesRef: DatabaseReference
    private lateinit var alertsRef: DatabaseReference
    private lateinit var eventsRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = FirebaseDatabase.getInstance(
            "https://smart-home-system-d7702-default-rtdb.asia-southeast1.firebasedatabase.app"
        )
        devicesRef = database.getReference("devices")
        alertsRef = database.getReference("alerts")
        eventsRef = database.getReference("deviceEvents")

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

        alertsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updated = snapshot.children
                    .mapNotNull { child -> child.getValue(Alert::class.java)?.copy(id = child.key ?: "") }
                    .sortedByDescending { it.timestamp }
                alertList.clear()
                alertList.addAll(updated)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseTest", "Failed to read alerts", error.toException())
            }
        })

        eventsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updated = snapshot.children
                    .mapNotNull { child -> child.getValue(DeviceEvent::class.java)?.copy(id = child.key ?: "") }
                    .sortedBy { it.timestamp }
                eventList.clear()
                eventList.addAll(updated)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseTest", "Failed to read device events", error.toException())
            }
        })

        enableEdgeToEdge()
        setContent {
            SmartHomeSimulatorTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute != "welcome") {
                            AppBottomBar(navController)
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "welcome",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("welcome") {
                            WelcomeScreen(
                                onEnter = {
                                    navController.navigate("home") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                devices = deviceList,
                                onToggle = { device -> toggleDevice(device) },
                                onToggleChannel = { device, index -> toggleChannel(device, index) },
                                onIronOverdue = { device -> forceOffIron(device) },
                                onDeviceClick = { device -> navController.navigate("device/${device.id}") }
                            )
                        }
                        composable("reports") {
                            ReportsScreen(devices = deviceList, events = eventList, alerts = alertList)
                        }
                        composable("floorplan") {
                            FloorPlanScreen(
                                devices = deviceList,
                                onDeviceClick = { device -> navController.navigate("device/${device.id}") }
                            )
                        }
                        composable("alerts") { AlertsScreen(alerts = alertList) }
                        composable(
                            route = "device/{deviceId}",
                            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                            val device = deviceList.find { it.id == deviceId }
                            if (device != null) {
                                DeviceDetailScreen(
                                    device = device,
                                    alerts = alertList.filter { it.deviceId == deviceId },
                                    onToggle = { toggleDevice(device) },
                                    onToggleChannel = { index -> toggleChannel(device, index) },
                                    onIronOverdue = { forceOffIron(device) },
                                    onSaveMaxDuration = { newValue ->
                                        devicesRef.child(deviceId).child("maxOnDuration").setValue(newValue)
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Device not found")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Writes go straight to Firebase — the listener above fires automatically
    // and updates deviceList, which recomposes the UI. Same pattern as the
    // web simulator: no local state faking.
    private fun toggleDevice(device: Device) {
        val turningOn = device.state != "on"
        val newState = if (turningOn) "on" else "off"
        devicesRef.child(device.id).child("state").setValue(newState)
        if (device.type == "iron") {
            devicesRef.child(device.id).child("turnedOnAt")
                .setValue(if (turningOn) System.currentTimeMillis() else null)
        }
        logDeviceEvent(device.id, device.name, newState)
    }

    private fun logDeviceEvent(deviceId: String, deviceName: String, newState: String) {
        val eventRef = eventsRef.push()
        eventRef.setValue(
            mapOf(
                "deviceId" to deviceId,
                "deviceName" to deviceName,
                "toState" to newState,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }

    private fun toggleChannel(device: Device, index: Int) {
        val channels = device.channels ?: return
        val updated = channels.toMutableList()
        updated[index] = !updated[index]
        val anyOn = updated.any { it }
        val newState = if (anyOn) "on" else "off"
        devicesRef.child(device.id).child("channels").setValue(updated)
        devicesRef.child(device.id).child("state").setValue(newState)
        if (newState != device.state) {
            logDeviceEvent(device.id, device.name, newState)
        }
    }

    // Client-side safety fallback: if this app is open and an iron's timer
    // hits zero, force it off directly — same write the eventual backend
    // Cloud Function/Action will make. This does NOT run when the app is
    // closed; only the deployed scheduled job guarantees that. Kept as a
    // clearly separate function so it's obvious which guarantee is which.
    private fun forceOffIron(device: Device) {
        devicesRef.child(device.id).child("state").setValue("off")
        devicesRef.child(device.id).child("turnedOnAt").setValue(null)
        logDeviceEvent(device.id, device.name, "off")
        val alertRef = devicesRef.root.child("alerts").push()
        alertRef.setValue(
            mapOf(
                "deviceId" to device.id,
                "deviceName" to device.name,
                "message" to "Safety cutoff (client-side) — exceeded ${device.maxOnDuration}s max on-duration",
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
}

/* =========================================================================
   BOTTOM NAVIGATION
   ========================================================================= */

private data class NavTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val navTabs = listOf(
    NavTab("home", "Home", Icons.Filled.Home),
    NavTab("floorplan", "Floor Plan", Icons.Filled.Map),
    NavTab("reports", "Reports", Icons.Filled.List),
    NavTab("alerts", "Alerts", Icons.Filled.Warning),
)

@Composable
fun AppBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        navTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    navController.navigate(tab.route) {
                        // Avoids stacking up duplicate copies of the same
                        // screen as you tap between tabs repeatedly.
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}

/* =========================================================================
   PLACEHOLDER SCREENS — built out next, one at a time
   ========================================================================= */

@Composable
fun AlertsScreen(alerts: List<Alert>) {
    if (alerts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No alerts yet.\nSafety cutoffs and device errors will appear here.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val formatter = remember { SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "${alerts.size} alert${if (alerts.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(alerts, key = { it.id }) { alert ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(alert.deviceName, fontWeight = FontWeight.Bold)
                        Text(
                            formatter.format(Date(alert.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(alert.message, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ReportsScreen(devices: List<Device>, events: List<DeviceEvent>, alerts: List<Alert>) {
    val now = remember { System.currentTimeMillis() }

    // Pairs consecutive on->off events per device to compute total ON time.
    // If a device is currently on, its ongoing session counts up to "now".
    // Note: only accounts for time since events started being logged —
    // devices already on before you started using the app won't have
    // their earlier ON time counted. Good enough for a demo/reporting view.
    val onDurations = remember(events, devices) {
        val result = mutableMapOf<String, Long>()
        val byDevice = events.groupBy { it.deviceId }
        byDevice.forEach { (deviceId, deviceEvents) ->
            var total = 0L
            var lastOnAt: Long? = null
            deviceEvents.sortedBy { it.timestamp }.forEach { event ->
                if (event.toState == "on" && lastOnAt == null) {
                    lastOnAt = event.timestamp
                } else if (event.toState == "off" && lastOnAt != null) {
                    total += event.timestamp - lastOnAt!!
                    lastOnAt = null
                }
            }
            if (lastOnAt != null) {
                total += now - lastOnAt!! // still on right now
            }
            result[deviceId] = total
        }
        result
    }

    val sortedDevices = devices.sortedByDescending { onDurations[it.id] ?: 0L }
    val cutoffAlerts = alerts.filter { it.message.contains("cutoff", ignoreCase = true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricChip(label = "Devices tracked", value = devices.size)
                MetricChip(label = "Cutoff events", value = cutoffAlerts.size)
            }
        }

        item {
            Text(
                "ON-time by device",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (sortedDevices.none { (onDurations[it.id] ?: 0L) > 0 }) {
            item {
                Text(
                    "No usage recorded yet — toggle some devices, then check back here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(sortedDevices, key = { it.id }) { device ->
                val millis = onDurations[device.id] ?: 0L
                if (millis > 0) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(device.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "${findFloor(device.floorId)?.label} / ${findRoomLabel(device.floorId, device.roomId)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(formatDuration(millis), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Cutoff & error events",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        if (alerts.isEmpty()) {
            item {
                Text(
                    "None yet. See the Alerts tab for full details as they occur.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(alerts.take(5), key = { it.id }) { alert ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(alert.deviceName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(alert.message, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (alerts.size > 5) {
                item {
                    Text(
                        "+ ${alerts.size - 5} more — see Alerts tab",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

/* =========================================================================
   SCREEN: floor tabs + room filter + grouped device list
   ========================================================================= */

/* =========================================================================
   WELCOME SCREEN — shown once on launch
   ========================================================================= */

@Composable
fun WelcomeScreen(onEnter: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Smart Home System",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Monitor and control every device across your home, synced live.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onEnter) {
                Text("Enter Dashboard")
            }
        }
    }
}

@Composable
fun HomeScreen(
    devices: List<Device>,
    modifier: Modifier = Modifier,
    onToggle: (Device) -> Unit,
    onToggleChannel: (Device, Int) -> Unit,
    onIronOverdue: (Device) -> Unit,
    onDeviceClick: (Device) -> Unit
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

        Divider(modifier = Modifier.padding(vertical = 8.dp))

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
                        onToggleChannel = { index -> onToggleChannel(device, index) },
                        onIronOverdue = { onIronOverdue(device) },
                        onClick = { onDeviceClick(device) }
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
   FLOOR PLAN SCREEN — abstract grid mapping with device markers
   ========================================================================= */

@Composable
fun FloorPlanScreen(devices: List<Device>, onDeviceClick: (Device) -> Unit) {
    var activeFloor by remember { mutableStateOf(FLOOR_PLAN.first().id) }
    val layout = layoutForFloor(activeFloor)
    val floorDevices = devices.filter { it.floorId == activeFloor }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
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

        Text(
            "Tap a marker to open that device",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .aspectRatio(GRID_COLS.toFloat() / GRID_ROWS.toFloat())
        ) {
            val widthDp = maxWidth
            val heightDp = maxHeight

            // --- Grid + room rectangles, drawn once as a background ---
            Canvas(modifier = Modifier.matchParentSize()) {
                val cellW = size.width / GRID_COLS
                val cellH = size.height / GRID_ROWS

                layout.forEach { room ->
                    val left = room.colStart * cellW
                    val top = room.rowStart * cellH
                    val w = room.colSpan * cellW
                    val h = room.rowSpan * cellH

                    drawRect(
                        color = Color(0xFFEFEFEF),
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(w, h)
                    )
                    drawRect(
                        color = Color(0xFFBBBBBB),
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(w, h),
                        style = Stroke(width = 2f)
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        findRoomLabel(activeFloor, room.roomId),
                        left + 8f,
                        top + 26f,
                        AndroidPaint().apply {
                            color = AndroidColor.DKGRAY
                            textSize = 26f
                            isAntiAlias = true
                        }
                    )
                }

                for (c in 0..GRID_COLS) {
                    drawLine(
                        color = Color(0x22000000),
                        start = Offset(c * cellW, 0f),
                        end = Offset(c * cellW, size.height),
                        strokeWidth = 1f
                    )
                }
                for (r in 0..GRID_ROWS) {
                    drawLine(
                        color = Color(0x22000000),
                        start = Offset(0f, r * cellH),
                        end = Offset(size.width, r * cellH),
                        strokeWidth = 1f
                    )
                }
            }

            // --- Device markers, real composables so they're tappable ---
            layout.forEach { room ->
                val roomDevices = floorDevices.filter { it.roomId == room.roomId }
                val roomLeftDp = widthDp * (room.colStart.toFloat() / GRID_COLS)
                val roomTopDp = heightDp * (room.rowStart.toFloat() / GRID_ROWS)

                roomDevices.forEachIndexed { index, device ->
                    val markersPerRow = 3
                    val spacing = 20.dp
                    val col = index % markersPerRow
                    val rowIdx = index / markersPerRow
                    val markerX = roomLeftDp + 10.dp + (spacing * col)
                    val markerY = roomTopDp + 34.dp + (spacing * rowIdx)

                    val markerColor = when (device.state) {
                        "on" -> Color(0xFF3FA96A)
                        "error" -> Color(0xFFE2564F)
                        "disconnected" -> Color(0xFFD9A438)
                        else -> Color(0xFF9E9E9E)
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = markerX, y = markerY)
                            .size(16.dp)
                            .background(markerColor, shape = CircleShape)
                            .clickable { onDeviceClick(device) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendDot(color = Color(0xFF3FA96A), label = "ON")
            LegendDot(color = Color(0xFF9E9E9E), label = "OFF")
            LegendDot(color = Color(0xFFE2564F), label = "ERROR")
            LegendDot(color = Color(0xFFD9A438), label = "DISCONNECTED")
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/* =========================================================================
   DEVICE CARD — renders differently per type
   ========================================================================= */

/* =========================================================================
   DEVICE DETAIL SCREEN
   ========================================================================= */

@Composable
fun DeviceDetailScreen(
    device: Device,
    alerts: List<Alert>,
    onToggle: () -> Unit,
    onToggleChannel: (Int) -> Unit,
    onIronOverdue: () -> Unit,
    onSaveMaxDuration: (Long) -> Unit,
    onBack: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {

        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${device.type.replaceFirstChar { it.uppercase() }} · ${findFloor(device.floorId)?.label} / ${findRoomLabel(device.floorId, device.roomId)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(state = device.state)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // --- Manual control (reuses the same bodies as the list cards) ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Control", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        when (device.type) {
                            "multiswitch" -> MultiSwitchBody(device, onToggleChannel)
                            "camera" -> CameraBody(device, onToggle)
                            "iron" -> IronBody(device, onToggle, onIronOverdue)
                            else -> SimpleToggleBody(device, onToggle)
                        }
                    }
                }
            }

            // --- Config (editable where relevant) ---
            if (device.type == "iron") {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Config", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))

                            var durationText by remember(device.id) {
                                mutableStateOf((device.maxOnDuration ?: 1200L).toString())
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = durationText,
                                    onValueChange = { durationText = it.filter(Char::isDigit) },
                                    label = { Text("Max on-duration (seconds)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    durationText.toLongOrNull()?.let { onSaveMaxDuration(it) }
                                }) {
                                    Text("Save")
                                }
                            }
                        }
                    }
                }
            }

            if (device.details.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Notes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            device.details.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }

            // --- Event log, scoped to this device only ---
            item {
                Text(
                    "Recent events",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (alerts.isEmpty()) {
                item {
                    Text(
                        "No events for this device yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(alerts, key = { it.id }) { alert ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                formatter.format(Date(alert.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(alert.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceCard(device: Device, onToggle: () -> Unit, onToggleChannel: (Int) -> Unit, onIronOverdue: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(Modifier.clickable(onClick = onClick))
    ) {
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
                "camera" -> CameraBody(device, onToggle)
                "iron" -> IronBody(device, onToggle, onIronOverdue)
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
fun IronBody(device: Device, onToggle: () -> Unit, onOverdue: () -> Unit) {
    Column {
        SimpleToggleBody(device, onToggle)

        if (device.state == "on" && device.turnedOnAt != null && device.maxOnDuration != null) {
            // Ticks once a second so the countdown actually moves, instead
            // of freezing at whatever value it had when Firebase last pushed.
            var nowMillis by remember(device.turnedOnAt) { mutableStateOf(System.currentTimeMillis()) }
            var overdueFired by remember(device.turnedOnAt) { mutableStateOf(false) }

            LaunchedEffect(device.turnedOnAt) {
                while (true) {
                    nowMillis = System.currentTimeMillis()
                    delay(1000)
                }
            }

            val elapsedSeconds = (nowMillis - device.turnedOnAt) / 1000
            val remaining = (device.maxOnDuration - elapsedSeconds).coerceAtLeast(0)
            val minutes = remaining / 60
            val seconds = remaining % 60

            // Client-side fallback: only fires once, only while this screen
            // is open. The real guarantee ("works even if app is closed")
            // still needs the scheduled backend job deployed separately.
            LaunchedEffect(remaining) {
                if (remaining == 0L && !overdueFired) {
                    overdueFired = true
                    onOverdue()
                }
            }

            Text(
                text = "Auto safety cutoff in %02d:%02d".format(minutes, seconds),
                style = MaterialTheme.typography.labelSmall,
                color = if (remaining <= 30)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (device.maxOnDuration != null) {
            Text(
                "Max on-duration: ${device.maxOnDuration}s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CameraBody(device: Device, onToggle: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Power", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = device.state == "on", onCheckedChange = { onToggle() })
        }
        Spacer(modifier = Modifier.height(8.dp))
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
                    val label = when {
                        device.state == "disconnected" -> "No signal"
                        device.state == "error" -> "Camera error"
                        device.state == "off" -> "Camera off"
                        else -> "Mock camera feed"
                    }
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
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