package com.example.usb

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import com.example.model.UsbDeviceModel

class UsbDeviceDetector(private val context: Context) {

    private val usbManager: UsbManager by lazy {
        context.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    companion object {
        const val USB_CLASS_VIDEO = 14
        const val USB_SUBCLASS_VIDEO_CONTROL = 1
        const val USB_SUBCLASS_VIDEO_STREAMING = 2

        const val USB_CLASS_AUDIO = 1
        const val USB_SUBCLASS_AUDIO_STREAMING = 2

        // Common HDMI Capture Card Vendor IDs
        val KNOWN_CAPTURE_VIDS = setOf(
            0x534D, // MacroSilicon (MS2109, etc. - most popular HDMI to USB dongle)
            0x0851, // MacroSilicon
            0x0FD9, // Elgato (Cam Link 4K)
            0x04B4, // Cypress / Genki ShadowCast
            0x07CA, // AVerMedia
            0x1E4E, // Cubeternet
            0x18EC, // Arkmicro
            0x0BDA, // Realtek
            0x1B80, // Afatech
            0x1908  // GEMBIRD / generic capture
        )
    }

    /**
     * Enumerates all attached USB devices and returns those identified as video capture devices.
     */
    fun findConnectedCaptureDevices(): List<UsbDeviceModel> {
        val deviceList = usbManager.deviceList ?: return emptyList()
        val results = mutableListOf<UsbDeviceModel>()

        // Pass 1: Strict check (UVC video class, known capture VID, or video interfaces)
        for ((_, device) in deviceList) {
            val isVideo = isVideoDevice(device)
            val isAudio = hasAudioInterface(device)

            if (isVideo || KNOWN_CAPTURE_VIDS.contains(device.vendorId)) {
                results.add(toDeviceModel(device, isVideo = true, isAudio = isAudio))
            }
        }

        // Pass 2: If no strict UVC match found, include any non-hub external USB device
        // so that unlisted capture card chipsets still trigger permission and connect attempts!
        if (results.isEmpty()) {
            for ((_, device) in deviceList) {
                // Ignore standard USB hubs (class 9)
                if (device.deviceClass != UsbConstants.USB_CLASS_HUB) {
                    val isVideo = isVideoDevice(device)
                    val isAudio = hasAudioInterface(device)
                    results.add(toDeviceModel(device, isVideo = isVideo || device.interfaceCount > 0, isAudio = isAudio))
                }
            }
        }

        return results
    }

    /**
     * Returns the primary or best candidate USB HDMI capture device.
     */
    fun getPrimaryCaptureDevice(): UsbDeviceModel? {
        val devices = findConnectedCaptureDevices()
        if (devices.isEmpty()) return null

        // Prefer devices with confirmed UVC video interface
        return devices.firstOrNull { it.hasVideoClass } ?: devices.first()
    }

    /**
     * Checks if a device is a UVC video capture device.
     */
    fun isVideoDevice(device: UsbDevice): Boolean {
        // Direct class check
        if (device.deviceClass == USB_CLASS_VIDEO) return true

        // Interface check
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == USB_CLASS_VIDEO) {
                return true
            }
        }

        // Check if VID is in known capture cards list
        if (KNOWN_CAPTURE_VIDS.contains(device.vendorId)) {
            return true
        }

        return false
    }

    /**
     * Checks if device includes audio interface.
     */
    fun hasAudioInterface(device: UsbDevice): Boolean {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == USB_CLASS_AUDIO) {
                return true
            }
        }
        return false
    }

    /**
     * Converts native UsbDevice to UsbDeviceModel.
     */
    fun toDeviceModel(device: UsbDevice, isVideo: Boolean, isAudio: Boolean): UsbDeviceModel {
        val hasPermission = usbManager.hasPermission(device)
        val productName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            device.productName ?: "USB HDMI Capture"
        } else {
            "USB HDMI Capture"
        }

        val manufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            device.manufacturerName ?: ""
        } else {
            ""
        }

        return UsbDeviceModel(
            deviceId = device.deviceId,
            deviceName = device.deviceName,
            productName = productName,
            manufacturerName = manufacturer,
            vendorId = device.vendorId,
            productId = device.productId,
            deviceClass = device.deviceClass,
            hasVideoClass = isVideo,
            hasAudioClass = isAudio,
            hasPermission = hasPermission,
            rawDevice = device
        )
    }

    /**
     * Finds the video streaming interface and IN endpoint for UVC capture.
     */
    fun findVideoStreamingInterface(device: UsbDevice): Pair<UsbInterface, UsbEndpoint>? {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == USB_CLASS_VIDEO && iface.interfaceSubclass == USB_SUBCLASS_VIDEO_STREAMING) {
                // Find IN endpoint
                for (j in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(j)
                    if (ep.direction == UsbConstants.USB_DIR_IN) {
                        return Pair(iface, ep)
                    }
                }
            }
        }

        // Fallback: any interface with class 14 that has an IN endpoint
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == USB_CLASS_VIDEO) {
                for (j in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(j)
                    if (ep.direction == UsbConstants.USB_DIR_IN) {
                        return Pair(iface, ep)
                    }
                }
            }
        }

        return null
    }

    /**
     * Finds the audio streaming interface and IN endpoint for capture.
     */
    fun findAudioStreamingInterface(device: UsbDevice): Pair<UsbInterface, UsbEndpoint>? {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == USB_CLASS_AUDIO && iface.interfaceSubclass == USB_SUBCLASS_AUDIO_STREAMING) {
                for (j in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(j)
                    if (ep.direction == UsbConstants.USB_DIR_IN) {
                        return Pair(iface, ep)
                    }
                }
            }
        }
        return null
    }
}
