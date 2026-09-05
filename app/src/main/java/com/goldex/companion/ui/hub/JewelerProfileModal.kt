package com.goldex.companion.ui.hub

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
import androidx.compose.material.icons.filled.*
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.goldex.companion.data.AppSettings
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.goldGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JewelerProfileModal(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSaveProfile: (galleryName: String, managerName: String, unionCode: String, phone: String, address: String) -> Unit
) {
    var galleryName by remember { mutableStateOf(settings.galleryName) }
    var managerName by remember { mutableStateOf(settings.managerName) }
    var unionCode by remember { mutableStateOf(settings.unionCode) }
    var phone by remember { mutableStateOf(settings.galleryPhone) }
    var address by remember { mutableStateOf(settings.galleryAddress) }

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

    // Calculate 2-letter monogram from gallery name
    val monogram = remember(galleryName) {
        val parts = galleryName.trim().split(" ").filter { it.isNotBlank() }
        if (parts.size >= 2) {
            "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}"
        } else {
            galleryName.take(2).ifBlank { "جا" }
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
                        .wrapContentHeight()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {} // Consume click so it doesn't dismiss
                            ),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        color = colors.surface,
                        border = androidx.compose.foundation.BorderStroke(
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
                                .heightIn(max = 620.dp)
                                .navigationBarsPadding()
                        ) {
                            // Grabber & Header
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            // Grabber Handle
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        colors.goldContainer.copy(alpha = 0.4f),
                                                        colors.goldContainer.copy(alpha = 0.7f)
                                                    )
                                                )
                                            )
                                            .border(1.dp, colors.goldBorder, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = colors.goldPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "ویرایش پروفایل بنکداری و گالری",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = colors.textMain
                                        )
                                        Text(
                                            text = "اطلاعات حقوقی و ثبت‌شده در فاکتورهای رسمی",
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

                        Divider(color = colors.border.copy(alpha = 0.5f), thickness = 0.8.dp)

                        // Scrollable Form Body
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Stamp & Logo Upload Field Card
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (colors.isDark) colors.surfaceElevated else Color(0xFFFCFAF5),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.BottomStart) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(
                                                                Color(0xFFF59E0B),
                                                                Color(0xFFD97706)
                                                            )
                                                        )
                                                    )
                                                    .border(2.dp, colors.goldPrimary.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = monogram,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 17.sp,
                                                    color = Color(0xFF1E293B)
                                                )
                                            }

                                            // Green verified dot badge
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .offset(x = (-3).dp, y = 3.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.surface)
                                                    .padding(1.5.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                        .background(colors.profitGreen),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(9.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = "نشان و مهر رسمی زرگری",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = colors.textMain
                                            )
                                            Text(
                                                text = "نمایش در سربرگ فاکتور و رسید ترازو",
                                                fontSize = 10.5.sp,
                                                color = colors.textMuted
                                            )
                                        }
                                    }

                                    GoldButton(
                                        text = "تغییر نشان",
                                        icon = HubCamera,
                                        onClick = { /* Change stamp visual action */ },
                                        isSecondary = true,
                                        modifier = Modifier.height(36.dp)
                                    )
                                }
                            }

                            // Field 1: Commercial Gallery / Jeweler Name
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "نام تجاری جواهری / بنکداری",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                    Text(
                                        text = " *",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.errorRed
                                    )
                                }

                                OutlinedTextField(
                                    value = galleryName,
                                    onValueChange = { galleryName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textMain
                                    ),
                                    trailingIcon = {
                                        Icon(
                                            imageVector = HubStorefront,
                                            contentDescription = null,
                                            tint = colors.goldPrimary,
                                            modifier = Modifier.size(18.dp)
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
                            }

                            // Field 2: Manager Full Name
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "نام و نام خانوادگی مدیر مسئول",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                    Text(
                                        text = " *",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.errorRed
                                    )
                                }

                                OutlinedTextField(
                                    value = managerName,
                                    onValueChange = { managerName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textMain
                                    ),
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = colors.goldPrimary,
                                            modifier = Modifier.size(18.dp)
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
                            }

                            // Field 3: Contact Phone Number
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "تلفن تماس و ارتباط مشتریان",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                    Text(
                                        text = " *",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.errorRed
                                    )
                                }

                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textMain
                                    ),
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = colors.goldPrimary,
                                            modifier = Modifier.size(18.dp)
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
                            }

                            // Field 5: Bazaar Chamber / Store Address
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "آدرس بنکداری / حجره بازار",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                    Text(
                                        text = " *",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.errorRed
                                    )
                                }

                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3,
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = colors.textMain,
                                        lineHeight = 20.sp
                                    ),
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = colors.goldPrimary,
                                            modifier = Modifier.size(18.dp)
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
                            }

                            // Notification Hint Banner
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.goldContainer.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "تغییرات فوراً در تمامی فاکتورهای صادره و سربرگ رسمی اعمال خواهند شد.",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textMain
                                    )
                                }
                            }
                        }

                        Divider(color = colors.border.copy(alpha = 0.5f), thickness = 0.8.dp)

                        // Actions Footer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceElevated.copy(alpha = 0.4f))
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Primary Save Button
                            GoldButton(
                                text = "ذخیره تغییرات",
                                onClick = {
                                    onSaveProfile(
                                        galleryName.trim(),
                                        managerName.trim(),
                                        unionCode.trim(),
                                        phone.trim(),
                                        address.trim()
                                    )
                                    handleDismiss()
                                },
                                isSecondary = false,
                                icon = Icons.Default.Check,
                                modifier = Modifier.weight(1.6f)
                            )

                            // Cancel Button
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
