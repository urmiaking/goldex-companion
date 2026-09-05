package com.goldex.companion.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.data.AppSettings
import com.goldex.companion.data.ConnectionStatus
import com.goldex.companion.data.MarketRates
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.AppTab
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.goldGradient
import kotlinx.coroutines.launch

// --- Lightweight Stitch Luxury Vector Icons ---

private val DrawerCalculatorVector: ImageVector = ImageVector.Builder(
    name = "DrawerCalc",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(19f, 3f)
        horizontalLineTo(5f)
        curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
        verticalLineTo(19f)
        curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
        horizontalLineTo(19f)
        curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
        verticalLineTo(5f)
        curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
        close()
        moveTo(19f, 19f)
        horizontalLineTo(5f)
        verticalLineTo(5f)
        horizontalLineTo(19f)
        verticalLineTo(19f)
        close()
        moveTo(7f, 7f)
        horizontalLineTo(17f)
        verticalLineTo(9f)
        horizontalLineTo(7f)
        verticalLineTo(7f)
        close()
        moveTo(7f, 11f)
        horizontalLineTo(9f)
        verticalLineTo(13f)
        horizontalLineTo(7f)
        verticalLineTo(11f)
        close()
        moveTo(11f, 11f)
        horizontalLineTo(13f)
        verticalLineTo(13f)
        horizontalLineTo(11f)
        verticalLineTo(11f)
        close()
        moveTo(15f, 11f)
        horizontalLineTo(17f)
        verticalLineTo(13f)
        horizontalLineTo(15f)
        verticalLineTo(11f)
        close()
        moveTo(7f, 15f)
        horizontalLineTo(9f)
        verticalLineTo(17f)
        horizontalLineTo(7f)
        verticalLineTo(15f)
        close()
        moveTo(11f, 15f)
        horizontalLineTo(13f)
        verticalLineTo(17f)
        horizontalLineTo(11f)
        verticalLineTo(15f)
        close()
        moveTo(15f, 15f)
        horizontalLineTo(17f)
        verticalLineTo(17f)
        horizontalLineTo(15f)
        verticalLineTo(15f)
        close()
    }
}.build()

private val DrawerInvoiceDollarVector: ImageVector = ImageVector.Builder(
    name = "DrawerInvoiceDollar",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(14f, 2f)
        horizontalLineTo(6f)
        curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
        verticalLineTo(20f)
        curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
        horizontalLineTo(18f)
        curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
        verticalLineTo(8f)
        lineTo(14f, 2f)
        close()
        moveTo(13f, 9f)
        verticalLineTo(3.5f)
        lineTo(18.5f, 9f)
        horizontalLineTo(13f)
        close()
        moveTo(12f, 18f)
        curveTo(10.34f, 18f, 9f, 16.66f, 9f, 15f)
        horizontalLineTo(11f)
        curveTo(11f, 15.55f, 11.45f, 16f, 12f, 16f)
        curveTo(12.55f, 16f, 13f, 15.55f, 13f, 15f)
        curveTo(13f, 14.5f, 12.5f, 14.1f, 11.6f, 13.7f)
        curveTo(10.4f, 13.2f, 9f, 12.5f, 9f, 11f)
        curveTo(9f, 9.6f, 10.1f, 8.4f, 11.5f, 8.1f)
        verticalLineTo(7f)
        horizontalLineTo(12.5f)
        verticalLineTo(8.1f)
        curveTo(13.7f, 8.3f, 14.6f, 9.2f, 14.8f, 10.3f)
        lineTo(13f, 10.7f)
        curveTo(12.8f, 10.2f, 12.4f, 9.8f, 11.8f, 9.8f)
        curveTo(11.3f, 9.8f, 10.8f, 10.2f, 10.8f, 10.8f)
        curveTo(10.8f, 11.3f, 11.2f, 11.6f, 12.2f, 12.1f)
        curveTo(13.4f, 12.6f, 15f, 13.3f, 15f, 15f)
        curveTo(15f, 16.4f, 13.9f, 17.6f, 12.5f, 17.9f)
        verticalLineTo(19f)
        horizontalLineTo(11.5f)
        verticalLineTo(17.9f)
        curveTo(11.7f, 18f, 11.8f, 18f, 12f, 18f)
        close()
    }
}.build()

