package com.goldex.companion.desktop.ui.controls

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.*

/** Windows presentation contract. Android tokens and financial state remain owned by their hosts. */
internal object DesktopControls {
    val BodyShape = ButtonShape
    val CapsuleShape = RoundedCornerShape(24.dp)
    val Height = 48.dp
    val LabelSize = 14.sp
    val IconSize = 20.dp
    // Android CustomerLedgerScreen.LedgerFilterCapsuleItem selected-state palette.
    val FilterSelectedBackground = Color(0xFF1E232E)
    val FilterSelectedBorder = Color(0x66F59E0B)
    val FilterSelectedText = Color(0xFFFCD34D)
}

internal val LocalDesktopReduceMotion = staticCompositionLocalOf { false }

/** An icon is mandatory at the call site; geometry cannot be overridden by a feature. */
@Composable internal fun DesktopActionButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null,
    trailingIcon: ImageVector? = null, isSecondary: Boolean = false, enabled: Boolean = true,
    isLoading: Boolean = false, height: Dp = DesktopControls.Height
) = ActionButton(text, onClick, modifier, icon, trailingIcon, isSecondary, enabled, isLoading,
    height.coerceAtLeast(44.dp), DesktopControls.BodyShape)

/** Only the workspace header and navigation footer may use this capsule variant. */
@Composable internal fun DesktopHeaderButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null,
    isSecondary: Boolean = false, enabled: Boolean = true
) = ActionButton(text, onClick, modifier, icon, null, isSecondary, enabled, false,
    DesktopControls.Height, DesktopControls.CapsuleShape)

@Composable private fun ActionButton(
    text: String, onClick: () -> Unit, modifier: Modifier, icon: ImageVector?, trailingIcon: ImageVector?,
    secondary: Boolean, enabled: Boolean, loading: Boolean, height: Dp, shape: Shape
) {
    val c = LocalGoldExColors.current
    val active = enabled && !loading && LocalDesktopDialogInputEnabled.current
    val foreground = if (!enabled) c.textMuted else if (secondary) c.textMain else c.goldButtonText
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val hovered by interactions.collectIsHoveredAsState()
    val pressed by interactions.collectIsPressedAsState()
    Surface(onClick, modifier.height(height), enabled = active, shape = shape,
        color = if (secondary || !enabled) c.surfaceElevated else Color.Transparent, contentColor = foreground,
        border = if (focused) BorderStroke(2.dp, c.goldPrimary) else if (secondary || !enabled) c.hairlineBorder else null,
        shadowElevation = when { secondary || !enabled -> 0.dp; pressed -> .5.dp; hovered -> 2.dp; else -> 1.5.dp },
        interactionSource = interactions) {
        Row(Modifier.fillMaxHeight().then(if (!secondary && enabled) Modifier.background(c.goldButtonGradient) else Modifier)
            .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            if (loading) CircularProgressIndicator(Modifier.size(DesktopControls.IconSize), color = foreground, strokeWidth = 2.dp)
            else icon?.let { Icon(it, null, Modifier.size(DesktopControls.IconSize).testTag("desktop-action-icon"), tint = if (!enabled) c.textMuted else if (secondary) c.goldPrimary else foreground) }
            Text(text, fontFamily = VazirmatnFamily, fontSize = DesktopControls.LabelSize,
                fontWeight = FontWeight.SemiBold, color = foreground, maxLines = 1, softWrap = false,
                style = TextStyle(fontFeatureSettings = VazirmatnFeatureSettings))
            trailingIcon?.let { Icon(it, null, Modifier.size(DesktopControls.IconSize).testTag("desktop-action-icon"), tint = if (!enabled) c.textMuted else if (secondary) c.goldPrimary else foreground) }
        }
    }
}

/** Compact text navigation shared by both dashboard card links. */
@Composable internal fun DesktopNavigationLink(text: String, onClick: () -> Unit, icon: ImageVector, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = LocalGoldExColors.current
    val tint = if (enabled) c.goldPrimary else c.textMuted
    TextButton(onClick, modifier, enabled = enabled && LocalDesktopDialogInputEnabled.current, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text, Modifier.testTag("desktop-navigation-label"), color = tint, fontFamily = VazirmatnFamily, fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1)
            Icon(icon, null, Modifier.size(16.dp).testTag("desktop-navigation-icon"), tint = tint)
        }
    }
}

