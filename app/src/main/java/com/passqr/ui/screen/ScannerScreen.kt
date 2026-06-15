package com.passqr.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.passqr.ui.theme.CoralPrimary
import com.passqr.ui.theme.OnPrimary
import com.passqr.ui.theme.SurfaceDark
import com.passqr.ui.theme.SurfaceElevated
import com.passqr.util.WifiCredentialsStore
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

// ─────────────────────────────────────────────────────────────────────────────
// ScannerScreen — PROMPT-2 §1
//
// • 1:1 PreviewView with crosshair overlay
// • Auto-close camera on successful scan
// • Bottom Sheet with parsed Wi-Fi info, masked password (eye toggle),
//   and "Connect Now" button using WifiNetworkSuggestion
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen() {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        CameraScannerView()
    } else {
        PermissionRequestView { permissionLauncher.launch(Manifest.permission.CAMERA) }
    }
}

// ── Permission Request ───────────────────────────────────────────────────────

@Composable
private fun PermissionRequestView(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = CoralPrimary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Camera Access Required",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Camera permission is needed to scan\nWi-Fi QR codes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(
                containerColor = CoralPrimary,
                contentColor = OnPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Grant Permission")
        }
    }
}

// ── Camera Scanner View ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraScannerView() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val store = remember { WifiCredentialsStore(context) }

    var scanResult by remember { mutableStateOf<String?>(null) }
    var wifiInfo by remember { mutableStateOf<WifiQrData?>(null) }
    var cameraClosed by remember { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val scanned = remember { AtomicBoolean(false) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
    )

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    // Expand bottom sheet when scan succeeds
    LaunchedEffect(scanResult) {
        if (scanResult != null) {
            scope.launch { scaffoldState.bottomSheetState.expand() }
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContainerColor = SurfaceElevated,
        sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        sheetContent = {
            if (wifiInfo != null) {
                WifiBottomSheetContent(
                    data = wifiInfo!!,
                    passwordVisible = passwordVisible,
                    onTogglePassword = { passwordVisible = !passwordVisible },
                    onConnect = {
                        connectToWifi(context, wifiInfo!!)
                        scope.launch { scaffoldState.bottomSheetState.hide() }
                    },
                    onDismiss = {
                        scope.launch { scaffoldState.bottomSheetState.hide() }
                    }
                )
            } else if (scanResult != null) {
                RawQrBottomSheetContent(
                    text = scanResult!!,
                    onDismiss = {
                        scope.launch { scaffoldState.bottomSheetState.hide() }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
        ) {
            if (!cameraClosed) {
                // ── 1:1 Camera Preview with Crosshair ──
                // PROMPT-3 §1: clipToBounds prevents camera overshoot on various densities
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                // PROMPT-3 §1: FILL_CENTER ensures consistent 1:1 mapping
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { previewView ->
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                    // Only analyse if we haven't scanned yet
                                    if (!scanned.get()) {
                                        processImage(imageProxy) { raw ->
                                            if (scanned.compareAndSet(false, true)) {
                                                scanResult = raw
                                                wifiInfo = parseWifiQr(raw)
                                                cameraClosed = true
                                                // Save credentials for Generator tab
                                                val parsed = parseWifiQr(raw)
                                                if (parsed != null) {
                                                    store.save(parsed.ssid, parsed.security, parsed.password)
                                                }
                                            }
                                        }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraRef = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (_: Exception) { }
                            }, ContextCompat.getMainExecutor(context))
                        }
                    )

                    // Crosshair overlay — Industrial Brutalist (Coral)
                    CrosshairOverlay()
                }

                // Hint text below preview
                Text(
                    text = "Point camera at a Wi-Fi QR code",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            } else {
                // Camera closed — show "Scan again" state
                ScanCompleteView(
                    wifiInfo = wifiInfo,
                    scanResult = scanResult,
                    onScanAgain = {
                        scanned.set(false)
                        scanResult = null
                        wifiInfo = null
                        cameraClosed = false
                        passwordVisible = false
                    }
                )
            }
        }
    }
}

// ── Crosshair Overlay ────────────────────────────────────────────────────────

@Composable
private fun CrosshairOverlay() {
    val coral = CoralPrimary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeW = 3.dp.toPx()
        val cornerLen = size.width * 0.12f
        val inset = size.width * 0.15f
        val cornerRadius = CornerRadius(12.dp.toPx())

        // Scanning rectangle with rounded corners
        drawRoundRect(
            color = coral.copy(alpha = 0.35f),
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeW, pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f)))
        )

        // Corner brackets (top-left)
        drawLine(coral, Offset(inset, inset + cornerLen), Offset(inset, inset), strokeWidth = strokeW)
        drawLine(coral, Offset(inset, inset), Offset(inset + cornerLen, inset), strokeWidth = strokeW)
        // top-right
        drawLine(coral, Offset(size.width - inset - cornerLen, inset), Offset(size.width - inset, inset), strokeWidth = strokeW)
        drawLine(coral, Offset(size.width - inset, inset), Offset(size.width - inset, inset + cornerLen), strokeWidth = strokeW)
        // bottom-left
        drawLine(coral, Offset(inset, size.height - inset - cornerLen), Offset(inset, size.height - inset), strokeWidth = strokeW)
        drawLine(coral, Offset(inset, size.height - inset), Offset(inset + cornerLen, size.height - inset), strokeWidth = strokeW)
        // bottom-right
        drawLine(coral, Offset(size.width - inset - cornerLen, size.height - inset), Offset(size.width - inset, size.height - inset), strokeWidth = strokeW)
        drawLine(coral, Offset(size.width - inset, size.height - inset - cornerLen), Offset(size.width - inset, size.height - inset), strokeWidth = strokeW)

        // Center crosshair
        val cx = size.width / 2f
        val cy = size.height / 2f
        val crossLen = 16.dp.toPx()
        drawLine(coral, Offset(cx - crossLen, cy), Offset(cx + crossLen, cy), strokeWidth = strokeW)
        drawLine(coral, Offset(cx, cy - crossLen), Offset(cx, cy + crossLen), strokeWidth = strokeW)
    }
}

