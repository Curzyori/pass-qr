package com.passqr.ui.screen

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.passqr.ui.component.SegmentedControl

private const val TAB_SCANNER   = 0
private const val TAB_GENERATOR = 1

@Composable
fun DashboardScreen() {
    var selectedTab by remember { mutableIntStateOf(TAB_GENERATOR) }
    val tabs = listOf("Scanner", "Generator")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        // App Title
        Text(
            text      = "PassQR",
            style     = MaterialTheme.typography.headlineLarge,
            color     = MaterialTheme.colorScheme.onBackground,
            modifier  = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text      = "Secure Wi-Fi sharing via QR codes",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier  = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        // DESIGN.md: "Top segmented control (Active: Coral background, Inactive: Transparent)"
        SegmentedControl(
            tabs          = tabs,
            selectedIndex = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        Spacer(Modifier.height(24.dp))

        // Content area with crossfade transition
        // DESIGN.md: "Smooth transition between Camera preview (Scanner) and Input form (Generator)"
        Crossfade(
            targetState   = selectedTab,
            animationSpec = tween(300),
            label         = "tabContent",
            modifier      = Modifier.fillMaxWidth()
        ) { tab ->
            when (tab) {
                TAB_SCANNER   -> ScannerPlaceholder()
                TAB_GENERATOR -> GeneratorPlaceholder()
            }
        }
    }
}

/**
 * Placeholder for the Scanner tab.
 * Will be replaced by CameraX preview + ZXing analysis in a future phase.
 */
@Composable
private fun ScannerPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text      = "Scanner",
            style     = MaterialTheme.typography.headlineMedium,
            color     = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Point your camera at a Wi-Fi QR code\nto connect instantly.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Placeholder for the Generator tab.
 * Will be replaced by the Wi-Fi input form + QR output in a future phase.
 */
@Composable
private fun GeneratorPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text      = "Generator",
            style     = MaterialTheme.typography.headlineMedium,
            color     = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Enter your Wi-Fi credentials to generate\na shareable QR code.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}
