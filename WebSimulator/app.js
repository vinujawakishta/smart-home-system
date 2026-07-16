import { initializeApp } from "https://www.gstatic.com/firebasejs/10.13.0/firebase-app.js";
import {
  getDatabase,
  ref,
  onValue,
  set,
} from "https://www.gstatic.com/firebasejs/10.13.0/firebase-database.js";

// Same project as your simulator's earlier version — Project settings → Your apps
const firebaseConfig = {
  apiKey: "AIzaSyBaqqagDyLKfqEFMFLJFOP1-DQL2pxg2nk",
  authDomain: "smart-home-system-d7702.firebaseapp.com",
  databaseURL: "https://smart-home-system-d7702-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "smart-home-system-d7702",
  storageBucket: "smart-home-system-d7702.firebasestorage.app",
  messagingSenderId: "649826242719",
  appId: "1:649826242719:web:39ae846901ba3d708152aa",
};

const app = initializeApp(firebaseConfig);
const db = getDatabase(app);
const devicesRef = ref(db, "devices");

const FLOOR_PLAN = [
	{
		id: 'floor1',
		label: '1st Floor',
		rooms: [
			{ id: 'entrance', label: 'Entrance' },
			{ id: 'living', label: 'Living Room' },
			{ id: 'kitchen', label: 'Kitchen' },
			{ id: 'bath1', label: 'Bathroom' },
			{ id: 'stairs', label: 'Staircase' },
		],
	},
	{
		id: 'floor2',
		label: '2nd Floor',
		rooms: [
			{ id: 'master', label: 'Master Bedroom' },
			{ id: 'bed2', label: 'Bedroom 2' },
			{ id: 'study', label: 'Study Room' },
			{ id: 'bath2', label: 'Bathroom' },
			{ id: 'balcony', label: 'Balcony' },
		],
	},
];

// Used only to seed Firebase the first time the database is empty, and as
// the "factory defaults" the Reset button restores a device to.
const DEVICE_SEED = [
	{ id: 'cam-front', type: 'camera', name: 'Front Door Camera', floorId: 'floor1', roomId: 'entrance', state: 'on', details: ['Live snapshot ready'] },
	{ id: 'light-porch', type: 'bulb', name: 'Porch Light', floorId: 'floor1', roomId: 'entrance', state: 'off', details: ['Schedule: 18:00-22:00'] },
	{ id: 'switch-living-5g', type: 'multiswitch', name: 'Living 5-Gang Panel', floorId: 'floor1', roomId: 'living', state: 'on', details: ['Controls main light, fan, TV, decor, socket'], channels: [true, false, true, false, true] },
	{ id: 'outlet-living-1', type: 'outlet', name: 'Living Outlet A', floorId: 'floor1', roomId: 'living', state: 'on', details: ['Load: 36%'] },
	{ id: 'outlet-living-2', type: 'outlet', name: 'Living Outlet B', floorId: 'floor1', roomId: 'living', state: 'off', details: ['Idle'] },
	{ id: 'cam-living', type: 'camera', name: 'Living Camera', floorId: 'floor1', roomId: 'living', state: 'on', details: ['Motion monitor active'] },
	{ id: 'switch-kitchen-4g', type: 'multiswitch', name: 'Kitchen 4-Gang Panel', floorId: 'floor1', roomId: 'kitchen', state: 'on', details: ['Ceiling, exhaust, counter, socket'], channels: [true, true, false, true] },
	{ id: 'iron-kitchen', type: 'iron', name: 'Iron Outlet', floorId: 'floor2', roomId: 'bed2', state: 'off', details: ['Safety timer: 20 minutes max'] },
	{ id: 'outlet-fridge', type: 'outlet', name: 'Refrigerator Outlet', floorId: 'floor1', roomId: 'kitchen', state: 'on', details: ['Always ON'] },
	{ id: 'outlet-microwave', type: 'outlet', name: 'Microwave Outlet', floorId: 'floor1', roomId: 'kitchen', state: 'off', details: ['Ready'] },
	{ id: 'cam-kitchen', type: 'camera', name: 'Kitchen Camera', floorId: 'floor1', roomId: 'kitchen', state: 'disconnected', details: ['Network check needed'] },
	{ id: 'switch-bath1-2g', type: 'multiswitch', name: 'Bathroom 2-Gang', floorId: 'floor1', roomId: 'bath1', state: 'off', details: ['Light and exhaust'], channels: [false, false] },
	{ id: 'light-stairs', type: 'bulb', name: 'Stair Light', floorId: 'floor1', roomId: 'stairs', state: 'on', details: ['Motion linked'] },
	{ id: 'switch-master-4g', type: 'multiswitch', name: 'Master 4-Gang Panel', floorId: 'floor2', roomId: 'master', state: 'on', details: ['Main, fan, lamp, AC'], channels: [true, false, true, true] },
	{ id: 'outlet-master-tv', type: 'outlet', name: 'Master TV Outlet', floorId: 'floor2', roomId: 'master', state: 'on', details: ['Streaming active'] },
	{ id: 'cam-master', type: 'camera', name: 'Master Camera', floorId: 'floor2', roomId: 'master', state: 'off', details: ['Privacy mode'] },
	{ id: 'switch-bed2-3g', type: 'multiswitch', name: 'Bedroom2 3-Gang', floorId: 'floor2', roomId: 'bed2', state: 'off', details: ['Light, fan, AC'], channels: [false, false, false] },
	{ id: 'outlet-bed2', type: 'outlet', name: 'Bedroom2 Outlet', floorId: 'floor2', roomId: 'bed2', state: 'on', details: ['Charging station'] },
	{ id: 'switch-study-3g', type: 'multiswitch', name: 'Study 3-Gang', floorId: 'floor2', roomId: 'study', state: 'on', details: ['Light, fan, computer socket'], channels: [true, false, true] },
	{ id: 'outlet-printer', type: 'outlet', name: 'Printer Outlet', floorId: 'floor2', roomId: 'study', state: 'off', details: ['Standby'] },
	{ id: 'cam-study', type: 'camera', name: 'Study Camera', floorId: 'floor2', roomId: 'study', state: 'on', details: ['Desk view stream'] },
	{ id: 'switch-bath2-2g', type: 'multiswitch', name: 'Bath2 2-Gang', floorId: 'floor2', roomId: 'bath2', state: 'off', details: ['Light and exhaust'], channels: [false, true] },
	{ id: 'light-balcony', type: 'bulb', name: 'Balcony Light', floorId: 'floor2', roomId: 'balcony', state: 'off', details: ['Outdoor mode'] },
	{ id: 'cam-balcony', type: 'camera', name: 'Balcony Camera', floorId: 'floor2', roomId: 'balcony', state: 'error', details: ['Lens obstruction test'] },
];

