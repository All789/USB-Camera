package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppLanguage
import com.example.ui.theme.MonitorBackground
import com.example.ui.theme.MonitorSurfaceElevated
import com.example.ui.theme.MonitorTextPrimary
import com.example.ui.theme.MonitorTextSecondary
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashIntroOverlay(
    language: AppLanguage,
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logoScale = remember { Animatable(0.5f) }
    val logoAlpha = remember { Animatable(0f) }
    val progressAnim = remember { Animatable(0f) }
    val beamWidthAnim = remember { Animatable(0f) }

    var showTitle by remember { mutableStateOf(false) }
    var showSubtitle by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_glow")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(320))
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 4850, easing = FastOutSlowInEasing)
            )
        }

        delay(220)
        showTitle = true
        launch {
            beamWidthAnim.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
        }
        delay(220)
        showSubtitle = true

        delay(4560)
        onAnimationFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0A1D42),
                        MonitorBackground,
                        Color(0xFF030508)
                    )
                )
            )
            .safeDrawingPadding()
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            // Animated Logo with Scanning Halo Ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(196.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(glowPulse)
                        .alpha(logoAlpha.value)
                ) {
                    val radius = size.minDimension / 2f
                    // Soft cyan-blue aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SignalCyan.copy(alpha = 0.28f),
                                Color(0xFF005CE6).copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        ),
                        radius = radius
                    )
                    // Outer subtle ring
                    drawCircle(
                        color = SignalCyan.copy(alpha = 0.2f),
                        radius = radius - 10.dp.toPx(),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Rotating scanner arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                SignalCyan,
                                SignalAmber,
                                Color.Transparent
                            )
                        ),
                        startAngle = ringRotation,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(10.dp.toPx(), 10.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(
                            size.width - 20.dp.toPx(),
                            size.height - 20.dp.toPx()
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.app_brand_logo),
                    contentDescription = stringResource(id = R.string.app_name),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(144.dp)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                        .clip(RoundedCornerShape(28.dp))
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Staggered Software Name Reveal
            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn(tween(400)) + slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetY = { it / 2 }
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        color = MonitorTextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Expanding luminous beam underline
                    Box(
                        modifier = Modifier
                            .width(180.dp * beamWidthAnim.value.coerceAtLeast(0.01f))
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        SignalCyan,
                                        SignalAmber,
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedVisibility(
                visible = showSubtitle,
                enter = fadeIn(tween(420)) + slideInVertically(
                    animationSpec = tween(360),
                    initialOffsetY = { it / 3 }
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MonitorSurfaceElevated.copy(alpha = 0.85f))
                            .border(1.dp, SignalCyan.copy(alpha = 0.35f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.BENGALI) {
                                "পোর্টেবল ৪K / FHD HDMI মনিটর • USB-C OTG"
                            } else {
                                "PORTABLE 4K / FHD HDMI MONITOR • USB-C OTG"
                            },
                            color = SignalCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Bottom initialization progress track
                    Box(
                        modifier = Modifier
                            .width(190.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressAnim.value)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF005CE6), SignalCyan, SignalAmber)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (language == AppLanguage.BENGALI) {
                            "ইউভিসি ভিডিও ইঞ্জিন প্রস্তুত হচ্ছে..."
                        } else {
                            "INITIALIZING UVC VIDEO ENGINE..."
                        },
                        color = MonitorTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        // Footer Developer Credit at the bottom of the Intro
        AnimatedVisibility(
            visible = showSubtitle,
            enter = fadeIn(tween(500)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Develop by ISMAIL",
                color = SignalAmber,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.4.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
