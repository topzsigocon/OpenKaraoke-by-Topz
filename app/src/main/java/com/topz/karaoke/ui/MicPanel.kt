package com.topz.karaoke.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/** Mic echo/reverb: switch, at mga slider para sa volume, reverb at echo. */
@Composable
fun MicPanel(vm: KaraokeViewModel) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) vm.enableMic() }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KColors.NavySurface)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("🎙 Mic echo / reverb", color = KColors.Cream, fontWeight = FontWeight.Bold)
                Text(
                    "Gumamit ng headphones o wired na mic para hindi humiyaw (feedback) ang speaker.",
                    color = KColors.CreamDim,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = vm.micEnabled,
                onCheckedChange = { on ->
                    if (!on) {
                        vm.disableMic()
                    } else {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) vm.enableMic() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )
        }

        val error = vm.micError
        if (error != null) {
            Text(error, color = KColors.Cream, style = MaterialTheme.typography.bodySmall)
        }

        MicSlider("Volume", vm.micVolume) { vm.changeMicVolume(it) }
        MicSlider("Reverb", vm.micReverb) { vm.changeMicReverb(it) }
        MicSlider("Echo", vm.micEcho) { vm.changeMicEcho(it) }
    }
}

@Composable
private fun MicSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = KColors.CreamDim, modifier = Modifier.width(64.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = KColors.Gold,
                activeTrackColor = KColors.Gold,
                inactiveTrackColor = KColors.NavyLight
            )
        )
    }
}
