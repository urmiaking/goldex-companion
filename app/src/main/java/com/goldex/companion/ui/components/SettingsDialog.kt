package com.goldex.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.data.AppSettings
import com.goldex.companion.data.PriceSource
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.WageType
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun SettingsDialog(
    initialSettings: AppSettings,
    onDismiss: () -> Unit,
    onSaveSettings: (AppSettings) -> Unit
) {
    val colors = LocalGoldExColors.current

    var selectedSource by remember { mutableStateOf(initialSettings.priceSource) }
    var profitPct by remember { mutableStateOf(initialSettings.defaultProfitPercent) }
    var taxPct by remember { mutableStateOf(initialSettings.defaultTaxPercent) }
    var wageType by remember { mutableStateOf(initialSettings.defaultWageType) }
    var autoSync by remember { mutableStateOf(initialSettings.autoSyncRates) }

    var galleryName by remember { mutableStateOf(initialSettings.galleryName) }
    var galleryPhone by remember { mutableStateOf(initialSettings.galleryPhone) }
    var galleryAddress by remember { mutableStateOf(initialSettings.galleryAddress) }
    var galleryLicense by remember { mutableStateOf(initialSettings.galleryLicense) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.goldBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.goldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "تنظیمات و پیکربندی قیراط",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            Text(
                                text = "شخصی‌سازی نرخ‌ها، سود و مشخصات فاکتور",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = colors.border, thickness = 0.6.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Price Source
                    Text(
                        text = "منبع استعلام مظنه و طلا",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PriceSource.entries.forEach { src ->
                            val isSelected = selectedSource == src
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) colors.goldContainer else colors.surfaceElevated)
                                    .border(
                                        width = if (isSelected) 1.2.dp else 0.5.dp,
                                        color = if (isSelected) colors.goldPrimary else colors.border,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedSource = src }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = src.labelFa,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.goldPrimary else colors.textMain
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Auto Sync Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceElevated)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "بروزرسانی خودکار هر ۱ دقیقه",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMain
                            )
                            Text(
                                text = "استعلام خودکار آخرین مظنه از سرور",
                                fontSize = 10.sp,
                                color = colors.textMuted
                            )
                        }
                        Switch(
                            checked = autoSync,
                            onCheckedChange = { autoSync = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.goldPrimary,
                                checkedTrackColor = colors.goldContainer
                            )
                        )
                    }

                    HorizontalDivider(color = colors.border, thickness = 0.5.dp)

                    // 2. Defaults
                    Text(
                        text = "پارامترهای پیش‌فرض معاملات",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = profitPct,
                            onValueChange = { profitPct = it },
                            label = { Text("سود طلافروش (٪)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.goldPrimary,
                                unfocusedBorderColor = colors.border
                            )
                        )

                        OutlinedTextField(
                            value = taxPct,
                            onValueChange = { taxPct = it },
                            label = { Text("مالیات ارزش افزوده (٪)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.goldPrimary,
                                unfocusedBorderColor = colors.border
                            )
                        )
                    }

                    HorizontalDivider(color = colors.border, thickness = 0.5.dp)

                    // 3. Gallery Profile
                    Text(
                        text = "مشخصات گالری (درج در فاکتور رسمی PDF)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary
                    )

                    OutlinedTextField(
                        value = galleryName,
                        onValueChange = { galleryName = it },
                        label = { Text("نام فروشگاه / گالری طلا", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.goldPrimary,
                            unfocusedBorderColor = colors.border
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = galleryPhone,
                            onValueChange = { galleryPhone = it },
                            label = { Text("تلفن تماس", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.goldPrimary,
                                unfocusedBorderColor = colors.border
                            )
                        )

                        OutlinedTextField(
                            value = galleryLicense,
                            onValueChange = { galleryLicense = it },
                            label = { Text("شماره پروانه کسب / اتحادیه", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.goldPrimary,
                                unfocusedBorderColor = colors.border
                            )
                        )
                    }

                    OutlinedTextField(
                        value = galleryAddress,
                        onValueChange = { galleryAddress = it },
                        label = { Text("نشانی گالری (درج در انتهای فاکتور)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.goldPrimary,
                            unfocusedBorderColor = colors.border
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action
                Button(
                    onClick = {
                        val updated = AppSettings(
                            priceSource = selectedSource,
                            defaultProfitPercent = profitPct.ifBlank { "7" },
                            defaultTaxPercent = taxPct.ifBlank { "9" },
                            defaultWageType = wageType,
                            autoSyncRates = autoSync,
                            galleryName = galleryName.ifBlank { "گالری طلای قیراط" },
                            galleryPhone = galleryPhone.ifBlank { "۰۲۱-۸۸۸۸۸۸۸۸" },
                            galleryAddress = galleryAddress.ifBlank { "تهران، بازار بزرگ طلافروشان" },
                            galleryLicense = galleryLicense.ifBlank { "صنف طلا و جواهر: ۱۱۰۲۴" }
                        )
                        onSaveSettings(updated)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.goldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "ذخیره تغییرات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
