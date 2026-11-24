package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.getStrings
import com.example.model.AppLanguage
import com.example.ui.theme.MonitorBackground
import com.example.ui.theme.MonitorSurface
import com.example.ui.theme.MonitorSurfaceElevated
import com.example.ui.theme.MonitorTextMuted
import com.example.ui.theme.MonitorTextPrimary
import com.example.ui.theme.MonitorTextSecondary
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan

@Composable
fun NoSignalView(
    language: AppLanguage,
    connectedUsbCount: Int = 0,
    detectedDevices: List<String> = emptyList(),
    onToggleLanguage: () -> Unit,
    onRefresh: () -> Unit,
    onStartTestPattern: () -> Unit,
    onOpenAboutPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = getStrings(language)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MonitorBackground)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar Row with App Badge & Language Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High-Tech Header Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MonitorSurfaceElevated)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = SignalCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = strings.appTitle,
                        color = SignalCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp
                    )
                }

                // In-App Language Switcher Toggle Pill [ বাংলা | EN ]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MonitorSurfaceElevated)
                        .border(1.dp, SignalCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .clickable(onClick = onToggleLanguage)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = SignalCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == AppLanguage.BENGALI) "বাংলা (BN)" else "English (EN)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Pulse Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (connectedUsbCount > 0) Color(0xFF00E676).copy(alpha = 0.15f)
                        else SignalAmber.copy(alpha = 0.15f)
                    )
                    .border(
                        1.dp,
                        if (connectedUsbCount > 0) Color(0xFF00E676).copy(alpha = pulseAlpha)
                        else SignalAmber.copy(alpha = pulseAlpha),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (connectedUsbCount > 0) Color(0xFF00E676) else SignalAmber)
                        .alpha(pulseAlpha)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (connectedUsbCount > 0) {
                        if (language == AppLanguage.BENGALI) "ইউএসবি ডিভাইস শনাক্ত হয়েছে ($connectedUsbCount টি)"
                        else "USB DEVICE DETECTED ($connectedUsbCount CONNECTED)"
                    } else {
                        strings.noSignalStatus
                    },
                    color = if (connectedUsbCount > 0) Color(0xFF00E676) else SignalAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }

            // If devices detected on bus, show device list card with Direct Connect button!
            if (detectedDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MonitorSurfaceElevated)
                        .border(1.dp, SignalCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (language == AppLanguage.BENGALI) "সংযুক্ত ডিভাইস:" else "Connected Hardware:",
                            color = SignalCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (language == AppLanguage.BENGALI) "পারমিশন নিন" else "Request Permission",
                            color = Color(0xFFFFB300),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(onClick = onRefresh)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    detectedDevices.forEach { devString ->
                        Text(
                            text = "• $devString",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Connection Workflow Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MonitorSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.connectionWorkflow,
                        color = MonitorTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4-Step Diagram
                    WorkflowStepRow(
                        icon = Icons.Default.Videocam,
                        stepNumber = "1",
                        title = strings.step1Title,
                        subtitle = strings.step1Desc
                    )

                    WorkflowConnector()

                    WorkflowStepRow(
                        icon = Icons.Default.Usb,
                        stepNumber = "2",
                        title = strings.step2Title,
                        subtitle = strings.step2Desc
                    )

                    WorkflowConnector()

                    WorkflowStepRow(
                        icon = Icons.Default.Cable,
                        stepNumber = "3",
                        title = strings.step3Title,
                        subtitle = strings.step3Desc
                    )

                    WorkflowConnector()

                    WorkflowStepRow(
                        icon = Icons.Default.Tv,
                        stepNumber = "4",
                        title = strings.step4Title,
                        subtitle = strings.step4Desc
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Actions: Scan USB or Launch 4K/FHD Test Signal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRefresh,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.scanUsb, fontSize = 13.sp)
                }

                Button(
                    onClick = onStartTestPattern,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SignalCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.testSignal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // App Info, Version & Privacy Policy Button
            TextButton(
                onClick = onOpenAboutPrivacy,
                colors = ButtonDefaults.textButtonColors(contentColor = SignalCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.aboutPrivacyBtn,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.standbyHint,
                color = MonitorTextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WorkflowStepRow(
    icon: ImageVector,
    stepNumber: String,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MonitorSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SignalCyan,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$stepNumber. $title",
                color = MonitorTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = MonitorTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun WorkflowConnector() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .background(SignalCyan.copy(alpha = 0.4f))
        )
    }
}