const elements = {
	floorTabs: document.getElementById('floorTabs'),
	roomTabs: document.getElementById('roomTabs'),
	summaryCards: document.getElementById('summaryCards'),
	deviceTitle: document.getElementById('deviceTitle'),
	deviceCount: document.getElementById('deviceCount'),
	roomSections: document.getElementById('roomSections'),
	template: document.getElementById('deviceCardTemplate'),
};

const stateLabels = {
	on: 'ON',
	off: 'OFF',
	error: 'ERROR',
	disconnected: 'DISCONNECTED',
};

const state = {
	activeFloor: 'floor1',
	activeRoom: 'all',
	devices: [], // populated live from Firebase — see listener at bottom of file
};

function findFloor(floorId) {
	return FLOOR_PLAN.find((floor) => floor.id === floorId);
}

function findRoomLabel(floorId, roomId) {
	const floor = findFloor(floorId);
	const room = floor?.rooms.find((item) => item.id === roomId);
	return room?.label || roomId;
}

function badgeForType(type) {
	const map = {
		camera: 'C',
		bulb: 'B',
		outlet: 'O',
		multiswitch: 'MS',
		iron: 'I',
	};
	return map[type] || 'D';
}

function visibleDevices() {
	return state.devices.filter((device) => {
		const floorMatch = device.floorId === state.activeFloor;
		const roomMatch = state.activeRoom === 'all' || device.roomId === state.activeRoom;
		return floorMatch && roomMatch;
	});
}

function buildRoomGroups(devices) {
	const groups = new Map();
	devices.forEach((device) => {
		if (!groups.has(device.roomId)) {
			groups.set(device.roomId, []);
		}
		groups.get(device.roomId).push(device);
	});
	return groups;
}

