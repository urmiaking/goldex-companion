package com.goldex.companion.ui.invoices.modals

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.foundation.border
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.BarterBalance
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.Customer
import com.goldex.companion.model.CustomerRole
import com.goldex.companion.model.InvoiceListItem
import com.goldex.companion.model.SettlementMethod
import com.goldex.companion.model.SettlementPaymentItem
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.LuxurySegmentedControl
import com.goldex.companion.ui.invoices.components.InvoiceCheckVector
import com.goldex.companion.ui.invoices.components.InvoiceCloseVector
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.VazirmatnFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@Composable
fun AddInvoicePaymentModal(
    invoice: BarterInvoice,
    balance: BarterBalance,
    customerList: List<Customer>,
    invoicesList: List<InvoiceListItem>,
    existingPayment: SettlementPaymentItem? = null,
    onDismiss: () -> Unit,
    onConfirm: (SettlementPaymentItem) -> Unit
) {
    val colors = LocalGoldExColors.current
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

    val scrimAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 0.65f else 0f,
        animationSpec = tween(
            durationMillis = if (isVisible) LuxuryMotion.DURATION_MODAL_ENTER else LuxuryMotion.DURATION_MODAL_EXIT,
            easing = FastOutSlowInEasing
        ),
        label = "scrimAlpha"
    )

    // Financial balance values
    val absNetPayableLong = kotlin.math.abs(balance.netPayableAmount).toLong()
    val totalPaid = invoice.totalPaymentsAmount
    val existingContribution = if (existingPayment != null) {
        if (existingPayment.method == SettlementMethod.BULLION) {
            (existingPayment.goldWeight18k * invoice.spotPrice18k).toLong()
        } else {
            existingPayment.amountTomans
        }
    } else 0L
    val remainingBalance = (absNetPayableLong - (totalPaid - existingContribution)).coerceAtLeast(0L)

    // Channel selection state
    var selectedChannel by remember(existingPayment) {
        mutableStateOf(existingPayment?.method ?: SettlementMethod.POS)
    }

    // POS inputs
    var posStr by remember(existingPayment) {
        mutableStateOf(
            if (existingPayment?.method == SettlementMethod.POS && existingPayment.amountTomans > 0) existingPayment.amountTomans.toString()
            else if (existingPayment == null && remainingBalance > 0L) remainingBalance.toString()
            else ""
        )
    }
    var trackingCode by remember(existingPayment) {
        mutableStateOf(if (existingPayment?.method == SettlementMethod.POS) existingPayment.trackingCode else "")
    }

    // LEDGER inputs
    var ledgerStr by remember(existingPayment) {
        mutableStateOf(if (existingPayment?.method == SettlementMethod.LEDGER && existingPayment.amountTomans > 0) existingPayment.amountTomans.toString() else "")
    }
    var ledgerDueDate by remember(existingPayment) {
        mutableStateOf(
            if (existingPayment?.method == SettlementMethod.LEDGER && existingPayment.description.startsWith("موعد: ")) {
                existingPayment.description.removePrefix("موعد: ").trim()
            } else "تسویه ماهانه"
        )
    }
    var ledgerCustomNote by remember(existingPayment) {
        mutableStateOf(
            if (existingPayment?.method == SettlementMethod.LEDGER && !existingPayment.description.startsWith("موعد: ")) {
                existingPayment.description
            } else ""
        )
    }

    // BULLION inputs
    var bullionWeightStr by remember(existingPayment) {
        mutableStateOf(
            if (existingPayment?.method == SettlementMethod.BULLION && existingPayment.goldWeight18k > 0.0) existingPayment.goldWeight18k.toString()
            else if (existingPayment == null && invoice.remainingBalanceGold18k > 0.001) String.format(Locale.US, "%.3f", invoice.remainingBalanceGold18k)
            else ""
        )
    }
    var bullionKaratStr by remember(existingPayment) {
        mutableStateOf(if (existingPayment?.method == SettlementMethod.BULLION && existingPayment.bullionKarat > 0) existingPayment.bullionKarat.toString() else "750")
    }
    var bullionAngNumber by remember(existingPayment) {
        mutableStateOf(if (existingPayment?.method == SettlementMethod.BULLION) existingPayment.bullionAngNumber else "")
    }

    // TRANSFER inputs
    var thirdPartyCustomer by remember(existingPayment) {
        mutableStateOf<Customer?>(
            if (existingPayment?.thirdPartyCustomerName?.isNotBlank() == true) {
                customerList.firstOrNull { it.name == existingPayment.thirdPartyCustomerName } ?: Customer(name = existingPayment.thirdPartyCustomerName)
            } else null
        )
    }
    var thirdPartyInvoiceId by remember { mutableStateOf("") }
    var thirdPartyInvoiceNumber by remember { mutableStateOf("") }
    var transferIsGoldMode by remember(existingPayment) {
        mutableStateOf(existingPayment?.method == SettlementMethod.TRANSFER && existingPayment.goldWeight18k > 0.0 && existingPayment.amountTomans == 0L)
    }
    var transferWeightStr by remember(existingPayment) {
        mutableStateOf(if (existingPayment?.method == SettlementMethod.TRANSFER && existingPayment.goldWeight18k > 0.0) existingPayment.goldWeight18k.toString() else "")
    }
    var transferAmountStr by remember(existingPayment) {
        mutableStateOf(if (existingPayment?.method == SettlementMethod.TRANSFER && existingPayment.amountTomans > 0L) existingPayment.amountTomans.toString() else "")
    }
    var transferTrackingCode by remember(existingPayment) {
        mutableStateOf(if (existingPayment?.method == SettlementMethod.TRANSFER) existingPayment.trackingCode else "")
    }
    var isThirdPartyPickerVisible by remember { mutableStateOf(false) }

    if (isThirdPartyPickerVisible) {
        ThirdPartyBarterPickerDialog(
            customers = customerList,
            invoices = invoicesList,
            currentInvoiceId = invoice.id,
            onSelectInvoice = { cust, inv ->
                thirdPartyCustomer = cust
                thirdPartyInvoiceId = inv.id
                thirdPartyInvoiceNumber = inv.cleanInvoiceNumber
                val w = transferWeightStr.toDoubleOrNull() ?: kotlin.math.abs(balance.net18kWeightDelta)
                val amt = transferAmountStr.toLongOrNull() ?: remainingBalance
                if (transferIsGoldMode) {
                    transferWeightStr = String.format(Locale.US, "%.3f", w)
                    transferAmountStr = ""
                } else {
                    transferAmountStr = amt.toString()
                    transferWeightStr = ""
                }
                isThirdPartyPickerVisible = false
            },
            onSelectCustomerLedger = { cust ->
                thirdPartyCustomer = cust
                thirdPartyInvoiceId = ""
                thirdPartyInvoiceNumber = "حساب دفتری باز"
                val w = transferWeightStr.toDoubleOrNull() ?: kotlin.math.abs(balance.net18kWeightDelta)
                val amt = transferAmountStr.toLongOrNull() ?: remainingBalance
                if (transferIsGoldMode) {
                    transferWeightStr = String.format(Locale.US, "%.3f", w)
                    transferAmountStr = ""
                } else {
                    transferAmountStr = amt.toString()
                    transferWeightStr = ""
                }
                isThirdPartyPickerVisible = false
            },
            onDismiss = { isThirdPartyPickerVisible = false }
        )
    }

    // Validation
    val isFormValid = when (selectedChannel) {
        SettlementMethod.POS -> (posStr.toLongOrNull() ?: 0L) > 0L
        SettlementMethod.LEDGER -> (ledgerStr.toLongOrNull() ?: 0L) > 0L
        SettlementMethod.BULLION -> (bullionWeightStr.toDoubleOrNull() ?: 0.0) > 0.0
        SettlementMethod.TRANSFER -> {
            if (transferIsGoldMode) (transferWeightStr.toDoubleOrNull() ?: 0.0) > 0.0
            else (transferAmountStr.toLongOrNull() ?: 0L) > 0L
        }
    }

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
                        .fillMaxHeight(0.90f)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
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
                                .fillMaxSize()
                                .navigationBarsPadding()
                        ) {
                            // Top Drag Handle
                            Box(
                                modifier = Modifier
                                    .padding(top = 10.dp, bottom = 4.dp)
                                    .size(width = 44.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(colors.border)
                                    .align(Alignment.CenterHorizontally)
                            )

                            // Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = if (existingPayment != null) "ویرایش مرحله پرداخت" else "ثبت مرحله پرداخت فاکتور",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Text(
                                        text = if (invoice.customerRole == CustomerRole.WHOLESALER) {
                                            "صافی کل: ${PersianNumberFormatter.formatWeight(kotlin.math.abs(balance.net18kWeightDelta))} گرم ۱۸ عیار • مانده: ${PersianNumberFormatter.formatWeight(invoice.remainingBalanceGold18k)} گرم ۱۸ عیار"
                                        } else {
                                            "صافی کل: ${PersianNumberFormatter.formatTomans(absNetPayableLong)} تومان • مانده: ${PersianNumberFormatter.formatTomans(remainingBalance)} تومان"
                                        },
                                        fontSize = 11.sp,
                                        color = colors.goldPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = VazirmatnFamily
                                    )
                                }

                                IconButton(
                                    onClick = handleDismiss,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(ButtonShape)
                                        .background(colors.surfaceVariant)
                                        .border(0.6.dp, colors.goldBorder, ButtonShape)
                                ) {
                                    Icon(
                                        imageVector = InvoiceCloseVector,
                                        contentDescription = "بستن",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Payment Method Channel Tabs
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.surfaceVariant,
                                border = BorderStroke(0.6.dp, colors.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    SettlementMethod.entries.forEach { method ->
                                        val isSelected = selectedChannel == method
                                        val tabTitle = when (method) {
                                            SettlementMethod.POS -> "کارتخوان"
                                            SettlementMethod.LEDGER -> "دفتر معین"
                                            SettlementMethod.BULLION -> "شمش / آبشده"
                                            SettlementMethod.TRANSFER -> "حواله همکار"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isSelected) colors.goldPrimary else Color.Transparent
                                                )
                                                .clickable { selectedChannel = method },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = tabTitle,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else colors.textSecondary,
                                                fontFamily = VazirmatnFamily,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Scrollable Body
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Balance Summary Card
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.surfaceElevated,
                                    border = BorderStroke(0.7.dp, colors.goldBorder.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isWholesaler = invoice.customerRole == CustomerRole.WHOLESALER
                                        val totalPaidWeight = if (invoice.payments.isNotEmpty()) {
                                            invoice.payments.sumOf { p ->
                                                if (p.goldWeight18k > 0.0) p.goldWeight18k
                                                else if (invoice.spotPrice18k > 0L) p.amountTomans.toDouble() / invoice.spotPrice18k
                                                else 0.0
                                            }
                                        } else {
                                            when (invoice.settlementMethod) {
                                                SettlementMethod.BULLION -> invoice.bullionWeight * (invoice.bullionKarat.toDouble() / 750.0)
                                                SettlementMethod.TRANSFER -> if (invoice.thirdPartyTransferWeight18k > 0.0) invoice.thirdPartyTransferWeight18k else if (invoice.spotPrice18k > 0L) invoice.thirdPartyTransferAmount.toDouble() / invoice.spotPrice18k else 0.0
                                                SettlementMethod.POS -> if (invoice.spotPrice18k > 0L) invoice.cashPosAmount.toDouble() / invoice.spotPrice18k else 0.0
                                                SettlementMethod.LEDGER -> 0.0
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.Start) {
                                            Text(
                                                text = "صافی فاکتور",
                                                fontSize = 10.5.sp,
                                                color = colors.textSecondary,
                                                fontFamily = VazirmatnFamily
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = if (isWholesaler) {
                                                    "${PersianNumberFormatter.formatWeight(kotlin.math.abs(balance.net18kWeightDelta))} گرم"
                                                } else {
                                                    "${PersianNumberFormatter.formatTomans(absNetPayableLong)} ت"
                                                },
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textMain,
                                                fontFamily = VazirmatnFamily
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(24.dp)
                                                .background(colors.border.copy(alpha = 0.6f))
                                        )

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "پرداخت تاکنون",
                                                fontSize = 10.5.sp,
                                                color = colors.textSecondary,
                                                fontFamily = VazirmatnFamily
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = if (isWholesaler) {
                                                    "${PersianNumberFormatter.formatWeight(totalPaidWeight)} گرم"
                                                } else {
                                                    "${PersianNumberFormatter.formatTomans(totalPaid)} ت"
                                                },
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if ((if (isWholesaler) totalPaidWeight > 0.0 else totalPaid > 0)) colors.profitGreen else colors.textMuted,
                                                fontFamily = VazirmatnFamily
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(24.dp)
                                                .background(colors.border.copy(alpha = 0.6f))
                                        )

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "مانده قابل تسویه",
                                                fontSize = 10.5.sp,
                                                color = colors.textSecondary,
                                                fontFamily = VazirmatnFamily
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = if (isWholesaler) {
                                                    "${PersianNumberFormatter.formatWeight(invoice.remainingBalanceGold18k)} گرم"
                                                } else {
                                                    "${PersianNumberFormatter.formatTomans(remainingBalance)} ت"
                                                },
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = colors.goldPrimary,
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                    }
                                }

                                // Inputs based on selected channel
                                AnimatedContent(
                                    targetState = selectedChannel,
                                    transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                                    label = "modalPaymentFields"
                                ) { method ->
                                    when (method) {
                                        SettlementMethod.POS -> {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    GoldInputField(
                                                        value = posStr,
                                                        onValueChange = { input ->
                                                            posStr = input.filter { it.isDigit() }
                                                        },
                                                        label = "مبلغ پرداختی",
                                                        trailingText = "تومان",
                                                        useThousandsSeparator = true,
                                                        modifier = Modifier.weight(1.2f)
                                                    )
                                                    GoldInputField(
                                                        value = trackingCode,
                                                        onValueChange = { trackingCode = it },
                                                        label = "کد پیگیری",
                                                        trailingText = "اختیاری",
                                                        keyboardType = KeyboardType.Number,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }

                                                if (invoice.customerRole == CustomerRole.WHOLESALER) {
                                                    val posAmount = posStr.toLongOrNull() ?: 0L
                                                    val eqGold = if (invoice.spotPrice18k > 0L) posAmount.toDouble() / invoice.spotPrice18k else 0.0
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = colors.goldContainer.copy(alpha = 0.4f),
                                                        border = BorderStroke(0.6.dp, colors.goldBorder),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "معادل طلای ۱۸ عیار تسویه‌شده:",
                                                                fontSize = 11.sp,
                                                                color = colors.textSecondary,
                                                                fontFamily = VazirmatnFamily
                                                            )
                                                            Text(
                                                                text = "${PersianNumberFormatter.formatWeight(eqGold)} گرم ۱۸ عیار",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = colors.goldPrimary,
                                                                fontFamily = VazirmatnFamily
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        SettlementMethod.LEDGER -> {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                GoldInputField(
                                                    value = ledgerStr,
                                                    onValueChange = { input ->
                                                        ledgerStr = input.filter { it.isDigit() }
                                                    },
                                                    label = "مبلغ تعهد دفتری / نسیه",
                                                    trailingText = "تومان",
                                                    useThousandsSeparator = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Text(
                                                    text = "موعد یا شرایط تسویه دفتری:",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textSecondary,
                                                    fontFamily = VazirmatnFamily
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    listOf("تسویه ماهانه", "۱۰ روزه", "پایان هفته", "تسویه امانی").forEach { term ->
                                                        val isSelected = ledgerDueDate == term
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = if (isSelected) colors.goldContainer else colors.surfaceVariant,
                                                            border = BorderStroke(0.6.dp, if (isSelected) colors.goldPrimary else colors.border),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clickable { ledgerDueDate = term }
                                                        ) {
                                                            Text(
                                                                text = term,
                                                                fontSize = 9.5.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (isSelected) colors.goldPrimary else colors.textSecondary,
                                                                fontFamily = VazirmatnFamily,
                                                                modifier = Modifier.padding(vertical = 6.dp),
                                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }

                                                GoldInputField(
                                                    value = ledgerCustomNote,
                                                    onValueChange = { ledgerCustomNote = it },
                                                    label = "توضیحات اضافی سند دفتری",
                                                    trailingText = "اختیاری",
                                                    keyboardType = KeyboardType.Text,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                        SettlementMethod.BULLION -> {
                                            val bullionWeight = bullionWeightStr.toDoubleOrNull() ?: 0.0
                                            val bullionKarat = bullionKaratStr.toIntOrNull() ?: 750
                                            val bullion18kEq = bullionWeight * (bullionKarat.toDouble() / 750.0)
                                            val bullionValuation = (bullion18kEq * invoice.spotPrice18k).toLong()

                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    GoldInputField(
                                                        value = bullionWeightStr,
                                                        onValueChange = { bullionWeightStr = it },
                                                        label = "وزن شمش / آبشده",
                                                        trailingText = "گرم",
                                                        keyboardType = KeyboardType.Decimal,
                                                        modifier = Modifier.weight(1.2f)
                                                    )
                                                    GoldInputField(
                                                        value = bullionKaratStr,
                                                        onValueChange = { bullionKaratStr = it.filter { ch -> ch.isDigit() } },
                                                        label = "عیار",
                                                        trailingText = "عیار",
                                                        keyboardType = KeyboardType.Number,
                                                        modifier = Modifier.weight(0.8f)
                                                    )
                                                }

                                                // Quick karat chips
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    listOf("750", "705", "995", "999").forEach { kPreset ->
                                                        val isSelected = bullionKaratStr == kPreset
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = if (isSelected) colors.goldContainer else colors.surfaceVariant,
                                                            border = BorderStroke(0.6.dp, if (isSelected) colors.goldPrimary else colors.border),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clickable { bullionKaratStr = kPreset }
                                                        ) {
                                                            Text(
                                                                text = "${PersianNumberFormatter.toPersianDigits(kPreset)} عیار",
                                                                fontSize = 9.5.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (isSelected) colors.goldPrimary else colors.textSecondary,
                                                                fontFamily = VazirmatnFamily,
                                                                modifier = Modifier.padding(vertical = 5.dp),
                                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }

                                                GoldInputField(
                                                    value = bullionAngNumber,
                                                    onValueChange = { bullionAngNumber = it },
                                                    label = "شماره انگ و آزمایشگاه",
                                                    trailingText = "اختیاری",
                                                    keyboardType = KeyboardType.Text,
                                                    useThousandsSeparator = false,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                if (bullionValuation > 0L) {
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = colors.goldContainer.copy(alpha = 0.35f),
                                                        border = BorderStroke(0.6.dp, colors.goldBorder),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(
                                                                    text = "معادل طلای ۱۸ عیار:",
                                                                    fontSize = 11.sp,
                                                                    color = colors.textSecondary,
                                                                    fontFamily = VazirmatnFamily
                                                                )
                                                                Text(
                                                                    text = "${PersianNumberFormatter.formatWeight(bullion18kEq)} گرم",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = colors.goldPrimary,
                                                                    fontFamily = VazirmatnFamily
                                                                )
                                                            }
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(
                                                                    text = "ارزش ریالی معادل:",
                                                                    fontSize = 11.sp,
                                                                    color = colors.textSecondary,
                                                                    fontFamily = VazirmatnFamily
                                                                )
                                                                Text(
                                                                    text = "${PersianNumberFormatter.formatTomans(bullionValuation)} تومان",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = colors.goldPrimary,
                                                                    fontFamily = VazirmatnFamily
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        SettlementMethod.TRANSFER -> {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = colors.surfaceVariant,
                                                    border = BorderStroke(0.8.dp, colors.goldBorder),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { isThirdPartyPickerVisible = true }
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = if (thirdPartyCustomer != null) "همکار: ${thirdPartyCustomer?.name} (${if (thirdPartyInvoiceNumber.isNotBlank()) "فاکتور: $thirdPartyInvoiceNumber" else "حساب دفتری"})" else "انتخاب همکار یا فاکتور باز برای حواله سه‌طرفه",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = if (thirdPartyCustomer != null) colors.textMain else colors.goldPrimary,
                                                            fontFamily = VazirmatnFamily
                                                        )
                                                        Text(
                                                            text = "انتخاب ›",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = colors.goldPrimary,
                                                            fontFamily = VazirmatnFamily
                                                        )
                                                    }
                                                }

                                                LuxurySegmentedControl(
                                                    items = listOf(false, true),
                                                    selectedItem = transferIsGoldMode,
                                                    onItemSelected = { isGold ->
                                                        transferIsGoldMode = isGold
                                                        if (isGold) transferAmountStr = "" else transferWeightStr = ""
                                                    },
                                                    label = { if (!it) "حواله نقدی (بانکی / پایا)" else "حواله وزنی طلا (گرم)" },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    height = 32.dp,
                                                    fontSize = 10.5.sp
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    if (!transferIsGoldMode) {
                                                        GoldInputField(
                                                            value = transferAmountStr,
                                                            onValueChange = { input ->
                                                                transferAmountStr = input.filter { it.isDigit() }
                                                                transferWeightStr = ""
                                                            },
                                                            label = "مبلغ حواله",
                                                            trailingText = "تومان",
                                                            useThousandsSeparator = true,
                                                            modifier = Modifier.weight(1.2f)
                                                        )
                                                    } else {
                                                        GoldInputField(
                                                            value = transferWeightStr,
                                                            onValueChange = { input ->
                                                                transferWeightStr = input
                                                                transferAmountStr = ""
                                                            },
                                                            label = "وزن حواله",
                                                            trailingText = "گرم",
                                                            keyboardType = KeyboardType.Decimal,
                                                            modifier = Modifier.weight(1.2f)
                                                        )
                                                    }
                                                    GoldInputField(
                                                        value = transferTrackingCode,
                                                        onValueChange = { transferTrackingCode = it },
                                                        label = "کد پیگیری",
                                                        trailingText = "اختیاری",
                                                        keyboardType = KeyboardType.Number,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Quick Auto-Fill remaining balance button
                                val hasRemaining = if (invoice.customerRole == CustomerRole.WHOLESALER) {
                                    invoice.remainingBalanceGold18k > 0.001 || remainingBalance > 0L
                                } else {
                                    remainingBalance > 0L
                                }
                                if (hasRemaining) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = colors.goldContainer.copy(alpha = 0.35f),
                                        border = BorderStroke(0.6.dp, colors.goldBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                when (selectedChannel) {
                                                    SettlementMethod.POS -> posStr = remainingBalance.toString()
                                                    SettlementMethod.LEDGER -> ledgerStr = remainingBalance.toString()
                                                    SettlementMethod.TRANSFER -> {
                                                        if (!transferIsGoldMode) {
                                                            transferAmountStr = remainingBalance.toString()
                                                            transferWeightStr = ""
                                                        } else {
                                                            val w = if (invoice.customerRole == CustomerRole.WHOLESALER && invoice.remainingBalanceGold18k > 0.001) {
                                                                invoice.remainingBalanceGold18k
                                                            } else if (invoice.spotPrice18k > 0) {
                                                                remainingBalance.toDouble() / invoice.spotPrice18k
                                                            } else 0.0
                                                            transferWeightStr = String.format(Locale.US, "%.3f", w)
                                                            transferAmountStr = ""
                                                        }
                                                    }
                                                    SettlementMethod.BULLION -> {
                                                        val w = if (invoice.customerRole == CustomerRole.WHOLESALER && invoice.remainingBalanceGold18k > 0.001) {
                                                            invoice.remainingBalanceGold18k
                                                        } else if (invoice.spotPrice18k > 0) {
                                                            remainingBalance.toDouble() / invoice.spotPrice18k
                                                        } else 0.0
                                                        bullionWeightStr = String.format(Locale.US, "%.3f", w)
                                                    }
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "⚡ تنظیم این روش با باقیمانده مانده صافی:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textMain,
                                                fontFamily = VazirmatnFamily
                                            )
                                            Text(
                                                text = if (invoice.customerRole == CustomerRole.WHOLESALER) {
                                                    "${PersianNumberFormatter.formatWeight(invoice.remainingBalanceGold18k)} گرم ۱۸ عیار"
                                                } else {
                                                    "${PersianNumberFormatter.formatTomans(remainingBalance)} تومان"
                                                },
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.goldPrimary,
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.7.dp)

                            // Sticky Footer Actions per RTL Invariant:
                            // Secondary action (انصراف) on the right (first child in Row)
                            // Primary action (ثبت پرداخت) on the left (second child in Row)
                            Surface(
                                color = colors.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GoldButton(
                                        text = "انصراف",
                                        onClick = handleDismiss,
                                        isSecondary = true,
                                        modifier = Modifier.weight(1f)
                                    )

                                    GoldButton(
                                        text = if (existingPayment != null) "ذخیره تغییرات" else "ثبت پرداخت",
                                        onClick = {
                                            val newPayment: SettlementPaymentItem? = when (selectedChannel) {
                                                SettlementMethod.POS -> {
                                                    val amt = posStr.toLongOrNull() ?: 0L
                                                    val eqGold = if (invoice.customerRole == CustomerRole.WHOLESALER && invoice.spotPrice18k > 0L) {
                                                        amt.toDouble() / invoice.spotPrice18k
                                                    } else 0.0
                                                    if (amt > 0) {
                                                        SettlementPaymentItem(
                                                            id = existingPayment?.id ?: UUID.randomUUID().toString(),
                                                            method = SettlementMethod.POS,
                                                            amountTomans = amt,
                                                            goldWeight18k = eqGold,
                                                            trackingCode = trackingCode.trim()
                                                        )
                                                    } else null
                                                }
                                                SettlementMethod.LEDGER -> {
                                                    val amt = ledgerStr.toLongOrNull() ?: 0L
                                                    if (amt > 0) {
                                                        val desc = if (ledgerCustomNote.isNotBlank()) "موعد: $ledgerDueDate • $ledgerCustomNote" else "موعد: $ledgerDueDate"
                                                        SettlementPaymentItem(
                                                            id = existingPayment?.id ?: UUID.randomUUID().toString(),
                                                            method = SettlementMethod.LEDGER,
                                                            amountTomans = amt,
                                                            description = desc
                                                        )
                                                    } else null
                                                }
                                                SettlementMethod.BULLION -> {
                                                    val w = bullionWeightStr.toDoubleOrNull() ?: 0.0
                                                    val k = bullionKaratStr.toIntOrNull() ?: 750
                                                    val eq18k = w * (k.toDouble() / 750.0)
                                                    val valTomans = (eq18k * invoice.spotPrice18k).toLong()
                                                    if (w > 0.0 || valTomans > 0L) {
                                                        SettlementPaymentItem(
                                                            id = existingPayment?.id ?: UUID.randomUUID().toString(),
                                                            method = SettlementMethod.BULLION,
                                                            amountTomans = valTomans,
                                                            goldWeight18k = eq18k,
                                                            bullionKarat = k,
                                                            bullionAngNumber = bullionAngNumber.trim()
                                                        )
                                                    } else null
                                                }
                                                SettlementMethod.TRANSFER -> {
                                                    val amt = if (!transferIsGoldMode) (transferAmountStr.toLongOrNull() ?: 0L) else 0L
                                                    var w = if (transferIsGoldMode) (transferWeightStr.toDoubleOrNull() ?: 0.0) else 0.0
                                                    if (!transferIsGoldMode && invoice.customerRole == CustomerRole.WHOLESALER && invoice.spotPrice18k > 0L && amt > 0L) {
                                                        w = amt.toDouble() / invoice.spotPrice18k
                                                    }
                                                    if (amt > 0L || w > 0.0) {
                                                        SettlementPaymentItem(
                                                            id = existingPayment?.id ?: UUID.randomUUID().toString(),
                                                            method = SettlementMethod.TRANSFER,
                                                            amountTomans = amt,
                                                            goldWeight18k = w,
                                                            trackingCode = transferTrackingCode.trim(),
                                                            thirdPartyCustomerName = thirdPartyCustomer?.name ?: ""
                                                        )
                                                    } else null
                                                }
                                            }

                                            if (newPayment != null) {
                                                onConfirm(newPayment)
                                                handleDismiss()
                                            }
                                        },
                                        icon = InvoiceCheckVector,
                                        isSecondary = false,
                                        enabled = isFormValid,
                                        modifier = Modifier.weight(1.8f)
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
