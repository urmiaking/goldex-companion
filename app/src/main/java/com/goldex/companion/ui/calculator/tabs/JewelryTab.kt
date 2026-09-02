package com.goldex.companion.ui.calculator.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.CalculatorUiState
import com.goldex.companion.ui.calculator.GoldCalculatorViewModel
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.ResultRow
import com.goldex.companion.ui.theme.*

@Composable
fun JewelryTab(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Karat & Auto Sync Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "عیار قطعه طلا",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "مظنه زنده بازار",
                            fontSize = 11.sp,
                            color = if (uiState.autoSyncPrice) ProfitGreen else TextMuted
                        )
                        Switch(
                            checked = uiState.autoSyncPrice,
                            onCheckedChange = { viewModel.toggleAutoSyncPrice(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GoldPrimary,
                                checkedTrackColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Karat.values().forEach { karat ->
                        val isSelected = uiState.selectedKarat == karat
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onKaratSelected(karat) },
                            label = {
                                Text(
                                    text = karat.labelFa,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "مشخصات معامله و قیمت روز",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoldInputField(
                        value = uiState.grossWeightInput,
                        onValueChange = { viewModel.onGrossWeightChanged(it) },
                        label = "وزن کل طلا",
                        trailingText = "گرم",
                        isDecimal = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    GoldInputField(
                        value = uiState.stoneWeightInput,
                        onValueChange = { viewModel.onStoneWeightChanged(it) },
                        label = "کسر نگین",
                        trailingText = "گرم",
                        isDecimal = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1.0, 5.0, 10.0, 20.0).forEach { w ->
                        OutlinedButton(
                            onClick = { viewModel.addGrossWeight(w) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight)
                        ) {
                            Text("+${w.toInt()}g", fontSize = 11.sp)
                        }
                    }
                }

                GoldInputField(
                    value = uiState.spotPriceInput,
                    onValueChange = { viewModel.onSpotPriceChanged(it) },
                    label = "قیمت مظنه هر گرم طلا ۱۸ عیار",
                    trailingText = "تومان",
                    subLabel = uiState.priceInWords.ifBlank { null }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.applyPresetSpotPrice(uiState.rates.gold18) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("مظنه ۱۸ عیار", fontSize = 11.sp, color = GoldSecondary)
                    }
                    OutlinedButton(
                        onClick = { viewModel.applyPresetSpotPrice(uiState.rates.gold24) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("۲۴ عیار شمش", fontSize = 11.sp, color = GoldSecondary)
                    }
                }

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoldInputField(
                        value = uiState.wageInput,
                        onValueChange = { viewModel.onWageChanged(it) },
                        label = "اجرت ساخت",
                        trailingText = "٪",
                        isDecimal = true,
                        modifier = Modifier.weight(1f)
                    )
                    GoldInputField(
                        value = uiState.profitPercentInput,
                        onValueChange = { viewModel.onProfitPercentChanged(it) },
                        label = "سود فروشنده",
                        trailingText = "٪",
                        isDecimal = true,
                        modifier = Modifier.weight(1f)
                    )
                    GoldInputField(
                        value = uiState.taxPercentInput,
                        onValueChange = { viewModel.onTaxPercentChanged(it) },
                        label = "مالیات ۹٪",
                        trailingText = "٪",
                        isDecimal = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "* طبق قانون جدید، ۹٪ مالیات ارزش افزوده صرفاً به مجموع سود و اجرت تعلق می‌گیرد.",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        // Result Invoice Card
        AnimatedVisibility(
            visible = uiState.jewelryResult != null,
            enter = fadeIn() + slideInVertically()
        ) {
            uiState.jewelryResult?.let { res ->
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(listOf(DarkSurface, DarkBg)),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "جمع کل پرداختی خریدار (فاکتور نهایی)",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "${PersianNumberFormatter.formatPrice(res.totalPayable)} تومان",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ResultRow("وزن خالص طلا:", "${PersianNumberFormatter.formatWeight(res.netWeight)} گرم")
                            ResultRow("ارزش خام طلا:", "${PersianNumberFormatter.formatPrice(res.rawGoldValue)} تومان")
                            ResultRow("مبلغ اجرت ساخت:", "${PersianNumberFormatter.formatPrice(res.wageAmount)} تومان", ProfitGreen)
                            ResultRow("مبلغ سود فروشنده:", "${PersianNumberFormatter.formatPrice(res.profitAmount)} تومان", ProfitGreen)
                            ResultRow("مالیات بر ارزش افزوده:", "${PersianNumberFormatter.formatPrice(res.taxAmount)} تومان", TextMuted)
                            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                            ResultRow("نرخ تمام‌شده هر گرم:", "${PersianNumberFormatter.formatPrice(res.effectiveGramPrice)} تومان", GoldLight)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val spot = uiState.spotPriceInput.toLongOrNull() ?: 0L
                                    val invoice = res.formatInvoice(spot, uiState.selectedKarat)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("فاکتور طلا", invoice))
                                    Toast.makeText(context, "فاکتور با موفقیت کپی شد", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkBg),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("کپی فاکتور", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val spot = uiState.spotPriceInput.toLongOrNull() ?: 0L
                                    val invoice = res.formatInvoice(spot, uiState.selectedKarat)
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, invoice)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "ارسال فاکتور طلا"))
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اشتراک‌گذاری", fontSize = 12.sp)
                            }

                            IconButton(
                                onClick = { viewModel.resetJewelry() },
                                modifier = Modifier.background(DarkSurface, CircleShape)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "بازنشانی", tint = GoldSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
