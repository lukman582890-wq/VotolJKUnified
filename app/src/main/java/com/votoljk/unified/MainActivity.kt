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
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.Button
import android.os.Bundle
import android.widget.*
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MainActivity : Activity() {
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var adapter: BluetoothAdapter
    private var votolSocket: BluetoothSocket? = null
    private var jkGatt: BluetoothGatt? = null
    private val classic = linkedMapOf<String, BluetoothDevice>()
    private val ble = linkedMapOf<String, BluetoothDevice>()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var votolStatus: TextView; private lateinit var jkStatus: TextView
    private lateinit var votolDevices: TextView; private lateinit var jkDevices: TextView; private lateinit var log: TextView
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.action == BluetoothDevice.ACTION_FOUND) {
                val d = i.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
                classic[d.address] = d; renderClassic()
            }
        }
    }
    

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

        findViewById<android.widget.TextView>(R.id.speedValue).text =
            String.format("%.0f", location.speed * 3.6f)

        findViewById<android.widget.TextView>(R.id.tripDistance).text =
            String.format("Jarak     %.2f km", tripDistanceKm)

        findViewById<android.widget.TextView>(R.id.tripTime).text =
            "Waktu     " + formatTripTime(System.currentTimeMillis() - tripStartTime)
    }
}

private val tripTimer = object : Runnable {
    override fun run() {
        if (tripRunning) {
            findViewById<android.widget.TextView>(R.id.tripTime).text =
                "Waktu     " + formatTripTime(System.currentTimeMillis() - tripStartTime)
            tripHandler.postDelayed(this, 1000)
        }
    }
}

private fun formatTripTime(ms: Long): String {
    val total = ms / 1000
    return String.format("%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60)
}

private fun startRealTrip() {
    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), 9001
        )
        return
    }

    tripRunning = true
    tripDistanceKm = 0.0
    lastTripLocation = null
    tripStartTime = System.currentTimeMillis()

    findViewById<android.widget.TextView>(R.id.tripDistance).text = "Jarak     0.00 km"
    findViewById<android.widget.TextView>(R.id.tripTime).text = "Waktu     00:00:00"

    locationManager.requestLocationUpdates(
        LocationManager.GPS_PROVIDER,
        1000L,
        1f,
        tripLocationListener
    )
    tripHandler.post(tripTimer)
}

private fun stopRealTrip() {
    tripRunning = false
    if (::locationManager.isInitialized) {
        locationManager.removeUpdates(tripLocationListener)
    }
    tripHandler.removeCallbacks(tripTimer)

    findViewById<android.widget.TextView>(R.id.tripDistance).text =
        String.format("Jarak     %.2f km", tripDistanceKm)
}

