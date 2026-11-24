package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.localization.getStrings
import com.example.model.MonitorDisplaySettings
import com.example.ui.theme.MonitorSurface
import com.example.ui.theme.MonitorSurfaceElevated
import com.example.ui.theme.MonitorTextMuted
import com.example.ui.theme.MonitorTextPrimary
import com.example.ui.theme.MonitorTextSecondary
import com.example.ui.theme.SignalCyan

@Composable
fun PictureAdjustmentDialog(
    settings: MonitorDisplaySettings,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onSafeGridToggle: () -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = getStrings(settings.language)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MonitorSurface)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = strings.pictureAdjustmentsTitle,
                            color = SignalCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = strings.pictureAdjustmentsSubtitle,
                            color = MonitorTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Brightness Slider
                TuningSliderRow(
                    label = strings.brightness,
                    value = settings.brightness,
                    displayValue = String.format("%+.2f", settings.brightness),
                    range = -0.5f..0.5f,
                    onValueChange = onBrightnessChange
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Contrast Slider
                TuningSliderRow(
                    label = strings.contrast,
                    value = settings.contrast,
                    displayValue = String.format("%.2fx", settings.contrast),
                    range = 0.5f..2.0f,
                    onValueChange = onContrastChange
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Saturation Slider
                TuningSliderRow(
                    label = strings.saturation,
                    value = settings.saturation,
                    displayValue = String.format("%.2fx", settings.saturation),
                    range = 0.0f..2.0f,
                    onValueChange = onSaturationChange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Safe Area Grid Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MonitorSurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = strings.safeAreaGrid,
                            color = MonitorTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = strings.safeAreaGridDesc,
                            color = MonitorTextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Switch(
                        checked = settings.showSafeGrid,
                        onCheckedChange = { onSafeGridToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = SignalCyan,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onResetDefaults,
                        colors = ButtonDefaults.textButtonColors(contentColor = MonitorTextSecondary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = strings.resetDefaults, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SignalCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = strings.done, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TuningSliderRow(
    label: String,
    value: Float,
    displayValue: String,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = MonitorTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = displayValue,
                color = SignalCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = SignalCyan,
                activeTrackColor = SignalCyan,
                inactiveTrackColor = Color.DarkGray
            )
        )
    }
}
