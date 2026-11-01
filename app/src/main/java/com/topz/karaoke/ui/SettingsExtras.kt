package com.topz.karaoke.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.topz.karaoke.data.BubbleSide
import com.topz.karaoke.data.BubbleSize
import com.topz.karaoke.data.HighlightColor
import com.topz.karaoke.data.LyricsCase
import com.topz.karaoke.data.LyricsSize
import com.topz.karaoke.data.PerformanceMode
import kotlin.math.roundToInt

/** (code ng charset, paliwanag). "AUTO" = UTF-8; kung hindi valid, Windows-1252. */
private val ENCODINGS = listOf(
    "AUTO" to "Auto (UTF-8; kung hindi, Windows-1252)",
    "UTF-8" to "UTF-8",
    "windows-1252" to "Windows-1252 (Kanluranin, Tagalog)",
    "windows-874" to "Thai (Windows-874)",
    "Shift_JIS" to "Hapon (Shift_JIS)",
    "GBK" to "Tsino, simplified (GBK)",
    "Big5" to "Tsino, traditional (Big5)",
    "EUC-KR" to "Koreano (EUC-KR)",
    "windows-1251" to "Cyrillic (Windows-1251)"
)

/**
 * Mga dagdag na setting: itsura ng lyrics, performance, bubble, at encoding.
 * Ang lahat ng default ay ang kasalukuyang ayos ng app.
 */
@Composable
fun ExtraSettings(vm: KaraokeViewModel) {
    // ---------- Itsura ng lyrics ----------
    SectionTitle("Itsura ng lyrics (Karaoke Mode)")
    Hint("Ang mga default ay ang kasalukuyang itsura. Makikita ang epekto sa preview sa itaas.")
    Spacer(Modifier.height(8.dp))

    ChoiceRow(
        title = "Laki ng letra",
        options = listOf(
            LyricsSize.SMALL to "Maliit",
            LyricsSize.MEDIUM to "Medium",
            LyricsSize.LARGE to "Malaki",
            LyricsSize.XLARGE to "XL"
        ),
        selected = vm.lyricsSize,
        onSelect = { vm.chooseLyricsSize(it) }
    )
    Spacer(Modifier.height(8.dp))

    ColorChoiceRow(vm.highlightColor) { vm.chooseHighlightColor(it) }
    Spacer(Modifier.height(8.dp))

    ChoiceRow(
        title = "Uri ng titik",
        options = listOf(
            LyricsCase.ORIGINAL to "Orihinal",
            LyricsCase.UPPERCASE to "ABC",
            LyricsCase.TITLE to "Abc"
        ),
        selected = vm.lyricsCase,
        onSelect = { vm.chooseLyricsCase(it) }
    )
    Hint("ABC: lahat malalaking titik. Abc: karaniwan (ang unang titik lang ng salita ang malaki).")
    Spacer(Modifier.height(8.dp))

    Text("Dilim ng video: ${vm.videoDimPercent}%", color = KColors.Cream, fontWeight = FontWeight.Bold)
    Slider(
        value = vm.videoDimPercent.toFloat(),
        onValueChange = { vm.changeVideoDim(it.roundToInt()) },
        valueRange = 0f..80f,
        colors = sliderColors()
    )
    Spacer(Modifier.height(16.dp))

    // ---------- Tunog at performance ----------
    SectionTitle("Tunog at performance")
    ChoiceRow(
        title = "Performance mode",
        options = listOf(
            PerformanceMode.HIGH to "Mataas",
            PerformanceMode.MEDIUM to "Katamtaman",
            PerformanceMode.ECONOMY to "Matipid"
        ),
        selected = vm.performanceMode,
        onSelect = { vm.choosePerformanceMode(it) }
    )
    Hint(
        "Mataas: hanggang 64 sabay-sabay na note. Katamtaman: 40. Matipid: 24 at walang background video. " +
            "Piliin ang Matipid kung nauutal ang tunog."
    )
    Spacer(Modifier.height(12.dp))

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("I-mute ang melody bilang default", color = KColors.Cream, fontWeight = FontWeight.Bold)
            Hint("Para sa mga kantang wala pang naka-save na setting. Mababago mo pa rin sa bawat kanta.")
        }
        Switch(checked = vm.muteMelodyDefault, onCheckedChange = { vm.toggleMuteMelodyDefault() })
    }
    Spacer(Modifier.height(16.dp))

    // ---------- Floating bubble ----------
    SectionTitle("Floating bubble")
    ChoiceRow(
        title = "Laki",
        options = listOf(
            BubbleSize.SMALL to "Maliit",
            BubbleSize.MEDIUM to "Medium",
            BubbleSize.LARGE to "Malaki"
        ),
        selected = vm.bubbleSize,
        onSelect = { vm.chooseBubbleSize(it) }
    )
    Spacer(Modifier.height(8.dp))

    Text("Opacity: ${vm.bubbleOpacityPercent}%", color = KColors.Cream, fontWeight = FontWeight.Bold)
    Slider(
        value = vm.bubbleOpacityPercent.toFloat(),
        onValueChange = { vm.changeBubbleOpacity(it.roundToInt()) },
        valueRange = 30f..100f,
        colors = sliderColors()
    )
    ChoiceRow(
        title = "Unang puwesto",
        options = listOf(BubbleSide.RIGHT to "Kanan", BubbleSide.LEFT to "Kaliwa"),
        selected = vm.bubbleSide,
        onSelect = { vm.chooseBubbleSide(it) }
    )
    Hint("Hilahin ang bubble para ilipat kahit saan. Ang opacity ay para sa saradong bubble.")
    Spacer(Modifier.height(16.dp))

    // ---------- Encoding ng lyrics ----------
    SectionTitle("Encoding ng lyrics")
    Hint(
        "Pumili ng iba kung sira ang mga titik ng lyrics (halimbawa mga kantang Thai, Hapon, Tsino, o Koreano). " +
            "Gagana ito sa susunod na kantang bubuksan."
    )
    for ((code, label) in ENCODINGS) {
        SoundFontRow(
            name = label,
            selected = vm.lyricsEncoding == code,
            onClick = { vm.chooseLyricsEncoding(code) }
        )
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = KColors.Gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun Hint(text: String) {
    Text(text, color = KColors.CreamDim, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = KColors.Gold,
    activeTrackColor = KColors.Gold,
    inactiveTrackColor = KColors.NavyLight
)

/** Mga button na may mismong kulay ng highlight; ang napili ay may puting gilid. */
@Composable
private fun ColorChoiceRow(selected: HighlightColor, onSelect: (HighlightColor) -> Unit) {
    Text("Kulay ng highlight", color = KColors.Cream, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (color in HighlightColor.values()) {
            val isSelected = color == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.toColor())
                    .then(
                        if (isSelected) Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp)) else Modifier
                    )
                    .clickable { onSelect(color) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(color.label(), color = KColors.Navy, fontWeight = FontWeight.Bold)
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}