private val DrawerChartLineVector: ImageVector = ImageVector.Builder(
    name = "DrawerChartLine",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(3.5f, 18.5f)
        lineTo(10.5f, 11.5f)
        lineTo(14.5f, 15.5f)
        lineTo(20.5f, 9.5f)
        lineTo(22f, 11f)
        verticalLineTo(6f)
        horizontalLineTo(17f)
        lineTo(18.5f, 7.5f)
        lineTo(14.5f, 11.5f)
        lineTo(10.5f, 7.5f)
        lineTo(2f, 16f)
        lineTo(3.5f, 18.5f)
        close()
    }
}.build()

private val DrawerCoinBubbleVector: ImageVector = ImageVector.Builder(
    name = "DrawerCoinBubble",
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
        moveTo(12f, 20f)
        curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f)
        curveTo(4f, 7.59f, 7.59f, 4f, 12f, 4f)
        curveTo(16.41f, 4f, 20f, 7.59f, 20f, 12f)
        curveTo(20f, 16.41f, 16.41f, 20f, 12f, 20f)
        close()
        moveTo(12f, 6f)
        curveTo(8.69f, 6f, 6f, 8.69f, 6f, 12f)
        curveTo(6f, 15.31f, 8.69f, 18f, 12f, 18f)
        curveTo(15.31f, 18f, 18f, 15.31f, 18f, 12f)
        curveTo(18f, 8.69f, 15.31f, 6f, 12f, 6f)
        close()
    }
}.build()

private val DrawerBoxesVector: ImageVector = ImageVector.Builder(
    name = "DrawerBoxes",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(19f, 3f)
        horizontalLineTo(5f)
        curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
        verticalLineTo(9f)
        curveTo(3f, 9.9f, 3.6f, 10.6f, 4.4f, 10.9f)
        verticalLineTo(19f)
        curveTo(4.4f, 20.1f, 5.3f, 21f, 6.4f, 21f)
        horizontalLineTo(17.6f)
        curveTo(18.7f, 21f, 19.6f, 20.1f, 19.6f, 19f)
        verticalLineTo(10.9f)
        curveTo(20.4f, 10.6f, 21f, 9.9f, 21f, 9f)
        verticalLineTo(5f)
        curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
        close()
        moveTo(5f, 5f)
        horizontalLineTo(19f)
        verticalLineTo(9f)
        horizontalLineTo(5f)
        verticalLineTo(5f)
        close()
        moveTo(17.6f, 19f)
        horizontalLineTo(6.4f)
        verticalLineTo(11f)
        horizontalLineTo(17.6f)
        verticalLineTo(19f)
        close()
        moveTo(9f, 13f)
        horizontalLineTo(15f)
        verticalLineTo(15f)
        horizontalLineTo(9f)
        verticalLineTo(13f)
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

private val DrawerScaleVector: ImageVector = ImageVector.Builder(
    name = "DrawerScale",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 2f)
        curveTo(11.45f, 2f, 11f, 2.45f, 11f, 3f)
        verticalLineTo(4.09f)
        curveTo(7.16f, 4.56f, 4.14f, 7.64f, 4.01f, 11.5f)
        horizontalLineTo(2f)
        verticalLineTo(13.5f)
        horizontalLineTo(4.01f)
        curveTo(4.14f, 17.36f, 7.16f, 20.44f, 11f, 20.91f)
        verticalLineTo(22f)
        horizontalLineTo(13f)
        verticalLineTo(20.91f)
        curveTo(16.84f, 20.44f, 19.86f, 17.36f, 19.99f, 13.5f)
        horizontalLineTo(22f)
        verticalLineTo(11.5f)
        horizontalLineTo(19.99f)
        curveTo(19.86f, 7.64f, 16.84f, 4.56f, 13f, 4.09f)
        verticalLineTo(3f)
        curveTo(13f, 2.45f, 12.55f, 2f, 12f, 2f)
        close()
        moveTo(12f, 6f)
        curveTo(15.31f, 6f, 18f, 8.69f, 18f, 12f)
        curveTo(18f, 15.31f, 15.31f, 18f, 12f, 18f)
        curveTo(8.69f, 18f, 6f, 15.31f, 6f, 12f)
        curveTo(6f, 8.69f, 8.69f, 6f, 12f, 6f)
        close()
    }
}.build()

