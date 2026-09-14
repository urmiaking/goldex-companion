package com.goldex.companion.ui.wizard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.goldex.companion.data.AppSettings
import com.goldex.companion.ui.calculator.AppTab
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun OnboardingWizardScreen(
    currentSettings: AppSettings,
    liveGold18Price: Long,
    onFinish: (targetTab: AppTab, updatedSettings: AppSettings, initialInventory: WizardInventoryState) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    var currentStep by remember { mutableStateOf(WizardStep.INTRO) }
    var profileState by remember {
        mutableStateOf(
            WizardProfileState(
                galleryName = currentSettings.galleryName,
                managerName = currentSettings.managerName,
                unionCode = currentSettings.unionCode,
                phone = currentSettings.galleryPhone,
                address = currentSettings.galleryAddress
            )
        )
    }

    var financialState by remember {
        mutableStateOf(
            WizardFinancialState(
                profitPercent = currentSettings.defaultProfitPercent,
                isVatEnabled = true,
                vatRate = currentSettings.defaultTaxPercent
            )
        )
    }

    var inventoryState by remember { mutableStateOf(WizardInventoryState()) }

    // Intercept Back Button
    BackHandler(enabled = true) {
        when (currentStep) {
            WizardStep.INTRO -> onSkip()
            WizardStep.PROFILE -> currentStep = WizardStep.INTRO
            WizardStep.FINANCIAL_DEFAULTS -> currentStep = WizardStep.PROFILE
            WizardStep.INVENTORY -> currentStep = WizardStep.FINANCIAL_DEFAULTS
            WizardStep.COMPLETION -> currentStep = WizardStep.INVENTORY
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState.stepNumber > initialState.stepNumber) {
                        (slideInHorizontally(animationSpec = tween(350)) { -it } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(350)) { it } + fadeOut(tween(300)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(350)) { it } + fadeIn(tween(350)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(350)) { -it } + fadeOut(tween(300)))
                    }
                },
                label = "wizard_step_transition"
            ) { step ->
                when (step) {
                    WizardStep.INTRO -> {
                        WizardIntroSlides(
                            onStartWizard = { currentStep = WizardStep.PROFILE },
                            onSkipAsGuest = onSkip
                        )
                    }

                    WizardStep.PROFILE -> {
                        WizardProfileStep(
                            profileState = profileState,
                            onProfileChange = { profileState = it },
                            onNext = { currentStep = WizardStep.FINANCIAL_DEFAULTS },
                            onBack = { currentStep = WizardStep.INTRO }
                        )
                    }

                    WizardStep.FINANCIAL_DEFAULTS -> {
                        WizardFinancialStep(
                            financialState = financialState,
                            onFinancialChange = { financialState = it },
                            onNext = { currentStep = WizardStep.INVENTORY },
                            onBack = { currentStep = WizardStep.PROFILE }
                        )
                    }

                    WizardStep.INVENTORY -> {
                        WizardInventoryStep(
                            inventoryState = inventoryState,
                            onInventoryChange = { inventoryState = it },
                            liveGold18Price = liveGold18Price,
                            onNext = { currentStep = WizardStep.COMPLETION },
                            onBack = { currentStep = WizardStep.FINANCIAL_DEFAULTS }
                        )
                    }

                    WizardStep.COMPLETION -> {
                        WizardCompletionStep(
                            profileState = profileState,
                            financialState = financialState,
                            onEnterApp = { targetTab ->
                                val updatedSettings = currentSettings.copy(
                                    galleryName = profileState.galleryName,
                                    managerName = profileState.managerName,
                                    unionCode = profileState.unionCode,
                                    galleryPhone = profileState.phone,
                                    galleryAddress = profileState.address,
                                    galleryLicense = "صنف طلا و جواهر: ${profileState.unionCode}",
                                    defaultProfitPercent = financialState.profitPercent,
                                    defaultTaxPercent = if (financialState.isVatEnabled) financialState.vatRate else "0",
                                    hasCompletedOnboarding = true
                                )
                                onFinish(targetTab, updatedSettings, inventoryState)
                            },
                            onBack = { currentStep = WizardStep.INVENTORY }
                        )
                    }
                }
            }
        }
    }
}