// ── Scan Complete View (after camera closed) ─────────────────────────────────

@Composable
private fun ScanCompleteView(
    wifiInfo: WifiQrData?,
    scanResult: String?,
    onScanAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = CoralPrimary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Scan Complete",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        if (wifiInfo != null) {
            Text(
                text = "Detected: ${wifiInfo.ssid}",
                style = MaterialTheme.typography.bodyLarge,
                color = CoralPrimary
            )
        } else if (scanResult != null) {
            Text(
                text = "Non-Wi-Fi QR code detected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onScanAgain,
            colors = ButtonDefaults.buttonColors(
                containerColor = CoralPrimary,
                contentColor = OnPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Scan Again")
        }
    }
}

// ── Bottom Sheet: Wi-Fi Result ───────────────────────────────────────────────

@Composable
private fun WifiBottomSheetContent(
    data: WifiQrData,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    onConnect: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(CoralPrimary.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(20.dp))

        Text(
            text = "Wi-Fi Network Found",
            style = MaterialTheme.typography.titleLarge,
            color = CoralPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        // Network info rows
        InfoRow(label = "Network", value = data.ssid)
        InfoRow(label = "Security", value = data.security)

        if (data.password.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            // Password row with eye toggle — DESIGN.md: "Secure by default"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.width(90.dp)
                )
                Text(
                    text = if (passwordVisible) data.password else "\u2022".repeat(minOf(data.password.length, 12)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = CoralPrimary
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Connect Now button — Coral, full width
        Button(
            onClick = onConnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CoralPrimary,
                contentColor = OnPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Connect Now", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))

        // Dismiss
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Dismiss")
        }
    }
}

// ── Bottom Sheet: Raw QR (non-Wi-Fi) ────────────────────────────────────────

@Composable
private fun RawQrBottomSheetContent(text: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(CoralPrimary.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(20.dp))

        Text(
            text = "QR Code Detected",
            style = MaterialTheme.typography.titleLarge,
            color = CoralPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CoralPrimary,
                contentColor = OnPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Dismiss")
        }
    }
}

// ── Info Row Helper ──────────────────────────────────────────────────────────

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ── Wi-Fi Connection via WifiNetworkSuggestion ───────────────────────────────

private fun connectToWifi(context: Context, data: WifiQrData) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val suggestionBuilder = WifiNetworkSuggestion.Builder()
            .setSsid(data.ssid)

        if (!data.security.equals("nopass", ignoreCase = true) && data.password.isNotEmpty()) {
            // WPA/WPA2 passphrase (WEP is deprecated and unsupported in WifiNetworkSuggestion)
            suggestionBuilder.setWpa2Passphrase(data.password)
        }

        val suggestion = suggestionBuilder.build()
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        val status = wifiManager.addNetworkSuggestions(listOf(suggestion))

        if (status == android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            Toast.makeText(context, "Network suggestion added. Connect via Wi-Fi settings.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Failed to add network suggestion (status=$status)", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context, "Wi-Fi auto-connect requires Android 10+", Toast.LENGTH_LONG).show()
    }
}

// ── QR Image Processing ─────────────────────────────────────────────────────

private fun processImage(imageProxy: ImageProxy, onResult: (String) -> Unit) {
    try {
        val buffer = imageProxy.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        val source = PlanarYUVLuminanceSource(
            data,
            imageProxy.width,
            imageProxy.height,
            0, 0,
            imageProxy.width,
            imageProxy.height,
            false
        )

        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader().apply {
            setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
        }

        val result = reader.decode(bitmap)
        onResult(result.text)
    } catch (_: Exception) {
        // No QR code found in this frame — expected.
    } finally {
        imageProxy.close()
    }
}

// ── Wi-Fi QR Data Model & Parser ────────────────────────────────────────────

data class WifiQrData(
    val ssid: String,
    val security: String,
    val password: String
)

fun parseWifiQr(raw: String): WifiQrData? {
    if (!raw.startsWith("WIFI:", ignoreCase = true)) return null

    val ssid = Regex("""S:([^;]*)""").find(raw)?.groupValues?.get(1) ?: return null
    val type = Regex("""T:([^;]*)""").find(raw)?.groupValues?.get(1) ?: "WPA"
    val pass = Regex("""P:([^;]*)""").find(raw)?.groupValues?.get(1) ?: ""

    return WifiQrData(
        ssid = ssid.unescapeWifi(),
        security = type,
        password = pass.unescapeWifi()
    )
}

private fun String.unescapeWifi(): String =
    this.replace("\\;", ";")
        .replace("\\:", ":")
        .replace("\\\\", "\\")
        .replace("\\,", ",")