@Composable internal fun DesktopDropdownControl(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    icon: ImageVector? = null) {
    val c = LocalGoldExColors.current
    OutlinedButton(onClick, modifier.height(DesktopControls.Height), shape = DesktopControls.BodyShape,
        border = c.hairlineBorder, contentPadding = PaddingValues(horizontal = 12.dp)) {
        icon?.let { Icon(it, null, Modifier.size(20.dp), tint = c.goldPrimary); Spacer(Modifier.width(6.dp)) }
        Text(text, Modifier.weight(1f), fontSize = 14.sp, color = c.textMain, fontFamily = VazirmatnFamily,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.Outlined.ExpandMore, null, Modifier.size(20.dp), tint = c.goldPrimary)
    }
}

@Composable internal fun DesktopIconButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val c = LocalGoldExColors.current
    val focused by interactionSource.collectIsFocusedAsState()
    // Material owns the circular state layer. A second background would draw a square hover behind it.
    IconButton(onClick, modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp).clip(CircleShape)
        .border(if (focused) 1.dp else 0.dp, if (focused) c.goldPrimary else Color.Transparent, CircleShape),
        enabled && LocalDesktopDialogInputEnabled.current, colors, interactionSource, content)
}

/** Approved compact capsule for opening a quote's detail; not a save/delete action variant. */
@Composable internal fun DesktopRateDetailsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalGoldExColors.current
    Surface(onClick, modifier.height(32.dp), shape = DesktopControls.CapsuleShape,
        color = c.surfaceElevated, border = c.hairlineBorder) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("جزئیات", Modifier.testTag("rate-details-label"), fontFamily = VazirmatnFamily,
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.textMain, maxLines = 1)
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(16.dp).testTag("rate-details-arrow"),
                tint = c.goldPrimary)
        }
    }
}

/** Independent choices derived from Android's customer LedgerFilterCapsuleItem; no sliding track. */
@Composable internal fun DesktopFilterChip(
    text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    count: Int? = null, countColor: Color? = null, enabled: Boolean = true, capsule: Boolean = true
) {
    val c = LocalGoldExColors.current
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val hovered by interactions.collectIsHoveredAsState()
    val duration = if (LocalDesktopReduceMotion.current) 0 else 200
    val bg by animateColorAsState(if (selected) DesktopControls.FilterSelectedBackground else if (hovered) c.surfaceElevated else c.surface,
        tween(duration, easing = LuxuryMotion.StandardEasing), label = "desktop-filter-background")
    val border by animateColorAsState(if (focused) c.goldPrimary else if (selected) DesktopControls.FilterSelectedBorder else c.border,
        tween(duration, easing = LuxuryMotion.StandardEasing), label = "desktop-filter-border")
    val fg by animateColorAsState(if (selected) DesktopControls.FilterSelectedText else c.textSecondary,
        tween(duration, easing = LuxuryMotion.StandardEasing), label = "desktop-filter-text")
    val shape = if (capsule) DesktopControls.CapsuleShape else DesktopControls.BodyShape
    Row(modifier.height(DesktopControls.Height).clip(shape).background(bg)
        .border(if (focused) 2.dp else 1.dp, border, shape)
        .hoverable(interactions).selectable(selected, enabled = enabled, role = Role.Tab,
            interactionSource = interactions, indication = null, onClick = onClick)
        .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text, fontFamily = VazirmatnFamily, fontSize = DesktopControls.LabelSize,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (enabled) fg else c.textMuted, maxLines = 1, softWrap = false)
        count?.let {
            val tint = countColor ?: if (selected) c.goldSecondary else c.textMuted
            Surface(shape = RoundedCornerShape(6.dp), color = tint.copy(alpha = .12f)) {
                Text(PersianNumberFormatter.toPersianDigits(it.toString()), Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontFamily = VazirmatnFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint, maxLines = 1)
            }
        }
    }
}

@Composable internal fun <T> DesktopFilterRow(
    items: List<T>, selectedItem: T, onItemSelected: (T) -> Unit, label: (T) -> String,
    modifier: Modifier = Modifier, count: ((T) -> Int)? = null, countColor: ((T) -> Color)? = null,
    isItemEnabled: (T) -> Boolean = { true }, fillWidth: Boolean = true
) = ChoiceRow(items, selectedItem, onItemSelected, label, modifier, count, countColor, isItemEnabled, true, fillWidth)

