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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.data.AppSettings
import com.goldex.companion.data.PriceSource
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Price Source & Auto-Sync Configuration Bottom Sheet Modal
 *
 * Allows jewelers to select the online gold & currency rate provider:
 * 1. TGJU (Tehran Gold & Jewelry Union - Official)
 * 2. Tala.ir (Information Network for Gold & Coins)
 * 3. ISignal (Intelligent Gold Market Signals)
 *
 * And toggle background auto-sync rates.
 */
@Composable
fun PriceSourceModal(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (selectedSource: PriceSource, autoSync: Boolean) -> Unit
) {
    var selectedSource by remember { mutableStateOf(settings.priceSource) }
    var autoSync by remember { mutableStateOf(settings.autoSyncRates) }

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
                                                imageVector = HubCloudDownload,
                                                contentDescription = null,
                                                tint = colors.goldPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "مرجع استعلام نرخ‌ها و بروزرسانی",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = colors.textMain
                                            )
                                            Text(
                                                text = "انتخاب پایگاه آنلاین قیمت طلا و بروزرسانی خودکار",
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
                                // Info Notice Card
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
                                                text = "پایگاه‌های معتبر صنفی بازار طلا",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textMain
                                            )
                                            Text(
                                                text = "مظنه آبشده، هر گرم طلای ۱۸ عیار و انواع مسکوکات بانکی مستقیماً از منابع رسمی بازار استعلام و در کلیه محاسبات و فاکتورها همگام‌سازی می‌گردند.",
                                                fontSize = 10.5.sp,
                                                color = colors.textMuted,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }

                                // Price Source Options
                                Text(
                                    text = "پایگاه قیمت‌گذاری فعال",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PriceSource.entries.forEach { src ->
                                        val isSelected = selectedSource == src
                                        val (badgeTitle, badgeColor) = when (src) {
                                            PriceSource.TGJU -> "مرجع رسمی بازار" to colors.goldPrimary
                                            PriceSource.TALA_IR -> "شبکه پایدار" to colors.profitGreen
                                            PriceSource.ISIGNAL -> "سیگنال سریع" to Color(0xFF38BDF8)
                                        }

                                        val desc = when (src) {
                                            PriceSource.TGJU -> "تابلوی اتحادیه طلا و جواهر تهران • مظنه آبشده و مسکوکات"
                                            PriceSource.TALA_IR -> "شبکه اطلاع‌رسانی طلا و ارز • پوشش لحظه‌ای بازار"
                                            PriceSource.ISIGNAL -> "شبکه هوشمند تحلیلی • پوشش انس جهانی و ارز آزاد"
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) colors.goldContainer.copy(alpha = 0.45f) else colors.surfaceElevated,
                                            border = BorderStroke(
                                                width = if (isSelected) 1.2.dp else 0.6.dp,
                                                color = if (isSelected) colors.goldPrimary else colors.border
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedSource = src }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    // Selection Radio Dot
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .border(
                                                                width = if (isSelected) 5.dp else 1.2.dp,
                                                                color = if (isSelected) colors.goldPrimary else colors.border,
                                                                shape = CircleShape
                                                            )
                                                            .background(if (isSelected) colors.surface else Color.Transparent)
                                                    )

                                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = src.labelFa,
                                                                fontSize = 13.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                                color = if (isSelected) colors.goldPrimary else colors.textMain
                                                            )

                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = badgeColor.copy(alpha = 0.14f),
                                                                border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.4f))
                                                            ) {
                                                                Text(
                                                                    text = badgeTitle,
                                                                    fontSize = 9.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = badgeColor,
                                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }

                                                        Text(
                                                            text = desc,
                                                            fontSize = 10.5.sp,
                                                            color = colors.textMuted
                                                        )
                                                    }
                                                }

                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = colors.goldPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Auto Sync Card
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.surfaceElevated,
                                    border = BorderStroke(0.8.dp, colors.border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "بروزرسانی خودکار مداوم نرخ‌ها",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textMain
                                            )
                                            Text(
                                                text = "استعلام خودکار نوسانات طلا به محض اتصال اینترنت",
                                                fontSize = 10.5.sp,
                                                color = colors.textMuted
                                            )
                                        }

                                        Switch(
                                            checked = autoSync,
                                            onCheckedChange = { autoSync = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = colors.goldPrimary,
                                                uncheckedThumbColor = colors.textMuted,
                                                uncheckedTrackColor = colors.surfaceElevated
                                            )
                                        )
                                    }
                                }

                                // Bottom Hint
                                Text(
                                    text = "در صورت عدم دسترسی به اینترنت، آخرین مظنه دریافت شده در حافظه آفلاین معتبر خواهد بود.",
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
                                    text = "ذخیره و همگام‌سازی",
                                    onClick = {
                                        onSave(selectedSource, autoSync)
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
