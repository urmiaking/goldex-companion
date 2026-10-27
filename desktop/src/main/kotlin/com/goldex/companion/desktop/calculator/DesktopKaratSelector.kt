package com.goldex.companion.desktop.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.goldex.companion.desktop.DesktopInlineCustomKaratChip
import com.goldex.companion.desktop.ui.controls.DesktopFilterChip

/** Custom mode stays selected while typing a value that happens to match a preset. */
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun DesktopKaratSelector(
    value: String,
    onValueChange: (String) -> Unit,
    presets: List<Pair<String, String>>,
    tag: String
) {
    var custom by remember { mutableStateOf(value.isNotBlank() && presets.none { it.first == value }) }
    var draft by remember { mutableStateOf(if (custom) value else "") }
    LaunchedEffect(value) {
        if (value != draft) {
            custom = value.isNotBlank() && presets.none { it.first == value }
            if (custom) draft = value
        }
    }
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { preset ->
            DesktopFilterChip(preset.second, !custom && preset.first == value,
                { custom = false; onValueChange(preset.first) }, capsule = false,
                modifier = Modifier.testTag("$tag-${preset.first}"))
        }
        DesktopInlineCustomKaratChip(
            value = if (custom) draft else "",
            onValueChange = { custom = true; draft = it; onValueChange(it) },
            isSelected = custom,
            onSelect = { if (!custom) { custom = true; onValueChange(draft) } },
            modifier = Modifier.width(120.dp).height(48.dp),
            inputTag = if (tag == "karat") "input-KARAT" else "$tag-field"
        )
    }
}