/** Form choices share typography/states with filters but retain the normal 12dp body curvature. */
@Composable internal fun <T> DesktopChoiceRow(
    items: List<T>, selectedItem: T, onItemSelected: (T) -> Unit, label: (T) -> String,
    modifier: Modifier = Modifier, isItemEnabled: (T) -> Boolean = { true }
) = ChoiceRow(items, selectedItem, onItemSelected, label, modifier, null, null, isItemEnabled, false)

/** Narrow form columns wrap choices rather than reducing text or hiding a custom option. */
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun <T> DesktopChoiceFlow(
    items: List<T>, selectedItem: T, onItemSelected: (T) -> Unit, label: (T) -> String,
    modifier: Modifier = Modifier, enabled: Boolean = true
) {
    FlowRow(modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item -> DesktopFilterChip(label(item), item == selectedItem, { onItemSelected(item) },
            enabled = enabled, capsule = false) }
    }
}

@Composable private fun <T> ChoiceRow(
    items: List<T>, selectedItem: T, onItemSelected: (T) -> Unit, label: (T) -> String,
    modifier: Modifier, count: ((T) -> Int)?, countColor: ((T) -> Color)?, isItemEnabled: (T) -> Boolean,
    capsule: Boolean, fillWidth: Boolean = true
) {
    val scroll = rememberScrollState()
    val widths = remember(items) { mutableStateMapOf<T, Int>() }
    var viewport by remember { mutableStateOf(0) }
    val gap = with(LocalDensity.current) { 8.dp.roundToPx() }
    val reduceMotion = LocalDesktopReduceMotion.current
    LaunchedEffect(selectedItem, widths.toMap(), viewport) {
        val index = items.indexOf(selectedItem)
        if (index >= 0 && viewport > 0 && items.all { widths.containsKey(it) }) {
            val start = items.take(index).sumOf { widths.getValue(it) } + gap * index
            val end = start + widths.getValue(selectedItem)
            val target = when {
                start < scroll.value -> start
                end > scroll.value + viewport -> end - viewport
                else -> scroll.value
            }.coerceIn(0, scroll.maxValue)
            if (reduceMotion) scroll.scrollTo(target) else scroll.animateScrollTo(target)
        }
    }
    Box(modifier.height(DesktopControls.Height).onSizeChanged { viewport = it.width }) {
        Row(Modifier.then(if (fillWidth) Modifier.fillMaxWidth() else Modifier).horizontalScroll(scroll).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            items.forEach { item -> DesktopFilterChip(label(item), item == selectedItem, { onItemSelected(item) },
                modifier = Modifier.onSizeChanged { widths[item] = it.width },
                count = count?.invoke(item), countColor = countColor?.invoke(item), enabled = isItemEnabled(item), capsule = capsule) }
        }
        if (scroll.maxValue > 0) HorizontalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp))
    }
}

/** Standard desktop text field colors with prominent gold focus and subtle gold outline in dark mode. */
@Composable internal fun desktopTextFieldColors(
    colors: GoldExAppColors = LocalGoldExColors.current,
    focusedBorderColor: Color = if (colors.isDark) colors.goldSecondary else colors.goldPrimary,
    unfocusedBorderColor: Color = if (colors.isDark) Color(0x52D4AF37) else colors.border,
    focusedContainerColor: Color = colors.surfaceElevated,
    unfocusedContainerColor: Color = colors.surface,
    errorBorderColor: Color = colors.errorRed,
    errorLabelColor: Color = colors.errorRed,
    cursorColor: Color = colors.goldPrimary,
    focusedLabelColor: Color = if (colors.isDark) colors.goldSecondary else colors.goldPrimary,
    unfocusedLabelColor: Color = colors.textMuted,
    focusedTextColor: Color = colors.textMain,
    unfocusedTextColor: Color = colors.textMain,
    disabledBorderColor: Color = colors.border,
    disabledTextColor: Color = colors.textMuted
): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = focusedBorderColor,
    unfocusedBorderColor = unfocusedBorderColor,
    focusedContainerColor = focusedContainerColor,
    unfocusedContainerColor = unfocusedContainerColor,
    errorBorderColor = errorBorderColor,
    errorLabelColor = errorLabelColor,
    cursorColor = cursorColor,
    focusedLabelColor = focusedLabelColor,
    unfocusedLabelColor = unfocusedLabelColor,
    focusedTextColor = focusedTextColor,
    unfocusedTextColor = unfocusedTextColor,
    disabledBorderColor = disabledBorderColor,
    disabledTextColor = disabledTextColor
)

