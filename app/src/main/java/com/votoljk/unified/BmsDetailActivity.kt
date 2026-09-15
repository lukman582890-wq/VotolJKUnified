package com.votoljk.unified

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class BmsDetailActivity : Activity() {
    private val cyan = Color.rgb(0, 232, 255)
    private val green = Color.rgb(0, 245, 140)
    private val gray = Color.rgb(145, 164, 197)
    private val white = Color.WHITE
    private val handler = Handler(Looper.getMainLooper())
    private val realtimeRows = linkedMapOf<String, TextView>()
    private val batteryRows = linkedMapOf<String, TextView>()
    private val cellRows = ArrayList<TextView>()
    private val resistanceRows = ArrayList<TextView>()
    private var statusRow: TextView? = null
    private var detailsRow: TextView? = null

    private val refresh = object : Runnable {
        override fun run() {
            updateFromStore()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(5, 8, 14)) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 20, 16, 30)
        }

        root.addView(title("JK BMS  •  DETAIL"))
        root.addView(subtitle("20S LFP BATTERY SYSTEM"))
        root.addView(section("REAL-TIME"))

        realtimeRows["power"] = addRow(root, "Battery Power", "-- W")
        realtimeRows["capacity"] = addRow(root, "Capacity", "-- Ah")
        realtimeRows["remaining"] = addRow(root, "Remaining Capacity", "-- Ah")
        realtimeRows["mos"] = addRow(root, "MOS / CMOS Temp", "-- °C")
        realtimeRows["t1"] = addRow(root, "Battery T1", "-- °C")
        realtimeRows["t2"] = addRow(root, "Battery T2", "-- °C")
        realtimeRows["emergency"] = addRow(root, "Emergency Timer", "-- s")
        realtimeRows["sleep"] = addRow(root, "Sleep Timer", "-- s")
        realtimeRows["alarm"] = addRow(root, "LCD Alarm", "--")
        realtimeRows["type"] = addRow(root, "Cell Type", "LFP")
        realtimeRows["average"] = addRow(root, "Cell Average", "-- V")
        realtimeRows["delta"] = addRow(root, "Voltage Difference", "-- V")
        realtimeRows["balance"] = addRow(root, "Balance Current", "-- A")
        realtimeRows["cycles"] = addRow(root, "Cycle Count", "--")
        realtimeRows["cycleAh"] = addRow(root, "Cycle Capacity", "-- Ah")
        realtimeRows["charger"] = addRow(root, "Charger Status", "--")
        realtimeRows["balancer"] = addRow(root, "Balancer", "--")

        root.addView(section("BATTERY"))
        batteryRows["voltage"] = addRow(root, "Battery Voltage", "-- V", cyan)
        batteryRows["current"] = addRow(root, "Battery Current", "-- A", cyan)
        batteryRows["power"] = addRow(root, "Battery Power", "-- W", cyan)

        root.addView(section("CELL VOLTAGES"))
        for (i in 1..20) cellRows.add(addRow(root, "Cell %02d".format(i), "-- V", cyan))

        root.addView(section("BALANCE WIRE RESISTANCE"))
        for (i in 1..20) resistanceRows.add(addRow(root, "Cell %02d".format(i), "-- Ω", green))

        root.addView(section("ALARMS / PROTECTION"))
        realtimeRows["protection"] = addRow(root, "Protection Status", "--")
        realtimeRows["chargeMos"] = addRow(root, "Charge MOS", "--")
        realtimeRows["dischargeMos"] = addRow(root, "Discharge MOS", "--")
        statusRow = addRow(root, "BMS Status", "WAITING FOR DATA", gray)

        root.addView(section("DETAILS LOG"))
        detailsRow = addRow(root, "JK BMS data", "Belum ada data diterima", gray)

        scroll.addView(root)
        setContentView(scroll)
        updateFromStore()
        handler.post(refresh)
    }

    private fun addRow(root: LinearLayout, label: String, value: String, color: Int = white): TextView {
        val v = row(label, value, color)
        root.addView(v)
        return v
    }

    private fun setValue(row: TextView?, label: String, value: String) {
        row?.text = "$label    $value"
    }

    private fun updateFromStore() {
        val d = JkBmsDataStore.latest ?: return
        val delta = if (d.cellVoltages.isEmpty()) 0f else d.cellVoltages.maxOrNull()!! - d.cellVoltages.minOrNull()!!
        val avg = if (d.cellVoltages.isEmpty()) 0f else d.cellVoltages.average().toFloat()

        setValue(realtimeRows["power"], "Battery Power", "%.0f W".format(Locale.US, d.power))
        setValue(realtimeRows["capacity"], "Capacity", "%.3f Ah".format(Locale.US, d.fullAh))
        setValue(realtimeRows["remaining"], "Remaining Capacity", "%.3f Ah".format(Locale.US, d.remainingAh))
        setValue(realtimeRows["mos"], "MOS / CMOS Temp", "%.1f °C".format(Locale.US, d.mosTemp))
        setValue(realtimeRows["t1"], "Battery T1", "%.1f °C".format(Locale.US, d.temp1))
        setValue(realtimeRows["t2"], "Battery T2", "%.1f °C".format(Locale.US, d.temp2))
        setValue(realtimeRows["emergency"], "Emergency Timer", "%d s".format(d.emergencySeconds))
        setValue(realtimeRows["sleep"], "Sleep Timer", "%d s".format(d.sleepSeconds))
        setValue(realtimeRows["alarm"], "LCD Alarm", "NORMAL")
        setValue(realtimeRows["type"], "Cell Type", d.batteryType)
        setValue(realtimeRows["average"], "Cell Average", "%.3f V".format(Locale.US, avg))
        setValue(realtimeRows["delta"], "Voltage Difference", "%.3f V".format(Locale.US, delta))
        setValue(realtimeRows["balance"], "Balance Current", "%.3f A".format(Locale.US, d.balanceCurrent))
        setValue(realtimeRows["cycles"], "Cycle Count", "%d".format(d.cycleCount))
        setValue(realtimeRows["cycleAh"], "Cycle Capacity", "%.3f Ah".format(Locale.US, d.cycleCapacityAh))
        setValue(realtimeRows["charger"], "Charger Status", if (d.chargeStatus != 0) "ON" else "OFF")
        setValue(realtimeRows["balancer"], "Balancer", if (d.balancer || d.balanceOn) "ON" else "OFF")
        setValue(realtimeRows["protection"], "Protection Status", if (d.emergencySeconds > 0) "ACTIVE" else "NORMAL")
        setValue(realtimeRows["chargeMos"], "Charge MOS", if (d.chargeMos) "ON" else "OFF")
        setValue(realtimeRows["dischargeMos"], "Discharge MOS", if (d.dischargeMos) "ON" else "OFF")
        setValue(statusRow, "BMS Status", "ONLINE • ${d.cellCount}S • SOH ${d.soh}%")
        setValue(detailsRow, "JK BMS data", "LIVE • SOC ${d.soc}% • %.3f V • %.3f A".format(Locale.US, d.totalVoltage, d.current))

        setValue(batteryRows["voltage"], "Battery Voltage", "%.3f V".format(Locale.US, d.totalVoltage))
        setValue(batteryRows["current"], "Battery Current", "%.3f A".format(Locale.US, d.current))
        setValue(batteryRows["power"], "Battery Power", "%.0f W".format(Locale.US, d.power))

        for (i in cellRows.indices) {
            val v = d.cellVoltages.getOrNull(i)
            setValue(cellRows[i], "Cell %02d".format(i + 1), if (v == null) "-- V" else "%.3f V".format(Locale.US, v))
        }
        for (i in resistanceRows.indices) {
            val v = d.cellResistances.getOrNull(i)
            setValue(resistanceRows[i], "Cell %02d".format(i + 1), if (v == null) "-- Ω" else "%.3f Ω".format(Locale.US, v))
        }
    }

    private fun title(text: String) = TextView(this).apply { this.text = text; textSize = 24f; setTextColor(cyan); setTypeface(typeface, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER; setPadding(0,0,0,6) }
    private fun subtitle(text: String) = TextView(this).apply { this.text = text; textSize = 13f; setTextColor(gray); gravity = Gravity.CENTER; setPadding(0,0,0,18) }
    private fun section(text: String) = TextView(this).apply { this.text = text; textSize = 15f; setTextColor(cyan); setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(4,20,4,8) }
    private fun row(label: String, value: String, valueColor: Int = white) = TextView(this).apply { this.text = "$label    $value"; textSize = 15f; setTextColor(valueColor); setPadding(12,11,12,11); setBackgroundColor(Color.rgb(12,17,27)) }

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        super.onDestroy()
    }
}
