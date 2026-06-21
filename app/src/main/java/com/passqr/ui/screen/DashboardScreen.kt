package com.passqr.ui.screen

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.passqr.R
import com.passqr.ui.component.SegmentedControl
import com.passqr.ui.theme.CoralPrimary
import com.passqr.util.LocaleManager

private const val TAB_WEB = 0
private const val TAB_APP = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    initialScanResult: String? = null
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable {
        mutableIntStateOf(if (initialScanResult != null) TAB_APP else TAB_WEB)
    }
    var appUrl by rememberSaveable {
        mutableStateOf<String?>(initialScanResult)
    }
    var langTick by remember { mutableIntStateOf(0) }
    val currentLang: String = remember(langTick) { LocaleManager.getLanguage(context) }
    val flagEmoji = remember(langTick) {
        if (currentLang == LocaleManager.LANG_INDONESIAN) "🇺🇸" else "🇮🇩"
    }
    var showCoffeeModal by remember { mutableStateOf(false) }

    // Sync appUrl with initialScanResult when it changes
    LaunchedEffect(initialScanResult) {
        if (initialScanResult != null) {
            appUrl = initialScanResult
            selectedTab = TAB_APP
        }
    }

    val webLabel = stringResource(R.string.tab_web)
    val appLabel = stringResource(R.string.tab_app)
    val tabs = listOf(webLabel, appLabel)

    // App mode: show WebView only after URL has been scanned
    val hasScannedUrl = appUrl != null && appUrl != ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = if (hasScannedUrl) 0.dp else 20.dp)
    ) {
        if (!hasScannedUrl) {
            // Header — hidden in App mode
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = flagEmoji,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            LocaleManager.toggle(context as Activity)
                        }
                )
            }

            Spacer(Modifier.height(20.dp))

            SegmentedControl(
                tabs = tabs,
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Spacer(Modifier.height(24.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(300),
                label = "tabContent",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    TAB_WEB -> {
                        ScannerScreen(
                            onUrlScanned = { url ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    TAB_APP -> {
                        if (hasScannedUrl) {
                            AppWebView(url = appUrl!!, modifier = Modifier.fillMaxSize(), cornerRadius = 0.dp)
                        } else {
                            ScannerScreen(
                                onUrlScanned = { url ->
                                    appUrl = url
                                }
                            )
                        }
                    }
                }
            }
        }

        if (!hasScannedUrl) {
            Spacer(Modifier.height(12.dp))

            // Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))

                    // GitHub
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Curzyori/pass-qr"))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("⭐", fontSize = 14.sp)
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = "GitHub",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoralPrimary
                        )
                    }

                    Text(
                        text = "|",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                    )

                    // Buy Coffee
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                showCoffeeModal = true
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("☕", fontSize = 14.sp)
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = stringResource(R.string.footer_buy_coffee),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoralPrimary
                        )
                    }

                    Spacer(Modifier.weight(1f))
                }

                Spacer(Modifier.height(1.dp))

                Text(
                    text = stringResource(R.string.footer_copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        }

        // Buy Coffee Bottom Sheet Modal
        if (showCoffeeModal) {
            ModalBottomSheet(
                onDismissRequest = { showCoffeeModal = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Buy Me a Coffee",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Support this project with crypto",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(Modifier.height(20.dp))

                    // EVM
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "EVM (ETH / BNB / Polygon)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "0x54e18F0345a099D9FE6dd0576bb1699733c44735",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "📋",
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clickable {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("EVM", "0x54e18F0345a099D9FE6dd0576bb1699733c44735")
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Address copied!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .padding(8.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // BTC
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BTC",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "bc1q7g5whvwjvrh7mtuap2tu7qh3tyyhvls36cp7fs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "📋",
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clickable {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("BTC", "bc1q7g5whvwjvrh7mtuap2tu7qh3tyyhvls36cp7fs")
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Address copied!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .padding(8.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Thank you for your support!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun AppWebView(url: String, modifier: Modifier = Modifier, cornerRadius: androidx.compose.ui.unit.Dp = 12.dp) {
    var webViewRef: WebView? by remember { mutableStateOf(null) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        canGoBack = view?.canGoBack() ?: false
                    }
                }
                loadUrl(url)
                webViewRef = this
            }
        },
        update = { webView ->
            webViewRef = webView
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        },
        modifier = modifier.clip(RoundedCornerShape(cornerRadius))
    )
}