private val DrawerSlidersVector: ImageVector = ImageVector.Builder(
    name = "DrawerSliders",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(3f, 17f)
        verticalLineTo(19f)
        horizontalLineTo(9f)
        verticalLineTo(17f)
        horizontalLineTo(3f)
        close()
        moveTo(3f, 5f)
        verticalLineTo(7f)
        horizontalLineTo(13f)
        verticalLineTo(5f)
        horizontalLineTo(3f)
        close()
        moveTo(13f, 21f)
        verticalLineTo(19f)
        horizontalLineTo(21f)
        verticalLineTo(17f)
        horizontalLineTo(13f)
        verticalLineTo(15f)
        horizontalLineTo(11f)
        verticalLineTo(21f)
        horizontalLineTo(13f)
        close()
        moveTo(7f, 9f)
        verticalLineTo(11f)
        horizontalLineTo(3f)
        verticalLineTo(13f)
        horizontalLineTo(7f)
        verticalLineTo(15f)
        horizontalLineTo(9f)
        verticalLineTo(9f)
        horizontalLineTo(7f)
        close()
        moveTo(21f, 13f)
        verticalLineTo(11f)
        horizontalLineTo(11f)
        verticalLineTo(13f)
        horizontalLineTo(21f)
        close()
        moveTo(17f, 9f)
        horizontalLineTo(19f)
        verticalLineTo(7f)
        horizontalLineTo(21f)
        verticalLineTo(5f)
        horizontalLineTo(19f)
        verticalLineTo(3f)
        horizontalLineTo(17f)
        verticalLineTo(9f)
        close()
    }
}.build()

private val DrawerShieldVector: ImageVector = ImageVector.Builder(
    name = "DrawerShield",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 1f)
        lineTo(3f, 5f)
        verticalLineTo(11f)
        curveTo(3f, 16.55f, 6.84f, 21.74f, 12f, 23f)
        curveTo(17.16f, 21.74f, 21f, 16.55f, 21f, 11f)
        verticalLineTo(5f)
        lineTo(12f, 1f)
        close()
        moveTo(12f, 11.99f)
        horizontalLineTo(19f)
        curveTo(18.47f, 16.11f, 15.72f, 19.78f, 12f, 20.93f)
        verticalLineTo(12f)
        horizontalLineTo(5f)
        verticalLineTo(6.3f)
        lineTo(12f, 3.19f)
        verticalLineTo(11.99f)
        close()
    }
}.build()

