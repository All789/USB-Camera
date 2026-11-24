package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.AspectRatioMode
import com.example.model.MonitorDisplaySettings

@Composable
fun VideoCanvasView(
    frame: Bitmap?,
    isNativeUvcActive: Boolean,
    videoWidth: Int,
    videoHeight: Int,
    settings: MonitorDisplaySettings,
    onSurfaceAttached: (Surface) -> Unit,
    onSurfaceDetached: (Surface) -> Unit,
    onFrameRendered: () -> Unit,
    onTextureViewReady: (TextureView?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Picture adjustments ColorMatrix (shared between Bitmap mode and TextureView hardware Paint)
    val colorMatrixValues = remember(settings.brightness, settings.contrast, settings.saturation) {
        ColorMatrix().apply {
            setToSaturation(settings.saturation)
            val c = settings.contrast
            val b = settings.brightness * 255f
            val cm = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, b,
                    0f, c, 0f, 0f, b,
                    0f, 0f, c, 0f, b,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            timesAssign(cm)
        }
    }

    val isDefaultColor = settings.brightness == 0f && settings.contrast == 1f && settings.saturation == 1f

    // Pan state when zoomed in
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    val currentOnSurfaceAttached by rememberUpdatedState(onSurfaceAttached)
    val currentOnSurfaceDetached by rememberUpdatedState(onSurfaceDetached)
    val currentOnFrameRendered by rememberUpdatedState(onFrameRendered)
    val currentOnTextureViewReady by rememberUpdatedState(onTextureViewReady)

    var activeSurface by remember { mutableStateOf<Surface?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            currentOnTextureViewReady(null)
            activeSurface?.let { surface ->
                currentOnSurfaceDetached(surface)
                try {
                    surface.release()
                } catch (_: Throwable) {
                }
            }
            activeSurface = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .pointerInput(settings.zoomLevel) {
                if (settings.zoomLevel > 1.0f) {
                    detectTransformGestures { _, pan, _, _ ->
                        panOffsetX += pan.x
                        panOffsetY += pan.y
                    }
                } else {
                    panOffsetX = 0f
                    panOffsetY = 0f
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val nativeRatio = if (videoWidth > 0 && videoHeight > 0) {
            videoWidth.toFloat() / videoHeight.toFloat()
        } else {
            16f / 9f
        }

        val frameModifier = when (settings.aspectRatio) {
            AspectRatioMode.RATIO_16_9 -> Modifier.aspectRatio(16f / 9f)
            AspectRatioMode.RATIO_4_3 -> Modifier.aspectRatio(4f / 3f)
            AspectRatioMode.FIT -> if (isNativeUvcActive) Modifier.aspectRatio(nativeRatio) else Modifier.fillMaxSize()
            AspectRatioMode.FILL -> Modifier.fillMaxSize()
        }

        if (isNativeUvcActive) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        keepScreenOn = true
                        isOpaque = true
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surfaceTexture: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                val surface = Surface(surfaceTexture)
                                activeSurface = surface
                                currentOnSurfaceAttached(surface)
                                currentOnTextureViewReady(this@apply)
                            }

                            override fun onSurfaceTextureSizeChanged(
                                surfaceTexture: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                // Handled automatically by hardware Surface scaling
                            }

                            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                                currentOnTextureViewReady(null)
                                activeSurface?.let { s ->
                                    currentOnSurfaceDetached(s)
                                    try {
                                        s.release()
                                    } catch (_: Throwable) {
                                    }
                                }
                                activeSurface = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                                currentOnFrameRendered()
                            }
                        }
                    }
                },
                update = { textureView ->
                    if (isDefaultColor) {
                        textureView.setLayerPaint(null)
                    } else {
                        val paint = Paint().apply {
                            colorFilter = ColorMatrixColorFilter(android.graphics.ColorMatrix(colorMatrixValues.values))
                        }
                        textureView.setLayerPaint(paint)
                    }
                },
                modifier = frameModifier
                    .graphicsLayer {
                        scaleX = settings.zoomLevel
                        scaleY = settings.zoomLevel
                        translationX = panOffsetX
                        translationY = panOffsetY
                    }
            )
        } else if (frame != null && !frame.isRecycled) {
            val contentScale = when (settings.aspectRatio) {
                AspectRatioMode.FILL -> ContentScale.FillBounds
                AspectRatioMode.FIT -> ContentScale.Fit
                AspectRatioMode.RATIO_16_9, AspectRatioMode.RATIO_4_3 -> ContentScale.FillBounds
            }

            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = "Live HDMI Video Feed",
                contentScale = contentScale,
                colorFilter = ColorFilter.colorMatrix(colorMatrixValues),
                modifier = frameModifier
                    .graphicsLayer {
                        scaleX = settings.zoomLevel
                        scaleY = settings.zoomLevel
                        translationX = panOffsetX
                        translationY = panOffsetY
                    }
            )
        }

        // Optional Safe Area Framing Grid (rule-of-thirds / center crosshairs)
        if (settings.showSafeGrid) {
            SafeGridOverlay(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun SafeGridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val gridColor = Color(0x6600E5FF)
        val strokeW = 1.5.dp.toPx()

        drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = strokeW)
        drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = strokeW)

        drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = strokeW)
        drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = strokeW)

        val cx = w / 2f
        val cy = h / 2f
        val chLen = 20.dp.toPx()
        val chColor = Color(0xAA00E5FF)
        drawLine(chColor, Offset(cx - chLen, cy), Offset(cx + chLen, cy), strokeWidth = 2.dp.toPx())
        drawLine(chColor, Offset(cx, cy - chLen), Offset(cx, cy + chLen), strokeWidth = 2.dp.toPx())

        val safeMarginX = w * 0.05f
        val safeMarginY = h * 0.05f
        drawRect(
            color = Color(0x44FFFFFF),
            topLeft = Offset(safeMarginX, safeMarginY),
            size = androidx.compose.ui.geometry.Size(w - safeMarginX * 2f, h - safeMarginY * 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
    }
}
