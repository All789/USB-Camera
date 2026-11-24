package com.example.model

import android.hardware.usb.UsbDevice

data class UsbDeviceModel(
    val deviceId: Int,
    val deviceName: String,
    val productName: String,
    val manufacturerName: String,
    val vendorId: Int,
    val productId: Int,
    val deviceClass: Int,
    val hasVideoClass: Boolean,
    val hasAudioClass: Boolean,
    val hasPermission: Boolean,
    val rawDevice: UsbDevice? = null
) {
    val displayName: String
        get() = when {
            productName.isNotBlank() -> productName
            deviceName.isNotBlank() -> deviceName
            else -> "USB HDMI Capture (${String.format("%04X:%04X", vendorId, productId)})"
        }

    val technicalSummary: String
        get() = buildString {
            append("VID: 0x${vendorId.toString(16).uppercase().padStart(4, '0')} ")
            append("PID: 0x${productId.toString(16).uppercase().padStart(4, '0')} ")
            if (hasVideoClass) append("[UVC Video] ")
            if (hasAudioClass) append("[HDMI Audio]")
        }
}
