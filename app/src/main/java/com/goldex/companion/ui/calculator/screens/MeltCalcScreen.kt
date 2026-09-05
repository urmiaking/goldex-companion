package com.goldex.companion.ui.calculator.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PersianWordsFormatter
import com.goldex.companion.ui.calculator.*
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.hub.HubArrowRight
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.heroCardGradient

data class MeltSummary(
    val weightGrams: Double,
    val mesghalPrice: Double,
    val gramPrice18k: Double,
    val totalPrice: Double
)

/**
 * Dedicated Full Screen: Melt Gold Calculator (مظنه آبشده و تبدیل مثقال)
 * Adheres strictly to Stitch Persian Sovereign Aurum design system.
 */
@Composable
fun MeltCalcScreen(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalGoldExColors.current
    val scrollState = rememberScrollState()

    val meltResult = MeltSummary(
        weightGrams = PersianNumberFormatter.parsePersianOrEnglish(uiState.meltWeightInput) ?: 0.0,
        mesghalPrice = PersianNumberFormatter.parsePersianOrEnglish(uiState.mesghalPriceInput) ?: 0.0,
        gramPrice18k = uiState.meltGram18kPrice.toDouble(),
        totalPrice = uiState.meltTotalValue
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
            containerColor = colors.background,
            topBar = {
                Surface(
                    color = colors.surface,
                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated)
                                    .border(0.6.dp, colors.goldBorder, CircleShape)
                            ) {
                                Icon(
                                    imageVector = HubArrowRight,
                                    contentDescription = "بازگشت",
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF59E0B))
                                    )
                                    Text(
                                        text = "مظنه آبشده و تبدیل مثقال",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.textMain
                                    )
                                }
                                Text(
                                    text = "فرمول رسمی صنف طلا و جواهر تهران (÷ ۴.۳۳۱۸۵)",
                                    fontSize = 10.5.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        // Bind Live Melt Rate
                        Button(
                            onClick = {
                                viewModel.onMesghalPriceChanged(uiState.rates.goldMelt.toString())
                                Toast.makeText(context, "مظنه زنده آبشده درج شد ✓", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.goldPrimary,
                                contentColor = Color(0xFF141B2B)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = CalcSync,
                                contentDescription = "درج مظنه زنده",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "مظنه زنده", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ─── 1. Hero Dark Card (ارزش کل قطعه آبشده) ─────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    border = BorderStroke(0.6.dp, colors.goldBorder),
                    shadowElevation = if (colors.isDark) 0.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.heroCardGradient)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = CalcScale,
                                    contentDescription = null,
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "ارزش کل قطعه آبشده",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Copy Action
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val text = "ارزش کل قطعه آبشده: ${PersianNumberFormatter.formatPrice(meltResult.totalPrice)} تومان"
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Melt Result", text))
                                        Toast.makeText(context, "ارزش قطعه در کلیپ‌بورد کپی شد ✓", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = CalcContentCopy,
                                        contentDescription = "کپی",
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Big Price Ticker
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AnimatedPriceTicker(
                                text = "${PersianNumberFormatter.formatPrice(meltResult.totalPrice)} تومان",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.goldPrimary
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = if (meltResult.totalPrice > 0) PersianWordsFormatter.toWords(meltResult.totalPrice.toLong()) + " تومان" else "صفر تومان",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center
                            )
                        }

                        // 3-Column Metrics Breakdown
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(0.6.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("وزن قطعه", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${PersianNumberFormatter.formatWeight(meltResult.weightGrams)} گ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("مظنه مثقال ۱۷", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${PersianNumberFormatter.formatPrice(meltResult.mesghalPrice)} ت",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("معادل هر گرم ۱۸", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${PersianNumberFormatter.formatPrice(meltResult.gramPrice18k)} ت",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.profitGreen
                                )
                            }
                        }

                        // Formula Banner inside Dark Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.35f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "فرمول تبدیل مثقال به گرم ۱۸:",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "مظنه ۱۷ ÷ ۴.۳۳۱۸۵ = هر گرم ۱۸ عیار",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldPrimary
                                )
                            }
                        }
                    }
                }

                // ─── 2. Input Weight Card ────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface,
                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                    shadowElevation = if (colors.isDark) 0.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = CalcScale,
                                    contentDescription = null,
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "وزن قطعه آبشده",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                            }
                            Text(
                                text = "دقت ۳ رقم اعشار (میلی‌گرم)",
                                fontSize = 10.sp,
                                color = colors.textMuted
                            )
                        }

                        GoldInputField(
                            value = uiState.meltWeightInput,
                            onValueChange = { viewModel.onMeltWeightChanged(it) },
                            label = "وزن آبشده بر حسب گرم",
                            trailingText = "گرم",
                            isDecimal = true,
                            useThousandsSeparator = false
                        )

                        // Quick Weight Stepper Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(10.0 to "+۱۰ گ", 50.0 to "+۵۰ گ", 100.0 to "+۱۰۰ گ", 250.0 to "+۲۵۰ گ").forEach { (step, label) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.surfaceElevated,
                                    border = BorderStroke(0.5.dp, colors.border),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val current = PersianNumberFormatter.parsePersianOrEnglish(uiState.meltWeightInput) ?: 0.0
                                            val next = current + step
                                            viewModel.onMeltWeightChanged(
                                                if (next % 1.0 == 0.0) next.toLong().toString() else next.toString()
                                            )
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.goldPrimary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── 3. Mesghal Spot Price Input Card ────────────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface,
                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                    shadowElevation = if (colors.isDark) 0.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = CalcStars,
                                    contentDescription = null,
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "مظنه مثقال آبشده (عیار ۷۰۵)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                            }

                            if (uiState.rates.goldMelt > 0L) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.profitGreen.copy(alpha = 0.12f),
                                    border = BorderStroke(0.5.dp, colors.profitGreen.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        viewModel.onMesghalPriceChanged(uiState.rates.goldMelt.toString())
                                    }
                                ) {
                                    Text(
                                        text = "نرخ زنده: ${PersianNumberFormatter.formatPrice(uiState.rates.goldMelt.toDouble())} ت",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.profitGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        GoldInputField(
                            value = uiState.mesghalPriceInput,
                            onValueChange = { viewModel.onMesghalPriceChanged(it) },
                            label = "قیمت یک مثقال طلای ۱۷ عیار",
                            trailingText = "تومان",
                            useThousandsSeparator = true
                        )
                    }
                }

                // ─── 4. Action Buttons ───────────────────────────────────────
                GoldButton(
                    text = "کپی خلاصه محاسبه آبشده",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val summaryText = buildString {
                            appendLine("📋 گزارش محاسبه آبشده و مثقال بازار (قیراط)")
                            appendLine("─────────────────────────")
                            appendLine("وزن قطعه آبشده: ${PersianNumberFormatter.formatWeight(meltResult.weightGrams)} گرم")
                            appendLine("مظنه یک مثقال ۱۷: ${PersianNumberFormatter.formatPrice(meltResult.mesghalPrice)} تومان")
                            appendLine("معادل هر گرم ۱۸: ${PersianNumberFormatter.formatPrice(meltResult.gramPrice18k)} تومان")
                            appendLine("ارزش کل قطعه: ${PersianNumberFormatter.formatPrice(meltResult.totalPrice)} تومان")
                        }
                        clipboard.setPrimaryClip(ClipData.newPlainText("Melt Summary", summaryText))
                        Toast.makeText(context, "خلاصه محاسبه آبشده در کلیپ‌بورد کپی شد ✓", Toast.LENGTH_SHORT).show()
                    },
                    icon = CalcContentCopy,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
