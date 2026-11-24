package com.example.ui.components

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.ui.theme.MonitorBackground
import com.example.ui.theme.SignalCyan
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdMobBannerView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bannerUnitId = remember(context) { context.getString(R.string.admob_banner_id) }
    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                adViewRef?.destroy()
            } catch (_: Throwable) {
            }
            adViewRef = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MonitorBackground)
            .navigationBarsPadding()
            .testTag("admob_banner_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = SignalCyan.copy(alpha = 0.18f)
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            val adWidthDp = maxWidth.value.toInt().coerceAtLeast(320)

            AndroidView(
                factory = { ctx ->
                    try {
                        AdView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            adUnitId = bannerUnitId
                            setAdSize(
                                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                                    ctx,
                                    adWidthDp
                                )
                            )
                            adListener = object : AdListener() {
                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    Log.d("AdMobBannerView", "Banner ad load note: ${error.message} (code=${error.code})")
                                }
                            }
                            loadAd(AdRequest.Builder().build())
                            adViewRef = this
                        }
                    } catch (e: Throwable) {
                        Log.w("AdMobBannerView", "AdView unavailable on this device: ${e.message}")
                        android.view.View(ctx)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .testTag("admob_banner_view")
            )
        }
    }
}
