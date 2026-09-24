package com.goldex.companion.ui.reporting

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.domain.reporting.ShamsiCalendarHelper
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.hub.HubArrowRight
import com.goldex.companion.ui.hub.HubChevronLeft
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily

@Composable
fun ShamsiDateRangePickerDialog(
    currentStartShamsi: String = "",
    currentEndShamsi: String = "",
    onDismiss: () -> Unit,
    onConfirmRange: (startMs: Long, endMs: Long, startShamsi: String, endShamsi: String) -> Unit
) {
    val colors = LocalGoldExColors.current
    val today = remember {
        ShamsiCalendarHelper.millisToShamsi(System.currentTimeMillis())
    }

    // Parse initial start and end, or default to 1st of current month and today
    val initialStart = remember {
        ShamsiCalendarHelper.parseShamsi(currentStartShamsi)
            ?: Triple(today.first, today.second, 1)
    }
    val initialEnd = remember {
        ShamsiCalendarHelper.parseShamsi(currentEndShamsi)
            ?: today
    }

    var selectedStart by remember { mutableStateOf(initialStart) }
    var selectedEnd by remember { mutableStateOf(initialEnd) }

    // Active selection target: true = picking start date, false = picking end date
    var isSelectingStart by remember { mutableStateOf(false) }

    // Displayed month and year in calendar grid
    var viewYear by remember { mutableStateOf(selectedEnd.first) }
    var viewMonth by remember { mutableStateOf(selectedEnd.second) }

    val daysInViewMonth = remember(viewYear, viewMonth) {
        ShamsiCalendarHelper.getDaysInShamsiMonth(viewYear, viewMonth)
    }
    val firstDayOffset = remember(viewYear, viewMonth) {
        ShamsiCalendarHelper.getFirstDayWeekdayIndex(viewYear, viewMonth)
    }

    val startMillis = remember(selectedStart) {
        ShamsiCalendarHelper.shamsiToMillis(selectedStart.first, selectedStart.second, selectedStart.third, 0, 0, 0)
    }
    val endMillis = remember(selectedEnd) {
        ShamsiCalendarHelper.shamsiToMillis(selectedEnd.first, selectedEnd.second, selectedEnd.third, 23, 59, 59)
    }

    fun applyQuickPreset(daysBack: Int) {
        val now = System.currentTimeMillis()
        val startMs = now - (daysBack.toLong() * 86_400_000L)
        selectedStart = ShamsiCalendarHelper.millisToShamsi(startMs)
        selectedEnd = today
        viewYear = today.first
        viewMonth = today.second
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            shadowElevation = 20.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "انتخاب بازه زمانی گزارش",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                        Text(
                            text = "تاریخ شروع و پایان گزارش مالی را تعیین فرمایید",
                            fontSize = 10.5.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.goldContainer.copy(alpha = 0.5f),
                        border = BorderStroke(0.6.dp, colors.goldBorder)
                    ) {
                        Text(
                            text = "تقویم خورشیدی",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            fontFamily = VazirmatnFamily
                        )
                    }
                }

                // 2. Quick Presets Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PresetChip("۷ روز اخیر") { applyQuickPreset(7) }
                    PresetChip("۳۰ روز اخیر") { applyQuickPreset(30) }
                    PresetChip("ماه جاری") {
                        selectedStart = Triple(today.first, today.second, 1)
                        selectedEnd = today
                        viewYear = today.first
                        viewMonth = today.second
                    }
                    PresetChip("۳ ماهه اخیر") { applyQuickPreset(90) }
                    PresetChip("از اول امسال") {
                        selectedStart = Triple(today.first, 1, 1)
                        selectedEnd = today
                        viewYear = today.first
                        viewMonth = today.second
                    }
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.35f), thickness = 0.6.dp)

                // 3. Range Display Selector (Start Date <-> End Date)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start Date Card
                    DateSelectorPill(
                        label = "از تاریخ (مبدأ)",
                        shamsiDate = ShamsiCalendarHelper.formatShamsi(selectedStart.first, selectedStart.second, selectedStart.third),
                        isActive = isSelectingStart,
                        onClick = {
                            isSelectingStart = true
                            viewYear = selectedStart.first
                            viewMonth = selectedStart.second
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "←",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary
                    )

                    // End Date Card
                    DateSelectorPill(
                        label = "تا تاریخ (مقصد)",
                        shamsiDate = ShamsiCalendarHelper.formatShamsi(selectedEnd.first, selectedEnd.second, selectedEnd.third),
                        isActive = !isSelectingStart,
                        onClick = {
                            isSelectingStart = false
                            viewYear = selectedEnd.first
                            viewMonth = selectedEnd.second
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 4. Calendar Month Navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceElevated, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Next Month Button (points to future: in RTL this is right arrow or chevron)
                    IconButton(
                        onClick = {
                            if (viewMonth < 12) {
                                viewMonth++
                            } else {
                                viewMonth = 1
                                viewYear++
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = HubChevronLeft,
                            contentDescription = "ماه بعد",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Month & Year Label
                    Text(
                        text = "${ShamsiCalendarHelper.getMonthNameFa(viewMonth)} ${PersianNumberFormatter.toPersianDigits(viewYear.toString())}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )

                    // Previous Month Button
                    IconButton(
                        onClick = {
                            if (viewMonth > 1) {
                                viewMonth--
                            } else {
                                viewMonth = 12
                                viewYear--
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = HubArrowRight,
                            contentDescription = "ماه قبل",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // 5. Weekday Column Headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ShamsiCalendarHelper.WEEKDAY_SHORT_NAMES.forEachIndexed { index, name ->
                        val isFriday = index == 6
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFriday) colors.errorRed else colors.textMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                            fontFamily = VazirmatnFamily
                        )
                    }
                }

                // 6. Day Grid
                val totalCells = firstDayOffset + daysInViewMonth
                val rows = (totalCells + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNumber = cellIndex - firstDayOffset + 1

                                if (dayNumber in 1..daysInViewMonth) {
                                    val currentCellShamsi = Triple(viewYear, viewMonth, dayNumber)
                                    val currentCellMillis = ShamsiCalendarHelper.shamsiToMillis(viewYear, viewMonth, dayNumber)

                                    val isStart = currentCellShamsi == selectedStart
                                    val isEnd = currentCellShamsi == selectedEnd
                                    val isInRange = currentCellMillis in startMillis..endMillis

                                    DayCell(
                                        dayNumber = dayNumber,
                                        isStart = isStart,
                                        isEnd = isEnd,
                                        isInRange = isInRange,
                                        onClick = {
                                            if (isSelectingStart) {
                                                selectedStart = currentCellShamsi
                                                if (currentCellMillis > endMillis) {
                                                    selectedEnd = currentCellShamsi
                                                }
                                                // Automatically switch to selecting end date for smooth 2-tap range entry
                                                isSelectingStart = false
                                            } else {
                                                if (currentCellMillis < startMillis) {
                                                    selectedStart = currentCellShamsi
                                                } else {
                                                    selectedEnd = currentCellShamsi
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.35f), thickness = 0.6.dp)

                // 7. Dialog Action Buttons (Two-action rule: Cancel on right, Primary on left)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Right button: Cancel / Dismiss
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.8.dp, colors.border)
                    ) {
                        Text(
                            text = "انصراف",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            fontFamily = VazirmatnFamily
                        )
                    }

                    // Left button: Confirm & Apply Range
                    GoldButton(
                        text = "اعمال بازه انتخابی",
                        onClick = {
                            val finalStartShamsi = ShamsiCalendarHelper.formatShamsi(selectedStart.first, selectedStart.second, selectedStart.third)
                            val finalEndShamsi = ShamsiCalendarHelper.formatShamsi(selectedEnd.first, selectedEnd.second, selectedEnd.third)
                            val finalStartMs = ShamsiCalendarHelper.shamsiToMillis(selectedStart.first, selectedStart.second, selectedStart.third, 0, 0, 0)
                            val finalEndMs = ShamsiCalendarHelper.shamsiToMillis(selectedEnd.first, selectedEnd.second, selectedEnd.third, 23, 59, 59)
                            onConfirmRange(finalStartMs, finalEndMs, finalStartShamsi, finalEndShamsi)
                        },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    onClick: () -> Unit
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(0.6.dp, colors.border.copy(alpha = 0.5f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            fontFamily = VazirmatnFamily
        )
    }
}

@Composable
private fun DateSelectorPill(
    label: String,
    shamsiDate: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val borderColor by animateColorAsState(
        targetValue = if (isActive) colors.goldPrimary else colors.border.copy(alpha = 0.6f),
        animationSpec = tween(180)
    )
    val bgColor by animateColorAsState(
        targetValue = if (isActive) colors.goldContainer.copy(alpha = 0.4f) else colors.surfaceElevated,
        animationSpec = tween(180)
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(if (isActive) 1.2.dp else 0.6.dp, borderColor),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 9.5.sp,
                color = if (isActive) colors.goldPrimary else colors.textMuted,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                fontFamily = VazirmatnFamily
            )
            Text(
                text = PersianNumberFormatter.toPersianDigits(shamsiDate),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain,
                fontFamily = VazirmatnFamily
            )
        }
    }
}

@Composable
private fun DayCell(
    dayNumber: Int,
    isStart: Boolean,
    isEnd: Boolean,
    isInRange: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val isSelectedEndpoint = isStart || isEnd

    Box(
        modifier = modifier
            .height(34.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Range strip connector background
        if (isInRange && !isSelectedEndpoint) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(colors.goldContainer.copy(alpha = 0.45f))
            )
        } else if (isStart && isInRange) {
            // Half strip to left (RTL)
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 3.dp)
                        .background(colors.goldContainer.copy(alpha = 0.45f))
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        } else if (isEnd && isInRange) {
            // Half strip to right (RTL)
            Row(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 3.dp)
                        .background(colors.goldContainer.copy(alpha = 0.45f))
                )
            }
        }

        // Circular active day indicator
        val cellBg = if (isSelectedEndpoint) colors.goldPrimary else Color.Transparent
        val textColor = if (isSelectedEndpoint) {
            Color(0xFF141B2B)
        } else if (isInRange) {
            colors.goldPrimary
        } else {
            colors.textMain
        }

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(cellBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = PersianNumberFormatter.toPersianDigits(dayNumber.toString()),
                fontSize = 11.5.sp,
                fontWeight = if (isSelectedEndpoint) FontWeight.Black else if (isInRange) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center,
                fontFamily = VazirmatnFamily
            )
        }
    }
}
