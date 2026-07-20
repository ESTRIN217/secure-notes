package com.example.data.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment

enum class DeviceClass {
    LOW, MEDIUM, HIGH
}

data class DeviceInfo(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val maxAppHeapMb: Long,
    val deviceClass: DeviceClass,
    val supportedAbis: List<String>,
    val apiLevel: Int
) {
    companion object {
        fun detect(context: Context): DeviceInfo {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            val totalMb = mi.totalMem / (1024 * 1024)
            val availMb = mi.availMem / (1024 * 1024)
            val heapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
            val cls = when {
                totalMb >= 6144 -> DeviceClass.HIGH
                totalMb >= 3072 -> DeviceClass.MEDIUM
                else -> DeviceClass.LOW
            }
            return DeviceInfo(
                totalRamMb = totalMb,
                availableRamMb = availMb,
                maxAppHeapMb = heapMb,
                deviceClass = cls,
                supportedAbis = Build.SUPPORTED_ABIS.toList(),
                apiLevel = Build.VERSION.SDK_INT
            )
        }
    }
}
