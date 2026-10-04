package com.goldex.companion.ui.main

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goldex.companion.data.MarketRates
import com.goldex.companion.ui.calculator.AppTab
import com.goldex.companion.ui.components.LiveRatesTicker

/** Every animated destination owns its scroll constraints, including an outgoing lazy list. */
@Composable
internal fun MainDestinationViewport(
    destination: AppTab,
    scrollState: ScrollState,
    rates: MarketRates,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().then(
            if (destination == AppTab.INVOICES) Modifier else Modifier.verticalScroll(scrollState)
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LiveRatesTicker(rates = rates)
        content()
        if (destination != AppTab.INVOICES) Spacer(modifier = Modifier.height(78.dp))
    }
}
