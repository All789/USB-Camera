package com.example.model

sealed interface VideoSignalState {
    object NoDevice : VideoSignalState

    data class DeviceDetected(
        val device: UsbDeviceModel
    ) : VideoSignalState

    data class PermissionRequested(
        val device: UsbDeviceModel
    ) : VideoSignalState

    data class PermissionDenied(
        val device: UsbDeviceModel
    ) : VideoSignalState

    data class Connecting(
        val device: UsbDeviceModel,
        val message: String = "Initializing HDMI Stream..."
    ) : VideoSignalState

    data class Streaming(
        val device: UsbDeviceModel?,
        val resolution: String = "1920x1080",
        val fps: Int = 60,
        val bitrateMbps: Float = 12.5f,
        val isAudioActive: Boolean = false,
        val isCamera2Pipeline: Boolean = false,
        val isTestPattern: Boolean = false
    ) : VideoSignalState

    data class Error(
        val message: String,
        val device: UsbDeviceModel? = null,
        val canRetry: Boolean = true
    ) : VideoSignalState
}