override fun onCreate(b: Bundle?) { super.onCreate(b); setContentView(R.layout.activity_main)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        findViewById<Button>(R.id.startTrip).setOnClickListener { startRealTrip() }
        findViewById<Button>(R.id.endTrip).setOnClickListener { stopRealTrip() }

        adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        votolStatus=findViewById(R.id.votolStatus); jkStatus=findViewById(R.id.jkStatus); votolDevices=findViewById(R.id.votolDevices); jkDevices=findViewById(R.id.jkDevices); log=findViewById(R.id.log)
        findViewById<Button>(R.id.votolScan).setOnClickListener { scanClassic() }; findViewById<Button>(R.id.votolDisconnect).setOnClickListener { disconnectVotol() }
        findViewById<Button>(R.id.jkScan).setOnClickListener { scanBle() }; findViewById<Button>(R.id.jkDisconnect).setOnClickListener { disconnectJk() }
        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        requestBtPermissions()
    }
    private fun requestBtPermissions() { val p=if(android.os.Build.VERSION.SDK_INT>=31) arrayOf(Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION); if(p.any{checkSelfPermission(it)!=PackageManager.PERMISSION_GRANTED}) requestPermissions(p,42) }
    private fun scanClassic() { if(!adapter.isEnabled){startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)); return}; classic.clear(); adapter.cancelDiscovery(); adapter.bondedDevices.forEach{classic[it.address]=it}; if(adapter.startDiscovery()) { votolStatus.text="SCANNING…"; votolStatus.setTextColor(0xFFFFB86B.toInt()); log.text="Classic discovery active. Tap a device to connect."; handler.postDelayed({ if(adapter.isDiscovering) adapter.cancelDiscovery(); if(votolStatus.text=="SCANNING…") votolStatus.text="SELECT DEVICE" },12000) }; renderClassic() }
    private fun renderClassic(){ val rows=classic.values.map{d->"${d.name ?: "Unknown"}\n${d.address}"}; votolDevices.text=if(rows.isEmpty())"No devices found. Pair VOTOL in Android Bluetooth settings first if it does not appear." else rows.joinToString("\n\n"); votolDevices.setOnClickListener { val d=classic.values.firstOrNull(); if(d!=null) connectVotol(d) } }
    private fun connectVotol(d:BluetoothDevice){ Thread{runCatching{adapter.cancelDiscovery(); runOnUiThread{votolStatus.text="CONNECTING…"}; val s=d.createRfcommSocketToServiceRecord(SPP_UUID); s.connect(); votolSocket=s; runOnUiThread{votolStatus.text="CONNECTED"; votolStatus.setTextColor(0xFF45D6A3.toInt()); log.text="VOTOL SPP connected: ${d.name ?: d.address}. Raw packets will be captured; decoder pending."}; readClassic(s.inputStream)}.onFailure{e->runOnUiThread{votolStatus.text="ERROR"; log.text="VOTOL: ${e.message}"}}}.start() }
    private fun readClassic(input:InputStream){ val buf=ByteArray(1024); while(votolSocket?.isConnected==true){ val n=runCatching{input.read(buf)}.getOrDefault(-1); if(n<=0)break; val hex=buf.copyOf(n).joinToString(" "){String.format("%02X",it)}; runOnUiThread{log.text="VOTOL RX ${n}B  $hex"} } }
    private fun disconnectVotol(){runCatching{votolSocket?.close()};votolSocket=null;votolStatus.text="DISCONNECTED";votolStatus.setTextColor(0xFFFFB86B.toInt())}
    private var scanner:BluetoothLeScanner?=null
    private val bleCallback=object:ScanCallback(){override fun onScanResult(t:Int,r:ScanResult){val d=r.device;ble[d.address]=d;runOnUiThread{renderBle()}} override fun onScanFailed(e:Int){runOnUiThread{jkStatus.text="SCAN ERROR $e"}}}
    private fun scanBle(){ if(!adapter.isEnabled){startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));return}; ble.clear(); scanner=adapter.bluetoothLeScanner; scanner?.startScan(bleCallback); jkStatus.text="SCANNING…"; log.text="BLE scan active. Tap a JK BMS device to connect."; handler.postDelayed({scanner?.stopScan(bleCallback);if(jkStatus.text=="SCANNING…")jkStatus.text="SELECT DEVICE"},10000);renderBle() }
    private fun renderBle(){ val rows=ble.values.map{d->"${d.name ?: "Unnamed BLE device"}\n${d.address}"};jkDevices.text=if(rows.isEmpty())"Scanning…" else rows.joinToString("\n\n");jkDevices.setOnClickListener{ble.values.firstOrNull()?.let{connectJk(it)}} }
    private fun connectJk(d:BluetoothDevice){jkStatus.text="CONNECTING…";log.text="JK BMS BLE connecting to ${d.name ?: d.address}";jkGatt=d.connectGatt(this,false,gattCallback)}
    private val gattCallback=object:BluetoothGattCallback(){override fun onConnectionStateChange(g:BluetoothGatt,s:Int,n:Int){runOnUiThread{if(n==BluetoothProfile.STATE_CONNECTED){jkStatus.text="CONNECTED";jkStatus.setTextColor(0xFF45D6A3.toInt());log.text="JK BMS BLE connected. Discovering services…";g.discoverServices()}else{jkStatus.text="DISCONNECTED";jkStatus.setTextColor(0xFFFFB86B.toInt())}}};override fun onServicesDiscovered(g:BluetoothGatt,s:Int){val count=g.services.sumOf{it.characteristics.size};runOnUiThread{log.text="JK BMS GATT ready: ${g.services.size} services / $count characteristics. Protocol UUIDs not assumed."}}}
    private fun disconnectJk(){runCatching{jkGatt?.disconnect();jkGatt?.close()};jkGatt=null;jkStatus.text="DISCONNECTED";jkStatus.setTextColor(0xFFFFB86B.toInt())}
    override fun onDestroy(){unregisterReceiver(receiver);disconnectVotol();disconnectJk();super.onDestroy()}
}
