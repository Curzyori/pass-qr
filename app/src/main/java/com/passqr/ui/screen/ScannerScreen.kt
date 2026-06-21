package com.passqr.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.passqr.R
import com.passqr.ui.theme.CoralPrimary
import com.passqr.ui.theme.OnPrimary
import com.shouzhong.scanner.Callback
import com.shouzhong.scanner.DefaultViewFinder
import com.shouzhong.scanner.ScannerView
import java.net.URLEncoder

@Composable
fun ScannerScreen(
    onUrlScanned: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    BackHandler(onBack = onNavigateBack)
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(
                context,
                context.getString(R.string.permission_denied_hint),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        CameraScannerView(onUrlScanned = onUrlScanned)
    } else {
        PermissionRequestView { permissionLauncher.launch(Manifest.permission.CAMERA) }
    }
}

@Composable
private fun PermissionRequestView(onRequest: () -> Unit) {
    val context = LocalContext.current
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
            text = stringResource(R.string.camera_permission_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.camera_permission_body),
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
            Text(stringResource(R.string.grant_permission))
        }
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        ) {
            Text(
                text = stringResource(R.string.open_settings),
                color = CoralPrimary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun CameraScannerView(onUrlScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var activeScannerView by remember { mutableStateOf<ScannerView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                activeScannerView?.onResume()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                activeScannerView?.onPause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activeScannerView?.onPause()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    ScannerView(ctx).apply {
                        setViewFinder(DefaultViewFinder(ctx))
                        setEnableZXing(true)
                        setSaveBmp(false)
                        setRotateDegree90Recognition(true)
                        setCallback(object : Callback {
                            override fun result(res: com.shouzhong.scanner.Result?) {
                                if (res != null && res.data != null) {
                                    val raw = res.data
                                    val isUrl = raw.startsWith("http://", ignoreCase = true) ||
                                        raw.startsWith("https://", ignoreCase = true) ||
                                        raw.startsWith("www.", ignoreCase = true)
                                    val displayUrl = if (isUrl) {
                                        if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw
                                    } else {
                                        "https://www.google.com/search?q=" + URLEncoder.encode(raw, "UTF-8")
                                    }
                                    onUrlScanned(displayUrl)
                                    restartPreviewAfterDelay(2000)
                                }
                            }
                        })
                        activeScannerView = this
                        onResume()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.scanner_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
