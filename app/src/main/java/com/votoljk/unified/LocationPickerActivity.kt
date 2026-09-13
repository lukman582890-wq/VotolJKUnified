package com.votoljk.unified

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LocationPickerActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var searchBox: EditText
    private lateinit var searchButton: Button

    private var selectedLat = 0.0
    private var selectedLon = 0.0
    private var selectedLabel = ""

    data class Place(
        val label: String,
        val lat: Double,
        val lon: Double
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF070B0F.toInt())
        }

        val title = TextView(this).apply {
            text = if (intent.getStringExtra("MODE") == "ORIGIN")
                "Pilih Asal"
            else
                "Pilih Tujuan"

            setTextColor(0xFFFFFFFF.toInt())
            textSize = 20f
            setPadding(24, 22, 24, 14)
        }
        root.addView(title)

        val searchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 0, 16, 10)
        }

        searchBox = EditText(this).apply {
            hint = "Cari alamat atau tempat"
            setSingleLine(true)
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF8A939C.toInt())
            setBackgroundColor(0xFF151B21.toInt())
        }

        searchRow.addView(
            searchBox,
            LinearLayout.LayoutParams(0, 54.dp(), 1f)
        )

        searchButton = Button(this).apply {
            text = "CARI"
            setOnClickListener {
                searchPlace(searchBox.text.toString().trim())
            }
        }

        searchRow.addView(
            searchButton,
            LinearLayout.LayoutParams(90.dp(), 54.dp()).apply {
                leftMargin = 8.dp()
            }
        )

        root.addView(searchRow)

        val currentButton = Button(this).apply {
            text = "📍 Gunakan lokasi saya"
            setOnClickListener {
                useCurrentLocation()
            }
        }

        root.addView(
            currentButton,
            LinearLayout.LayoutParams(-1, 52.dp()).apply {
                leftMargin = 16.dp()
                rightMargin = 16.dp()
                bottomMargin = 8.dp()
            }
        )

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            addJavascriptInterface(MapBridge(), "Android")
        }

        root.addView(
            webView,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        val selectButton = Button(this).apply {
            text = "PILIH LOKASI"
            setOnClickListener {
                finishSelection()
            }
        }

        root.addView(
            selectButton,
            LinearLayout.LayoutParams(-1, 56.dp()).apply {
                leftMargin = 16.dp()
                rightMargin = 16.dp()
                topMargin = 8.dp()
                bottomMargin = 16.dp()
            }
        )

        setContentView(root)

        webView.loadDataWithBaseURL(
            "https://localhost/",
            mapHtml(),
            "text/html",
            "UTF-8",
            null
        )

        requestLocationIfNeeded()
    }

    private fun Int.dp(): Int =
        (this * resources.displayMetrics.density).toInt()

    private fun mapHtml(): String = """
        <!doctype html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width,initial-scale=1">

        <link rel="stylesheet"
          href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>

        <style>
          html,body,#map {
            height:100%;
            margin:0;
            background:#10151a;
          }
          .leaflet-control-attribution {
            font-size:9px;
          }
        </style>
        </head>

        <body>
        <div id="map"></div>

        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>

        <script>
          const map = L.map('map').setView([-6.2,106.82],12);

          L.tileLayer(
            'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
            {
              maxZoom:19,
              attribution:'© OpenStreetMap contributors'
            }
          ).addTo(map);

          let marker = null;

          function setPoint(lat,lon,label) {
            if(marker) marker.remove();

            marker = L.marker([lat,lon])
              .addTo(map)
              .bindPopup(label || 'Lokasi dipilih')
              .openPopup();

            map.setView([lat,lon],16);
          }

          function centerPoint(lat,lon) {
            map.setView([lat,lon],16);
            setPoint(lat,lon,'Lokasi saya');
          }

          map.on('click',function(e) {
            Android.mapClicked(
              e.latlng.lat,
              e.latlng.lng
            );
          });
        </script>
        </body>
        </html>
    """

    inner class MapBridge {

        @JavascriptInterface
        fun mapClicked(lat: Double, lon: Double) {

            runOnUiThread {
                selectedLat = lat
                selectedLon = lon
                selectedLabel =
                    "Titik peta (%.5f, %.5f)"
                        .format(lat, lon)

                webView.evaluateJavascript(
                    "setPoint($lat,$lon,'Lokasi dipilih')",
                    null
                )
            }

            reverseGeocode(lat, lon)
        }
    }

    private fun searchPlace(query: String) {

        if (query.isBlank()) return

        searchButton.isEnabled = false

        Thread {

            val places = runCatching {

                val url = URL(
                    "https://nominatim.openstreetmap.org/search" +
                    "?format=jsonv2" +
                    "&limit=8" +
                    "&accept-language=id" +
                    "&q=" +
                    URLEncoder.encode(query, "UTF-8")
                )

                val c = url.openConnection() as HttpURLConnection

                c.setRequestProperty(
                    "User-Agent",
                    "VotolJKUnified/0.1 Android"
                )

                c.connectTimeout = 8000
                c.readTimeout = 8000

                val text =
                    c.inputStream.bufferedReader().use {
                        it.readText()
                    }

                val arr = JSONArray(text)

                (0 until arr.length()).map {

                    val o = arr.getJSONObject(it)

                    Place(
                        o.optString("display_name"),
                        o.getDouble("lat"),
                        o.getDouble("lon")
                    )
                }

            }.getOrDefault(emptyList())

            runOnUiThread {

                searchButton.isEnabled = true

                if (places.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Lokasi tidak ditemukan",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@runOnUiThread
                }

                AlertDialog.Builder(this)
                    .setTitle("Pilih lokasi")
                    .setItems(
                        places.map { it.label }.toTypedArray()
                    ) { _, which ->
                        choosePlace(places[which])
                    }
                    .show()
            }

        }.start()
    }

    private fun choosePlace(p: Place) {

        selectedLabel = p.label
        selectedLat = p.lat
        selectedLon = p.lon

        webView.evaluateJavascript(
            "setPoint(${p.lat},${p.lon},${js(p.label)})",
            null
        )

        searchBox.setText(p.label)
    }

    private fun reverseGeocode(
        lat: Double,
        lon: Double
    ) {

        Thread {

            val label = runCatching {

                val url = URL(
                    "https://nominatim.openstreetmap.org/reverse" +
                    "?format=jsonv2" +
                    "&accept-language=id" +
                    "&lat=$lat" +
                    "&lon=$lon"
                )

                val c = url.openConnection() as HttpURLConnection

                c.setRequestProperty(
                    "User-Agent",
                    "VotolJKUnified/0.1 Android"
                )

                c.connectTimeout = 8000
                c.readTimeout = 8000

                val text =
                    c.inputStream.bufferedReader().use {
                        it.readText()
                    }

                org.json.JSONObject(text)
                    .optString("display_name")

            }.getOrDefault(
                "Titik peta (%.5f, %.5f)"
                    .format(lat, lon)
            )

            runOnUiThread {
                selectedLabel = label
                searchBox.setText(label)
            }

        }.start()
    }

    private fun js(s: String): String =
        "'" +
        s.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", " ") +
        "'"

    private fun requestLocationIfNeeded() {

        if (
            checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                9100
            )
        }
    }

    private fun useCurrentLocation() {

        if (
            checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationIfNeeded()
            return
        }

        val lm =
            getSystemService(LOCATION_SERVICE)
                as LocationManager

        val provider = when {

            lm.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            ) ->
                LocationManager.GPS_PROVIDER

            lm.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            ) ->
                LocationManager.NETWORK_PROVIDER

            else ->
                null
        }

        if (provider == null) {
            Toast.makeText(
                this,
                "Aktifkan lokasi/GPS",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val listener =
            object : LocationListener {

                override fun onLocationChanged(
                    location: Location
                ) {

                    lm.removeUpdates(this)

                    selectedLat =
                        location.latitude

                    selectedLon =
                        location.longitude

                    selectedLabel =
                        "Lokasi saya (%.5f, %.5f)"
                            .format(
                                selectedLat,
                                selectedLon
                            )

                    webView.evaluateJavascript(
                        "centerPoint($selectedLat,$selectedLon)",
                        null
                    )

                    reverseGeocode(
                        selectedLat,
                        selectedLon
                    )
                }
            }

        runCatching {

            lm.requestLocationUpdates(
                provider,
                1000L,
                1f,
                listener,
                mainLooper
            )

        }.onFailure {

            Toast.makeText(
                this,
                "Tidak bisa mengambil lokasi",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun finishSelection() {

        if (selectedLat == 0.0 && selectedLon == 0.0) {

            Toast.makeText(
                this,
                "Pilih titik di peta atau cari lokasi",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        setResult(
            RESULT_OK,
            Intent().apply {

                putExtra(
                    "LABEL",
                    selectedLabel.ifBlank {
                        "%.5f, %.5f"
                            .format(
                                selectedLat,
                                selectedLon
                            )
                    }
                )

                putExtra(
                    "LAT",
                    selectedLat
                )

                putExtra(
                    "LON",
                    selectedLon
                )

                putExtra(
                    "MODE",
                    intent.getStringExtra("MODE")
                        ?: "DESTINATION"
                )
            }
        )

        finish()
    }
}
