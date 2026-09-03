package com.goldex.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.data.MarketRates
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.goldGradient
import com.goldex.companion.ui.theme.heroCardGradient
import kotlinx.coroutines.launch

// Custom lightweight vector icons to avoid external icon library bloat
private val DrawerCrownVector: ImageVector = ImageVector.Builder(
    name = "DrawerCrown",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(5f, 16f)
        lineTo(3f, 7f)
        lineTo(8.5f, 11f)
        lineTo(12f, 4f)
        lineTo(15.5f, 11f)
        lineTo(21f, 7f)
        lineTo(19f, 16f)
        horizontalLineTo(5f)
        close()
        moveTo(5f, 18f)
        horizontalLineTo(19f)
        verticalLineTo(20f)
        horizontalLineTo(5f)
        verticalLineTo(18f)
        close()
    }
}.build()

private val DrawerCustomerVector: ImageVector = ImageVector.Builder(
    name = "DrawerCustomer",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 12f)
        curveTo(14.21f, 12f, 16f, 10.21f, 16f, 8f)
        curveTo(16f, 5.79f, 14.21f, 4f, 12f, 4f)
        curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f)
        curveTo(8f, 10.21f, 9.79f, 12f, 12f, 12f)
        close()
        moveTo(12f, 14f)
        curveTo(9.33f, 14f, 4f, 15.34f, 4f, 18f)
        verticalLineTo(20f)
        horizontalLineTo(20f)
        verticalLineTo(18f)
        curveTo(20f, 15.34f, 14.67f, 14f, 12f, 14f)
        close()
    }
}.build()

private val DrawerSettingsVector: ImageVector = ImageVector.Builder(
    name = "DrawerSettings",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(19.14f, 12.94f)
        curveTo(19.18f, 12.63f, 19.2f, 12.32f, 19.2f, 12f)
        curveTo(19.2f, 11.68f, 19.18f, 11.37f, 19.14f, 11.06f)
        lineTo(21.28f, 9.39f)
        curveTo(21.47f, 9.24f, 21.53f, 8.97f, 21.41f, 8.76f)
        lineTo(19.39f, 5.24f)
        curveTo(19.27f, 5.03f, 19.01f, 4.95f, 18.79f, 5.03f)
        lineTo(16.27f, 6.05f)
        curveTo(15.75f, 5.65f, 15.18f, 5.33f, 14.56f, 5.09f)
        lineTo(14.18f, 2.41f)
        curveTo(14.14f, 2.18f, 13.94f, 2f, 13.71f, 2f)
        horizontalLineTo(9.67f)
        curveTo(9.44f, 2f, 9.24f, 2.18f, 9.2f, 2.41f)
        lineTo(8.82f, 5.09f)
        curveTo(8.2f, 5.33f, 7.63f, 5.65f, 7.11f, 6.05f)
        lineTo(4.59f, 5.03f)
        curveTo(4.37f, 4.95f, 4.11f, 5.03f, 3.99f, 5.24f)
        lineTo(1.97f, 8.76f)
        curveTo(1.85f, 8.97f, 1.91f, 9.24f, 2.1f, 9.39f)
        lineTo(4.24f, 11.06f)
        curveTo(4.2f, 11.37f, 4.18f, 11.68f, 4.18f, 12f)
        curveTo(4.18f, 12.32f, 4.2f, 12.63f, 4.24f, 12.94f)
        lineTo(2.1f, 14.61f)
        curveTo(1.91f, 14.76f, 1.85f, 15.03f, 1.97f, 15.24f)
        lineTo(3.99f, 18.76f)
        curveTo(4.11f, 18.97f, 4.37f, 19.05f, 4.59f, 18.97f)
        lineTo(7.11f, 17.95f)
        curveTo(7.63f, 18.35f, 8.2f, 18.67f, 8.82f, 18.91f)
        lineTo(9.2f, 21.59f)
        curveTo(9.24f, 21.82f, 9.44f, 22f, 9.67f, 22f)
        horizontalLineTo(13.71f)
        curveTo(13.94f, 22f, 14.14f, 21.82f, 14.18f, 21.59f)
        lineTo(14.56f, 18.91f)
        curveTo(15.18f, 18.67f, 15.75f, 18.35f, 16.27f, 17.95f)
        lineTo(18.79f, 18.97f)
        curveTo(19.01f, 19.05f, 19.27f, 18.97f, 19.39f, 18.76f)
        lineTo(21.41f, 15.24f)
        curveTo(21.53f, 15.03f, 21.47f, 14.76f, 21.28f, 14.61f)
        lineTo(19.14f, 12.94f)
        close()
        moveTo(11.69f, 15.5f)
        curveTo(9.76f, 15.5f, 8.19f, 13.93f, 8.19f, 12f)
        curveTo(8.19f, 10.07f, 9.76f, 8.5f, 11.69f, 8.5f)
        curveTo(13.62f, 8.5f, 15.19f, 10.07f, 15.19f, 12f)
        curveTo(15.19f, 13.93f, 13.62f, 15.5f, 11.69f, 15.5f)
        close()
    }
}.build()

