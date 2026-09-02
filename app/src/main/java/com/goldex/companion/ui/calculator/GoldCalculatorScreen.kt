package com.goldex.companion.ui.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PriceBasis
import com.goldex.companion.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldCalculatorScreen(
    viewModel: GoldCalculatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        brush = Brush.linearGradient(listOf(GoldPrimary, GoldDark)),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = DarkBg,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "همراه گلدکس",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = GoldPrimary
                                )
                                Text(
                                    text = "محاسبه‌گر پیشرفته مظنه طلا و عیار",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "بازنشانی",
                                tint = GoldSecondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkSurface
                    )
                )
            },
            containerColor = DarkBg
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Karat Selector Section
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "عیار قطعه طلا / جواهر",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Karat.values().forEach { karat ->
                                val selected = uiState.selectedKarat == karat
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.onKaratSelected(karat) },
                                    label = {
                                        Text(
                                            text = karat.labelFa,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) DarkBg else TextMain
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldPrimary,
                                        selectedLabelColor = DarkBg,
                                        containerColor = DarkSurfaceVariant,
                                        labelColor = TextMain
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Price basis toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PriceBasis.values().forEach { basis ->
                                val isSelected = uiState.priceBasis == basis
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onPriceBasisSelected(basis) },
                                    label = {
                                        Text(
                                            text = basis.labelFa,
                                            fontSize = 11.sp,
                                            color = if (isSelected) DarkBg else TextMuted
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldSecondary,
                                        selectedLabelColor = DarkBg,
                                        containerColor = DarkSurfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Inputs Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "مشخصات معامله",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )

                        // Gold Weight
                        OutlinedTextField(
                            value = uiState.weightInput,
                            onValueChange = { viewModel.onWeightChanged(it) },
                            label = { Text("وزن طلا (گرم)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = outlinedTextFieldColors()
                        )

                        // Quick Weight additions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1.0, 5.0, 10.0).forEach { addVal ->
                                OutlinedButton(
                                    onClick = { viewModel.addWeight(addVal) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = GoldLight
                                    )
                                ) {
                                    Text("+${addVal.toInt()} گرم", fontSize = 12.sp)
                                }
                            }
                        }

                        // Spot Price
                        OutlinedTextField(
                            value = uiState.spotPriceInput,
                            onValueChange = { viewModel.onSpotPriceChanged(it) },
                            label = { Text("قیمت مظنه هر گرم (تومان)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = outlinedTextFieldColors()
                        )

                        // Margin Percentage
                        OutlinedTextField(
                            value = uiState.marginPercentInput,
                            onValueChange = { viewModel.onMarginChanged(it) },
                            label = { Text("درصد سود، اجرت و کارمزد (٪)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = outlinedTextFieldColors()
                        )

                        // Margin Quick Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("5", "7", "9", "12").forEach { m ->
                                SuggestionChip(
                                    onClick = { viewModel.onMarginChanged(m) },
                                    label = { Text("$m٪", fontSize = 11.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = DarkSurfaceVariant,
                                        labelColor = TextMain
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Output Result Card
                AnimatedVisibility(
                    visible = uiState.calculationResult != null,
                    enter = fadeIn() + slideInVertically()
                ) {
                    uiState.calculationResult?.let { result ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.horizontalGradient(listOf(GoldPrimary, GoldDark)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Main Total Trade Banner
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.linearGradient(listOf(DarkSurface, DarkBg)),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "ارزش کل معامله (تخمین نهایی)",
                                            fontSize = 13.sp,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = "${PersianNumberFormatter.formatPrice(result.totalTradeValue)} تومان",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPrimary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                // Calculation Breakdown Details
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ResultRow(
                                        label = "وزن طلای خالص (۲۴ عیار):",
                                        value = "${PersianNumberFormatter.formatWeight(result.pureGoldWeightGrams)} گرم"
                                    )
                                    ResultRow(
                                        label = "ارزش خام طلا (بدون کارمزد):",
                                        value = "${PersianNumberFormatter.formatPrice(result.rawGoldValue)} تومان"
                                    )
                                    ResultRow(
                                        label = "مبلغ کارمزد و سود معامله:",
                                        value = "${PersianNumberFormatter.formatPrice(result.marginAmount)} تومان",
                                        valueColor = ProfitGreen
                                    )
                                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                                    ResultRow(
                                        label = "نرخ تمام‌شده هر گرم قطعه:",
                                        value = "${PersianNumberFormatter.formatPrice(result.effectivePricePerGram)} تومان",
                                        valueColor = GoldLight
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultRow(
    label: String,
    value: String,
    valueColor: Color = TextMain
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextMuted
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GoldPrimary,
    unfocusedBorderColor = DarkBorder,
    focusedLabelColor = GoldLight,
    unfocusedLabelColor = TextMuted,
    focusedTextColor = TextMain,
    unfocusedTextColor = TextMain,
    cursorColor = GoldPrimary
)
