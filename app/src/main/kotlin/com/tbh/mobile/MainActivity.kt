package com.tbh.mobile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tbh.mobile.service.OverlayService
import com.tbh.mobile.ui.theme.TBHMobileTheme

class MainActivity : ComponentActivity() {

    // Compose-observable states backed by Activity lifecycle
    private var canDrawOverlays by mutableStateOf(false)
    private var overlayRunning by mutableStateOf(false)

    private val overlaySettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        canDrawOverlays = Settings.canDrawOverlays(this)
    }

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted — service starts regardless on API < 33 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        canDrawOverlays = Settings.canDrawOverlays(this)
        enableEdgeToEdge()
        setContent {
            TBHMobileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    OverlaySetupScreen(
                        modifier = Modifier.padding(padding),
                        canDrawOverlays = canDrawOverlays,
                        overlayRunning = overlayRunning,
                        onRequestPermission = ::requestOverlayPermission,
                        onStartOverlay = ::startOverlay,
                        onStopOverlay = ::stopOverlay
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check after user returns from system settings.
        canDrawOverlays = Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlaySettingsLauncher.launch(intent)
    }

    private fun startOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        OverlayService.start(this)
        overlayRunning = true
    }

    private fun stopOverlay() {
        OverlayService.stop(this)
        overlayRunning = false
    }
}

@Composable
private fun OverlaySetupScreen(
    modifier: Modifier = Modifier,
    canDrawOverlays: Boolean,
    overlayRunning: Boolean,
    onRequestPermission: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "TBH: Task Bar Hero",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(40.dp))

        when {
            !canDrawOverlays -> {
                Text(
                    text = "Nakładka wymaga uprawnienia\n\"Wyświetlanie nad innymi aplikacjami\"",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRequestPermission) {
                    Text("Nadaj uprawnienie nakładki")
                }
            }
            !overlayRunning -> {
                Button(onClick = onStartOverlay) {
                    Text("Uruchom nakładkę")
                }
            }
            else -> {
                Text(
                    text = "Nakładka działa — przeciągnij kwadrat po ekranie",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onStopOverlay) {
                    Text("Zatrzymaj nakładkę")
                }
            }
        }
    }
}
