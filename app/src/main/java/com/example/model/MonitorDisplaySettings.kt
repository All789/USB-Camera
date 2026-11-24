package com.example.model

enum class AspectRatioMode(val label: String, val ratio: Float?) {
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_4_3("4:3", 4f / 3f),
    FIT("Native Fit", null),
    FILL("Stretch Fill", null)
}

enum class ScreenOrientationMode(val label: String) {
    SENSOR("Auto Rotate"),
    LANDSCAPE("Landscape"),
    PORTRAIT("Portrait")
}

data class MonitorDisplaySettings(
    val aspectRatio: AspectRatioMode = AspectRatioMode.RATIO_16_9,
    val orientationMode: ScreenOrientationMode = ScreenOrientationMode.SENSOR,
    val zoomLevel: Float = 1.0f,
    val brightness: Float = 0.0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val showSafeGrid: Boolean = false,
    val isAudioEnabled: Boolean = true,
    val isMuted: Boolean = false,
    val audioVolume: Float = 0.85f,
    val isFullScreen: Boolean = true,
    val showOsd: Boolean = true,
    val language: AppLanguage = AppLanguage.BENGALI
)