function summaryItems() {
	const currentFloorDevices = state.devices.filter((item) => item.floorId === state.activeFloor);
	const currentVisible = visibleDevices();
	const powered = currentFloorDevices.filter((item) => item.state === 'on').length;
	const alerts = currentFloorDevices.filter((item) => item.state === 'error' || item.state === 'disconnected').length;

	return [
		{ label: 'Devices on selected floor', value: currentFloorDevices.length },
		{ label: 'Visible after filters', value: currentVisible.length },
		{ label: 'Powered ON', value: powered },
		{ label: 'Alert states', value: alerts },
	];
}

function renderSummary() {
	elements.summaryCards.innerHTML = summaryItems()
		.map(
			(item) => `
			<article class="metric-card">
				<strong>${item.value}</strong>
				<span class="muted">${item.label}</span>
			</article>
		`,
		)
		.join('');
}

function renderFloorTabs() {
	elements.floorTabs.innerHTML = FLOOR_PLAN.map(
		(floor) => `
			<button type="button" class="selector-btn ${floor.id === state.activeFloor ? 'active' : ''}" data-floor="${floor.id}">${floor.label}</button>
		`,
	).join('');
}

function renderRoomTabs() {
	const floor = findFloor(state.activeFloor);
	const allBtn = `<button type="button" class="selector-btn ${state.activeRoom === 'all' ? 'active' : ''}" data-room="all">All areas</button>`;
	const roomBtns = floor.rooms
		.map(
			(room) => `
			<button type="button" class="selector-btn ${state.activeRoom === room.id ? 'active' : ''}" data-room="${room.id}">${room.label}</button>
		`,
		)
		.join('');

	elements.roomTabs.innerHTML = allBtn + roomBtns;
}

function renderDetails(device) {
	return (device.details || []).map((line) => `<div>${line}</div>`).join('');
}

function renderChannels(device) {
	if (!Array.isArray(device.channels) || device.channels.length === 0) {
		return '';
	}

	return device.channels
		.map(
			(isOn, index) => `
			<div class="channel-row">
				<span>Switch ${index + 1}</span>
				<button type="button" class="channel-btn ${isOn ? 'is-on' : ''}" data-action="toggle-channel" data-channel-index="${index}">
					${isOn ? 'ON' : 'OFF'}
				</button>
			</div>
		`,
		)
		.join('');
}

function renderCard(device) {
	const fragment = elements.template.content.cloneNode(true);
	const card = fragment.querySelector('.device-card');

	card.dataset.deviceId = device.id;
	card.classList.add(`status-${device.state}`);
	fragment.querySelector('[data-role="badge"]').textContent = badgeForType(device.type);
	fragment.querySelector('[data-role="type"]').textContent = device.type;
	fragment.querySelector('[data-role="name"]').textContent = device.name;
	fragment.querySelector('[data-role="path"]').textContent = `${findFloor(device.floorId)?.label} / ${findRoomLabel(device.floorId, device.roomId)}`;

	const statusEl = fragment.querySelector('[data-role="status"]');
	statusEl.textContent = stateLabels[device.state];
	statusEl.className = `status-pill ${device.state}`;

	fragment.querySelector('[data-role="idChip"]').textContent = `ID: ${device.id}`;
	fragment.querySelector('[data-role="roomChip"]').textContent = `Room: ${findRoomLabel(device.floorId, device.roomId)}`;
	fragment.querySelector('[data-role="details"]').innerHTML = renderDetails(device);
	fragment.querySelector('[data-role="channels"]').innerHTML = renderChannels(device);

	return fragment;
}

function renderRooms() {
	const devices = visibleDevices();
	const floor = findFloor(state.activeFloor);
	const roomGroups = buildRoomGroups(devices);

	elements.deviceTitle.textContent = `${floor.label}${state.activeRoom === 'all' ? '' : ` - ${findRoomLabel(state.activeFloor, state.activeRoom)}`}`;
	elements.deviceCount.textContent = `${devices.length} device${devices.length === 1 ? '' : 's'}`;

	if (devices.length === 0) {
		elements.roomSections.innerHTML = '<div class="empty">No devices match the current floor, area, and search filter.</div>';
		return;
	}

	const orderedRooms = floor.rooms.filter((room) => roomGroups.has(room.id));
	elements.roomSections.innerHTML = '';

	orderedRooms.forEach((room) => {
		const section = document.createElement('section');
		section.className = 'room-section';
		section.innerHTML = `
			<header class="room-title">
				<h3>${room.label}</h3>
				<span class="muted">${roomGroups.get(room.id).length} devices</span>
			</header>
			<div class="device-grid"></div>
		`;

		const grid = section.querySelector('.device-grid');
		roomGroups.get(room.id).forEach((device) => {
			grid.appendChild(renderCard(device));
		});

		elements.roomSections.appendChild(section);
	});
}

