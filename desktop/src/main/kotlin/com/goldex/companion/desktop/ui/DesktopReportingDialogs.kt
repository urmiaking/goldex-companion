package com.goldex.companion.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.desktop.ui.controls.*
import com.goldex.companion.desktop.ui.controls.DesktopDialog as Dialog
import com.goldex.companion.domain.reporting.ShamsiCalendarHelper
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.goldButtonText

/**
 * Desktop Shamsi Date Range Picker Dialog.
 * Conforms to Windows control contract and Persian RTL layout rules.
 */
@Composable
internal fun DesktopShamsiDateRangeDialog(
    currentStartShamsi: String = "",
    currentEndShamsi: String = "",
    onDismiss: () -> Unit,
    onConfirmRange: (startMs: Long, endMs: Long, startShamsi: String, endShamsi: String) -> Unit
) {
    val colors = LocalGoldExColors.current
    val today = remember {
        ShamsiCalendarHelper.millisToShamsi(System.currentTimeMillis())
    }

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
    var isSelectingStart by remember { mutableStateOf(false) }

    var viewYear by remember { mutableStateOf(selectedEnd.first) }
    var viewMonth by remember { mutableStateOf(selectedEnd.second) }

    val daysInViewMonth = remember(viewYear, viewMonth) {
        ShamsiCalendarHelper.getDaysInShamsiMonth(viewYear, viewMonth)
    }
    val firstDayOffset = remember(viewYear, viewMonth) {
        ShamsiCalendarHelper.getFirstDayWeekdayIndex(viewYear, viewMonth)
    }

    fun applyQuickPreset(daysBack: Int) {
        val now = System.currentTimeMillis()
        val startMs = now - (daysBack.toLong() * 86_400_000L)
        selectedStart = ShamsiCalendarHelper.millisToShamsi(startMs)
        selectedEnd = today
        viewYear = today.first
        viewMonth = today.second
    }

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.35f)),
                shadowElevation = 16.dp,
                modifier = Modifier.widthIn(min = 440.dp, max = 520.dp).padding(16.dp).testTag("shamsi-dialog")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "انتخاب بازه زمانی شمسی",
                                fontFamily = VazirmatnFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            Text(
                                "تعیین تاریخ آغاز و پایان برای محاسبه گزارش",
                                fontFamily = VazirmatnFamily,
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        }
                        DesktopIconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Outlined.Close, "بستن", Modifier.size(18.dp), tint = colors.textMuted)
                        }
                    }

                    // Quick Presets
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DesktopFilterChip(
                            text = "۷ روز گذشته",
                            selected = false,
                            onClick = { applyQuickPreset(7) },
                            modifier = Modifier.testTag("preset-7d")
                        )
                        DesktopFilterChip(
                            text = "۳۰ روز گذشته",
                            selected = false,
                            onClick = { applyQuickPreset(30) },
                            modifier = Modifier.testTag("preset-30d")
                        )
                        DesktopFilterChip(
                            text = "ماه جاری",
                            selected = false,
                            onClick = {
                                selectedStart = Triple(today.first, today.second, 1)
                                selectedEnd = today
                                viewYear = today.first
                                viewMonth = today.second
                            },
                            modifier = Modifier.testTag("preset-month")
                        )
                        DesktopFilterChip(
                            text = "سال جاری",
                            selected = false,
                            onClick = {
                                selectedStart = Triple(today.first, 1, 1)
                                selectedEnd = today
                                viewYear = today.first
                                viewMonth = today.second
                            },
                            modifier = Modifier.testTag("preset-year")
                        )
                    }

                    // Active Start / End Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start Date Tile
                        Surface(
                            shape = ButtonShape,
                            color = if (isSelectingStart) colors.goldContainer.copy(alpha = 0.25f) else colors.surfaceElevated,
                            border = BorderStroke(1.dp, if (isSelectingStart) colors.goldPrimary else colors.border),
                            modifier = Modifier.weight(1f).clickable {
                                isSelectingStart = true
                                viewYear = selectedStart.first
                                viewMonth = selectedStart.second
                            }.testTag("select-start-date")
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text("از تاریخ (آغاز):", fontSize = 11.sp, color = colors.textMuted)
                                Text(
                                    PersianNumberFormatter.toPersianDigits(
                                        ShamsiCalendarHelper.formatShamsi(selectedStart.first, selectedStart.second, selectedStart.third)
                                    ),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelectingStart) colors.goldPrimary else colors.textMain
                                )
                            }
                        }

                        // End Date Tile
                        Surface(
                            shape = ButtonShape,
                            color = if (!isSelectingStart) colors.goldContainer.copy(alpha = 0.25f) else colors.surfaceElevated,
                            border = BorderStroke(1.dp, if (!isSelectingStart) colors.goldPrimary else colors.border),
                            modifier = Modifier.weight(1f).clickable {
                                isSelectingStart = false
                                viewYear = selectedEnd.first
                                viewMonth = selectedEnd.second
                            }.testTag("select-end-date")
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text("تا تاریخ (پایان):", fontSize = 11.sp, color = colors.textMuted)
                                Text(
                                    PersianNumberFormatter.toPersianDigits(
                                        ShamsiCalendarHelper.formatShamsi(selectedEnd.first, selectedEnd.second, selectedEnd.third)
                                    ),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isSelectingStart) colors.goldPrimary else colors.textMain
                                )
                            }
                        }
                    }

                    // Month & Year Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DesktopIconButton(
                            onClick = {
                                if (viewMonth == 12) {
                                    viewYear++
                                    viewMonth = 1
                                } else {
                                    viewMonth++
                                }
                            },
                            modifier = Modifier.size(36.dp).testTag("next-month")
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, "ماه بعد", Modifier.size(18.dp), tint = colors.goldPrimary)
                        }

                        Text(
                            "${ShamsiCalendarHelper.getMonthNameFa(viewMonth)} ${PersianNumberFormatter.toPersianDigits(viewYear.toString())}",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            modifier = Modifier.testTag("current-month-year")
                        )

                        DesktopIconButton(
                            onClick = {
                                if (viewMonth == 1) {
                                    viewYear--
                                    viewMonth = 12
                                } else {
                                    viewMonth--
                                }
                            },
                            modifier = Modifier.size(36.dp).testTag("prev-month")
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "ماه قبل", Modifier.size(18.dp), tint = colors.goldPrimary)
                        }
                    }

                    // Calendar Weekday Headers
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        ShamsiCalendarHelper.WEEKDAY_SHORT_NAMES.forEach { dayName ->
                            Text(
                                dayName,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMuted
                            )
                        }
                    }

                    // Calendar Grid Days
                    val totalCells = firstDayOffset + daysInViewMonth
                    val rows = (totalCells + 6) / 7
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (rowIndex in 0 until rows) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                for (colIndex in 0 until 7) {
                                    val cellIndex = rowIndex * 7 + colIndex
                                    val dayNum = cellIndex - firstDayOffset + 1

                                    if (dayNum in 1..daysInViewMonth) {
                                        val thisDate = Triple(viewYear, viewMonth, dayNum)
                                        fun dateKey(d: Triple<Int, Int, Int>) = d.first * 10000 + d.second * 100 + d.third
                                        val isStart = thisDate == selectedStart
                                        val isEnd = thisDate == selectedEnd
                                        val isInRange = dateKey(thisDate) > dateKey(selectedStart) && dateKey(thisDate) < dateKey(selectedEnd)

                                        val cellColor = when {
                                            isStart || isEnd -> colors.goldPrimary
                                            isInRange -> colors.goldContainer.copy(alpha = 0.35f)
                                            else -> Color.Transparent
                                        }
                                        val textColor = when {
                                            isStart || isEnd -> colors.goldButtonText
                                            isInRange -> colors.goldPrimary
                                            else -> colors.textMain
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .clip(CircleShape)
                                                .background(cellColor)
                                                .clickable {
                                                    if (isSelectingStart) {
                                                        selectedStart = thisDate
                                                        if (dateKey(selectedStart) > dateKey(selectedEnd)) selectedEnd = selectedStart
                                                        isSelectingStart = false
                                                    } else {
                                                        selectedEnd = thisDate
                                                        if (dateKey(selectedEnd) < dateKey(selectedStart)) selectedStart = selectedEnd
                                                    }
                                                }
                                                .testTag("day-$dayNum"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                PersianNumberFormatter.toPersianDigits(dayNum.toString()),
                                                fontSize = 13.sp,
                                                fontWeight = if (isStart || isEnd) FontWeight.Bold else FontWeight.Normal,
                                                color = textColor
                                            )
                                        }
                                    } else {
                                        Spacer(Modifier.weight(1f).height(34.dp))
                                    }
                                }
                            }
                        }
                    }

                    // RTL Action Row: Secondary / Cancel on Right (first child), Primary / Apply on Left (second child)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DesktopActionButton(
                            text = "انصراف",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).testTag("cancel-range"),
                            icon = Icons.Outlined.Close,
                            isSecondary = true
                        )
                        DesktopActionButton(
                            text = "اعمال بازه زمانی",
                            onClick = {
                                val sMs = ShamsiCalendarHelper.shamsiToMillis(selectedStart.first, selectedStart.second, selectedStart.third, 0, 0, 0)
                                val eMs = ShamsiCalendarHelper.shamsiToMillis(selectedEnd.first, selectedEnd.second, selectedEnd.third, 23, 59, 59)
                                val sShamsi = ShamsiCalendarHelper.formatShamsi(selectedStart.first, selectedStart.second, selectedStart.third)
                                val eShamsi = ShamsiCalendarHelper.formatShamsi(selectedEnd.first, selectedEnd.second, selectedEnd.third)
                                onConfirmRange(sMs, eMs, sShamsi, eShamsi)
                            },
                            modifier = Modifier.weight(1.3f).testTag("confirm-range"),
                            icon = Icons.Outlined.Check,
                            isSecondary = false
                        )
                    }
                }
            }
        }
    }
}
