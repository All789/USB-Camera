package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.getStrings
import com.example.model.AspectRatioMode
import com.example.model.MonitorDisplaySettings
import com.example.model.ScreenOrientationMode
import com.example.ui.theme.MonitorSurfaceElevated
import com.example.ui.theme.OverlayBackground
import com.example.ui.theme.SignalCyan

@Composable
fun MonitorBottomBar(
    settings: MonitorDisplaySettings,
    onSelectAspectRatio: (AspectRatioMode) -> Unit,
    onCycleZoom: () -> Unit,
    onSelectOrientation: (ScreenOrientationMode) -> Unit,
    onToggleGrid: () -> Unit,
    onOpenPictureTuning: () -> Unit,
    onCaptureSnapshot: () -> Unit,
    applyNavBarPadding: Boolean = true,
    modifier: Modifier = Modifier
) {
    val strings = getStrings(settings.language)

    AnimatedVisibility(
        visible = settings.showOsd,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(OverlayBackground)
                .then(if (applyNavBarPadding) Modifier.navigationBarsPadding() else Modifier)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left group: Aspect Ratio switchers
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AspectRatioPill(
                        label = strings.ratio169,
                        isSelected = settings.aspectRatio == AspectRatioMode.RATIO_16_9,
                        onClick = { onSelectAspectRatio(AspectRatioMode.RATIO_16_9) }
                    )
                    AspectRatioPill(
                        label = strings.ratio43,
                        isSelected = settings.aspectRatio == AspectRatioMode.RATIO_4_3,
                        onClick = { onSelectAspectRatio(AspectRatioMode.RATIO_4_3) }
                    )
                    AspectRatioPill(
                        label = strings.ratioFit,
                        isSelected = settings.aspectRatio == AspectRatioMode.FIT,
                        onClick = { onSelectAspectRatio(AspectRatioMode.FIT) }
                    )
                    AspectRatioPill(
                        label = strings.ratioFill,
                        isSelected = settings.aspectRatio == AspectRatioMode.FILL,
                        onClick = { onSelectAspectRatio(AspectRatioMode.FILL) }
                    )
                }

                // Center group: Zoom & Orientation
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zoom quick cycler
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (settings.zoomLevel > 1.0f) SignalCyan.copy(alpha = 0.25f) else MonitorSurfaceElevated)
                            .clickable(onClick = onCycleZoom)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Cycle Zoom",
                                tint = if (settings.zoomLevel > 1.0f) SignalCyan else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${settings.zoomLevel}x",
                                color = if (settings.zoomLevel > 1.0f) SignalCyan else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Rotation switcher
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MonitorSurfaceElevated)
                            .clickable {
                                val nextMode = when (settings.orientationMode) {
                                    ScreenOrientationMode.SENSOR -> ScreenOrientationMode.LANDSCAPE
                                    ScreenOrientationMode.LANDSCAPE -> ScreenOrientationMode.PORTRAIT
                                    ScreenOrientationMode.PORTRAIT -> ScreenOrientationMode.SENSOR
                                }
                                onSelectOrientation(nextMode)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Screen Rotation",
                                tint = SignalCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when (settings.orientationMode) {
                                    ScreenOrientationMode.SENSOR -> strings.rotationAuto
                                    ScreenOrientationMode.LANDSCAPE -> strings.rotationLand
                                    ScreenOrientationMode.PORTRAIT -> strings.rotationPort
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Right group: Picture Adjustments, Grid, Snapshot
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Safe Area Grid Toggle
                    IconButton(
                        onClick = onToggleGrid,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (settings.showSafeGrid) SignalCyan.copy(alpha = 0.25f) else MonitorSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = strings.safeGrid,
                            tint = if (settings.showSafeGrid) SignalCyan else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Picture Tuning (Brightness / Contrast / Saturation)
                    IconButton(
                        onClick = onOpenPictureTuning,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MonitorSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = strings.pictureTuning,
                            tint = SignalCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Snapshot Shutter Button
                    IconButton(
                        onClick = onCaptureSnapshot,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SignalCyan),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Black)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = strings.takeSnapshot,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AspectRatioPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) SignalCyan else MonitorSurfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}
