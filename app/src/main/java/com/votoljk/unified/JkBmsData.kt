package com.votoljk.unified

data class JkBmsData(
    val cellCount: Int,
    val cellVoltages: List<Float>,
    val cellResistances: List<Float>,
    val totalVoltage: Float,
    val current: Float,
    val power: Float,
    val temp1: Float,
    val temp2: Float,
    val mosTemp: Float,
    val soc: Int,
    val remainingAh: Float,
    val fullAh: Float,
    val cycleCount: Int,
    val cycleCapacityAh: Float,
    val soh: Int,
    val balanceCurrent: Float,
    val balanceOn: Boolean,
    val chargeMos: Boolean,
    val dischargeMos: Boolean,
    val precharge: Boolean,
    val balancer: Boolean,
    val emergencySeconds: Int,
    val sleepSeconds: Long,
    val batteryType: String,
    val chargeStatus: Int
)

object JkBmsDataStore {
    @Volatile
    var latest: JkBmsData? = null
}
