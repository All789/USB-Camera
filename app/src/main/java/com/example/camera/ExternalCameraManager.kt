package com.example.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface

class ExternalCameraManager(private val context: Context) {
    private val tag = "ExternalCameraManager"

    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private var activeCameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private val backgroundHandler = Handler(Looper.getMainLooper())

    /**
     * Finds if any Camera2 device is marked as LENS_FACING_EXTERNAL.
     */
    fun findExternalCameraId(): String? {
        try {
            val cameraIds = cameraManager.cameraIdList
            for (id in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                    return id
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Camera enumeration error: ${e.message}")
        }
        return null
    }

    fun hasExternalCamera(): Boolean {
        return findExternalCameraId() != null
    }

    @SuppressLint("MissingPermission")
    fun startPreview(
        cameraId: String,
        targetSurface: Surface,
        onFirstFrame: () -> Unit,
        onError: (String) -> Unit
    ) {
        stopPreview()

        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    activeCameraDevice = camera
                    try {
                        val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(targetSurface)
                            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                        }

                        @Suppress("DEPRECATION")
                        camera.createCaptureSession(
                            listOf(targetSurface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    captureSession = session
                                    try {
                                        session.setRepeatingRequest(
                                            previewRequestBuilder.build(),
                                            object : CameraCaptureSession.CaptureCallback() {
                                                private var reported = false
                                                override fun onCaptureStarted(
                                                    session: CameraCaptureSession,
                                                    request: CaptureRequest,
                                                    timestamp: Long,
                                                    frameNumber: Long
                                                ) {
                                                    if (!reported) {
                                                        reported = true
                                                        backgroundHandler.post { onFirstFrame() }
                                                    }
                                                }
                                            },
                                            backgroundHandler
                                        )
                                    } catch (e: Exception) {
                                        onError("Capture session error: ${e.message}")
                                    }
                                }

                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    onError("Failed to configure HDMI preview session.")
                                }
                            },
                            backgroundHandler
                        )
                    } catch (e: Exception) {
                        onError("Preview creation failed: ${e.message}")
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    stopPreview()
                    onError("External camera disconnected.")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    stopPreview()
                    onError("Camera device error code: $error")
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            onError("Unable to open external camera: ${e.message}")
        }
    }

    fun stopPreview() {
        try {
            captureSession?.stopRepeating()
            captureSession?.close()
        } catch (e: Exception) {
            Log.w(tag, "Error closing capture session: ${e.message}")
        }
        captureSession = null

        try {
            activeCameraDevice?.close()
        } catch (e: Exception) {
            Log.w(tag, "Error closing camera device: ${e.message}")
        }
        activeCameraDevice = null
    }
}
