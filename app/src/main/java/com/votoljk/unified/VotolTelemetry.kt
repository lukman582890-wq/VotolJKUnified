package com.votoljk.unified

data class VotolTelemetry(
    val voltage: Float,
    val current: Float,
    val rpm: Int,
    val controllerTemp: Int,
    val motorTemp: Int,
    val faultMask: Long,
    val statusCode: Int,
    val status: String,
    val gear: String,
    val reverse: Boolean,
    val park: Boolean,
    val brake: Boolean,
    val stand: Boolean,
    val antiTheft: Boolean,
    val regen: Boolean
)

object VotolTelemetryParser {
    fun parse(frame: ByteArray): VotolTelemetry? {
        if (frame.size != 24) return null
        val first = frame[0].toInt() and 0xFF
        if (first != 0xC0 && first != 0xC9) return null
        if ((frame[23].toInt() and 0xFF) != 0x0D) return null
        var xor = 0
        for (i in 0..21) xor = xor xor (frame[i].toInt() and 0xFF)
        if ((frame[22].toInt() and 0xFF) != xor) return null

        val voltage = (((frame[5].toInt() and 0xFF) shl 8) or (frame[6].toInt() and 0xFF)) / 10f
        val currentRaw = (((frame[7].toInt() and 0xFF) shl 8) or (frame[8].toInt() and 0xFF))
        val current = (if ((currentRaw and 0x8000) != 0) currentRaw - 0x10000 else currentRaw) / 10f
        val rpmRaw = (((frame[14].toInt() and 0xFF) shl 8) or (frame[15].toInt() and 0xFF))
        val rpm = if ((rpmRaw and 0x8000) != 0) rpmRaw - 0x10000 else rpmRaw
        val controllerTemp = (frame[16].toInt() and 0xFF) - 50
        val motorTemp = (frame[17].toInt() and 0xFF) - 50
        val indicators = frame[20].toInt() and 0xFF
        val statusCode = frame[21].toInt() and 0xFF
        val faultMask = ((frame[10].toLong() and 0xFF) shl 24) or
            ((frame[11].toLong() and 0xFF) shl 16) or
            ((frame[12].toLong() and 0xFF) shl 8) or
            (frame[13].toLong() and 0xFF)
        val reverse = (indicators and 0x04) != 0
        val park = (indicators and 0x08) != 0
        val brake = (indicators and 0x10) != 0
        val stand = (indicators and 0x20) != 0
        val antiTheft = (indicators and 0x40) != 0
        val regen = (indicators and 0x80) != 0
        val level = when (indicators and 0x03) { 0 -> "L"; 1 -> "M"; 2 -> "H"; else -> "S" }
        var status = when (statusCode) {
            0 -> "IDLE"; 1 -> "INIT"; 2 -> "START"; 3 -> "RUN"
            4 -> "STOP"; 5 -> "BRAKE"; 6 -> "WAIT"; 7 -> "FAULT"; else -> "ST:$statusCode"
        }
        if (statusCode == 7) status = "FAULT"
        else if (brake) status = "BRAKE"
        else if (reverse) status = "REVERSE"
        else if (park) status = "PARK"
        else if (stand) status = "STAND"
        else if (rpm > 10 || rpm < -10) status = "RUN"
        val gear = when { park -> "P"; reverse -> "R"; else -> level }
        return VotolTelemetry(voltage, current, rpm, controllerTemp, motorTemp, faultMask, statusCode, status, gear, reverse, park, brake, stand, antiTheft, regen)
    }
}
