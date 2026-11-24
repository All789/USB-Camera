package com.example.usb

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.hardware.usb.UsbDevice
import android.util.Log
import android.view.Surface
import com.herohan.uvcapp.CameraException
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper
import com.serenegiant.usb.Size
import com.serenegiant.usb.UVCCamera
import com.serenegiant.usb.UVCParam
import com.serenegiant.utils.UVCUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UvcStreamEngine(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val tag = "UvcStreamEngine"

    interface StreamListener {
        fun onDeviceAttached(device: UsbDevice)
        fun onDeviceOpened(device: UsbDevice, isFirstOpen: Boolean)
        fun onCameraOpened(device: UsbDevice, activeSize: Size?, supportedSizes: List<Size>)
        fun onCameraClosed(device: UsbDevice)
        fun onDeviceDetached(device: UsbDevice)
        fun onPermissionCancelled(device: UsbDevice)
        fun onStreamError(device: UsbDevice?, message: String)
    }

    private var listener: StreamListener? = null
    private var cameraHelper: ICameraHelper? = null
    private var previewSurface: Surface? = null
    private var isInitialized = false
    private var isTestPatternRunning = false
    private var testPatternJob: Job? = null

    private val _latestFrame = MutableStateFlow<Bitmap?>(null)
    val latestFrame: StateFlow<Bitmap?> = _latestFrame.asStateFlow()

    private val _currentFps = MutableStateFlow(0)
    val currentFps: StateFlow<Int> = _currentFps.asStateFlow()

    private val _currentBitrateMbps = MutableStateFlow(0f)
    val currentBitrateMbps: StateFlow<Float> = _currentBitrateMbps.asStateFlow()

    private val _streamResolution = MutableStateFlow("1920x1080")
    val streamResolution: StateFlow<String> = _streamResolution.asStateFlow()

    private val _supportedSizes = MutableStateFlow<List<Size>>(emptyList())
    val supportedSizes: StateFlow<List<Size>> = _supportedSizes.asStateFlow()

    private val _selectedSize = MutableStateFlow<Size?>(null)
    val selectedSize: StateFlow<Size?> = _selectedSize.asStateFlow()

    private val _videoWidth = MutableStateFlow(1920)
    val videoWidth: StateFlow<Int> = _videoWidth.asStateFlow()

    private val _videoHeight = MutableStateFlow(1080)
    val videoHeight: StateFlow<Int> = _videoHeight.asStateFlow()

    private val _isNativeUvcActive = MutableStateFlow(false)
    val isNativeUvcActive: StateFlow<Boolean> = _isNativeUvcActive.asStateFlow()

    // Live FPS calculation from TextureView hardware surface updates
    private var renderedFrameCount = 0
    private var lastFpsWindowStartMs = 0L

    private val stateCallback = object : ICameraHelper.StateCallback {
        override fun onAttach(device: UsbDevice) {
            Log.d(tag, "UVCAndroid onAttach: ${device.deviceName} VID=0x${Integer.toHexString(device.vendorId)} PID=0x${Integer.toHexString(device.productId)}")
            listener?.onDeviceAttached(device)
            try {
                cameraHelper?.selectDevice(device)
            } catch (e: Throwable) {
                Log.e(tag, "Error selecting attached device", e)
                listener?.onStreamError(device, "Failed to select USB camera: ${e.localizedMessage}")
            }
        }

        override fun onDeviceOpen(device: UsbDevice, isFirstOpen: Boolean) {
            Log.d(tag, "UVCAndroid onDeviceOpen: ${device.deviceName}, isFirstOpen=$isFirstOpen")
            listener?.onDeviceOpened(device, isFirstOpen)
            try {
                val param = UVCParam().apply {
                    quirks = UVCCamera.getRecommendedPlatformQuirks()
                }
                cameraHelper?.openCamera(param)
            } catch (e: Throwable) {
                Log.e(tag, "Error opening UVC camera", e)
                listener?.onStreamError(device, "Failed to open UVC camera: ${e.localizedMessage}")
            }
        }

        override fun onCameraOpen(device: UsbDevice) {
            Log.d(tag, "UVCAndroid onCameraOpen: ${device.deviceName}")
            stopTestPatternOnly()

            val helper = cameraHelper ?: return
            val sizes = try {
                helper.supportedSizeList ?: emptyList()
            } catch (e: Throwable) {
                Log.w(tag, "Could not query supportedSizeList: ${e.message}")
                emptyList()
            }

            sizes.forEach { s ->
                Log.d(tag, "  Supported size: ${s.width}x${s.height} type=${s.type} fps=${s.fps} fpsList=${s.fpsList}")
            }

            _supportedSizes.value = sizes
            val bestSize = pickBestSize(sizes)

            if (bestSize != null) {
                if (bestSize.fps <= 0) {
                    bestSize.fps = bestSize.fpsList?.maxOrNull() ?: 30
                }
                try {
                    helper.setPreviewSize(bestSize)
                    Log.d(tag, "Selected UVC preview size: ${bestSize.width}x${bestSize.height} @ ${bestSize.fps}fps (type=${bestSize.type})")
                } catch (e: Throwable) {
                    Log.w(tag, "setPreviewSize failed, falling back to device default: ${e.message}")
                }
            }

            val currentSize = try {
                helper.previewSize ?: bestSize
            } catch (_: Throwable) {
                bestSize
            }

            val w = currentSize?.width?.takeIf { it > 0 } ?: 1920
            val h = currentSize?.height?.takeIf { it > 0 } ?: 1080
            _videoWidth.value = w
            _videoHeight.value = h
            _selectedSize.value = currentSize
            _streamResolution.value = formatResolutionLabel(w, h, currentSize?.type)
            _isNativeUvcActive.value = true
            renderedFrameCount = 0
            lastFpsWindowStartMs = System.currentTimeMillis()

            listener?.onCameraOpened(device, currentSize, sizes)

            // Attach surface if already available, then start native UVC streaming
            previewSurface?.let { surface ->
                if (surface.isValid) {
                    try {
                        helper.addSurface(surface, false)
                        Log.d(tag, "Added existing preview surface on onCameraOpen")
                    } catch (e: Throwable) {
                        Log.w(tag, "addSurface warning in onCameraOpen: ${e.message}")
                    }
                }
            }

            try {
                helper.startPreview()
                Log.d(tag, "UVCAndroid startPreview() started")
            } catch (e: Throwable) {
                Log.e(tag, "startPreview failed", e)
                listener?.onStreamError(device, "Failed to start UVC video preview: ${e.localizedMessage}")
            }
        }

        override fun onCameraClose(device: UsbDevice) {
            Log.d(tag, "UVCAndroid onCameraClose: ${device.deviceName}")
            _isNativeUvcActive.value = false
            _currentFps.value = 0
            _currentBitrateMbps.value = 0f
            previewSurface?.let { surface ->
                try {
                    cameraHelper?.removeSurface(surface)
                } catch (_: Throwable) {
                }
            }
            listener?.onCameraClosed(device)
        }

        override fun onDeviceClose(device: UsbDevice) {
            Log.d(tag, "UVCAndroid onDeviceClose: ${device.deviceName}")
        }

        override fun onDetach(device: UsbDevice) {
            Log.d(tag, "UVCAndroid onDetach: ${device.deviceName}")
            _isNativeUvcActive.value = false
            _supportedSizes.value = emptyList()
            _selectedSize.value = null
            _currentFps.value = 0
            _currentBitrateMbps.value = 0f
            try {
                cameraHelper?.closeCamera()
            } catch (_: Throwable) {
            }
            listener?.onDeviceDetached(device)
        }

        override fun onCancel(device: UsbDevice) {
            Log.w(tag, "UVCAndroid onCancel (USB permission cancelled/denied): ${device.deviceName}")
            listener?.onPermissionCancelled(device)
        }

        override fun onError(device: UsbDevice?, e: CameraException) {
            Log.e(tag, "UVCAndroid onError (code=${e.code}): ${e.message}", e)
            listener?.onStreamError(device, e.message ?: "UVC camera initialization failed (code ${e.code})")
        }
    }

    fun setListener(streamListener: StreamListener) {
        this.listener = streamListener
    }

    fun initialize() {
        if (isInitialized) return
        try {
            UVCUtils.init(context.applicationContext)
            cameraHelper = CameraHelper().apply {
                setStateCallback(stateCallback)
            }
            isInitialized = true
            Log.d(tag, "UVCAndroid CameraHelper initialized cleanly")
        } catch (e: Throwable) {
            Log.e(tag, "Failed to initialize UVCAndroid CameraHelper", e)
        }
    }

    fun getUvcDevices(): List<UsbDevice> {
        initialize()
        return try {
            cameraHelper?.deviceList ?: emptyList()
        } catch (e: Throwable) {
            Log.w(tag, "getDeviceList failed: ${e.message}")
            emptyList()
        }
    }

    fun selectDevice(device: UsbDevice) {
        initialize()
        try {
            Log.d(tag, "Selecting UVC device: ${device.deviceName}")
            cameraHelper?.selectDevice(device)
        } catch (e: Throwable) {
            Log.e(tag, "selectDevice failed", e)
            listener?.onStreamError(device, "Unable to open USB device: ${e.localizedMessage}")
        }
    }

    fun isCameraOpened(): Boolean {
        return try {
            cameraHelper?.isCameraOpened == true
        } catch (_: Throwable) {
            false
        }
    }

    fun attachPreviewSurface(surface: Surface) {
        if (!surface.isValid) return
        val oldSurface = previewSurface
        if (oldSurface != null && oldSurface != surface) {
            try {
                cameraHelper?.removeSurface(oldSurface)
            } catch (_: Throwable) {
            }
        }
        previewSurface = surface

        val helper = cameraHelper ?: return
        if (helper.isCameraOpened) {
            try {
                helper.addSurface(surface, false)
                helper.startPreview()
                Log.d(tag, "Attached TextureView Surface to active CameraHelper")
            } catch (e: Throwable) {
                Log.e(tag, "Error attaching preview Surface", e)
            }
        }
    }

    fun detachPreviewSurface(surface: Surface) {
        if (previewSurface == surface) {
            previewSurface = null
        }
        try {
            cameraHelper?.removeSurface(surface)
        } catch (_: Throwable) {
        }
    }

    /**
     * Called from TextureView.SurfaceTextureListener.onSurfaceTextureUpdated
     * whenever the native UVC pipeline renders a fresh HDMI frame onto the GPU surface.
     */
    fun notifyFrameRendered() {
        if (!_isNativeUvcActive.value) return
        val now = System.currentTimeMillis()
        if (lastFpsWindowStartMs == 0L) {
            lastFpsWindowStartMs = now
        }
        renderedFrameCount++
        val elapsed = now - lastFpsWindowStartMs
        if (elapsed >= 1000L) {
            val fps = ((renderedFrameCount * 1000L) / elapsed).toInt().coerceAtLeast(1)
            _currentFps.value = fps

            val w = _videoWidth.value
            val h = _videoHeight.value
            val isMjpeg = _selectedSize.value?.type == UVCCamera.UVC_VS_FRAME_MJPEG
            val estMbps = if (isMjpeg) {
                (w.toFloat() * h.toFloat() * fps.toFloat() * 1.2f) / 1_000_000f
            } else {
                (w.toFloat() * h.toFloat() * 16f * fps.toFloat()) / 1_000_000f
            }
            _currentBitrateMbps.value = estMbps

            renderedFrameCount = 0
            lastFpsWindowStartMs = now
        }
    }

    fun changeResolution(size: Size) {
        val helper = cameraHelper ?: return
        if (!helper.isCameraOpened) return
        try {
            if (size.fps <= 0) {
                size.fps = size.fpsList?.maxOrNull() ?: 30
            }
            helper.stopPreview()
            helper.setPreviewSize(size)
            _selectedSize.value = size
            _videoWidth.value = size.width
            _videoHeight.value = size.height
            _streamResolution.value = formatResolutionLabel(size.width, size.height, size.type)
            renderedFrameCount = 0
            lastFpsWindowStartMs = System.currentTimeMillis()

            previewSurface?.let { surface ->
                if (surface.isValid) {
                    helper.addSurface(surface, false)
                }
            }
            helper.startPreview()
            Log.d(tag, "Switched UVC preview resolution to ${size.width}x${size.height} @ ${size.fps}fps")
        } catch (e: Throwable) {
            Log.e(tag, "Failed to change UVC resolution", e)
        }
    }

    private fun pickBestSize(sizes: List<Size>): Size? {
        if (sizes.isEmpty()) return null
        val valid = sizes.filter { it.width > 0 && it.height > 0 }
        if (valid.isEmpty()) return sizes.firstOrNull()

        fun fpsOf(s: Size): Int = if (s.fps > 0) s.fps else (s.fpsList?.maxOrNull() ?: 30)

        val mjpeg = valid.filter { it.type == UVCCamera.UVC_VS_FRAME_MJPEG }
        val pool = mjpeg.ifEmpty { valid }

        // 1. Prefer crisp 1920x1080 >= 24fps (ideal latency & sharpness on mobile OTG)
        pool.filter { it.width == 1920 && it.height == 1080 && fpsOf(it) >= 24 }
            .maxByOrNull { fpsOf(it) }?.let { return it }

        // 2. Next prefer 1280x720 >= 24fps
        pool.filter { it.width == 1280 && it.height == 720 && fpsOf(it) >= 24 }
            .maxByOrNull { fpsOf(it) }?.let { return it }

        // 3. Next allow 4K UHD 3840x2160 if smooth (>= 24fps MJPEG)
        pool.filter { it.width >= 3800 && fpsOf(it) >= 24 }
            .maxByOrNull { fpsOf(it) }?.let { return it }

        // 4. Highest resolution with >= 24fps
        pool.filter { fpsOf(it) >= 24 }
            .maxByOrNull { it.width * it.height }?.let { return it }

        // 5. Fallback to any highest-fps 1080p/720p or first entry
        return pool.maxByOrNull { fpsOf(it) } ?: valid.first()
    }

    private fun formatResolutionLabel(width: Int, height: Int, type: Int?): String {
        val codecTag = when (type) {
            UVCCamera.UVC_VS_FRAME_MJPEG -> "MJPEG"
            UVCCamera.UVC_VS_FRAME_UNCOMPRESSED -> "YUYV"
            else -> ""
        }
        val base = if (width >= 3800 || height >= 2100) {
            "${width}x${height} (4K UHD)"
        } else {
            "${width}x${height}"
        }
        return if (codecTag.isNotEmpty()) "$base $codecTag" else base
    }

    /**
     * Starts generating a SMPTE calibration test pattern (Bars & Tone / HDMI Test Signal).
     * Supports true 4K Ultra HD (3840x2160) and 1080p Full HD display verification.
     */
    fun startTestPattern(is4K: Boolean = true, onFirstFrame: () -> Unit) {
        stopStreaming()
        _isNativeUvcActive.value = false
        isTestPatternRunning = true

        testPatternJob = coroutineScope.launch(Dispatchers.Default) {
            val width = if (is4K) 3840 else 1920
            val height = if (is4K) 2160 else 1080
            _videoWidth.value = width
            _videoHeight.value = height
            _streamResolution.value = if (is4K) "3840x2160 (4K UHD)" else "1920x1080 (FHD)"
            _currentFps.value = 60
            _currentBitrateMbps.value = if (is4K) 48.5f else 16.2f

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()

            val colors = listOf(
                0xFFFFFFFF.toInt(),
                0xFFFFFF00.toInt(),
                0xFF00FFFF.toInt(),
                0xFF00FF00.toInt(),
                0xFFFF00FF.toInt(),
                0xFFFF0000.toInt(),
                0xFF0000FF.toInt()
            )

            var tick = 0
            onFirstFrame()

            while (isActive && isTestPatternRunning) {
                val barWidth = width.toFloat() / colors.size
                val barHeight = height.toFloat() * 0.75f

                colors.forEachIndexed { index, color ->
                    paint.color = color
                    canvas.drawRect(index * barWidth, 0f, (index + 1) * barWidth, barHeight, paint)
                }

                paint.color = 0xFF002244.toInt()
                canvas.drawRect(0f, barHeight, barWidth, height.toFloat(), paint)

                paint.color = 0xFFFFFFFF.toInt()
                canvas.drawRect(barWidth, barHeight, barWidth * 2, height.toFloat(), paint)

                paint.color = 0xFF330055.toInt()
                canvas.drawRect(barWidth * 2, barHeight, barWidth * 3, height.toFloat(), paint)

                paint.color = 0xFF111111.toInt()
                canvas.drawRect(barWidth * 3, barHeight, width.toFloat(), height.toFloat(), paint)

                paint.color = 0xFF00E5FF.toInt()
                val sweepX = ((tick * 15) % width).toFloat()
                canvas.drawRect(sweepX, height - 32f, sweepX + 60f, height.toFloat(), paint)

                val bannerH = if (is4K) 180f else 90f
                paint.color = 0xEE070A0F.toInt()
                canvas.drawRect(width / 4f, height / 2f - bannerH / 2f, width * 3 / 4f, height / 2f + bannerH / 2f, paint)

                paint.color = 0xFF00E5FF.toInt()
                paint.textSize = if (is4K) 72f else 36f
                paint.isAntiAlias = true
                paint.textAlign = Paint.Align.CENTER
                paint.isFakeBoldText = true
                canvas.drawText(
                    if (is4K) "EASY USB CAMERA • 4K ULTRA HD MONITOR" else "EASY USB CAMERA • HDMI MONITOR",
                    width / 2f,
                    height / 2f - (if (is4K) 10f else 5f),
                    paint
                )

                paint.color = 0xFFFFB300.toInt()
                paint.textSize = if (is4K) 44f else 22f
                paint.isFakeBoldText = false
                canvas.drawText(
                    if (is4K) "3840x2160 @ 60 FPS • 4K UVC CALIBRATION SIGNAL" else "1920x1080 @ 60 FPS • SMPTE SIGNAL GENERATOR",
                    width / 2f,
                    height / 2f + (if (is4K) 52f else 26f),
                    paint
                )

                _latestFrame.value = bitmap
                tick++
                delay(16)
            }
        }
    }

    private fun stopTestPatternOnly() {
        isTestPatternRunning = false
        testPatternJob?.cancel()
        testPatternJob = null
        _latestFrame.value = null
    }

    fun stopStreaming() {
        stopTestPatternOnly()
        _isNativeUvcActive.value = false
        try {
            cameraHelper?.let { helper ->
                previewSurface?.let { surface ->
                    try {
                        helper.removeSurface(surface)
                    } catch (_: Throwable) {
                    }
                }
                if (helper.isCameraOpened) {
                    helper.stopPreview()
                    helper.closeCamera()
                }
            }
        } catch (e: Throwable) {
            Log.w(tag, "Error stopping UVC stream: ${e.message}")
        }
        _currentFps.value = 0
        _currentBitrateMbps.value = 0f
    }

    fun release() {
        stopStreaming()
        try {
            cameraHelper?.setStateCallback(null)
            cameraHelper?.releaseAll()
        } catch (e: Throwable) {
            Log.w(tag, "Error releasing CameraHelper: ${e.message}")
        }
        cameraHelper = null
        previewSurface = null
        isInitialized = false
    }
}
