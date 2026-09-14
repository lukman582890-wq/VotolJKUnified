package com.votoljk.unified

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.app.Activity

class BmsDetailActivity : Activity() {

    private val cyan = Color.rgb(0, 232, 255)
    private val green = Color.rgb(0, 245, 140)
    private val gray = Color.rgb(145, 164, 197)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.rgb(5, 8, 14))

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(16, 20, 16, 30)

        root.addView(title("JK BMS  •  DETAIL"))
        root.addView(subtitle("20S LFP BATTERY SYSTEM"))

        root.addView(section("REAL-TIME"))

        root.addView(row("Battery Power", "-- W"))
        root.addView(row("Capacity", "-- Ah"))
        root.addView(row("Remaining Capacity", "-- Ah"))
        root.addView(row("MOS / CMOS Temp", "-- °C"))
        root.addView(row("Battery T1", "-- °C"))
        root.addView(row("Battery T2", "-- °C"))
        root.addView(row("Emergency Timer", "-- s"))
        root.addView(row("Sleep Timer", "-- s"))
        root.addView(row("LCD Alarm", "--"))
        root.addView(row("Cell Type", "LFP"))
        root.addView(row("Cell Average", "-- V"))
        root.addView(row("Voltage Difference", "-- V"))
        root.addView(row("Balance Current", "-- A"))
        root.addView(row("Cycle Count", "--"))
        root.addView(row("Cycle Capacity", "-- Ah"))
        root.addView(row("Charger Status", "--"))
        root.addView(row("Balancer", "--"))

        root.addView(section("BATTERY"))

        root.addView(row("Battery Voltage", "-- V", cyan))
        root.addView(row("Battery Current", "-- A", cyan))
        root.addView(row("Battery Power", "-- W", cyan))

        root.addView(section("CELL VOLTAGES"))

        for (i in 1..20) {
            root.addView(row(String.format("Cell %02d", i), "-- V", cyan))
        }

        root.addView(section("BALANCE WIRE RESISTANCE"))

        for (i in 1..20) {
            root.addView(row(String.format("Cell %02d", i), "-- Ω", green))
        }

        root.addView(section("ALARMS / PROTECTION"))
        root.addView(row("Protection Status", "--"))
        root.addView(row("Charge MOS", "--"))
        root.addView(row("Discharge MOS", "--"))
        root.addView(row("BMS Status", "WAITING FOR DATA", gray))

        root.addView(section("DETAILS LOG"))
        root.addView(row("JK BMS data", "Belum ada data diterima", gray))

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun title(text: String): TextView {
        return TextView(this@BmsDetailActivity).apply {
            this.text = text
            textSize = 24f
            setTextColor(cyan)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 6)
        }
    }

    private fun subtitle(text: String): TextView {
        return TextView(this@BmsDetailActivity).apply {
            this.text = text
            textSize = 13f
            setTextColor(gray)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 18)
        }
    }

    private fun section(text: String): TextView {
        return TextView(this@BmsDetailActivity).apply {
            this.text = text
            textSize = 15f
            setTextColor(cyan)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(4, 20, 4, 8)
        }
    }

    private fun row(label: String, value: String, valueColor: Int = white): TextView {
        return TextView(this@BmsDetailActivity).apply {
            this.text = "$label    $value"
            textSize = 15f
            setTextColor(valueColor)
            setPadding(12, 11, 12, 11)
            setBackgroundColor(Color.rgb(12, 17, 27))
        }
    }
}
