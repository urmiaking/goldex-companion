package com.goldex.companion.ui.customers.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.LedgerEntryType
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.customers.CustomerSettlementUiState
import com.goldex.companion.ui.customers.CustomerSettlementViewModel
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSettlementModal(
    state: CustomerSettlementUiState,
    actions: CustomerSettlementViewModel,
    onConfirm: () -> Unit,
    onIndependentEntry: () -> Unit
) {
    val colors = LocalGoldExColors.current
    val customer = state.customer ?: return
    val preview = state.preview
    var scopeMenu by remember { mutableStateOf(false) }
    var methodMenu by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = actions::close,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = colors.surface
        ) {
            Column(
                Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("تسویه حساب ${customer.name}", color = colors.textMain, fontFamily = VazirmatnFamily,
                    fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("پرداخت را به ماندهٔ موردنظر اختصاص دهید؛ تبدیل با نرخ همین سند ثبت می‌شود.",
                    color = colors.textSecondary, fontFamily = VazirmatnFamily, fontSize = 13.sp)
                Box {
                    GoldButton(
                        text = state.invoices.firstOrNull { it.id == state.invoiceId }?.let { "بابت فاکتور ${PersianNumberFormatter.toPersianDigits(it.number)}" }
                            ?: if (state.invoiceId == null) "بابت حساب کلی مشتری" else "فاکتور تسویه شده یا در دسترس نیست",
                        onClick = { scopeMenu = true }, isSecondary = true, modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = scopeMenu, onDismissRequest = { scopeMenu = false }) {
                        DropdownMenuItem(text = { Text("حساب کلی مشتری", fontFamily = VazirmatnFamily) },
                            onClick = { scopeMenu = false; actions.selectInvoice(null) })
                        state.invoices.forEach { invoice ->
                            DropdownMenuItem(text = { Text("فاکتور ${PersianNumberFormatter.toPersianDigits(invoice.number)}", fontFamily = VazirmatnFamily) },
                                onClick = { scopeMenu = false; actions.selectInvoice(invoice.id) })
                        }
                    }
                }
                SettlementBalances("ماندهٔ انتخاب‌شده", state.scope.goldGrams, state.scope.cashTomans)
                Text("کدام مانده تسویه شود؟", color = colors.textMain, fontFamily = VazirmatnFamily, fontWeight = FontWeight.Bold)
                UnitChoices(state.targetType, actions::selectTarget,
                    goldEnabled = abs(state.scope.goldGrams) > 1e-10, cashEnabled = state.scope.cashTomans != 0L)
                Text(if ((if (state.targetType == LedgerEntryType.GOLD_WEIGHT) state.scope.goldGrams else state.scope.cashTomans.toDouble()) >= 0)
                    "دریافت از مشتری" else "پرداخت به مشتری", color = colors.goldPrimary, fontFamily = VazirmatnFamily)
                if (state.canOffset) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("تهاتر با ماندهٔ مقابل (بدون دریافت جدید)", modifier = Modifier.weight(1f),
                            fontFamily = VazirmatnFamily, color = colors.textSecondary)
                        Switch(checked = state.offsetExistingCredit, onCheckedChange = actions::setOffset)
                    }
                }
                Text(if (state.offsetExistingCredit) "واحد بستانکاری برای تهاتر" else "روش پرداخت", color = colors.textMain,
                    fontFamily = VazirmatnFamily, fontWeight = FontWeight.Bold)
                UnitChoices(state.paymentType, actions::selectPayment, enabled = !state.offsetExistingCredit)
                if (state.needsRate) {
                    Text("نرخ هر گرم طلای ۱۸ عیار", color = colors.textMain, fontFamily = VazirmatnFamily)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = state.isMarketRate, onClick = actions::useMarketRate,
                            label = { Text("نرخ بازار", fontFamily = VazirmatnFamily) }, modifier = Modifier.heightIn(min = 48.dp))
                        FilterChip(selected = !state.isMarketRate, onClick = actions::useCustomRate,
                            label = { Text("نرخ دلخواه", fontFamily = VazirmatnFamily) }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                    GoldInputField(value = state.rateInput, onValueChange = actions::setRate,
                        label = "نرخ تبدیل (تومان / گرم)", trailingText = "تومان", enabled = !state.isMarketRate,
                        modifier = Modifier.testTag("settlementRate"))
                    Text(
                        if (state.isMarketRate) "${state.rateSource} • ساعت ${PersianNumberFormatter.toPersianDigits(state.rateObservedAt)}؛ نرخ تا ثبت ثابت می‌ماند"
                        else if (!state.marketRateAvailable) "نرخ زنده در دسترس نیست؛ نرخ توافق‌شده را وارد کنید."
                        else "نرخ توافق‌شدهٔ این تسویه؛ نرخ فاکتور قبلی تغییر نمی‌کند.",
                        color = colors.textSecondary, fontFamily = VazirmatnFamily, fontSize = 12.sp
                    )
                }
                if (state.paymentType == LedgerEntryType.CASH_RIAL) {
                    GoldInputField(value = state.amountInput, onValueChange = actions::setAmount,
                        label = if (state.offsetExistingCredit) "مبلغ تهاتر" else "مبلغ پرداخت", trailingText = "تومان",
                        modifier = Modifier.testTag("settlementAmount"))
                    if (!state.offsetExistingCredit) Box {
                        GoldButton(state.paymentMethod, { methodMenu = true }, isSecondary = true, modifier = Modifier.fillMaxWidth())
                        DropdownMenu(methodMenu, { methodMenu = false }) {
                            listOf("حواله بانکی / پایا", "کارتخوان (POS)", "اسکناس نقد").forEach { method ->
                                DropdownMenuItem(text = { Text(method, fontFamily = VazirmatnFamily) }, onClick = { actions.setMethod(method); methodMenu = false })
                            }
                        }
                    }
                } else {
                    GoldInputField(value = state.weightInput, onValueChange = actions::setWeight,
                        label = if (state.offsetExistingCredit) "وزن معادل ۷۵۰ برای تهاتر" else "وزن خالص طلای پرداختی (بدون سنگ)",
                        trailingText = "گرم", isDecimal = true, useThousandsSeparator = false,
                        modifier = Modifier.testTag("settlementWeight"))
                    GoldInputField(value = state.karatInput, onValueChange = actions::setKarat, label = "عیار هزارگانی (مثلاً ۷۵۰)",
                        enabled = !state.offsetExistingCredit, useThousandsSeparator = false)
                }
                TextButton(onClick = actions::fillRemaining, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (state.offsetExistingCredit) "محاسبهٔ تهاتر قابل انجام" else "محاسبهٔ پرداخت برای تسویهٔ کامل", color = colors.goldPrimary, fontFamily = VazirmatnFamily)
                }
                if (!state.offsetExistingCredit) GoldInputField(value = state.trackingCode, onValueChange = actions::setTracking,
                    label = "شماره پیگیری / رسید / انگ (اختیاری)", keyboardType = KeyboardType.Text, useThousandsSeparator = false)
                GoldInputField(value = state.note, onValueChange = actions::setNote, label = "توضیحات (اختیاری)",
                    keyboardType = KeyboardType.Text, useThousandsSeparator = false)
                if (preview != null) {
                    Surface(color = colors.surfaceElevated, shape = RoundedCornerShape(16.dp), border = BorderStroke(0.6.dp, colors.goldBorder)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("پیش‌نمایش تسویه", fontFamily = VazirmatnFamily, fontWeight = FontWeight.Bold, color = colors.textMain)
                            if (state.needsRate) Text(
                                "${PersianNumberFormatter.formatPrice(preview.cashAppliedTomans)} تومان ↔ ${PersianNumberFormatter.formatWeight(preview.goldAppliedGrams)} گرمِ عیار ۷۵۰",
                                fontFamily = VazirmatnFamily, color = colors.goldPrimary, modifier = Modifier.testTag("settlementConversion"))
                            SettlementBalances("ماندهٔ حساب مشتری پس از ثبت", preview.customerAfter.goldDebtGrams, preview.customerAfter.cashDebtTomans)
                            Text("مازاد پرداخت در واحد خودش ثبت می‌شود. مانده‌ها بدون انتخاب شما تبدیل نمی‌شوند.",
                                fontFamily = VazirmatnFamily, color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
                val hasPaymentInput = if (state.paymentType == LedgerEntryType.CASH_RIAL) state.amountInput.isNotBlank() else state.weightInput.isNotBlank()
                val error = state.error ?: state.validationError?.takeIf { hasPaymentInput }
                if (error != null) Text(error, fontFamily = VazirmatnFamily, color = colors.errorRed,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GoldButton("انصراف", actions::close, isSecondary = true, modifier = Modifier.weight(1f), enabled = !state.isSaving)
                    GoldButton("ثبت تسویه", onConfirm, modifier = Modifier.weight(1f).testTag("confirmSettlement"),
                        enabled = preview != null && state.error == null, isLoading = state.isSaving)
                }
                TextButton(onClick = onIndependentEntry, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("ثبت دریافت / پرداخت مستقل از تسویه", fontFamily = VazirmatnFamily, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun UnitChoices(selected: LedgerEntryType, onSelect: (LedgerEntryType) -> Unit,
    goldEnabled: Boolean = true, cashEnabled: Boolean = true, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(LedgerEntryType.GOLD_WEIGHT to "طلا (گرم ۷۵۰)", LedgerEntryType.CASH_RIAL to "پول (تومان)").forEach { (type, label) ->
            FilterChip(selected = selected == type, onClick = { onSelect(type) },
                enabled = enabled && if (type == LedgerEntryType.GOLD_WEIGHT) goldEnabled else cashEnabled,
                label = { Text(label, fontFamily = VazirmatnFamily) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
        }
    }
}

@Composable
private fun SettlementBalances(title: String, gold: Double, cash: Long) {
    val colors = LocalGoldExColors.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontFamily = VazirmatnFamily, color = colors.textSecondary, fontSize = 12.sp)
        Text("طلا: ${PersianNumberFormatter.formatAccountWeight(abs(gold))} گرم ${if (gold > 1e-10) "بدهکار" else if (gold < -1e-10) "بستانکار" else "تسویه"}",
            fontFamily = VazirmatnFamily, color = colors.textMain)
        Text("پول: ${PersianNumberFormatter.formatPrice(abs(cash))} تومان ${if (cash > 0) "بدهکار" else if (cash < 0) "بستانکار" else "تسویه"}",
            fontFamily = VazirmatnFamily, color = colors.textMain)
    }
}
