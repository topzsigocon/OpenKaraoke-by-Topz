package com.topz.karaoke.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
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

        // Mga preset: itinatakda ang reverb at echo (hindi ginagalaw ang volume)
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((name, reverb, echo) in MIC_PRESETS) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(KColors.NavyLight)
                        .clickable { vm.applyMicPreset(reverb, echo) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name, color = KColors.Cream, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
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

/** (pangalan, reverb, echo) */
private val MIC_PRESETS = listOf(
    Triple("Kuwarto", 0.25f, 0.10f),
    Triple("Bulwagan", 0.65f, 0.25f),
    Triple("Studio", 0.15f, 0.05f),
    Triple("Echo", 0.30f, 0.60f)
)