private val DrawerInfoVector: ImageVector = ImageVector.Builder(
    name = "DrawerInfo",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
        curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(13f, 17f)
        horizontalLineTo(11f)
        verticalLineTo(11f)
        horizontalLineTo(13f)
        verticalLineTo(17f)
        close()
        moveTo(13f, 9f)
        horizontalLineTo(11f)
        verticalLineTo(7f)
        horizontalLineTo(13f)
        verticalLineTo(9f)
        close()
    }
}.build()

/**
 * Luxury Persian Sovereign Aurum RTL Navigation Drawer wrapper.
 */
@Composable
fun LuxuryDrawer(
    drawerState: DrawerState,
    rates: MarketRates,
    customerCount: Int,
    onNavigateCustomers: () -> Unit,
    onNavigateSettings: () -> Unit,
    onCheckForUpdates: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            modifier = modifier,
            drawerContent = {
                LuxuryDrawerSheetContent(
                    rates = rates,
                    customerCount = customerCount,
                    onNavigateCustomers = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateCustomers()
                    },
                    onNavigateSettings = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateSettings()
                    },
                    onCheckForUpdates = {
                        coroutineScope.launch { drawerState.close() }
                        onCheckForUpdates()
                    },
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            },
            content = content
        )
    }
}

/**
 * Stitch Persian Sovereign Aurum Modal Drawer Sheet Content.
 */
@Composable
fun LuxuryDrawerSheetContent(
    rates: MarketRates,
    customerCount: Int,
    onNavigateCustomers: () -> Unit,
    onNavigateSettings: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 24.dp, bottomEnd = 24.dp),
        drawerContainerColor = colors.surface,
        drawerContentColor = colors.textMain,
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.surface)
        ) {
            // Header: Luxury Crest, Branding, and Live Spot Rate Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.heroCardGradient)
                    .border(
                        androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            Brush.verticalGradient(
                                listOf(colors.goldBorder, Color.Transparent)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Royal Crest Emblem Box
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.goldBorder, RoundedCornerShape(13.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = DrawerCrownVector,
                                    contentDescription = null,
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "گلدکس پرو",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = colors.textMain
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.goldContainer)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "PRO",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colors.goldPrimary
                                        )
                                    }
                                }
                                Text(
                                    text = "سامانه محاسبات و مدیریت زرگری",
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن منو",
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Live Spot Rate Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, colors.goldBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(colors.profitGreen)
                                )
                                Text(
                                    text = "نرخ زنده ۱۸ عیار:",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                            }
                            Text(
                                text = if (rates.gold18 > 0L) {
                                    " تومان"
                                } else {
                                    "در حال دریافت..."
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary
                            )
                        }
                    }
                }
            }

            // Top specular hairline gold ribbon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(colors.goldGradient)
            )

            // Middle Navigation Items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "امکانات و ابزارهای زرگری",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )

                // 1. Customer Directory
                LuxuryDrawerItem(
                    title = "مدیریت مشتریان",
                    subtitle = "دفترچه و سوابق مشتریان زرگری",
                    icon = DrawerCustomerVector,
                    badgeText = " مشتری",
                    onClick = onNavigateCustomers
                )

                // 2. Settings & Preferences
                LuxuryDrawerItem(
                    title = "تنظیمات نرم‌افزار",
                    subtitle = "پیکربندی سود، مالیات و مظنه",
                    icon = DrawerSettingsVector,
                    badgeText = null,
                    onClick = onNavigateSettings
                )

                // 3. About & Check for Updates
                LuxuryDrawerItem(
                    title = "بررسی بروزرسانی و درباره ما",
                    subtitle = "نسخه فعال، تاریخچه و دریافت آپدیت",
                    icon = DrawerInfoVector,
                    badgeText = "نسخه ۰.۸.۰",
                    onClick = onCheckForUpdates
                )
            }

            // Footer: Accreditation & Version Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceElevated)
            ) {
                HorizontalDivider(color = colors.border, thickness = 0.8.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "گلدکس پرو • نسخه ۰.۸.۰",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.goldContainer)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "ویرایش زرین",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary
                            )
                        }
                    }

                    Text(
                        text = "طراحی اختصاصی برای صنف طلا و جواهر ایران",
                        fontSize = 10.sp,
                        color = colors.textMuted
                    )

                    Text(
                        text = "Stitch Persian Sovereign Aurum Edition",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.goldSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun LuxuryDrawerItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(0.6.dp, colors.border),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.goldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = colors.textMuted
                    )
                }
            }

            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surface)
                        .border(0.5.dp, colors.goldBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.goldPrimary
                    )
                }
            }
        }
    }
}