/* =========================================================================
   FIREBASE WRITES
   -------------------------------------------------------------------------
   updater(device) always returns a *complete* new device object (same
   pattern as before), so we write it with set() — a full replace of that
   device's node — rather than update(), which only merges fields. This
   keeps "Reset" and channel edits correct even when the shape changes.
   We do NOT touch state.devices or call renderAll() here: the onValue()
   listener below does that automatically once Firebase confirms the write,
   which is what gives you real cross-tab / cross-client sync.
   ========================================================================= */

function updateDevice(deviceId, updater) {
	const current = state.devices.find((device) => device.id === deviceId);
	if (!current) {
		return;
	}
	const updated = updater(current);
	const { id, ...rest } = updated; // id is the Firebase key, not stored inside the node
	set(ref(db, `devices/${deviceId}`), rest);
}

function handleDeviceAction(button) {
	const card = button.closest('.device-card');
	if (!card) {
		return;
	}

	const deviceId = card.dataset.deviceId;
	const action = button.dataset.action;

	if (action === 'toggle') {
		updateDevice(deviceId, (device) => ({ ...device, state: device.state === 'on' ? 'off' : 'on' }));
		return;
	}

	if (action === 'error') {
		updateDevice(deviceId, (device) => ({ ...device, state: 'error' }));
		return;
	}

	if (action === 'disconnect') {
		updateDevice(deviceId, (device) => ({ ...device, state: 'disconnected' }));
		return;
	}

	if (action === 'reset') {
		updateDevice(deviceId, () => {
			const original = DEVICE_SEED.find((item) => item.id === deviceId);
			return original ? structuredClone(original) : state.devices.find((item) => item.id === deviceId);
		});
		return;
	}

	if (action === 'toggle-channel') {
		const channelIndex = Number(button.dataset.channelIndex);
		updateDevice(deviceId, (device) => {
			if (!Array.isArray(device.channels)) {
				return device;
			}
			const channels = [...device.channels];
			channels[channelIndex] = !channels[channelIndex];
			const anyOn = channels.some(Boolean);
			return { ...device, channels, state: anyOn ? 'on' : 'off' };
		});
	}
}

function syncRoomForFloor() {
	if (state.activeRoom === 'all') {
		return;
	}

	const valid = findFloor(state.activeFloor)?.rooms.some((room) => room.id === state.activeRoom);
	if (!valid) {
		state.activeRoom = 'all';
	}
}

function renderAll() {
	syncRoomForFloor();
	renderFloorTabs();
	renderRoomTabs();
	renderSummary();
	renderRooms();
}

elements.floorTabs.addEventListener('click', (event) => {
	const button = event.target.closest('[data-floor]');
	if (!button) {
		return;
	}
	state.activeFloor = button.dataset.floor;
	state.activeRoom = 'all';
	renderAll();
});

elements.roomTabs.addEventListener('click', (event) => {
	const button = event.target.closest('[data-room]');
	if (!button) {
		return;
	}
	state.activeRoom = button.dataset.room;
	renderAll();
});

elements.roomSections.addEventListener('click', (event) => {
	const button = event.target.closest('[data-action]');
	if (!button) {
		return;
	}
	handleDeviceAction(button);
});

/* =========================================================================
   FIREBASE READS
   ========================================================================= */

// One-time seed: only writes if /devices is currently empty.
onValue(
	devicesRef,
	(snapshot) => {
		if (!snapshot.exists()) {
			const seedObject = {};
			DEVICE_SEED.forEach((device) => {
				const { id, ...rest } = device;
				seedObject[id] = rest;
			});
			set(devicesRef, seedObject);
		}
	},
	{ onlyOnce: true },
);

// Live listener — the real sync mechanism. Fires on first load and again
// every time any client writes to /devices, whether that's this simulator,
// another browser tab, or later the mobile app.
onValue(devicesRef, (snapshot) => {
	const val = snapshot.val() || {};
	state.devices = Object.entries(val).map(([id, data]) => ({ id, ...data }));
	renderAll();
});