package com.passqr.ui.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.passqr.ui.theme.CoralPrimary
import com.passqr.ui.theme.OnPrimary
import com.passqr.ui.theme.SurfaceElevated
import com.passqr.util.WifiCredentialsStore
import com.passqr.util.getCurrentWifiInfo
import com.passqr.util.rememberWifiConnected

// ─────────────────────────────────────────────────────────────────────────────
// GeneratorScreen — PROMPT-2 §2
//
// • No manual input form — credentials auto-retrieved from EncryptedSharedPrefs
// • ConnectivityManager gate: empty state with "Connect to Wi-Fi" deep-link
// • QR code with ErrorCorrectionLevel.H + app logo overlay in center
// • Masked password display with eye toggle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GeneratorScreen() {
    val context = LocalContext.current
    val store = remember { WifiCredentialsStore(context) }
    val wifiConnected by rememberWifiConnected()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    // PROMPT-3 §2: Derive a reactive key from wifiConnected so the
    // credential-read branch re-evaluates when connectivity flips
    // from "No Wi-Fi" to "Connected" without requiring a restart.
    val credsKey = wifiConnected

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        when {
            // ── Not connected: empty state with deep-link to Wi-Fi settings ──
            !wifiConnected -> WifiDisconnectedEmptyState(context)

            // ── Connected: try store first, then auto-detect from system ──
            else -> {
                // credsKey forces re-read when wifiConnected toggles
                val storeSsid = store.getSsid()
                val currentWifi = remember(credsKey) { getCurrentWifiInfo(context) }

                // Determine credentials to use:
                // 1. If store has credentials matching current SSID → use stored (has password)
                // 2. If store has credentials for different SSID → use stored as fallback
                // 3. No stored credentials → auto-detect SSID/security from system (no password)
                val ssid: String
                val security: String
                val password: String

                when {
                    // Store has credentials for the currently connected network
                    storeSsid != null && currentWifi != null && storeSsid == currentWifi.ssid -> {
                        ssid = storeSsid
                        security = store.getSecurity()
                        password = store.getPassword()
                    }
                    // Store has credentials (possibly different SSID) — still use them
                    storeSsid != null -> {
                        ssid = storeSsid
                        security = store.getSecurity()
                        password = store.getPassword()
                    }
                    // No stored creds — auto-detect from system WiFi connection
                    currentWifi != null -> {
                        ssid = currentWifi.ssid
                        security = currentWifi.security
                        password = ""
                    }
                    // Connected but couldn't get WiFi info (very rare)
                    else -> {
                        NoCredentialsState()
                        return@Column
                    }
                }

                val qrBitmap = remember(ssid, security, password, credsKey) {
                    generateWifiQrBitmapWithLogo(context, ssid, security, password)
                }

                QrDisplayCard(
                    ssid = ssid,
                    password = password,
                    passwordVisible = passwordVisible,
                    onTogglePassword = { passwordVisible = !passwordVisible },
                    qrBitmap = qrBitmap,
                    isAutoDetected = storeSsid == null
                )
            }
        }
    }
}

// ── Empty State: No Wi-Fi ────────────────────────────────────────────────────

