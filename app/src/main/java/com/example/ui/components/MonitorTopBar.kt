package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.getStrings
import com.example.model.AppLanguage
import com.example.model.MonitorDisplaySettings
import com.example.model.VideoSignalState
import com.example.ui.theme.MonitorSurfaceElevated
import com.example.ui.theme.OverlayBackground
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalRed

@Composable
fun MonitorTopBar(
    signalState: VideoSignalState,
    settings: MonitorDisplaySettings,
    audioLevel: Float,
    onToggleMute: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenAboutPrivacy: () -> Unit,
    onToggleLanguage: () -> Unit,
    onSelectResolutionClick: (() -> Unit)? = null,
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
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Signal Status, 4K Badge, Resolution & FPS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (signalState) {
                        is VideoSignalState.Streaming -> {
                            val isTest = signalState.isTestPattern
                            // Live indicator pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isTest) SignalAmber.copy(alpha = 0.2f) else SignalGreen.copy(alpha = 0.2f))
                                    .border(1.dp, if (isTest) SignalAmber else SignalGreen, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isTest) SignalAmber else SignalGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isTest) strings.testPattern else strings.liveHdmi,
                                    color = if (isTest) SignalAmber else SignalGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // 4K UHD Badge if 4K resolution active
                            if (signalState.resolution.contains("4K") || signalState.resolution.startsWith("3840")) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SignalAmber.copy(alpha = 0.25f))
                                        .border(1.dp, SignalAmber, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "4K UHD",
                                        color = SignalAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Resolution badge (tappable on live UVC to change resolution)
                            OsdBadge(
                                text = signalState.resolution,
                                onClick = if (!isTest) onSelectResolutionClick else null
                            )

                            // FPS badge
                            OsdBadge(text = "${signalState.fps} FPS", highlight = true)

                            // Low latency tag
                            OsdBadge(text = "< 40ms")
                        }
                        is VideoSignalState.Connecting -> {
                            OsdBadge(text = strings.connecting, color = SignalCyan)
                        }
                        else -> {
                            OsdBadge(text = strings.noInput, color = SignalRed)
                        }
                    }
                }

                // Center: Device Name (if available)
                if (signalState is VideoSignalState.Streaming && signalState.device != null) {
                    Text(
                        text = signalState.device.displayName,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .weight(1f, fill = false)
                    )
                }

                // Right: Audio VU, Mute, Language Switcher, Info, Fullscreen
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Audio VU Level Meter
                    AudioVuMeter(
                        level = audioLevel,
                        isMuted = settings.isMuted,
                        modifier = Modifier
                            .width(40.dp)
                            .height(14.dp)
                    )

                    // Audio Mute Toggle
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (settings.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = if (settings.isMuted) strings.unmuteAudio else strings.muteAudio,
                            tint = if (settings.isMuted) SignalRed else SignalCyan,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // In-OSD Language Switcher Pill [ বাংলা / EN ]
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MonitorSurfaceElevated)
                            .border(1.dp, SignalCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .clickable(onClick = onToggleLanguage)
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = SignalCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = settings.language.shortLabel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // About, Version & Privacy Info
                    IconButton(
                        onClick = onOpenAboutPrivacy,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = strings.versionPrivacy,
                            tint = SignalCyan,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Fullscreen Toggle
                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (settings.isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = strings.toggleFullscreen,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OsdBadge(
    text: String,
    highlight: Boolean = false,
    color: Color = Color.White,
    onClick: (() -> Unit)? = null
) {
    val baseModifier = Modifier
        .clip(RoundedCornerShape(4.dp))
        .background(MonitorSurfaceElevated)
        .let { m ->
            if (onClick != null) {
                m.border(1.dp, SignalCyan.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                    .clickable(onClick = onClick)
            } else {
                m
            }
        }
        .padding(horizontal = 6.dp, vertical = 3.dp)

    Box(modifier = baseModifier) {
        Text(
            text = text,
            color = if (highlight) SignalCyan else color.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun AudioVuMeter(
    level: Float,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 10
    val activeBars = if (isMuted) 0 else (level * barCount).toInt().coerceIn(0, barCount)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val isLit = i < activeBars
            val color = when {
                !isLit -> Color.DarkGray.copy(alpha = 0.3f)
                i >= 8 -> SignalRed
                i >= 6 -> SignalAmber
                else -> SignalGreen
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .background(color)
            )
        }
    }
}
