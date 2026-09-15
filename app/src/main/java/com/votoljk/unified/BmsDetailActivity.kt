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

    private val refresh = object : Runnable {
        override fun run() {
            renderLiveData()
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

        val power = addRow(root, "Battery Power", "-- W")
        val capacity = addRow(root, "Capacity", "-- Ah")
        val remaining = addRow(root, "Remaining Capacity", "-- Ah")
        val mos = addRow(root, "MOS / CMOS Temp", "-- °C")
        val t1 = addRow(root, "Battery T1", "-- °C")
        val t2 = addRow(root, "Battery T2", "-- °C")
        val emergency = addRow(root, "Emergency Timer", "-- s")
        val sleep = addRow(root, "Sleep Timer", "-- s")
        val alarm = addRow(root, "LCD Alarm", "--")
        val type = addRow(root, "Cell Type", "LFP")
        val average = addRow(root, "Cell Average", "-- V")
        val delta = addRow(root, "Voltage Difference", "-- V")
        val balance = addRow(root, "Balance Current", "-- A")
        val cycles = addRow(root, "Cycle Count", "--")
        val cycleAh = addRow(root, "Cycle Capacity", "-- Ah")
        val charger = addRow(root, "Charger Status", "--")
        val balancer = addRow(root, "Balancer", "--")

        val batteryVoltage = run { root.addView(section("BATTERY")); addRow(root, "Battery Voltage", "-- V", cyan) }
        val batteryCurrent = addRow(root, "Battery Current", "-- A", cyan)
        val batteryPower = addRow(root, "Battery Power", "-- W", cyan)

        root.addView(section("CELL VOLTAGES"))
        val cellRows = (1..20).map { addRow(root, "Cell %02d".format(it), "-- V", cyan) }

        root.addView(section("BALANCE WIRE RESISTANCE"))
        val resistanceRows = (1..20).map { addRow(root, "Cell %02d".format(it), "-- Ω", green) }

        root.addView(section("ALARMS / PROTECTION"))
        val protection = addRow(root, "Protection Status", "--")
        val chargeMos = addRow(root, "Charge MOS", "--")
        val dischargeMos = addRow(root, "Discharge MOS", "--")
        val status = addRow(root, "BMS Status", "WAITING FOR DATA", gray)

        root.addView(section("DETAILS LOG"))
        val details = addRow(root, "JK BMS data", "Belum ada data diterima", gray)

        scroll.addView(root)
        setContentView(scroll)

        fun update() {
            val d = JkBmsDataStore.latest ?: return
            val min = d.cellVoltages.minOrNull() ?: 0f
            val max = d.cellVoltages.maxOrNull() ?: 0f
            val avg = if (d.cellVoltages.isEmpty()) 0f else d.cellVoltages.average().toFloat()
            val diff = max - min

            setValue(power, "Battery Power", "%.0f W".format(Locale.US, d.power))
            setValue(capacity, "Capacity", "%.3f Ah".format(Locale.US, d.fullAh))
            setValue(remaining, "Remaining Capacity", "%.3f Ah".format(Locale.US, d.remainingAh))
            setValue(mos, "MOS / CMOS Temp", "%.1f °C".format(Locale.US, d.mosTemp))
            setValue(t1, "Battery T1", "%.1f °C".format(Locale.US, d.temp1))
            setValue(t2, "Battery T2", "%.1f °C".format(Locale.US, d.temp2))
            setValue(emergency, "Emergency Timer", "%d s".format(d.emergencySeconds))
            setValue(sleep, "Sleep Timer", "%d s".format(d.sleepSeconds))
            setValue(alarm, "LCD Alarm", "NORMAL")
            setValue(type, "Cell Type", d.batteryType)
            setValue(average, "Cell Average", "%.3f V".format(Locale.US, avg))
            setValue(delta, "Voltage Difference", "%.3f V".format(Locale.US, diff))
            setValue(balance, "Balance Current", "%.3f A".format(Locale.US, d.balanceCurrent))
            setValue(cycles, "Cycle Count", "%d".format(d.cycleCount))
            setValue(cycleAh, "Cycle Capacity", "%.3f Ah".format(Locale.US, d.cycleCapacityAh))
            setValue(charger, "Charger Status", if (d.chargeStatus != 0) "ON" else "OFF")
            setValue(balancer, "Balancer", if (d.balancer || d.balanceOn) "ON" else "OFF")
            setValue(batteryVoltage, "Battery Voltage", "%.3f V".format(Locale.US, d.totalVoltage))
            setValue(batteryCurrent, "Battery Current", "%.3f A".format(Locale.US, d.current))
            setValue(batteryPower, "Battery Power", "%.0f W".format(Locale.US, d.power))
            setValue(protection, "Protection Status", if (d.emergencySeconds > 0) "ACTIVE" else "NORMAL")
            setValue(chargeMos, "Charge MOS", if (d.chargeMos) "ON" else "OFF")
            setValue(dischargeMos, "Discharge MOS", if (d.dischargeMos) "ON" else "OFF")
            setValue(status, "BMS Status", "ONLINE • ${d.cellCount}S • SOH ${d.soh}%")
            setValue(details, "JK BMS data", "LIVE • SOC ${d.soc}% • %.3f V • %.3f A".format(Locale.US, d.totalVoltage, d.current))

            cellRows.forEachIndexed { i, row ->
                val v = d.cellVoltages.getOrNull(i)
                setValue(row, "Cell %02d".format(i + 1), if (v == null) "-- V" else "%.3f V".format(Locale.US, v))
            }
            resistanceRows.forEachIndexed { i, row ->
                val v = d.cellResistances.getOrNull(i)
                setValue(row, "Cell %02d".format(i + 1), if (v == null) "-- Ω" else "%.3f Ω".format(Locale.US, v))
            }
        }

        renderLiveData = update
        update()
        handler.post(refresh)
    }

    private lateinit var renderLiveData: () -> Unit

    private fun addRow(root: LinearLayout, label: String, value: String, color: Int = white): TextView {
        val v = row(label, value, color)
        root.addView(v)
        return v
    }

    private fun setValue(row: TextView?, label: String, value: String) {
        row?.text = "$label    $value"
    }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        textSize = 24f
        setTextColor(cyan)
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 6)
    }

    private fun subtitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(gray)
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 18)
    }

    private fun section(text: String) = TextView(this).apply {
        this.text = text
        textSize = 15f
        setTextColor(cyan)
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(4, 20, 4, 8)
    }

    private fun row(label: String, value: String, valueColor: Int = white) = TextView(this).apply {
        this.text = "$label    $value"
        textSize = 15f
        setTextColor(valueColor)
        setPadding(12, 11, 12, 11)
        setBackgroundColor(Color.rgb(12, 17, 27))
    }

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        super.onDestroy()
    }
}