/** All Windows outlined inputs use the same 12dp shape. Floating labels stay transparent. */
@Composable internal fun DesktopOutlinedTextField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, readOnly: Boolean = false, textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null, placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null, trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null, suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null, isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false, maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE, minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TextFieldColors = desktopTextFieldColors()
) {
    com.goldex.companion.ui.components.GoldOutlinedTextField(value, onValueChange, modifier, enabled, readOnly,
        textStyle.copy(fontFamily = VazirmatnFamily, fontFeatureSettings = VazirmatnFeatureSettings, fontSize = 15.sp),
        label, placeholder, leadingIcon, trailingIcon, prefix, suffix, supportingText, isError,
        visualTransformation, keyboardOptions, keyboardActions, singleLine, maxLines, minLines, interactionSource,
        shape = DesktopControls.BodyShape, colors = colors)
}

@Composable internal fun DesktopSearchField(
    value: String, onValueChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier,
    inputTag: String? = null
) {
    val c = LocalGoldExColors.current
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Surface(modifier.height(DesktopControls.Height), shape = DesktopControls.BodyShape, color = c.surface,
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) (if (c.isDark) c.goldSecondary else c.goldPrimary) else if (c.isDark) Color(0x52D4AF37) else c.border)) {
        Row(Modifier.padding(start = 12.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Search, null, Modifier.size(DesktopControls.IconSize), tint = c.goldPrimary)
            BasicTextField(value, onValueChange, singleLine = true, interactionSource = source,
                textStyle = TextStyle(fontFamily = VazirmatnFamily, fontFeatureSettings = VazirmatnFeatureSettings,
                    fontSize = 14.sp, color = c.textMain),
                cursorBrush = SolidColor(c.goldPrimary), modifier = Modifier.weight(1f)
                    .then(if (inputTag != null) Modifier.testTag(inputTag) else Modifier)
                    .semantics { contentDescription = hint }, decorationBox = { input -> Box {
                        if (value.isEmpty()) Text(hint, color = c.textMuted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        input()
                    } })
            if (value.isNotEmpty()) IconButton({ onValueChange("") }, Modifier.size(44.dp)) {
                Icon(Icons.Outlined.Close, "پاک کردن جستجو", Modifier.size(20.dp), tint = c.textMuted)
            } else Spacer(Modifier.width(10.dp))
        }
    }
}

/** Android banking quick access: an icon tile above one readable line, never a stretched action button. */
@Composable internal fun DesktopQuickAccessTile(
    text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val c = LocalGoldExColors.current
    Surface(onClick, modifier, shape = DesktopControls.BodyShape, color = c.surface, border = c.goldHairlineBorder,
        shadowElevation = if (c.isDark) 0.dp else 1.dp) {
        Column(Modifier.heightIn(min = 100.dp).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
            Surface(shape = DesktopControls.BodyShape, color = c.surfaceElevated, modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp), tint = c.goldPrimary) }
            }
            Text(text, fontFamily = VazirmatnFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = c.textMain, maxLines = 1, softWrap = false)
        }
    }
}


/** Uniform compact chip field for custom numeric inputs (e.g. custom karat) matching chip heights. */
@Composable internal fun DesktopInlineChipField(
    value: String,
    onValueChange: (String) -> Unit,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "عیار دلخواه",
    height: Dp = 38.dp
) {
    val c = LocalGoldExColors.current
    val active = LocalDesktopDialogInputEnabled.current
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) c.goldContainer.copy(alpha = 0.5f) else c.surfaceElevated,
        border = BorderStroke(
            if (focused || isSelected) 1.5.dp else 1.dp,
            if (focused || isSelected) (if (c.isDark) c.goldSecondary else c.goldPrimary) else if (c.isDark) Color(0x52D4AF37) else c.border.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = active) { onSelect() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = value,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    onValueChange(filtered)
                    onSelect()
                },
                singleLine = true,
                enabled = active,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    fontFamily = VazirmatnFamily,
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = if (isSelected) c.goldPrimary else c.textMain,
                    textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                ),
                cursorBrush = SolidColor(c.goldPrimary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 11.sp,
                                color = c.textMuted,
                                fontFamily = VazirmatnFamily,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