private val DrawerCoinsVector: ImageVector = ImageVector.Builder(
    name = "DrawerCoins",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 3.79f, 2f, 6f)
        verticalLineTo(18f)
        curveTo(2f, 20.21f, 6.48f, 22f, 12f, 22f)
        curveTo(17.52f, 22f, 22f, 20.21f, 22f, 18f)
        verticalLineTo(6f)
        curveTo(22f, 3.79f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(12f, 4f)
        curveTo(16.42f, 4f, 20f, 5.34f, 20f, 6f)
        curveTo(20f, 6.66f, 16.42f, 8f, 12f, 8f)
        curveTo(7.58f, 8f, 4f, 6.66f, 4f, 6f)
        curveTo(4f, 5.34f, 7.58f, 4f, 12f, 4f)
        close()
        moveTo(20f, 10f)
        curveTo(19.24f, 10.74f, 16.89f, 12f, 12f, 12f)
        curveTo(7.11f, 12f, 4.76f, 10.74f, 4f, 10f)
        verticalLineTo(8.2f)
        curveTo(5.75f, 9.29f, 8.67f, 10f, 12f, 10f)
        curveTo(15.33f, 10f, 18.25f, 9.29f, 20f, 8.2f)
        verticalLineTo(10f)
        close()
        moveTo(20f, 14f)
        curveTo(19.24f, 14.74f, 16.89f, 16f, 12f, 16f)
        curveTo(7.11f, 16f, 4.76f, 14.74f, 4f, 14f)
        verticalLineTo(12.2f)
        curveTo(5.75f, 13.29f, 8.67f, 14f, 12f, 14f)
        curveTo(15.33f, 14f, 18.25f, 13.29f, 20f, 12.2f)
        verticalLineTo(14f)
        close()
        moveTo(20f, 18f)
        curveTo(19.24f, 18.74f, 16.89f, 20f, 12f, 20f)
        curveTo(7.11f, 20f, 4.76f, 18.74f, 4f, 18f)
        verticalLineTo(16.2f)
        curveTo(5.75f, 17.29f, 8.67f, 18f, 12f, 18f)
        curveTo(15.33f, 18f, 18.25f, 17.29f, 20f, 16.2f)
        verticalLineTo(18f)
        close()
    }
}.build()

private val DrawerTrendUpVector: ImageVector = ImageVector.Builder(
    name = "DrawerTrendUp",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(16f, 6f)
        lineTo(18.29f, 8.29f)
        lineTo(13.41f, 13.17f)
        lineTo(9.41f, 9.17f)
        lineTo(2f, 16.59f)
        lineTo(3.41f, 18f)
        lineTo(9.41f, 12f)
        lineTo(13.41f, 16f)
        lineTo(19.71f, 9.71f)
        lineTo(22f, 12f)
        verticalLineTo(6f)
        horizontalLineTo(16f)
        close()
    }
}.build()

private val DrawerHeadsetVector: ImageVector = ImageVector.Builder(
    name = "DrawerHeadset",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 1f)
        curveTo(6.48f, 1f, 2f, 5.48f, 2f, 11f)
        verticalLineTo(18f)
        curveTo(2f, 19.66f, 3.34f, 21f, 5f, 21f)
        horizontalLineTo(8f)
        verticalLineTo(13f)
        horizontalLineTo(4f)
        verticalLineTo(11f)
        curveTo(4f, 6.58f, 7.58f, 3f, 12f, 3f)
        curveTo(16.42f, 3f, 20f, 6.58f, 20f, 11f)
        verticalLineTo(13f)
        horizontalLineTo(16f)
        verticalLineTo(21f)
        horizontalLineTo(19f)
        curveTo(20.66f, 21f, 22f, 19.66f, 22f, 18f)
        verticalLineTo(11f)
        curveTo(22f, 5.48f, 17.52f, 1f, 12f, 1f)
        close()
    }
}.build()

private val DrawerLogoutVector: ImageVector = ImageVector.Builder(
    name = "DrawerLogout",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(10.09f, 15.59f)
        lineTo(11.5f, 17f)
        lineTo(16.5f, 12f)
        lineTo(11.5f, 7f)
        lineTo(10.09f, 8.41f)
        lineTo(12.67f, 11f)
        horizontalLineTo(3f)
        verticalLineTo(13f)
        horizontalLineTo(12.67f)
        lineTo(10.09f, 15.59f)
        close()
        moveTo(19f, 3f)
        horizontalLineTo(5f)
        curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
        verticalLineTo(9f)
        horizontalLineTo(5f)
        verticalLineTo(5f)
        horizontalLineTo(19f)
        verticalLineTo(19f)
        horizontalLineTo(5f)
        verticalLineTo(15f)
        horizontalLineTo(3f)
        verticalLineTo(19f)
        curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
        horizontalLineTo(19f)
        curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
        verticalLineTo(5f)
        curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
        close()
    }
}.build()

/**
 * Stitch Luxury Persian Sovereign Aurum RTL Navigation Drawer wrapper.
 */
