package com.goldex.companion.ui.wizard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.goldex.companion.data.AppSettings
import com.goldex.companion.ui.calculator.AppTab
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun OnboardingWizardScreen(
    currentSettings: AppSettings,
    liveGold18Price: Long,
    onFinish: (targetTab: AppTab, updatedSettings: AppSettings, initialInventory: WizardInventoryState, licenseState: WizardLicenseState) -> Unit,
    onValidateLicense: (choice: WizardLicenseChoice, code: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, onSuccess, _ -> onSuccess() },
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
    var licenseState by remember { mutableStateOf(WizardLicenseState()) }
    var isValidating by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun finishWizard(targetTab: AppTab) {
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
        onFinish(targetTab, updatedSettings, inventoryState, licenseState)
    }

    fun requestFinishWithValidation(targetTab: AppTab) {
        if (isValidating) return
        validationError = null

        when (licenseState.choice) {
            WizardLicenseChoice.TRIAL -> {
                isValidating = true
                onValidateLicense(
                    WizardLicenseChoice.TRIAL,
                    "",
                    {
                        isValidating = false
                        finishWizard(targetTab)
                    },
                    { error ->
                        isValidating = false
                        validationError = error
                    }
                )
            }
            WizardLicenseChoice.CODE -> {
                val code = licenseState.licenseCode.trim().uppercase()
                if (code.length < 5) {
                    validationError = "لطفاً کد اشتراک معتبر را وارد کنید."
                    return
                }
                isValidating = true
                onValidateLicense(
                    WizardLicenseChoice.CODE,
                    code,
                    {
                        isValidating = false
                        finishWizard(targetTab)
                    },
                    { error ->
                        isValidating = false
                        validationError = error
                    }
                )
            }
        }
    }

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
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. Fixed Sticky Stepper Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                if (currentStep == WizardStep.INTRO) {
                    WizardIntroTopBar()
                } else {
                    WizardStepHeader(currentStep = currentStep)
                }
            }

            // 2. Scrollable Middle Body with Slide and Fade Transition
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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
                            WizardIntroContent()
                        }

                        WizardStep.PROFILE -> {
                            WizardProfileContent(
                                profileState = profileState,
                                onProfileChange = { profileState = it }
                            )
                        }

                        WizardStep.FINANCIAL_DEFAULTS -> {
                            WizardFinancialContent(
                                financialState = financialState,
                                onFinancialChange = { financialState = it }
                            )
                        }

                        WizardStep.INVENTORY -> {
                            WizardInventoryContent(
                                inventoryState = inventoryState,
                                onInventoryChange = { inventoryState = it },
                                liveGold18Price = liveGold18Price
                            )
                        }

                        WizardStep.COMPLETION -> {
                            WizardCompletionContent(
                                profileState = profileState,
                                financialState = financialState,
                                licenseState = licenseState,
                                isValidating = isValidating,
                                validationError = validationError,
                                onLicenseStateChange = {
                                    licenseState = it
                                    validationError = null
                                },
                                onEnterApp = { targetTab -> requestFinishWithValidation(targetTab) }
                            )
                        }
                    }
                }
            }

            // 3. Fixed Sticky Footer Navigation (RTL: Secondary/Back on Right, Primary/Next on Left)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.surface,
                border = BorderStroke(0.6.dp, colors.border),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (currentStep) {
                        WizardStep.INTRO -> {
                            // Secondary: Right side in RTL (first in Row)
                            GoldButton(
                                text = "ورود مهمان",
                                isSecondary = true,
                                onClick = onSkip,
                                modifier = Modifier.weight(1f)
                            )

                            // Primary: Left side in RTL (second in Row)
                            GoldButton(
                                text = "شروع و ثبت مشخصات",
                                trailingIcon = WizardArrowLeft,
                                onClick = { currentStep = WizardStep.PROFILE },
                                modifier = Modifier.weight(1.8f)
                            )
                        }

                        WizardStep.PROFILE -> {
                            GoldButton(
                                text = "بازگشت",
                                icon = WizardArrowRight,
                                isSecondary = true,
                                onClick = { currentStep = WizardStep.INTRO },
                                modifier = Modifier.weight(1f)
                            )

                            GoldButton(
                                text = "تأیید و گام بعدی",
                                trailingIcon = WizardArrowLeft,
                                onClick = { currentStep = WizardStep.FINANCIAL_DEFAULTS },
                                modifier = Modifier.weight(2f)
                            )
                        }

                        WizardStep.FINANCIAL_DEFAULTS -> {
                            GoldButton(
                                text = "بازگشت",
                                icon = WizardArrowRight,
                                isSecondary = true,
                                onClick = { currentStep = WizardStep.PROFILE },
                                modifier = Modifier.weight(1f)
                            )

                            GoldButton(
                                text = "تأیید و گام بعدی",
                                trailingIcon = WizardArrowLeft,
                                onClick = { currentStep = WizardStep.INVENTORY },
                                modifier = Modifier.weight(2f)
                            )
                        }

                        WizardStep.INVENTORY -> {
                            GoldButton(
                                text = "بازگشت",
                                icon = WizardArrowRight,
                                isSecondary = true,
                                onClick = { currentStep = WizardStep.FINANCIAL_DEFAULTS },
                                modifier = Modifier.weight(1f)
                            )

                            GoldButton(
                                text = "تأیید و گام بعدی",
                                trailingIcon = WizardArrowLeft,
                                onClick = { currentStep = WizardStep.COMPLETION },
                                modifier = Modifier.weight(2f)
                            )
                        }

                        WizardStep.COMPLETION -> {
                            GoldButton(
                                text = "بازگشت",
                                icon = WizardArrowRight,
                                isSecondary = true,
                                enabled = !isValidating,
                                onClick = { currentStep = WizardStep.INVENTORY },
                                modifier = Modifier.weight(1f)
                            )

                            GoldButton(
                                text = if (isValidating) "در حال اعتبارسنجی..." else "ورود به داشبورد قیراط",
                                trailingIcon = if (isValidating) null else WizardArrowLeft,
                                isLoading = isValidating,
                                enabled = !isValidating,
                                onClick = { requestFinishWithValidation(AppTab.HOME) },
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }
            }
        }
    }
}
