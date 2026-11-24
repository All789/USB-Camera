package com.example.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppLanguage
import com.example.model.AspectRatioMode
import com.example.model.MonitorDisplaySettings
import com.example.model.ScreenOrientationMode
import com.example.model.UsbDeviceModel
import com.example.model.VideoSignalState
import com.example.usb.UsbAudioMonitor
import com.example.usb.UsbDeviceDetector
import com.example.usb.UvcStreamEngine
import com.serenegiant.usb.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonitorViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "MonitorViewModel"

    private val context: Context get() = getApplication()
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val usbDetector = UsbDeviceDetector(context)

    val uvcEngine = UvcStreamEngine(application, viewModelScope)
    val audioMonitor = UsbAudioMonitor(application, viewModelScope)

    private val _videoSignalState = MutableStateFlow<VideoSignalState>(VideoSignalState.NoDevice)
    val videoSignalState: StateFlow<VideoSignalState> = _videoSignalState.asStateFlow()

    private val _displaySettings = MutableStateFlow(MonitorDisplaySettings())
    val displaySettings: StateFlow<MonitorDisplaySettings> = _displaySettings.asStateFlow()

    private val _rawConnectedCount = MutableStateFlow(0)
    val rawConnectedCount: StateFlow<Int> = _rawConnectedCount.asStateFlow()

    private val _detectedDevicesSummary = MutableStateFlow<List<String>>(emptyList())
    val detectedDevicesSummary: StateFlow<List<String>> = _detectedDevicesSummary.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private var osdTimerJob: Job? = null

    private val usbInventoryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    updateUsbDeviceInventory()
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    // If USBMonitor didn't pick up a custom VID capture card automatically, assist it cleanly
                    if (device != null && !uvcEngine.isCameraOpened() && !uvcEngine.getUvcDevices().any { it.deviceId == device.deviceId }) {
                        if (usbDetector.isVideoDevice(device)) {
                            handleDeviceAttached(device)
                        }
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    updateUsbDeviceInventory()
                }
            }
        }
    }

    init {
        uvcEngine.setListener(object : UvcStreamEngine.StreamListener {
            override fun onDeviceAttached(device: UsbDevice) {
                updateUsbDeviceInventory()
                val model = usbDetector.toDeviceModel(
                    device,
                    isVideo = true,
                    isAudio = usbDetector.hasAudioInterface(device)
                )
                if (!uvcEngine.isCameraOpened()) {
                    _videoSignalState.value = VideoSignalState.DeviceDetected(model)
                }
            }

            override fun onDeviceOpened(device: UsbDevice, isFirstOpen: Boolean) {
                val model = usbDetector.toDeviceModel(
                    device,
                    isVideo = true,
                    isAudio = usbDetector.hasAudioInterface(device)
                )
                _videoSignalState.value = VideoSignalState.Connecting(
                    device = model,
                    message = "Initializing UVC Engine (${model.displayName})..."
                )
            }

            override fun onCameraOpened(device: UsbDevice, activeSize: Size?, supportedSizes: List<Size>) {
                val model = usbDetector.toDeviceModel(
                    device,
                    isVideo = true,
                    isAudio = usbDetector.hasAudioInterface(device)
                )
                val resLabel = uvcEngine.streamResolution.value
                _videoSignalState.value = VideoSignalState.Streaming(
                    device = model,
                    resolution = resLabel,
                    fps = activeSize?.fps?.takeIf { it > 0 } ?: 30,
                    bitrateMbps = 0f,
                    isAudioActive = model.hasAudioClass,
                    isCamera2Pipeline = false,
                    isTestPattern = false
                )
                emitToast("HDMI Connected: $resLabel")

                if (model.hasAudioClass && _displaySettings.value.isAudioEnabled) {
                    audioMonitor.setVolume(_displaySettings.value.audioVolume)
                    audioMonitor.setMuted(_displaySettings.value.isMuted)
                    audioMonitor.startAudio()
                }
            }

            override fun onCameraClosed(device: UsbDevice) {
                audioMonitor.stopAudio()
            }

            override fun onDeviceDetached(device: UsbDevice) {
                audioMonitor.stopAudio()
                updateUsbDeviceInventory()
                val current = _videoSignalState.value
                if (current !is VideoSignalState.Streaming || !current.isTestPattern) {
                    _videoSignalState.value = VideoSignalState.NoDevice
                }
                emitToast("HDMI Capture Device Disconnected")
            }

            override fun onPermissionCancelled(device: UsbDevice) {
                val model = usbDetector.toDeviceModel(
                    device,
                    isVideo = true,
                    isAudio = usbDetector.hasAudioInterface(device)
                )
                _videoSignalState.value = VideoSignalState.PermissionDenied(model)
                emitToast("USB Permission Denied")
            }

            override fun onStreamError(device: UsbDevice?, message: String) {
                val model = device?.let {
                    usbDetector.toDeviceModel(it, isVideo = true, isAudio = usbDetector.hasAudioInterface(it))
                }
                _videoSignalState.value = VideoSignalState.Error(message = message, device = model)
            }
        })

        // Register USB inventory receiver (attach/detach diagnostic counts only; CameraHelper handles UVC USBMonitor)
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbInventoryReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(usbInventoryReceiver, filter)
        }

        // Initialize native UVCAndroid CameraHelper (scans attached UVC devices automatically)
        uvcEngine.initialize()
        updateUsbDeviceInventory()

        // Sync live FPS / Bitrate / Resolution updates from UvcStreamEngine into Streaming state
        viewModelScope.launch {
            uvcEngine.currentFps.collect { fps ->
                val current = _videoSignalState.value
                if (current is VideoSignalState.Streaming && !current.isTestPattern) {
                    _videoSignalState.value = current.copy(
                        fps = fps,
                        bitrateMbps = uvcEngine.currentBitrateMbps.value,
                        resolution = uvcEngine.streamResolution.value
                    )
                } else if (current is VideoSignalState.Streaming && current.isTestPattern) {
                    _videoSignalState.value = current.copy(
                        fps = fps,
                        bitrateMbps = uvcEngine.currentBitrateMbps.value,
                        resolution = uvcEngine.streamResolution.value
                    )
                }
            }
        }

        triggerOsdAutoHide()
    }

    private fun updateUsbDeviceInventory() {
        val allDevices = usbManager.deviceList ?: emptyMap()
        _rawConnectedCount.value = allDevices.size

        val summaryList = mutableListOf<String>()
        for ((name, dev) in allDevices) {
            val pName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) dev.productName ?: "USB Device" else "USB Device"
            val devInfo = "$pName (VID: 0x${Integer.toHexString(dev.vendorId).uppercase()}, PID: 0x${Integer.toHexString(dev.productId).uppercase()})"
            summaryList.add(devInfo)
            Log.d(tag, "Inventory USB: $name -> $devInfo class=${dev.deviceClass} ifaces=${dev.interfaceCount}")
        }
        _detectedDevicesSummary.value = summaryList
    }

    fun refreshConnectedDevices() {
        updateUsbDeviceInventory()

        // Do not tear down an active UVC stream or test pattern during onResume
        if (uvcEngine.isCameraOpened()) return
        val current = _videoSignalState.value
        if (current is VideoSignalState.Streaming && current.isTestPattern) return

        val uvcList = uvcEngine.getUvcDevices()
        val candidate: UsbDevice? = uvcList.firstOrNull()
            ?: usbDetector.getPrimaryCaptureDevice()?.rawDevice

        if (candidate != null) {
            Log.d(tag, "Found UVC candidate on refresh: ${candidate.deviceName}")
            val model = usbDetector.toDeviceModel(
                candidate,
                isVideo = true,
                isAudio = usbDetector.hasAudioInterface(candidate)
            )
            if (current !is VideoSignalState.PermissionDenied) {
                _videoSignalState.value = VideoSignalState.DeviceDetected(model)
                uvcEngine.selectDevice(candidate)
            }
        } else {
            _videoSignalState.value = VideoSignalState.NoDevice
        }
    }

    fun manualScanDevices() {
        updateUsbDeviceInventory()
        val uvcList = uvcEngine.getUvcDevices()
        val candidate = uvcList.firstOrNull() ?: usbDetector.getPrimaryCaptureDevice()?.rawDevice

        if (candidate == null) {
            val total = _rawConnectedCount.value
            if (total == 0) {
                emitToast("No USB Device Found! Check OTG setting in Android Settings.")
            } else {
                emitToast("Found $total USB device(s), scanning for capture interface...")
            }
            _videoSignalState.value = VideoSignalState.NoDevice
            return
        }

        emitToast("Initializing ${candidate.productName ?: "USB HDMI Capture"}...")
        val model = usbDetector.toDeviceModel(
            candidate,
            isVideo = true,
            isAudio = usbDetector.hasAudioInterface(candidate)
        )
        _videoSignalState.value = VideoSignalState.PermissionRequested(model)
        uvcEngine.selectDevice(candidate)
    }

    fun requestUsbPermission(deviceModel: UsbDeviceModel) {
        val raw = deviceModel.rawDevice ?: return
        _videoSignalState.value = VideoSignalState.PermissionRequested(deviceModel)
        uvcEngine.selectDevice(raw)
    }

    fun handleDeviceAttached(device: UsbDevice?) {
        if (device == null) return
        updateUsbDeviceInventory()
        if (!uvcEngine.isCameraOpened()) {
            val model = usbDetector.toDeviceModel(
                device,
                isVideo = true,
                isAudio = usbDetector.hasAudioInterface(device)
            )
            _videoSignalState.value = VideoSignalState.DeviceDetected(model)
            uvcEngine.selectDevice(device)
        }
    }

    fun changeResolution(size: Size) {
        uvcEngine.changeResolution(size)
        val current = _videoSignalState.value
        if (current is VideoSignalState.Streaming) {
            _videoSignalState.value = current.copy(
                resolution = uvcEngine.streamResolution.value
            )
        }
        triggerOsdAutoHide()
    }

    /**
     * Toggles SMPTE test pattern generator (useful for display calibration or when testing without hardware).
     */
    fun toggleTestPattern(is4K: Boolean = true) {
        val current = _videoSignalState.value
        if (current is VideoSignalState.Streaming && current.isTestPattern) {
            stopAllStreams()
            refreshConnectedDevices()
        } else {
            stopAllStreams()
            uvcEngine.startTestPattern(is4K = is4K) {
                _videoSignalState.value = VideoSignalState.Streaming(
                    device = null,
                    resolution = if (is4K) "3840x2160 (4K UHD)" else "1920x1080 (FHD)",
                    fps = 60,
                    bitrateMbps = if (is4K) 48.5f else 16.2f,
                    isAudioActive = false,
                    isCamera2Pipeline = false,
                    isTestPattern = true
                )
            }
        }
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _displaySettings.update { it.copy(aspectRatio = mode) }
        triggerOsdAutoHide()
    }

    fun setOrientationMode(mode: ScreenOrientationMode) {
        _displaySettings.update { it.copy(orientationMode = mode) }
        triggerOsdAutoHide()
    }

    fun cycleZoom() {
        _displaySettings.update {
            val nextZoom = when (it.zoomLevel) {
                1.0f -> 1.25f
                1.25f -> 1.5f
                1.5f -> 2.0f
                else -> 1.0f
            }
            it.copy(zoomLevel = nextZoom)
        }
        triggerOsdAutoHide()
    }

    fun toggleOsd() {
        _displaySettings.update { it.copy(showOsd = !it.showOsd) }
        if (_displaySettings.value.showOsd) {
            triggerOsdAutoHide()
        }
    }

    fun showOsdBriefly() {
        _displaySettings.update { it.copy(showOsd = true) }
        triggerOsdAutoHide()
    }

    fun hideOsd() {
        _displaySettings.update { it.copy(showOsd = false) }
    }

    private fun triggerOsdAutoHide() {
        osdTimerJob?.cancel()
        osdTimerJob = viewModelScope.launch {
            delay(4000)
            _displaySettings.update { it.copy(showOsd = false) }
        }
    }

    fun setLanguage(language: AppLanguage) {
        _displaySettings.update { it.copy(language = language) }
    }

    fun toggleLanguage() {
        _displaySettings.update {
            val nextLang = if (it.language == AppLanguage.BENGALI) {
                AppLanguage.ENGLISH
            } else {
                AppLanguage.BENGALI
            }
            it.copy(language = nextLang)
        }
    }

    fun toggleFullScreen() {
        _displaySettings.update { it.copy(isFullScreen = !it.isFullScreen) }
    }

    fun setBrightness(brightness: Float) {
        _displaySettings.update { it.copy(brightness = brightness) }
    }

    fun setContrast(contrast: Float) {
        _displaySettings.update { it.copy(contrast = contrast) }
    }

    fun setSaturation(saturation: Float) {
        _displaySettings.update { it.copy(saturation = saturation) }
    }

    fun toggleSafeGrid() {
        _displaySettings.update { it.copy(showSafeGrid = !it.showSafeGrid) }
    }

    fun toggleMute() {
        _displaySettings.update {
            val newMute = !it.isMuted
            audioMonitor.setMuted(newMute)
            it.copy(isMuted = newMute)
        }
        triggerOsdAutoHide()
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        audioMonitor.setVolume(clamped)
        _displaySettings.update { it.copy(audioVolume = clamped, isMuted = false) }
    }

    /**
     * Captures current video frame (from TextureView hardware bitmap or test pattern bitmap) and saves to Gallery.
     */
    fun captureSnapshot(hardwareBitmap: Bitmap? = null) {
        val currentBitmap = hardwareBitmap ?: uvcEngine.latestFrame.value
        if (currentBitmap == null || currentBitmap.isRecycled) {
            emitToast("No live video frame to capture")
            return
        }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val filename = "EasyUSBCam_$timeStamp.jpg"

                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EasyUSBCamera")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }

                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            currentBitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                            context.contentResolver.update(uri, contentValues, null, null)
                        }
                        emitToast("Snapshot saved to Pictures: $filename")
                    } else {
                        emitToast("Failed to create snapshot file")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to capture snapshot", e)
                emitToast("Snapshot failed: ${e.localizedMessage}")
            }
        }
    }

    private fun emitToast(msg: String) {
        viewModelScope.launch {
            _toastEvent.emit(msg)
        }
    }

    fun stopAllStreams() {
        uvcEngine.stopStreaming()
        audioMonitor.stopAudio()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(usbInventoryReceiver)
        } catch (_: Exception) {
        }
        audioMonitor.stopAudio()
        uvcEngine.release()
    }
}
