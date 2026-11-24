package com.example

import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.model.ScreenOrientationMode
import com.example.ui.MonitorScreen
import com.example.ui.components.SplashIntroOverlay
import com.example.ui.theme.EasyUsbCameraTheme
import com.example.viewmodel.MonitorViewModel
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MonitorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on continuously while acting as an external HDMI monitor
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize Google Mobile Ads SDK off the main thread safely (supports non-GMS devices gracefully)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                MobileAds.initialize(applicationContext) {}
            } catch (_: Throwable) {
            }
        }

        setContent {
            EasyUsbCameraTheme {
                val settings by viewModel.displaySettings.collectAsStateWithLifecycle()
                var showSplash by rememberSaveable { mutableStateOf(true) }

                // Dynamic screen orientation lock
                LaunchedEffect(settings.orientationMode) {
                    requestedOrientation = when (settings.orientationMode) {
                        ScreenOrientationMode.SENSOR -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                        ScreenOrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        ScreenOrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    }
                }

                // Dynamic Fullscreen / Immersive Sticky Mode
                LaunchedEffect(settings.isFullScreen) {
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                    if (settings.isFullScreen) {
                        insetsController.hide(WindowInsetsCompat.Type.systemBars())
                    } else {
                        insetsController.show(WindowInsetsCompat.Type.systemBars())
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MonitorScreen(viewModel = viewModel)

                        AnimatedVisibility(
                            visible = showSplash,
                            exit = fadeOut(animationSpec = tween(durationMillis = 420))
                        ) {
                            SplashIntroOverlay(
                                language = settings.language,
                                onAnimationFinished = { showSplash = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check for newly connected or reconnecting USB capture cards
        viewModel.refreshConnectedDevices()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val device: android.hardware.usb.UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, android.hardware.usb.UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
        if (device != null) {
            viewModel.handleDeviceAttached(device)
        } else {
            viewModel.refreshConnectedDevices()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            viewModel.stopAllStreams()
        }
    }
}
