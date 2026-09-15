package com.votoljk.unified

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.drawable.GradientDrawable

class SettingsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("app_settings", MODE_PRIVATE) }
    private val blue = Color.rgb(45, 151, 255)
    private val bg = Color.rgb(7, 7, 8)
    private val card = Color.rgb(39, 39, 41)
    private val field = Color.rgb(29, 29, 31)
    private val textColor = Color.rgb(245, 245, 247)
    private val secondary = Color.rgb(165, 165, 170)
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        root.addView(header())
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(24)) }
        buildGeneral(body); buildAppearance(body); buildBattery(body); buildVehicle(body); buildTrip(body); buildIntegration(body)
        scroll.addView(body); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); root.addView(bottomBar()); setContentView(root)
    }

    private fun header(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(14), dp(14), dp(10))
        addView(TextView(this@SettingsActivity).apply { text = "⚙  Pengaturan"; textSize = 24f; setTextColor(textColor); typeface = Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(0, dp(54), 1f))
        addView(smallButton("×") { finish() }, LinearLayout.LayoutParams(dp(48), dp(48)))
    }

    private fun buildGeneral(parent: LinearLayout) {
        section(parent, "BAHASA & SATUAN"); val box = card()
        row(box, "Pilih Bahasa", spinner(arrayOf("Indonesia", "English"), "language"))
        row(box, "Sistem Satuan", segmented(arrayOf("Metric", "Imperial"), "units"))
        row(box, "Simbol Mata Uang", edit("Rp", "currency"))
        row(box, "Tarif Listrik per kWh", edit("1444,7", "electricityRate"))
        row(box, "Harga Bensin per Liter", edit("16250", "fuelPrice"), "Untuk perbandingan Hemat vs Bensin (asumsi 40 km/L).")
        parent.addView(box)
    }

    private fun buildAppearance(parent: LinearLayout) {
        section(parent, "TAMPILAN"); val box = card(); label(box, "Mode Tema"); box.addView(segmented(arrayOf("Terang", "Gelap"), "theme")); label(box, "Layout Dashboard")
        val grid = GridLayout(this).apply { columnCount = 2 }
        arrayOf("Tesla", "Sloped", "Gecit", "Tilano", "Tech", "Petaa", "Motovlog", "Dragger", "Dyno", "Canvas").forEach { name ->
            val b = smallButton(name) { prefs.edit().putString("dashboardLayout", name).apply(); Toast.makeText(this, "Layout $name dipilih", Toast.LENGTH_SHORT).show() }
            val lp = GridLayout.LayoutParams().apply { width = 0; height = dp(46); columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); setMargins(dp(4), dp(4), dp(4), dp(4)) }
            grid.addView(b, lp)
        }
        box.addView(grid); label(box, "Kalibrasi Gyro (Kemiringan)")
        val gyro = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        gyro.addView(TextView(this@SettingsActivity).apply { text = "Offset  0.0° / 0.0°"; setTextColor(secondary); textSize = 14f }, LinearLayout.LayoutParams(0, -2, 1f))
        gyro.addView(smallButton("Set 0°") { Toast.makeText(this@SettingsActivity, "Gyro di-nolkan", Toast.LENGTH_SHORT).show() }); box.addView(gyro); parent.addView(box)
    }

    private fun buildBattery(parent: LinearLayout) {
        section(parent, "KALIBRASI"); val box = card()
        row(box, "Kapasitas Baterai (Ah)", edit("27", "batteryCapacity")); row(box, "Jarak Maksimal (KM)", edit("70", "maxRange"))
        switchRow(box, "Kalibrasi Baterai (SOC)", "SOC BMS kacau? Pakai kalibrasi ini.", "socCalibration"); parent.addView(box)
        section(parent, "BATERAI TAMBAHAN"); val multi = card(); switchRow(multi, "Multi Baterai", "Gabungkan baterai utama dan baterai tambahan", "multiBattery"); parent.addView(multi)
        section(parent, "BOBOT KENDARAAN"); val weight = card()
        row(weight, "Bobot Motor Kosong (kg)", edit("90", "motorWeight")); row(weight, "Bobot Pack Baterai (kg)", edit("15", "batteryWeight")); row(weight, "Bobot Pengendara Utama (kg)", edit("50", "riderWeight")); row(weight, "Bobot Pembonceng / Bagasi (kg)", edit("65", "passengerWeight")); parent.addView(weight)
    }

    private fun buildVehicle(parent: LinearLayout) {
        section(parent, "KENDARAAN"); val box = card()
        row(box, "Sumber Kecepatan", spinner(arrayOf("Otomatis (prioritas VOTOL)", "VOTOL RPM", "GPS"), "speedSource"))
        row(box, "Tipe Motor", spinner(arrayOf("Hub Drive (Direct)", "Mid Drive", "Belt Drive"), "motorType"))
        row(box, "Daya BLDC (W)", edit("800", "motorPower")); row(box, "Diameter Velg + Ban (inci)", edit("16,6", "wheelDiameter"), "Velg R12 + Ban 3.00 ≈ 16.5 inci"); parent.addView(box)
    }

    private fun buildTrip(parent: LinearLayout) {
        section(parent, "TRIP METER"); val box = card(); switchRow(box, "Auto reset trip", "Arsipkan trip setelah berhenti lama.", "autoResetTrip")
        val reset = smallButton("Reset Trip") { prefs.edit().putFloat("tripDistance", 0f).apply(); Toast.makeText(this, "Trip di-reset", Toast.LENGTH_SHORT).show() }; reset.setTextColor(Color.rgb(255, 90, 90)); box.addView(reset, LinearLayout.LayoutParams(-1, dp(48))); parent.addView(box)
    }

    private fun buildIntegration(parent: LinearLayout) {
        section(parent, "INTEGRASI BLE"); val box = card()
        row(box, "Profil", spinner(arrayOf("Motor 1", "Motor 2", "Motor 3"), "profile")); row(box, "Integrasi BLE", spinner(arrayOf("BMS & Controller", "BMS saja", "Controller saja"), "bleIntegration"))
        label(box, "Protokol BMS"); box.addView(spinner(arrayOf("JK BMS", "Daly BMS", "ANT BMS"), "bmsProtocol")); label(box, "Protokol Controller"); box.addView(spinner(arrayOf("Votol Controller", "FarDriver Controller"), "controllerProtocol"))
        val status = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        status.addView(smallButton("BMS 1 OK\nklik lagi untuk diskonek") { Toast.makeText(this@SettingsActivity, "Koneksi BMS dikelola dari Dashboard", Toast.LENGTH_SHORT).show() }, LinearLayout.LayoutParams(0, dp(60), 1f)); status.addView(Space(this), LinearLayout.LayoutParams(dp(8), 1)); status.addView(smallButton("JDY 1 OK\nklik lagi untuk diskonek") { Toast.makeText(this@SettingsActivity, "Koneksi VOTOL dikelola dari Dashboard", Toast.LENGTH_SHORT).show() }, LinearLayout.LayoutParams(0, dp(60), 1f)); box.addView(status)
        label(box, "Advanced: Bluetooth Classic"); desc(box, "Untuk modul Bluetooth Classic SPP. Pair dulu di pengaturan Bluetooth Android, lalu pilih perangkat VOTOL dari Dashboard.")
        box.addView(smallButton("Pair Classic") { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }); box.addView(space(8)); box.addView(smallButton("Buka Pengaturan Bluetooth") { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) })
        switchRow(box, "Sambung Ulang Otomatis (M1)", "Coba lagi saat koneksi terputus", "autoReconnect"); parent.addView(box)
        section(parent, "NATIVE / OTOMATIS"); val native = card()
        switchRow(native, "Layar Selalu Menyala", "Jaga layar tetap menyala saat dashboard aktif", "keepScreenOn"); switchRow(native, "Layar Penuh", "Sembunyikan jam, baterai, dan tombol navigasi", "fullscreen"); switchRow(native, "Record di Background", "Saat diminimalkan: rekam GPS/rute", "backgroundRecord"); switchRow(native, "Mulai saat Booting", "Otomatis buka aplikasi saat HP dinyalakan", "startOnBoot"); switchRow(native, "Sambung BLE Otomatis saat Mulai", "Otomatis koneksi Bluetooth saat aplikasi dibuka", "autoBle"); parent.addView(native)
    }

    private fun bottomBar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; setPadding(dp(16), dp(10), dp(16), dp(10)); setBackgroundColor(Color.BLACK)
        addView(smallButton("Batal") { finish() }, LinearLayout.LayoutParams(0, dp(58), 1f)); addView(Space(this@SettingsActivity), LinearLayout.LayoutParams(dp(10), 1))
        val save = smallButton("Simpan") { Toast.makeText(this@SettingsActivity, "Pengaturan tersimpan", Toast.LENGTH_SHORT).show(); finish() }; save.background = pill(blue); save.setTextColor(Color.WHITE); addView(save, LinearLayout.LayoutParams(0, dp(58), 1f))
    }

    private fun section(parent: LinearLayout, title: String) { parent.addView(TextView(this).apply { text = title; setTextColor(Color.LTGRAY); textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setPadding(dp(4), dp(18), 0, dp(6)) }) }
    private fun card() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(16)); background = pill(card, 18); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(4) } }
    private fun row(parent: LinearLayout, title: String, value: View, hint: String? = null) { val line = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(4), 0, dp(7)) }; val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }; top.addView(TextView(this@SettingsActivity).apply { text = title; setTextColor(textColor); textSize = 16f; typeface = Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(0, -2, 1f)); top.addView(value, LinearLayout.LayoutParams(dp(190), dp(50))); line.addView(top); if (hint != null) desc(line, hint); parent.addView(line) }
    private fun label(parent: LinearLayout, title: String) { parent.addView(TextView(this).apply { text = title; setTextColor(textColor); textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(10), 0, dp(8)) }) }
    private fun desc(parent: LinearLayout, value: String) { parent.addView(TextView(this).apply { text = value; setTextColor(secondary); textSize = 13f; setPadding(0, 0, 0, dp(8)) }) }
    private fun space(h: Int) = Space(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }
    private fun edit(default: String, key: String) = EditText(this).apply { setText(prefs.getString(key, default) ?: default); setTextColor(textColor); textSize = 16f; gravity = Gravity.CENTER; singleLine = true; background = pill(field, 10); setPadding(dp(10), 0, dp(10), 0); setOnFocusChangeListener { _, has -> if (!has) prefs.edit().putString(key, text.toString()).apply() } }
    private fun spinner(values: Array<String>, key: String) = Spinner(this).apply { adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, values); setSelection(values.indexOf(prefs.getString(key, values[0])).coerceAtLeast(0)); background = pill(field, 10); onItemSelectedListener = object : AdapterView.OnItemSelectedListener { override fun onNothingSelected(p: AdapterView<*>?) {}; override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { prefs.edit().putString(key, values[pos]).apply() } } }
    private fun segmented(values: Array<String>, key: String): View { val box = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; background = pill(field, 999) }; values.forEach { value -> box.addView(smallButton(value) { prefs.edit().putString(key, value).apply(); Toast.makeText(this, "$value dipilih", Toast.LENGTH_SHORT).show() }, LinearLayout.LayoutParams(0, dp(50), 1f)) }; return box }
    private fun switchRow(parent: LinearLayout, title: String, subtitle: String, key: String) { val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(7), 0, dp(7)) }; val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; labels.addView(TextView(this@SettingsActivity).apply { text = title; setTextColor(textColor); textSize = 16f; typeface = Typeface.DEFAULT_BOLD }); labels.addView(TextView(this@SettingsActivity).apply { text = subtitle; setTextColor(secondary); textSize = 13f; setPadding(0, dp(5), 0, 0) }); row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(Switch(this).apply { isChecked = prefs.getBoolean(key, false); setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean(key, v).apply() } }); parent.addView(row) }
    private fun smallButton(title: String, action: () -> Unit) = Button(this).apply { text = title; textSize = 14f; isAllCaps = false; setTextColor(textColor); background = pill(field, 999); setPadding(dp(10), 0, dp(10), 0); setOnClickListener { action() } }
    private fun pill(color: Int, radius: Int = 999) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), Color.rgb(58, 58, 61)) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