@Composable
private fun WifiDisconnectedEmptyState(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = null,
            tint = CoralPrimary.copy(alpha = 0.6f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "No Wi-Fi Connection",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Connect to a Wi-Fi network first.\nYour credentials will be used to\ngenerate a shareable QR code.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = CoralPrimary,
                contentColor = OnPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Open Wi-Fi Settings", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ── Empty State: No Saved Credentials ────────────────────────────────────────

@Composable
private fun NoCredentialsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Wifi,
            contentDescription = null,
            tint = CoralPrimary.copy(alpha = 0.6f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "No Saved Credentials",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Scan a Wi-Fi QR code first to save\ncredentials, then come back here\nto regenerate the code.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

// ── QR Display Card ──────────────────────────────────────────────────────────

@Composable
private fun QrDisplayCard(
    ssid: String,
    password: String,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    qrBitmap: Bitmap?,
    isAutoDetected: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isAutoDetected) "Auto-detected network:" else "Scan to connect to:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = ssid,
                style = MaterialTheme.typography.headlineMedium,
                color = CoralPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(20.dp))

            // QR Code with logo overlay
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Wi-Fi QR code for $ssid",
                    modifier = Modifier.size(240.dp)
                )
            } else {
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "QR generation failed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Masked password with eye toggle — DESIGN.md: "Secure by default"
            if (password.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Password:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (passwordVisible) password else "\u2022".repeat(minOf(password.length, 16)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Show password" else "Hide password",
                            tint = CoralPrimary
                        )
                    }
                }
            } else if (isAutoDetected) {
                // No password available from system — show hint
                Text(
                    text = "Password not available from system.\nScan a QR code to save credentials.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Point another device's camera at this code",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── QR Generation with ErrorCorrectionLevel.H + Logo Overlay ─────────────────

/**
 * Generates a QR [Bitmap] encoding the Wi-Fi connection string.
 * Uses ErrorCorrectionLevel.H to allow a logo overlay in the center.
 * Draws the app launcher icon on top of the QR code.
 *
 * Format: WIFI:S:<SSID>;T:<TYPE>;P:<PASSWORD>;;
 */
private fun generateWifiQrBitmapWithLogo(
    context: Context,
    ssid: String,
    security: String,
    password: String,
    size: Int = 512
): Bitmap? {
    val wifiType = when {
        security.startsWith("WEP", ignoreCase = true) -> "WEP"
        security.startsWith("nopass", ignoreCase = true) ||
            security.startsWith("None", ignoreCase = true) -> "nopass"
        else -> "WPA"
    }

    val payload = buildString {
        append("WIFI:")
        append("S:${ssid.escapeWifi()};")
        append("T:$wifiType;")
        if (password.isNotEmpty()) append("P:${password.escapeWifi()};")
        append(";")
    }

    return try {
        val writer = QRCodeWriter()
        val hints = mapOf(
            EncodeHintType.MARGIN to 2,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H
        )
        val bitMatrix = writer.encode(payload, BarcodeFormat.QR_CODE, size, size, hints)

        // Render QR to bitmap
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val dark = AndroidColor.BLACK
        val light = AndroidColor.WHITE

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) dark else light)
            }
        }

        // Draw app logo in center — PROMPT-2 §3
        val logoBitmap = loadLogoBitmap(context, size / 5)
        if (logoBitmap != null) {
            val canvas = AndroidCanvas(bitmap)
            val logoSize = size / 5
            val logoX = (size - logoSize) / 2f
            val logoY = (size - logoSize) / 2f

            // White background circle behind logo for contrast
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(size / 2f, size / 2f, logoSize * 0.6f, bgPaint)

            // Draw logo
            val destRect = Rect(
                logoX.toInt(), logoY.toInt(),
                (logoX + logoSize).toInt(), (logoY + logoSize).toInt()
            )
            // PROMPT-3 §3: Use FILTER_BITMAP_FLAG for smooth logo rendering
            val logoPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(logoBitmap, null, destRect, logoPaint)
        }

        bitmap
    } catch (_: Exception) {
        null
    }
}

/**
 * Load the app's launcher icon as a bitmap for QR overlay.
 * PROMPT-3 §3: Uses createScaledBitmap with filter=true for cleaner
 * scaling on high-density displays (xhdpi/xxhdpi).
 */
private fun loadLogoBitmap(context: Context, targetSize: Int): Bitmap? {
    return try {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        // Render drawable at a higher resolution for density-crisp scaling
        val oversample = targetSize * 2
        val rawBitmap = Bitmap.createBitmap(oversample, oversample, Bitmap.Config.ARGB_8888)
        val rawCanvas = AndroidCanvas(rawBitmap)
        drawable.setBounds(0, 0, rawCanvas.width, rawCanvas.height)
        drawable.draw(rawCanvas)
        // Scale down with bilinear filtering for smooth results
        Bitmap.createScaledBitmap(rawBitmap, targetSize, targetSize, true)
    } catch (_: Exception) {
        null
    }
}

/** Escape special characters for Wi-Fi QR format. */
private fun String.escapeWifi(): String =
    this.replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(":", "\\:")
        .replace(",", "\\,")
