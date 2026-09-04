package com.goldex.companion.ui.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.data.AppSettings
import com.goldex.companion.model.WageType
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tax & Profit Configuration Bottom Sheet Modal
 *
 * Allows jewelers to configure:
 * 1. Legal dealer profit percentage (Union cap: 7%)
 * 2. VAT tax percentage (Article 26 VAT Law: 9% on wage + profit only)
 * 3. Default workshop wage type (Percentage vs Fixed Toman/gram)
 *
 * Follows Google Stitch "Persian Sovereign Aurum" design standards and LuxuryMotion specifications.
 */
@Composable
fun TaxProfitModal(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (profitPercent: String, taxPercent: String, defaultWageType: WageType) -> Unit
) {
    var profitPct by remember { mutableStateOf(settings.defaultProfitPercent) }
    var taxPct by remember { mutableStateOf(settings.defaultTaxPercent) }
    var wageType by remember { mutableStateOf(settings.defaultWageType) }

    val colors = LocalGoldExColors.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var isVisible by remember { mutableStateOf(false) }

    val handleDismiss: () -> Unit = {
        if (isVisible) {
            coroutineScope.launch {
                isVisible = false
                delay(LuxuryMotion.DURATION_MODAL_EXIT.toLong())
                onDismiss()
            }
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.65f else 0f,
        animationSpec = tween(
            durationMillis = if (isVisible) LuxuryMotion.DURATION_MODAL_ENTER else LuxuryMotion.DURATION_MODAL_EXIT,
            easing = FastOutSlowInEasing
        ),
        label = "scrimAlpha"
    )

    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = handleDismiss
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = LuxuryMotion.ModalEnter,
                    exit = LuxuryMotion.ModalExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {} // Consume click so sheet doesn't dismiss
                            ),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        color = colors.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    colors.goldPrimary.copy(alpha = 0.6f),
                                    colors.border.copy(alpha = 0.3f)
                                )
                            )
                        ),
                        shadowElevation = 24.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .heightIn(max = 640.dp)
                                .navigationBarsPadding()
                        ) {
                            // Grabber Handle & Header
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 44.dp, height = 4.dp)
                                        .clip(CircleShape)
                                        .background(colors.border)
                                        .align(Alignment.CenterHorizontally)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            colors.goldPrimary.copy(alpha = 0.22f),
                                                            colors.surfaceElevated
                                                        )
                                                    )
                                                )
                                                .border(1.dp, colors.goldBorder, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = HubPercent,
                                                contentDescription = null,
                                                tint = colors.goldPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "سود مصوب و مالیات ارزش افزوده",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = colors.textMain
                                            )
                                            Text(
                                                text = "تنظیم سود مجاز صنف و مالیات اجرت فاکتورها",
                                                fontSize = 11.sp,
                                                color = colors.textMuted
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = handleDismiss,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(colors.surfaceElevated)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "بستن",
                                            tint = colors.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.8.dp)

                            // Scrollable Body
                            Column(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .fillMaxWidth()
                                    .verticalScroll(scrollState)
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Union Law Notice Card
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.goldContainer.copy(alpha = 0.25f),
                                    border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = colors.goldPrimary,
                                            modifier = Modifier.size(18.dp).padding(top = 1.dp)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(
                                                text = "قانون معافیت اصل طلا (ماده ۲۶ ق.م.ا)",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textMain
                                            )
                                            Text(
                                                text = "مطابق قانون دائمی مالیات بر ارزش افزوده، اصل طلا ۱۰۰٪ از مالیات معاف است. مالیات ۹٪ صرفاً به مجموع (اجرت ساخت + سود فروشنده) تعلق می‌گیرد. حداکثر سود مصوب اتحادیه طلا ۷٪ می‌باشد.",
                                                fontSize = 10.5.sp,
                                                color = colors.textMuted,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }

                                // Field 1: Profit Percentage
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "درصد سود طلافروش (خرده‌فروشی)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textMain
                                        )
                                        Text(
                                            text = " *",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.goldPrimary
                                        )
                                    }

                                    OutlinedTextField(
                                        value = profitPct,
                                        onValueChange = { profitPct = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textMain
                                        ),
                                        trailingIcon = {
                                            Text(
                                                text = "٪",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = colors.goldPrimary,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.goldPrimary,
                                            unfocusedBorderColor = colors.border,
                                            focusedContainerColor = colors.surface,
                                            unfocusedContainerColor = colors.surfaceElevated.copy(alpha = 0.5f)
                                        )
                                    )

                                    // Quick preset chips for Profit
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf("۵", "۶", "۷", "۸").forEach { preset ->
                                            val isSelected = profitPct.trim() == preset
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) colors.goldPrimary else colors.surfaceElevated,
                                                border = BorderStroke(
                                                    0.6.dp,
                                                    if (isSelected) colors.goldPrimary else colors.border
                                                ),
                                                modifier = Modifier.clickable { profitPct = preset }
                                            ) {
                                                Text(
                                                    text = if (preset == "۷") "$preset٪ (مصوب)" else "$preset٪",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.Black else colors.textMuted,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Field 2: Tax Percentage
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "نرخ مالیات بر ارزش افزوده (اجرت و سود)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textMain
                                        )
                                        Text(
                                            text = " *",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.goldPrimary
                                        )
                                    }

                                    OutlinedTextField(
                                        value = taxPct,
                                        onValueChange = { taxPct = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textMain
                                        ),
                                        trailingIcon = {
                                            Text(
                                                text = "٪",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = colors.goldPrimary,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.goldPrimary,
                                            unfocusedBorderColor = colors.border,
                                            focusedContainerColor = colors.surface,
                                            unfocusedContainerColor = colors.surfaceElevated.copy(alpha = 0.5f)
                                        )
                                    )

                                    // Quick preset chips for Tax
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf("۰" to "۰٪ (معاف)", "۹" to "۹٪ (مصوب صنف)", "۱۰" to "۱۰٪").forEach { (value, label) ->
                                            val isSelected = taxPct.trim() == value
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) colors.goldPrimary else colors.surfaceElevated,
                                                border = BorderStroke(
                                                    0.6.dp,
                                                    if (isSelected) colors.goldPrimary else colors.border
                                                ),
                                                modifier = Modifier.clickable { taxPct = value }
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.Black else colors.textMuted,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Field 3: Default Wage Calculation Mode
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "نوع پیش‌فرض محاسبه اجرت در ماشین‌حساب",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Percentage Mode
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (wageType == WageType.PERCENTAGE) colors.goldContainer else colors.surfaceElevated,
                                            border = BorderStroke(
                                                width = if (wageType == WageType.PERCENTAGE) 1.2.dp else 0.8.dp,
                                                color = if (wageType == WageType.PERCENTAGE) colors.goldPrimary else colors.border
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { wageType = WageType.PERCENTAGE }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(if (wageType == WageType.PERCENTAGE) colors.goldPrimary else colors.border)
                                                )
                                                Text(
                                                    text = "درصدی (از قیمت طلا)",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (wageType == WageType.PERCENTAGE) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (wageType == WageType.PERCENTAGE) colors.textMain else colors.textMuted
                                                )
                                            }
                                        }

                                        // Fixed Amount Mode
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (wageType == WageType.TOMAN_PER_GRAM) colors.goldContainer else colors.surfaceElevated,
                                            border = BorderStroke(
                                                width = if (wageType == WageType.TOMAN_PER_GRAM) 1.2.dp else 0.8.dp,
                                                color = if (wageType == WageType.TOMAN_PER_GRAM) colors.goldPrimary else colors.border
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { wageType = WageType.TOMAN_PER_GRAM }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(if (wageType == WageType.TOMAN_PER_GRAM) colors.goldPrimary else colors.border)
                                                )
                                                Text(
                                                    text = "تومانی / هر گرم",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (wageType == WageType.TOMAN_PER_GRAM) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (wageType == WageType.TOMAN_PER_GRAM) colors.textMain else colors.textMuted
                                                )
                                            }
                                        }
                                    }
                                }

                                // Bottom Hint
                                Text(
                                    text = "این پارامترها به عنوان مقادیر اولیه در کلیه فاکتورها و ماشین‌حساب طلا استفاده می‌شوند.",
                                    fontSize = 10.5.sp,
                                    color = colors.textMuted,
                                    lineHeight = 16.sp
                                )
                            }

                            HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.8.dp)

                            // Actions Footer
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surfaceElevated.copy(alpha = 0.4f))
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GoldButton(
                                    text = "ذخیره تغییرات",
                                    onClick = {
                                        onSave(
                                            profitPct.ifBlank { "7" },
                                            taxPct.ifBlank { "9" },
                                            wageType
                                        )
                                        handleDismiss()
                                    },
                                    isSecondary = false,
                                    icon = Icons.Default.Check,
                                    modifier = Modifier.weight(1.6f)
                                )

                                GoldButton(
                                    text = "انصراف",
                                    onClick = handleDismiss,
                                    isSecondary = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
