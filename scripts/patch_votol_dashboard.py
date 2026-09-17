from pathlib import Path

main = Path('app/src/main/java/com/votoljk/unified/MainActivity.kt')
s = main.read_text()
start = s.index('    private fun connectVotol')
end = s.index('    private var scanner:', start)
block = '''    private val votolPollHandler = Handler(Looper.getMainLooper())
    private val votolRxBuffer = ArrayList<Byte>()

    private fun connectVotol(d: BluetoothDevice) {
        Thread {
            runCatching {
                adapter.cancelDiscovery()
                runOnUiThread { votolStatus.text = "CONNECTING…" }
                val socket = d.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                votolSocket = socket
                votolRxBuffer.clear()
                runOnUiThread {
                    votolStatus.text = "CONNECTED"
                    votolStatus.setTextColor(0xFF45D6A3.toInt())
                    log.text = "VOTOL SPP connected: ${d.name ?: d.address}. SHOW polling active."
                }
                startVotolPolling(socket)
                readClassic(socket.inputStream)
            }.onFailure { e ->
                runOnUiThread {
                    votolStatus.text = "ERROR"
                    log.text = "VOTOL: ${e.message}"
                }
            }
        }.start()
    }

    private fun createVotolShowPacket(): ByteArray {
        val p = ByteArray(24)
        p[0] = 0xC9.toByte(); p[1] = 0x14.toByte(); p[2] = 0x0D.toByte()
        p[3] = 'S'.code.toByte(); p[4] = 'H'.code.toByte(); p[5] = 'O'.code.toByte(); p[6] = 'W'.code.toByte()
        var xor = 0
        for (i in 0..21) xor = xor xor (p[i].toInt() and 0xFF)
        p[22] = xor.toByte(); p[23] = 0x0D.toByte()
        return p
    }

    private fun startVotolPolling(socket: BluetoothSocket) {
        votolPollHandler.removeCallbacksAndMessages(null)
        val poll = object : Runnable {
            override fun run() {
                if (votolSocket === socket && socket.isConnected) {
                    runCatching {
                        socket.outputStream.write(createVotolShowPacket())
                        socket.outputStream.flush()
                    }.onFailure { e -> runOnUiThread { log.append("\\nVOTOL TX ERROR: ${e.message}") } }
                    votolPollHandler.postDelayed(this, 700)
                }
            }
        }
        votolPollHandler.post(poll)
    }

    private fun consumeVotolBytes(data: ByteArray) {
        votolRxBuffer.addAll(data.toList())
        while (true) {
            var start = -1
            for (i in 0..(votolRxBuffer.size - 1)) {
                val b = votolRxBuffer[i].toInt() and 0xFF
                if (b == 0xC0 || b == 0xC9) { start = i; break }
            }
            if (start < 0) {
                if (votolRxBuffer.size > 23) votolRxBuffer.clear()
                return
            }
            if (start > 0) repeat(start) { votolRxBuffer.removeAt(0) }
            if (votolRxBuffer.size < 24) return
            val frame = votolRxBuffer.take(24).toByteArray()
            val telemetry = VotolTelemetryParser.parse(frame)
            if (telemetry != null) {
                repeat(24) { votolRxBuffer.removeAt(0) }
                runOnUiThread {
                    val speedKmh = telemetry.rpm.coerceAtLeast(0) * 72f / 500f
                    findViewById<TextView>(R.id.speedValue).text = String.format("%.0f", speedKmh)
                    findViewById<TextView>(R.id.votolTelemetryValue).text =
                        "Motor RPM    ${telemetry.rpm}\\n" +
                        "Controller  %.1f V / %.1f A / %.1f W\\n".format(telemetry.voltage, telemetry.current, telemetry.voltage * telemetry.current) +
                        "Speed       %.0f km/h\\n".format(speedKmh) +
                        "Status      ${telemetry.status}   Gear ${telemetry.gear}\\n" +
                        "Fault       0x%08X".format(telemetry.faultMask)
                    findViewById<TextView>(R.id.systemTempValues).text =
                        "Controller     ${telemetry.controllerTemp} °C\\n\\n" +
                        "Motor              ${telemetry.motorTemp} °C\\n\\n" +
                        "Battery            -- °C"
                }
            } else {
                votolRxBuffer.removeAt(0)
            }
        }
    }

    private fun readClassic(input: InputStream) {
        val buf = ByteArray(1024)
        while (votolSocket?.isConnected == true) {
            val n = runCatching { input.read(buf) }.getOrDefault(-1)
            if (n <= 0) break
            val data = buf.copyOf(n)
            val hex = data.joinToString(" ") { String.format("%02X", it) }
            consumeVotolBytes(data)
            runOnUiThread { log.append("\\nVOTOL RX ${n}B  $hex") }
        }
    }

    private fun disconnectVotol() {
        votolPollHandler.removeCallbacksAndMessages(null)
        runCatching { votolSocket?.close() }
        votolSocket = null
        votolRxBuffer.clear()
        votolStatus.text = "DISCONNECTED"
        votolStatus.setTextColor(0xFFFFB86B.toInt())
    }

'''
main.write_text(s[:start] + block + s[end:])

for layout_path in [
    Path('app/src/main/res/layout/activity_main.xml'),
    Path('app/src/main/res/layout-land/activity_main.xml'),
]:
    x = layout_path.read_text()
    if 'android:id="@+id/votolTelemetryValue"' not in x:
        marker = 'android:text="Motor RPM'
        pos = x.find(marker)
        if pos < 0:
            raise SystemExit(f'VOTOL telemetry TextView not found in {layout_path}')
        x = x[:pos] + 'android:id="@+id/votolTelemetryValue" ' + x[pos:]
        layout_path.write_text(x)
print('VOTOL DASHBOARD PATCHED')
