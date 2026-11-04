package com.topz.karaoke.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.topz.karaoke.data.LyricsOutline
import com.topz.karaoke.data.OrientationLock
import com.topz.karaoke.data.RepeatMode
import com.topz.karaoke.data.SongSort

/** Mga dagdag na setting: itsura, tunog, queue, keypad at bubble, library, at backup. */
@Composable
fun MoreSettings(vm: KaraokeViewModel) {
    MoreTitle("Karaoke Mode (dagdag)")
    ChoiceRow(
        title = "Anino/outline ng letra",
        options = listOf(
            LyricsOutline.NONE to "Wala",
            LyricsOutline.THIN to "Manipis",
            LyricsOutline.MEDIUM to "Medium",
            LyricsOutline.THICK to "Makapal"
        ),
        selected = vm.lyricsOutline,
        onSelect = { vm.chooseLyricsOutline(it) }
    )
    Spacer(Modifier.height(8.dp))
    MoreSwitch("Ipakita ang QUEUE indicator", null, vm.showQueueIndicator) { vm.toggleShowQueueIndicator() }
    MoreSwitch("Ipakita ang maliit na pamagat sa itaas", null, vm.showTopTitle) { vm.toggleShowTopTitle() }
    MoreSwitch("Isama ang numero sa pamagat sa itaas", null, vm.topTitleNumber) { vm.toggleTopTitleNumber() }
    MoreSwitch(
        "Countdown na tuldok",
        "Lalabas ang ● ● ● ● sa huling 4 na segundo bago ang unang linya o pagkatapos ng mahabang instrumental.",
        vm.countdownDots
    ) { vm.toggleCountdownDots() }
    Spacer(Modifier.height(8.dp))
    ChoiceRow(
        title = "Orientation sa Karaoke Mode",
        options = listOf(
            OrientationLock.AUTO to "Auto",
            OrientationLock.LANDSCAPE to "Landscape",
            OrientationLock.PORTRAIT to "Portrait"
        ),
        selected = vm.orientationLock,
        onSelect = { vm.chooseOrientationLock(it) }
    )
    Spacer(Modifier.height(16.dp))

    MoreTitle("Tunog")
    Text("Lakas ng tunog (gain): ${"%.1f".format(vm.masterGainDb)} dB", color = KColors.Cream, fontWeight = FontWeight.Bold)
    Slider(
        value = vm.masterGainDb,
        onValueChange = { vm.changeMasterGain(it) },
        valueRange = -16f..4f,
        colors = SliderDefaults.colors(
            thumbColor = KColors.Gold,
            activeTrackColor = KColors.Gold,
            inactiveTrackColor = KColors.NavyLight
        )
    )
    MoreHint("Ang default ay -4.0 dB. Itaas kung mahina; ibaba kung nagkakaputol-putol ang tunog (clipping).")
    Spacer(Modifier.height(16.dp))

    MoreTitle("Queue at pagtugtog")
    ChoiceRow(
        title = "Repeat",
        options = listOf(
            RepeatMode.OFF to "Wala",
            RepeatMode.ONE to "Isang kanta",
            RepeatMode.QUEUE to "Buong queue"
        ),
        selected = vm.repeatMode,
        onSelect = { vm.chooseRepeatMode(it) }
    )
    Spacer(Modifier.height(8.dp))
    MoreSwitch(
        "Party mode",
        "Kung walang queue, magpapatugtog ng random na paborito (★).",
        vm.autoPlayFavorites
    ) { vm.toggleAutoPlayFavorites() }
    Spacer(Modifier.height(16.dp))

    MoreTitle("Keypad at bubble")
    ChoiceRow(
        title = "Awtomatikong idagdag sa queue pagkatapos ng",
        options = listOf(0 to "Off", 4 to "4 digit", 5 to "5 digit", 6 to "6 digit"),
        selected = vm.keypadAutoDigits,
        onSelect = { vm.chooseKeypadAutoDigits(it) }
    )
    Spacer(Modifier.height(8.dp))
    MoreSwitch("Pag-vibrate sa pindot ng keypad", null, vm.keypadHaptics) { vm.toggleKeypadHaptics() }
    Spacer(Modifier.height(8.dp))
    ChoiceRow(
        title = "Kusang pagdilim ng bubble kapag walang gumagalaw",
        options = listOf(0 to "Off", 5 to "5 s", 10 to "10 s", 20 to "20 s"),
        selected = vm.bubbleFadeSeconds,
        onSelect = { vm.chooseBubbleFadeSeconds(it) }
    )
    MoreHint("Magiging malabo ang saradong bubble, pero puwede pa ring pindutin.")
    Spacer(Modifier.height(16.dp))

    MoreTitle("Library")
    ChoiceRow(
        title = "Pag-uuri",
        options = listOf(
            SongSort.NUMBER to "Numero",
            SongSort.TITLE to "Pamagat",
            SongSort.ARTIST to "Artist"
        ),
        selected = vm.songSort,
        onSelect = { vm.chooseSongSort(it) }
    )
    Spacer(Modifier.height(8.dp))
    MoreSwitch("Numero na may zero sa unahan (00006)", "Patayin para 6 lang ang ipakita.", vm.padNumbers) {
        vm.togglePadNumbers()
    }
    Spacer(Modifier.height(16.dp))

    MoreTitle("Backup at restore")
    MoreHint(
        "Sine-save ang mga setting, paborito, history, at queue sa OpenKaraoke/openkaraoke-backup.zip " +
            "(internal storage). Kapaki-pakinabang kung magre-reinstall ng app."
    )
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { vm.backupNow() }) { Text("I-backup ngayon") }
        OutlinedButton(onClick = { vm.restoreBackup() }) { Text("I-restore") }
    }
    val message = vm.backupMessage
    if (message != null) {
        Spacer(Modifier.height(6.dp))
        MoreHint(message)
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun MoreTitle(text: String) {
    Text(text, color = KColors.Gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun MoreHint(text: String) {
    Text(text, color = KColors.CreamDim, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun MoreSwitch(title: String, description: String?, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = KColors.Cream, fontWeight = FontWeight.Bold)
            if (description != null) MoreHint(description)
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
