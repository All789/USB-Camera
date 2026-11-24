package com.example

import com.example.model.AspectRatioMode
import com.example.model.MonitorDisplaySettings
import com.example.model.ScreenOrientationMode
import com.example.model.UsbDeviceModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun monitorDisplaySettings_defaults() {
    val settings = MonitorDisplaySettings()
    assertEquals(AspectRatioMode.RATIO_16_9, settings.aspectRatio)
    assertEquals(ScreenOrientationMode.SENSOR, settings.orientationMode)
    assertEquals(1.0f, settings.zoomLevel, 0.001f)
    assertEquals(0.0f, settings.brightness, 0.001f)
    assertEquals(1.0f, settings.contrast, 0.001f)
    assertEquals(1.0f, settings.saturation, 0.001f)
    assertTrue(settings.isFullScreen)
    assertFalse(settings.isMuted)
  }

  @Test
  fun usbDeviceModel_displayName() {
    val deviceWithProduct = UsbDeviceModel(
      deviceId = 1,
      deviceName = "/dev/bus/usb/001/002",
      productName = "Cam Link 4K",
      manufacturerName = "Elgato",
      vendorId = 0x0FD9,
      productId = 0x0066,
      deviceClass = 14,
      hasVideoClass = true,
      hasAudioClass = true,
      hasPermission = true
    )
    assertEquals("Cam Link 4K", deviceWithProduct.displayName)
    assertTrue(deviceWithProduct.technicalSummary.contains("0x0FD9"))
    assertTrue(deviceWithProduct.technicalSummary.contains("[UVC Video]"))
    assertTrue(deviceWithProduct.technicalSummary.contains("[HDMI Audio]"))

    val genericDevice = UsbDeviceModel(
      deviceId = 2,
      deviceName = "",
      productName = "",
      manufacturerName = "",
      vendorId = 0x534D,
      productId = 0x2109,
      deviceClass = 14,
      hasVideoClass = true,
      hasAudioClass = false,
      hasPermission = false
    )
    assertTrue(genericDevice.displayName.contains("534D:2109"))
  }

  @Test
  fun appLanguage_translations() {
    val bnStrings = com.example.localization.getStrings(com.example.model.AppLanguage.BENGALI)
    assertEquals("পোর্টেবল এইচডিএমআই মনিটর", bnStrings.appTitle)
    assertEquals("লাইভ এইচডিএমআই", bnStrings.liveHdmi)

    val enStrings = com.example.localization.getStrings(com.example.model.AppLanguage.ENGLISH)
    assertEquals("PORTABLE HDMI MONITOR", enStrings.appTitle)
    assertEquals("LIVE HDMI", enStrings.liveHdmi)
  }

  @Test
  fun exitDialog_strings() {
    val bnStrings = com.example.localization.getStrings(com.example.model.AppLanguage.BENGALI)
    assertTrue(bnStrings.exitDialogTitle.isNotEmpty())
    assertTrue(bnStrings.exitDialogYes.contains("হ্যাঁ"))
    assertTrue(bnStrings.exitDialogNo.contains("না"))

    val enStrings = com.example.localization.getStrings(com.example.model.AppLanguage.ENGLISH)
    assertTrue(enStrings.exitDialogTitle.isNotEmpty())
    assertTrue(enStrings.exitDialogYes.contains("Yes"))
    assertTrue(enStrings.exitDialogNo.contains("No"))
  }
}
