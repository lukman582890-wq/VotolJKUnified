package com.votoljk.unified

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.io.InputStream
import java.util.UUID

class MainActivity : Activity() {
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val JK_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val JK_WRITE_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private lateinit var adapter: BluetoothAdapter
    private var votolSocket: BluetoothSocket? = null
    private var jkGatt: BluetoothGatt? = null
    private var jkConnectedAddress: String? = null
    private val classic = linkedMapOf<String, BluetoothDevice>()
    private val ble = linkedMapOf<String, BluetoothDevice>()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var votolStatus: TextView
    private lateinit var jkStatus: TextView
    private lateinit var votolDevices: TextView
    private lateinit var jkDevices: TextView
    private lateinit var log: TextView
    private var jkCommandCounter = 0
    private val jkRxFrameBuffer = ArrayList<Byte>()

    private lateinit var locationManager: LocationManager
    private var tripRunning = false
    private var lastTripLocation: Location? = null
    private var tripDistanceKm = 0.0
    private var tripStartTime = 0L
    private val tripHandler = Handler(Looper.getMainLooper())

    private val tripLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (!tripRunning) return
            lastTripLocation?.let {
                val delta = it.distanceTo(location)
                if (delta > 2f) tripDistanceKm += delta / 1000.0
            }
            lastTripLocation = location
            findViewById<TextView>(R.id.speedValue).text = String.format("%.0f", location.speed * 3.6f)
            findViewById<TextView>(R.id.tripDistance).text = String.format("Jarak     %.2f km", tripDistanceKm)
            findViewById<TextView>(R.id.tripTime).text = "Waktu     " + formatTripTime(System.currentTimeMillis() - tripStartTime)
        }
    }

    private val tripTimer = object : Runnable {
        override fun run() {
            if (tripRunning) {
                findViewById<TextView>(R.id.tripTime).text = "Waktu     " + formatTripTime(System.currentTimeMillis() - tripStartTime)
                tripHandler.postDelayed(this, 1000)
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.action == BluetoothDevice.ACTION_FOUND) {
                val d = i.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
                classic[d.address] = d
                renderClassic()
            }
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_main)

        votolStatus = findViewById(R.id.votolStatus)
        jkStatus = findViewById(R.id.jkStatus)
        votolDevices = findViewById(R.id.votolDevices)
        jkDevices = findViewById(R.id.jkDevices)
        log = findViewById(R.id.log)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter

        findViewById<Button>(R.id.exportLog).setOnClickListener { exportSystemLog() }
        findViewById<Button>(R.id.bmsDetail).setOnClickListener {
            startActivity(Intent(this, BmsDetailActivity::class.java))
        }
        findViewById<Button>(R.id.startTrip).setOnClickListener { startRealTrip() }
        findViewById<Button>(R.id.endTrip).setOnClickListener { stopRealTrip() }
        findViewById<EditText>(R.id.tripOrigin).apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { launchLocationPicker("ORIGIN") }
        }
        findViewById<EditText>(R.id.tripDestination).apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { launchLocationPicker("DESTINATION") }
        }
        findViewById<Button>(R.id.votolScan).setOnClickListener { scanClassic() }
        findViewById<Button>(R.id.votolDisconnect).setOnClickListener { disconnectVotol() }
        findViewById<Button>(R.id.jkScan).setOnClickListener { scanBle() }
        findViewById<Button>(R.id.jkDisconnect).setOnClickListener { disconnectJk() }

        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        requestBtPermissions()
    }

    private fun requestBtPermissions() {
        val p = if (android.os.Build.VERSION.SDK_INT >= 31) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (p.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) requestPermissions(p, 42)
    }

    private fun scanClassic() {
        if (!adapter.isEnabled) {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        classic.clear()
        adapter.cancelDiscovery()
        adapter.bondedDevices.forEach { classic[it.address] = it }
        if (adapter.startDiscovery()) {
            votolStatus.text = "SCANNING…"
            log.text = "Classic discovery active. Select a VOTOL device below."
            handler.postDelayed({
                if (adapter.isDiscovering) adapter.cancelDiscovery()
                if (votolStatus.text == "SCANNING…") votolStatus.text = "SELECT DEVICE"
            }, 12000)
        }
        renderClassic()
    }

    private fun renderClassic() {
        val rows = classic.values.map { d -> "${d.name ?: "Unknown"}\n${d.address}" }
        votolDevices.text = if (rows.isEmpty()) "No devices found. Pair VOTOL in Android Bluetooth settings first if needed." else rows.joinToString("\n\n")
        votolDevices.setOnClickListener {
            showClassicPicker()
        }
    }

    private fun showClassicPicker() {
        val devices = classic.values.toList()
        if (devices.isEmpty()) return
        val labels = devices.map { "${it.name ?: "Unknown"}\n${it.address}" }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("Pilih VOTOL Controller")
            .setSingleChoiceItems(labels, -1) { dialog, which ->
                connectVotol(devices[which])
                dialog.dismiss()
            }
            .setNegativeButton("BATAL", null)
            .show()
    }

    private fun connectVotol(d: BluetoothDevice) {
        Thread {
            runCatching {
                adapter.cancelDiscovery()
                runOnUiThread { votolStatus.text = "CONNECTING…" }
                val s = d.createRfcommSocketToServiceRecord(SPP_UUID)
                s.connect()
                votolSocket = s
                runOnUiThread {
                    votolStatus.text = "CONNECTED"
                    votolStatus.setTextColor(0xFF45D6A3.toInt())
                    log.text = "VOTOL SPP connected: ${d.name ?: d.address}."
                }
                readClassic(s.inputStream)
            }.onFailure { e ->
                runOnUiThread {
                    votolStatus.text = "ERROR"
                    log.text = "VOTOL: ${e.message}"
                }
            }
        }.start()
    }

    private fun readClassic(input: InputStream) {
        val buf = ByteArray(1024)
        while (votolSocket?.isConnected == true) {
            val n = runCatching { input.read(buf) }.getOrDefault(-1)
            if (n <= 0) break
            val hex = buf.copyOf(n).joinToString(" ") { String.format("%02X", it) }
            runOnUiThread { log.text = "VOTOL RX ${n}B  $hex" }
        }
    }

    private fun disconnectVotol() {
        runCatching { votolSocket?.close() }
        votolSocket = null
        votolStatus.text = "DISCONNECTED"
        votolStatus.setTextColor(0xFFFFB86B.toInt())
    }

    private var scanner: BluetoothLeScanner? = null
    private val bleCallback = object : ScanCallback() {
        override fun onScanResult(t: Int, r: ScanResult) {
            val d = r.device
            ble[d.address] = d
            runOnUiThread { renderBle() }
        }
        override fun onScanFailed(e: Int) {
            runOnUiThread {
                jkStatus.text = "SCAN ERROR $e"
                log.text = "BLE scan failed: $e"
            }
        }
    }

    private fun scanBle() {
        if (!adapter.isEnabled) {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        ble.clear()
        scanner = adapter.bluetoothLeScanner
        scanner?.stopScan(bleCallback)
        scanner?.startScan(bleCallback)
        jkStatus.text = "SCANNING…"
        log.text = "BLE scan active. Select the JK BMS device you want to connect."
        handler.postDelayed({
            scanner?.stopScan(bleCallback)
            if (jkStatus.text == "SCANNING…") jkStatus.text = "SELECT DEVICE"
        }, 10000)
        renderBle()
    }

    private fun renderBle() {
        val rows = ble.values.map { d -> "${d.name ?: "Unnamed BLE device"}\n${d.address}" }
        jkDevices.text = if (rows.isEmpty()) "Scanning…" else rows.joinToString("\n\n")
        jkDevices.setOnClickListener { showJkPicker() }
    }

    private fun showJkPicker() {
        val devices = ble.values.toList()
        if (devices.isEmpty()) {
            Toast.makeText(this, "Belum ada device BLE ditemukan", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = devices.map { "${it.name ?: "Unnamed BLE device"}\n${it.address}" }.toTypedArray()
        val selected = devices.indexOfFirst { it.address == jkConnectedAddress }
        android.app.AlertDialog.Builder(this)
            .setTitle("Pilih JK BMS")
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                connectJk(devices[which])
                dialog.dismiss()
            }
            .setNegativeButton("BATAL", null)
            .show()
    }

    private fun connectJk(d: BluetoothDevice) {
        runCatching { jkGatt?.disconnect(); jkGatt?.close() }
        jkGatt = null
        jkConnectedAddress = d.address
        jkRxFrameBuffer.clear()
        jkStatus.text = "CONNECTING…"
        log.text = "JK BMS BLE connecting to ${d.name ?: d.address}"
        jkGatt = d.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            runOnUiThread {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    jkStatus.text = "CONNECTED"
                    jkStatus.setTextColor(0xFF45D6A3.toInt())
                    log.text = "JK BMS BLE connected to ${g.device.name ?: g.device.address}. Discovering services…"
                    g.discoverServices()
                } else {
                    if (jkGatt === g) jkGatt = null
                    jkStatus.text = "DISCONNECTED"
                    jkStatus.setTextColor(0xFFFFB86B.toInt())
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val lines = mutableListOf<String>()
            for (service in g.services) {
                lines.add("SERVICE ${service.uuid}")
                for (c in service.characteristics) {
                    val props = mutableListOf<String>()
                    if ((c.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0) props.add("READ")
                    if ((c.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) props.add("WRITE")
                    if ((c.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) props.add("WRITE_NR")
                    if ((c.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) props.add("NOTIFY")
                    if ((c.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) props.add("INDICATE")
                    lines.add("  CHAR ${c.uuid} [${props.joinToString(", ")}]")
                    if ((c.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        g.setCharacteristicNotification(c, true)
                        val cccd = c.getDescriptor(CCCD_UUID)
                        if (cccd != null) {
                            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            g.writeDescriptor(cccd)
                        }
                    }
                }
            }
            runOnUiThread { log.text = "JK BMS GATT\n" + lines.joinToString("\n") }

            handler.postDelayed({ sendJkCommand(g, 0x96) }, 1200)
            handler.postDelayed({ sendJkCommand(g, 0x97) }, 2200)
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            val data = c.value ?: return
            appendJkRxChunk(data)
            val hex = data.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
            runOnUiThread { log.append("\nJK RX [${c.uuid}] $hex\n") }
        }
    }

    private fun sendJkCommand(g: BluetoothGatt, command: Int) {
        if (g !== jkGatt) return
        val service = g.getService(JK_SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(JK_WRITE_UUID) ?: return
        val frame = ByteArray(20)
        frame[0] = 0xAA.toByte(); frame[1] = 0x55.toByte(); frame[2] = 0x90.toByte(); frame[3] = 0xEB.toByte(); frame[4] = command.toByte()
        frame[16] = (jkCommandCounter and 0xFF).toByte()
        jkCommandCounter = (jkCommandCounter + 1) and 0xFF
        var crc = 0
        for (i in 0..18) crc = (crc + (frame[i].toInt() and 0xFF)) and 0xFF
        frame[19] = crc.toByte()
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        characteristic.value = frame
        val ok = g.writeCharacteristic(characteristic)
        val hex = frame.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
        runOnUiThread { log.append("\nJK TX [${characteristic.uuid}] $hex\nWRITE RESULT: $ok\n") }
    }

    private fun appendJkRxChunk(value: ByteArray) {
        if (value.contentEquals(byteArrayOf(0x41, 0x54, 0x0D, 0x0A))) return
        jkRxFrameBuffer.addAll(value.toList())
        while (true) {
            var start = -1
            for (i in 0..jkRxFrameBuffer.size - 4) {
                if (jkRxFrameBuffer[i] == 0x55.toByte() && jkRxFrameBuffer[i + 1] == 0xAA.toByte() && jkRxFrameBuffer[i + 2] == 0xEB.toByte() && jkRxFrameBuffer[i + 3] == 0x90.toByte()) { start = i; break }
            }
            if (start < 0) {
                if (jkRxFrameBuffer.size > 3) {
                    val keep = jkRxFrameBuffer.takeLast(3); jkRxFrameBuffer.clear(); jkRxFrameBuffer.addAll(keep)
                }
                return
            }
            if (start > 0) repeat(start) { jkRxFrameBuffer.removeAt(0) }
            if (jkRxFrameBuffer.size < 300) return
            val frame = jkRxFrameBuffer.take(300).toByteArray()
            var crc = 0
            for (i in 0 until 299) crc = (crc + (frame[i].toInt() and 0xFF)) and 0xFF
            if (crc != (frame[299].toInt() and 0xFF)) { jkRxFrameBuffer.removeAt(0); continue }
            repeat(300) { jkRxFrameBuffer.removeAt(0) }
            if ((frame[4].toInt() and 0xFF) == 0x02) parseJkRuntimeFrame(frame)
        }
    }

    private fun u16(f: ByteArray, o: Int) = (f[o].toInt() and 0xFF) or ((f[o + 1].toInt() and 0xFF) shl 8)
    private fun i16(f: ByteArray, o: Int) = u16(f, o).let { if (it and 0x8000 != 0) it - 0x10000 else it }
    private fun i32(f: ByteArray, o: Int) = (f[o].toInt() and 0xFF) or ((f[o + 1].toInt() and 0xFF) shl 8) or ((f[o + 2].toInt() and 0xFF) shl 16) or ((f[o + 3].toInt() and 0xFF) shl 24)
    private fun u32(f: ByteArray, o: Int) = i32(f, o).toLong() and 0xFFFFFFFFL

    private fun parseJkRuntimeFrame(frame: ByteArray) {
        val mask = (frame[70].toInt() and 0xFF) or ((frame[71].toInt() and 0xFF) shl 8) or ((frame[72].toInt() and 0xFF) shl 16) or ((frame[73].toInt() and 0xFF) shl 24)
        val cellCount = (0 until 32).count { (mask and (1 shl it)) != 0 }.coerceAtMost(20)
        val cells = ArrayList<Float>(); val resistances = ArrayList<Float>()
        for (i in 0 until cellCount) { cells.add(u16(frame, 6 + i * 2) * 0.001f); resistances.add(u16(frame, 80 + i * 2) * 0.001f) }
        val data = JkBmsData(
            cellCount = cellCount,
            cellVoltages = cells,
            cellResistances = resistances,
            totalVoltage = i32(frame, 150) * 0.001f,
            current = i32(frame, 158) * 0.001f,
            power = u32(frame, 154) * 0.001f,
            temp1 = i16(frame, 162) * 0.1f,
            temp2 = i16(frame, 164) * 0.1f,
            mosTemp = i16(frame, 144) * 0.1f,
            soc = frame[173].toInt() and 0xFF,
            remainingAh = u32(frame, 174) * 0.001f,
            fullAh = u32(frame, 178) * 0.001f,
            cycleCount = u32(frame, 182).toInt(),
            cycleCapacityAh = u32(frame, 186) * 0.001f,
            soh = frame[190].toInt() and 0xFF,
            balanceCurrent = i16(frame, 170) * 0.001f,
            balanceOn = (frame[172].toInt() and 0xFF) != 0,
            chargeMos = (frame[198].toInt() and 0xFF) != 0,
            dischargeMos = (frame[199].toInt() and 0xFF) != 0,
            precharge = (frame[200].toInt() and 0xFF) != 0,
            balancer = (frame[201].toInt() and 0xFF) != 0,
            emergencySeconds = 0,
            sleepSeconds = 0L,
            batteryType = "LFP",
            chargeStatus = 0
        )
        JkBmsDataStore.latest = data
        runOnUiThread { updateJkDashboard(data) }
    }

    private fun updateJkDashboard(data: JkBmsData) {
        val delta = if (data.cellVoltages.isEmpty()) 0f else data.cellVoltages.maxOrNull()!! - data.cellVoltages.minOrNull()!!
        findViewById<TextView>(R.id.bmsSocValue).text = "${data.soc} %"
        findViewById<TextView>(R.id.bmsVoltageValue).text = "%.3f V".format(data.totalVoltage)
        findViewById<TextView>(R.id.bmsCurrentValue).text = "CURRENT\n%.3f A".format(data.current)
        findViewById<TextView>(R.id.bmsPowerValue).text = "POWER\n%.0f W".format(data.power)
        findViewById<TextView>(R.id.bmsDeltaValue).text = "DELTA CELL\n%.3f V".format(delta)
        findViewById<TextView>(R.id.bmsTempValue).text = "BMS TEMP\n%.1f °C".format(data.temp1)
        findViewById<TextView>(R.id.systemTempValues).text = "Controller     -- °C\n\nMotor              -- °C\n\nBattery            %.1f °C".format(data.temp1)
        log.append("\nJK DATA: %dS | %.3fV | %.3fA | %.0fW | SOC %d%% | Δ %.3fV | T %.1f°C\n".format(data.cellCount, data.totalVoltage, data.current, data.power, data.soc, delta, data.temp1))
    }

    private fun startRealTrip() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 9001); return
        }
        tripRunning = true; tripDistanceKm = 0.0; lastTripLocation = null; tripStartTime = System.currentTimeMillis()
        findViewById<TextView>(R.id.tripDistance).text = "Jarak     0.00 km"; findViewById<TextView>(R.id.tripTime).text = "Waktu     00:00:00"
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, tripLocationListener)
        tripHandler.post(tripTimer)
    }

    private fun stopRealTrip() {
        tripRunning = false
        if (::locationManager.isInitialized) locationManager.removeUpdates(tripLocationListener)
        tripHandler.removeCallbacks(tripTimer)
        findViewById<TextView>(R.id.tripDistance).text = String.format("Jarak     %.2f km", tripDistanceKm)
    }

    private fun formatTripTime(ms: Long): String { val total = ms / 1000; return String.format("%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60) }

    private fun launchLocationPicker(mode: String) {
        startActivityForResult(Intent(this, LocationPickerActivity::class.java).apply { putExtra("MODE", mode) }, 700)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 700 && resultCode == RESULT_OK && data != null) {
            val label = data.getStringExtra("LABEL") ?: return
            if (data.getStringExtra("MODE") == "ORIGIN") findViewById<EditText>(R.id.tripOrigin).setText(label) else findViewById<EditText>(R.id.tripDestination).setText(label)
        }
    }

    private fun disconnectJk() {
        runCatching { jkGatt?.disconnect(); jkGatt?.close() }
        jkGatt = null; jkConnectedAddress = null; jkRxFrameBuffer.clear()
        jkStatus.text = "DISCONNECTED"; jkStatus.setTextColor(0xFFFFB86B.toInt())
    }

    private fun exportSystemLog() {
        val text = log.text.toString()
        if (text.isBlank()) { Toast.makeText(this, "SYSTEM LOG masih kosong", Toast.LENGTH_SHORT).show(); return }
        val fileName = "VotolJKUnified_LOG_${System.currentTimeMillis()}.txt"
        val values = ContentValues().apply {
            put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/VotolJKUnified")
        }
        val uri = runCatching { contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) }.getOrNull()
        if (uri == null) { Toast.makeText(this, "Gagal membuat file log", Toast.LENGTH_SHORT).show(); return }
        runCatching { contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) } }.onFailure {
            contentResolver.delete(uri, null, null); Toast.makeText(this, "Gagal menyimpan log", Toast.LENGTH_SHORT).show(); return
        }
        Toast.makeText(this, "Log tersimpan di Download/VotolJKUnified", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(receiver) }
        stopRealTrip()
        disconnectJk()
        disconnectVotol()
        super.onDestroy()
    }
}
