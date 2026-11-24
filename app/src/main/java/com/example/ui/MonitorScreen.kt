package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.TextureView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.localization.getStrings
import com.example.model.VideoSignalState
import com.example.ui.components.AboutPrivacyDialog
import com.example.ui.components.AdMobBannerView
import com.example.ui.components.ExitConfirmationDialog
import com.example.ui.components.MonitorBottomBar
import com.example.ui.components.MonitorTopBar
import com.example.ui.components.NoSignalView
import com.example.ui.components.PermissionPromptDialog
import com.example.ui.components.PictureAdjustmentDialog
import com.example.ui.components.VideoCanvasView
import com.example.ui.theme.MonitorSurface
import com.example.ui.theme.MonitorSurfaceElevated
import com.example.ui.theme.SignalCyan
import com.example.viewmodel.MonitorViewModel
import com.serenegiant.usb.Size
import com.serenegiant.usb.UVCCamera

@Composable
fun MonitorScreen(
    viewModel: MonitorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val signalState by viewModel.videoSignalState.collectAsStateWithLifecycle()
    val settings by viewModel.displaySettings.collectAsStateWithLifecycle()
    val latestFrame by viewModel.uvcEngine.latestFrame.collectAsStateWithLifecycle()
    val isNativeUvcActive by viewModel.uvcEngine.isNativeUvcActive.collectAsStateWithLifecycle()
    val videoWidth by viewModel.uvcEngine.videoWidth.collectAsStateWithLifecycle()
    val videoHeight by viewModel.uvcEngine.videoHeight.collectAsStateWithLifecycle()
    val supportedSizes by viewModel.uvcEngine.supportedSizes.collectAsStateWithLifecycle()
    val selectedSize by viewModel.uvcEngine.selectedSize.collectAsStateWithLifecycle()
    val audioLevel by viewModel.audioMonitor.audioPeakLevel.collectAsStateWithLifecycle()

    val connectedUsbCount by viewModel.rawConnectedCount.collectAsStateWithLifecycle()
    val detectedDevicesSummary by viewModel.detectedDevicesSummary.collectAsStateWithLifecycle()

    var showPictureTuning by remember { mutableStateOf(false) }
    var showAboutPrivacyDialog by remember { mutableStateOf(false) }
    var showResolutionPicker by remember { mutableStateOf(false) }
    var dismissPermissionPrompt by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var activeTextureView by remember { mutableStateOf<TextureView?>(null) }

    // Android 10+ OEMs require runtime CAMERA permission for UVC Class 14 USB devices
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.refreshConnectedDevices()
        }
    }

    LaunchedEffect(Unit) {
        val hasCameraPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCameraPerm) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Intercept Back button so the app doesn't exit immediately without confirmation
    BackHandler {
        when {
            showResolutionPicker -> showResolutionPicker = false
            showAboutPrivacyDialog -> showAboutPrivacyDialog = false
            showPictureTuning -> showPictureTuning = false
            showExitDialog -> showExitDialog = false
            else -> showExitDialog = true
        }
    }

    // Toast/Feedback events
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
                .testTag("monitor_screen")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Tapping anywhere on the monitor screen toggles the OSD controls
                    viewModel.toggleOsd()
                }
        ) {
        when (val state = signalState) {
            is VideoSignalState.NoDevice -> {
                NoSignalView(
                    language = settings.language,
                    connectedUsbCount = connectedUsbCount,
                    detectedDevices = detectedDevicesSummary,
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onRefresh = { viewModel.manualScanDevices() },
                    onStartTestPattern = { viewModel.toggleTestPattern() },
                    onOpenAboutPrivacy = { showAboutPrivacyDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            }

            is VideoSignalState.Connecting -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MonitorSurface)
                            .padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = SignalCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            is VideoSignalState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MonitorSurface)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "SIGNAL ERROR",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        TextButton(onClick = { viewModel.manualScanDevices() }) {
                            Text(
                                text = getStrings(settings.language).scanUsb,
                                color = SignalCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            is VideoSignalState.Streaming -> {
                // Video rendering surface (native UVC TextureView for live HDMI, or Bitmap for SMPTE test signal)
                VideoCanvasView(
                    frame = latestFrame,
                    isNativeUvcActive = isNativeUvcActive && !state.isTestPattern,
                    videoWidth = videoWidth,
                    videoHeight = videoHeight,
                    settings = settings,
                    onSurfaceAttached = { surface -> viewModel.uvcEngine.attachPreviewSurface(surface) },
                    onSurfaceDetached = { surface -> viewModel.uvcEngine.detachPreviewSurface(surface) },
                    onFrameRendered = { viewModel.uvcEngine.notifyFrameRendered() },
                    onTextureViewReady = { tv -> activeTextureView = tv },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("video_feed")
                )

                // OSD Top Bar (Status, FPS, Resolution, Audio VU, Fullscreen, Info, Language)
                MonitorTopBar(
                    signalState = state,
                    settings = settings,
                    audioLevel = audioLevel,
                    onToggleMute = { viewModel.toggleMute() },
                    onToggleFullscreen = { viewModel.toggleFullScreen() },
                    onOpenAboutPrivacy = { showAboutPrivacyDialog = true },
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onSelectResolutionClick = if (supportedSizes.isNotEmpty()) {
                        { showResolutionPicker = true }
                    } else null,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                )

                // OSD Bottom Bar (Aspect Ratio, Zoom, Orientation, Grid, Shutter)
                MonitorBottomBar(
                    settings = settings,
                    onSelectAspectRatio = { viewModel.setAspectRatio(it) },
                    onCycleZoom = { viewModel.cycleZoom() },
                    onSelectOrientation = { viewModel.setOrientationMode(it) },
                    onToggleGrid = { viewModel.toggleSafeGrid() },
                    onOpenPictureTuning = { showPictureTuning = true },
                    onCaptureSnapshot = {
                        val hwBitmap = if (isNativeUvcActive && !state.isTestPattern) {
                            activeTextureView?.bitmap
                        } else null
                        viewModel.captureSnapshot(hwBitmap)
                    },
                    applyNavBarPadding = settings.isFullScreen,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            is VideoSignalState.DeviceDetected, is VideoSignalState.PermissionRequested, is VideoSignalState.PermissionDenied -> {
                val dev = when (state) {
                    is VideoSignalState.DeviceDetected -> state.device
                    is VideoSignalState.PermissionRequested -> state.device
                    is VideoSignalState.PermissionDenied -> state.device
                    else -> null
                }

                if (dev != null && !dismissPermissionPrompt) {
                    PermissionPromptDialog(
                        device = dev,
                        isDenied = state is VideoSignalState.PermissionDenied,
                        language = settings.language,
                        onRequestPermission = {
                            viewModel.requestUsbPermission(dev)
                        },
                        onDismiss = {
                            dismissPermissionPrompt = true
                        }
                    )
                }

                NoSignalView(
                    language = settings.language,
                    connectedUsbCount = connectedUsbCount,
                    detectedDevices = detectedDevicesSummary,
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onRefresh = {
                        dismissPermissionPrompt = false
                        viewModel.manualScanDevices()
                    },
                    onStartTestPattern = { viewModel.toggleTestPattern() },
                    onOpenAboutPrivacy = { showAboutPrivacyDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // UVC Hardware Resolution Picker Dialog
        if (showResolutionPicker && supportedSizes.isNotEmpty()) {
            ResolutionPickerDialog(
                sizes = supportedSizes,
                selectedSize = selectedSize,
                language = settings.language,
                onSelectSize = { size ->
                    viewModel.changeResolution(size)
                    showResolutionPicker = false
                },
                onDismiss = { showResolutionPicker = false }
            )
        }

        // Picture Tuning Sheet/Dialog
        if (showPictureTuning) {
            PictureAdjustmentDialog(
                settings = settings,
                onBrightnessChange = { viewModel.setBrightness(it) },
                onContrastChange = { viewModel.setContrast(it) },
                onSaturationChange = { viewModel.setSaturation(it) },
                onSafeGridToggle = { viewModel.toggleSafeGrid() },
                onResetDefaults = {
                    viewModel.setBrightness(0.0f)
                    viewModel.setContrast(1.0f)
                    viewModel.setSaturation(1.0f)
                },
                onDismiss = { showPictureTuning = false }
            )
        }

        // Version, Privacy Policy & Anti-Piracy Dialog
        if (showAboutPrivacyDialog) {
            AboutPrivacyDialog(
                language = settings.language,
                onToggleLanguage = { viewModel.toggleLanguage() },
                onDismiss = { showAboutPrivacyDialog = false }
            )
        }

        // Exit Confirmation Dialog (Yes/No)
        if (showExitDialog) {
            ExitConfirmationDialog(
                language = settings.language,
                onConfirmExit = {
                    showExitDialog = false
                    viewModel.stopAllStreams()
                    (context as? Activity)?.finish()
                },
                onDismiss = { showExitDialog = false }
            )
        }
        }

        // Non-intrusive Anchored Banner Ad (hidden in fullscreen monitor mode)
        if (!settings.isFullScreen) {
            AdMobBannerView()
        }
    }
}

@Composable
private fun ResolutionPickerDialog(
    sizes: List<Size>,
    selectedSize: Size?,
    language: com.example.model.AppLanguage,
    onSelectSize: (Size) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = getStrings(language)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MonitorSurface,
        title = {
            Column {
                Text(
                    text = strings.selectResolution,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.resolutionDesc,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sizes) { size ->
                    val isSelected = selectedSize?.width == size.width &&
                        selectedSize.height == size.height &&
                        selectedSize.type == size.type
                    val codec = when (size.type) {
                        UVCCamera.UVC_VS_FRAME_MJPEG -> "MJPEG"
                        UVCCamera.UVC_VS_FRAME_UNCOMPRESSED -> "YUYV"
                        else -> "UVC"
                    }
                    val fpsVal = if (size.fps > 0) size.fps else (size.fpsList?.maxOrNull() ?: 30)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SignalCyan.copy(alpha = 0.16f) else MonitorSurfaceElevated)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) SignalCyan else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectSize(size) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${size.width} x ${size.height}",
                            color = if (isSelected) SignalCyan else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "$codec • ${fpsVal} FPS",
                            color = Color.White.copy(alpha = 0.75f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = strings.done,
                    color = SignalCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