@Deprecated("Navigation Drawer is officially replaced by MoreHubScreen in Phase 1")
@Composable
fun LuxuryDrawer(
    drawerState: DrawerState,
    rates: MarketRates,
    customerCount: Int,
    invoiceCount: Int = 0,
    selectedTab: AppTab = AppTab.HOME,
    totalGoldWeight: Double = 342.500,
    appSettings: AppSettings = AppSettings(),
    connectionStatus: ConnectionStatus = ConnectionStatus.ONLINE,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onSelectTab: (AppTab) -> Unit = {},
    onNavigateCustomers: () -> Unit,
    onNavigateInvoices: () -> Unit = {},
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
                    invoiceCount = invoiceCount,
                    selectedTab = selectedTab,
                    totalGoldWeight = totalGoldWeight,
                    appSettings = appSettings,
                    connectionStatus = connectionStatus,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onSelectTab = { tab ->
                        coroutineScope.launch { drawerState.close() }
                        onSelectTab(tab)
                    },
                    onNavigateCustomers = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateCustomers()
                    },
                    onNavigateInvoices = {
                        coroutineScope.launch { drawerState.close() }
                        onNavigateInvoices()
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
@Deprecated("Navigation Drawer is officially replaced by MoreHubScreen in Phase 1")
@Composable
fun LuxuryDrawerSheetContent(
    rates: MarketRates,
    customerCount: Int,
    invoiceCount: Int = 0,
    selectedTab: AppTab = AppTab.HOME,
    totalGoldWeight: Double = 342.500,
    appSettings: AppSettings = AppSettings(),
    connectionStatus: ConnectionStatus = ConnectionStatus.ONLINE,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onSelectTab: (AppTab) -> Unit = {},
    onNavigateCustomers: () -> Unit,
    onNavigateInvoices: () -> Unit = {},
    onNavigateSettings: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val isOnline = connectionStatus == ConnectionStatus.ONLINE
    val statusText = when (connectionStatus) {
        ConnectionStatus.ONLINE -> "نرخ زنده متصل"
        ConnectionStatus.CONNECTING -> "در حال دریافت..."
        ConnectionStatus.OFFLINE -> "آفلاین • قطع اتصال"
    }

    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 24.dp, bottomEnd = 24.dp),
        drawerContainerColor = colors.surface,
        drawerContentColor = colors.textMain,
        modifier = modifier
            .width(330.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.surface)
        ) {
            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Section: Header & Jeweler Profile Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (colors.isDark) {
                                Brush.verticalGradient(listOf(Color(0xFF1B2232), colors.surface))
                            } else {
                                Brush.verticalGradient(listOf(Color(0xFFFAF8F2), Color.White))
                            }
                        )
                        .border(
                            androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                Brush.verticalGradient(listOf(colors.goldBorder, Color.Transparent))
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Quick Status & Close Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Live Pulse Status Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isOnline) Color(0xFFECFDF5) else colors.surfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                0.7.dp,
                                if (isOnline) Color(0xFFA7F3D0) else colors.border
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .scale(if (isOnline) pulseScale else 1f)
                                        .clip(CircleShape)
                                        .background(if (isOnline) colors.profitGreen else colors.errorRed)
                                )
                                Text(
                                    text = statusText,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOnline) Color(0xFF047857) else colors.textMuted
                                )
                            }
                        }

                        // Close button
                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن منو",
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Jeweler / Merchant Profile Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Merchant Avatar with Gold Ring & Verified Check Badge
                        Box(contentAlignment = Alignment.BottomStart) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.surfaceElevated)
                                    .border(1.8.dp, colors.goldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "پروفایل زرگر",
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            // Gold Verified Checkmark Badge
                            Box(
                                modifier = Modifier
                                    .size(17.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .padding(1.5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(colors.goldPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "تایید شده",
                                        tint = if (colors.isDark) Color(0xFF0A0B0E) else Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }

                        // Jeweler Details Column
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = appSettings.galleryName.ifBlank { "حاج محمد کاظمی" },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    color = colors.textMain
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.goldContainer)
                                        .border(0.5.dp, colors.goldBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "زرگر رسمی",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.goldPrimary
                                    )
                                }
                            }

                            Text(
                                text = "${appSettings.galleryName} • ${appSettings.galleryAddress.ifBlank { "شعبه بازار بزرگ" }}",
                                fontSize = 10.5.sp,
                                color = colors.textMuted,
                                maxLines = 1
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "کد پروانه:",
                                    fontSize = 10.sp,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = PersianNumberFormatter.toPersianDigits(
                                        appSettings.galleryLicense.ifBlank { "IR-88412" }
                                    ),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldPrimary
                                )
                            }
                        }
                    }

                    // Quick Gold Balance Summary Card (موجودی طلا - دفتر کل)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (colors.isDark) colors.surfaceElevated else Color(0xFFFCF8ED),
                        border = androidx.compose.foundation.BorderStroke(
                            0.7.dp,
                            colors.goldBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 11.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.goldContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = DrawerCoinsVector,
                                        contentDescription = null,
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        text = "موجودی طلا (دفتر کل)",
                                        fontSize = 10.sp,
                                        color = colors.textMuted
                                    )
                                    Text(
                                        text = "${PersianNumberFormatter.formatWeight(totalGoldWeight)} گرم ۱۸",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.textMain
                                    )
                                }
                            }

                            // Trend badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFECFDF5),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFA7F3D0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = DrawerTrendUpVector,
                                        contentDescription = null,
                                        tint = Color(0xFF047857),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "۱.۲٪+",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }
                        }
                    }
                }

                // Categorized Navigation Sections
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // --- Group 1: معاملات و مظنه روز ---
                    DrawerSectionHeader(title = "بخش معاملات و مظنه")

                    StitchDrawerNavItem(
                        title = "ماشین‌حساب طلا و جواهر",
                        icon = DrawerCalculatorVector,
                        isActive = selectedTab == AppTab.CALCULATOR,
                        badgeText = "اصلی",
                        onClick = { onSelectTab(AppTab.CALCULATOR) }
                    )

                    StitchDrawerNavItem(
                        title = "مدیریت فاکتورهای رسمی",
                        icon = DrawerInvoiceDollarVector,
                        isActive = selectedTab == AppTab.INVOICES,
                        badgeText = if (invoiceCount > 0) "${PersianNumberFormatter.toPersianDigits(invoiceCount.toString())} فاکتور" else "بایگانی",
                        badgeIsGold = true,
                        onClick = onNavigateInvoices
                    )

                    StitchDrawerNavItem(
                        title = "تابلو زنده مظنه و آبشده",
                        icon = DrawerChartLineVector,
                        isActive = selectedTab == AppTab.RATES,
                        hasLiveDot = rates.isLive,
                        onClick = { onSelectTab(AppTab.RATES) }
                    )

                    StitchDrawerNavItem(
                        title = "رادار حباب انواع مسکوکات",
                        icon = DrawerCoinBubbleVector,
                        isActive = selectedTab == AppTab.CALCULATOR,
                        onClick = { onSelectTab(AppTab.CALCULATOR) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // --- Group 2: انبارداری و طرف‌های حساب ---
                    DrawerSectionHeader(title = "انبارداری و طرف‌های حساب")

                    StitchDrawerNavItem(
                        title = "موجودی و کارگاه ساخت",
                        icon = DrawerBoxesVector,
                        isActive = selectedTab == AppTab.CALCULATOR,
                        onClick = { onSelectTab(AppTab.CALCULATOR) }
                    )

                    StitchDrawerNavItem(
                        title = "دفترچه کیفی و مشتریان",
                        icon = DrawerCustomerVector,
                        isActive = false,
                        badgeText = if (customerCount > 0) "${PersianNumberFormatter.toPersianDigits(customerCount.toString())} مشتری" else null,
                        onClick = onNavigateCustomers
                    )

                    StitchDrawerNavItem(
                        title = "تبدیل عیار ری‌گیری",
                        icon = DrawerScaleVector,
                        isActive = selectedTab == AppTab.CALCULATOR,
                        badgeText = "دقت ۰.۰۰۱",
                        onClick = { onSelectTab(AppTab.CALCULATOR) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // --- Group 3: سیستم و پیکربندی ---
                    DrawerSectionHeader(title = "سیستم و پیکربندی")

                    StitchDrawerNavItem(
                        title = "تنظیمات و پایگاه استعلام",
                        icon = DrawerSlidersVector,
                        isActive = false,
                        badgeText = appSettings.priceSource.name,
                        onClick = onNavigateSettings
                    )

                    StitchDrawerNavItem(
                        title = "پشتیبان‌گیری امن و بروزرسانی",
                        icon = DrawerShieldVector,
                        isActive = false,
                        badgeText = "v0.9.1",
                        onClick = onCheckForUpdates
                    )

                    StitchDrawerNavItem(
                        title = if (isDarkTheme) "حالت شب (تاریک)" else "حالت روز (روشن)",
                        icon = DrawerSlidersVector,
                        isActive = false,
                        badgeText = if (isDarkTheme) "🌙 فعال" else "☀️ فعال",
                        onClick = onToggleTheme
                    )
                }
            }

            // Bottom Drawer Footer: Brand Signature & Quick Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceElevated)
            ) {
                HorizontalDivider(color = colors.border, thickness = 0.8.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Brand Signature Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.goldContainer)
                                    .border(0.6.dp, colors.goldBorder, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ق",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = colors.goldPrimary
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = "قیراط",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = colors.textMain
                                    )
                                    Text(
                                        text = "QIRAT",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = colors.goldPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "سامانه هوشمند صنف طلا و جواهر",
                                    fontSize = 9.5.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        // Version tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.surface)
                                .border(0.5.dp, colors.border, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "v0.9.1",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Quick Action Buttons (پشتیبانی صنف و خروج / تغییر تم)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoldButton(
                            text = "پشتیبانی صنف",
                            icon = DrawerHeadsetVector,
                            onClick = onNavigateSettings,
                            isSecondary = true,
                            modifier = Modifier.weight(1f).height(38.dp)
                        )

                        GoldButton(
                            text = if (isDarkTheme) "حالت روز ☀️" else "حالت شب 🌙",
                            onClick = onToggleTheme,
                            isSecondary = true,
                            modifier = Modifier.weight(1f).height(38.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF94A3B8),
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun StitchDrawerNavItem(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    badgeText: String? = null,
    badgeIsGold: Boolean = false,
    hasLiveDot: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalGoldExColors.current

    val itemBackground = if (isActive) {
        Brush.horizontalGradient(
            listOf(
                colors.goldPrimary.copy(alpha = 0.14f),
                colors.goldPrimary.copy(alpha = 0.03f)
            )
        )
    } else {
        SolidColor(Color.Transparent)
    }

    Surface(
        shape = RoundedCornerShape(11.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(itemBackground)
                .then(
                    if (isActive) {
                        Modifier.border(
                            width = 0.dp,
                            color = Color.Transparent
                        )
                    } else Modifier
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isActive) colors.goldContainer else colors.surfaceElevated
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isActive) colors.goldPrimary else colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isActive) colors.goldPrimary else colors.textMain
                    )
                }

                // Trailing Badges or Indicators
                if (hasLiveDot) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.profitGreen)
                    )
                } else if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (badgeIsGold) colors.goldContainer else colors.surfaceElevated
                            )
                            .border(
                                0.5.dp,
                                if (badgeIsGold) colors.goldBorder else colors.border,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (badgeIsGold) colors.goldPrimary else colors.textMuted
                        )
                    }
                }
            }

            // Right Gold Active Accent Bar (In RTL, start edge is on the right)
            if (isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart) // In RTL, CenterStart is the Right edge!
                        .width(3.5.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                        .background(colors.goldPrimary)
                )
            }
        }
    }
}

