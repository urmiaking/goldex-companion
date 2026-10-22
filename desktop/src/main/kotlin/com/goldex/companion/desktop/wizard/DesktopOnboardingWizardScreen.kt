package com.goldex.companion.desktop.wizard

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons


import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.goldex.companion.desktop.state.DesktopWorkspace
import com.goldex.companion.data.license.LicenseResult
import com.goldex.companion.data.license.LicenseStatus
import com.goldex.companion.desktop.ui.controls.DesktopActionButton
import com.goldex.companion.domain.onboarding.OpeningInventoryInput
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.desktop.ui.controls.LocalDesktopReduceMotion
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion

@Composable
fun DesktopOnboardingWizardScreen(
    workspace: DesktopWorkspace,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val currentSettings = workspace.state.value.settings
    val currentSpotPrice = workspace.state.value.snapshot?.rates?.gold18 ?: 0L

    var currentStep by remember { mutableStateOf(DesktopWizardStep.INTRO) }

    var profileState by remember {
        mutableStateOf(
            DesktopWizardProfileState(
                galleryName = currentSettings.galleryName,
                managerName = currentSettings.managerName,
                unionCode = currentSettings.unionCode,
                phone = currentSettings.galleryPhone,
                address = currentSettings.galleryAddress,
                logoUri = currentSettings.invoiceLogoUri,
                signatureUri = currentSettings.invoiceSignatureUri
            )
        )
    }
    var profileErrors by remember { mutableStateOf(DesktopWizardProfileErrors()) }

    var financialState by remember {
        mutableStateOf(
            DesktopWizardFinancialState(
                profitPercent = currentSettings.defaultProfitPercent,
                isVatEnabled = currentSettings.defaultTaxPercent != "0",
                vatRate = if (currentSettings.defaultTaxPercent == "0" || currentSettings.defaultTaxPercent == "9") "10" else currentSettings.defaultTaxPercent,
                baseKarat = Karat.K18
            )
        )
    }
    var financialErrors by remember { mutableStateOf(DesktopWizardFinancialErrors()) }

    var inventoryState by remember { mutableStateOf(OpeningInventoryInput()) }
    var licenseState by remember { mutableStateOf(DesktopWizardLicenseState()) }
    var isOnlineSync by remember { mutableStateOf(workspace.cloud.state.value.enabled) }
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun handleNext() {
        when (currentStep) {
            DesktopWizardStep.INTRO -> {
                currentStep = DesktopWizardStep.PROFILE
            }
            DesktopWizardStep.PROFILE -> {
                val errors = validateDesktopWizardProfile(profileState)
                profileErrors = errors
                if (!errors.hasErrors) {
                    currentStep = DesktopWizardStep.FINANCIAL
                }
            }
            DesktopWizardStep.FINANCIAL -> {
                val errors = validateDesktopWizardFinancial(financialState)
                financialErrors = errors
                if (!errors.hasErrors) {
                    currentStep = DesktopWizardStep.INVENTORY
                }
            }
            DesktopWizardStep.INVENTORY -> {
                currentStep = DesktopWizardStep.COMPLETION
            }
            DesktopWizardStep.COMPLETION -> {
                // Submit final onboarding
                isSubmitting = true
                coroutineScope.launch {
                    workspace.cloud.setEnabled(isOnlineSync)
                    try {
                        if (licenseState.choice == DesktopWizardLicenseChoice.CODE && licenseState.licenseCode.isNotBlank()) {
                            val codeResult = workspace.license.activateCode(licenseState.licenseCode)
                            if (codeResult !is LicenseResult.Success &&
                                workspace.license.getCachedInfo().status == LicenseStatus.NONE) {
                                workspace.license.activateTrial()
                            }
                        } else {
                            workspace.license.activateTrial()
                        }
                    } catch (_: Exception) {
                        if (workspace.license.getCachedInfo().status == LicenseStatus.NONE) {
                            workspace.license.activateTrial()
                        }
                    }
                    val cleanProfit = PersianNumberFormatter.toEnglishDigits(financialState.profitPercent).ifBlank { "7" }
                val cleanVat = if (financialState.isVatEnabled) {
                    PersianNumberFormatter.toEnglishDigits(financialState.vatRate).ifBlank { "10" }
                } else "0"

                val updatedSettings = currentSettings.copy(
                    galleryName = profileState.galleryName.trim(),
                    managerName = profileState.managerName.trim(),
                    unionCode = profileState.unionCode.trim(),
                    galleryPhone = profileState.phone.trim(),
                    galleryAddress = profileState.address.trim(),
                    invoiceLogoUri = profileState.logoUri,
                    invoiceSignatureUri = profileState.signatureUri,
                    defaultProfitPercent = cleanProfit,
                    defaultTaxPercent = cleanVat,
                    hasCompletedOnboarding = true
                )

                    val success = workspace.completeWizard(updatedSettings, inventoryState)
                    isSubmitting = false
                    if (success) {
                        onClose()
                    }
                }
            }
        }
    }

    fun handlePrevious() {
        when (currentStep) {
            DesktopWizardStep.INTRO -> onClose()
            DesktopWizardStep.PROFILE -> currentStep = DesktopWizardStep.INTRO
            DesktopWizardStep.FINANCIAL -> currentStep = DesktopWizardStep.PROFILE
            DesktopWizardStep.INVENTORY -> currentStep = DesktopWizardStep.FINANCIAL
            DesktopWizardStep.COMPLETION -> currentStep = DesktopWizardStep.INVENTORY
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .testTag("desktop-onboarding-wizard-screen"),
            color = colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Bar with Step Indicator & Exit
                if (currentStep == DesktopWizardStep.INTRO) {
                    DesktopWizardIntroTopBar(onClose = onClose)
                } else {
                    DesktopWizardStepHeader(
                        currentStep = currentStep,
                        onStepClick = { targetStep ->
                            if (targetStep.stepNumber < currentStep.stepNumber) {
                                currentStep = targetStep
                            }
                        },
                        onClose = onClose
                    )
                }

                // Main Content Pane
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val reduceMotion = LocalDesktopReduceMotion.current
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (reduceMotion) fadeIn(tween(0)).togetherWith(fadeOut(tween(0)))
                            else if (targetState.stepNumber > initialState.stepNumber) {
                                (slideInHorizontally(animationSpec = tween(300, easing = LuxuryMotion.StandardEasing)) { -it } + fadeIn(tween(250)))
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(260, easing = LuxuryMotion.AccelerationEasing)) { it } + fadeOut(tween(200)))
                            } else {
                                (slideInHorizontally(animationSpec = tween(300, easing = LuxuryMotion.StandardEasing)) { it } + fadeIn(tween(250)))
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(260, easing = LuxuryMotion.AccelerationEasing)) { -it } + fadeOut(tween(200)))
                            }
                        },
                        label = "wizard_step_content"
                    ) { step ->
                        when (step) {
                            DesktopWizardStep.INTRO -> {
                                DesktopWizardIntroPane(
                                    onStartConfiguration = { currentStep = DesktopWizardStep.PROFILE },
                                    autoAdvance = !reduceMotion
                                )
                            }
                            DesktopWizardStep.PROFILE -> {
                                DesktopWizardProfilePane(
                                    profileState = profileState,
                                    onProfileChange = { profileState = it },
                                    profileErrors = profileErrors,
                                    showErrors = profileErrors.hasErrors,
                                    dataDirectory = workspace.dataDirectory
                                )
                            }
                            DesktopWizardStep.FINANCIAL -> {
                                DesktopWizardFinancialPane(
                                    financialState = financialState,
                                    onFinancialChange = { financialState = it },
                                    financialErrors = financialErrors,
                                    showErrors = financialErrors.hasErrors
                                )
                            }
                            DesktopWizardStep.INVENTORY -> {
                                DesktopWizardInventoryPane(
                                    inventoryState = inventoryState,
                                    onInventoryChange = { inventoryState = it },
                                    liveGold18Price = currentSpotPrice
                                )
                            }
                            DesktopWizardStep.COMPLETION -> {
                                DesktopWizardCompletionPane(
                                    profileState = profileState,
                                    financialState = financialState,
                                    inventoryState = inventoryState,
                                    licenseState = licenseState,
                                    onLicenseChange = { licenseState = it },
                                    currentSpotPrice = currentSpotPrice,
                                    onNavigateToStep = { targetStep -> currentStep = targetStep },
                                    isOnlineSync = isOnlineSync,
                                    onOnlineSyncChange = { isOnlineSync = it }
                                )
                            }
                        }
                    }
                }

                // Bottom Navigation Action Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface,
                    border = BorderStroke(0.8.dp, colors.goldBorder),
                    shadowElevation = if (colors.isDark) 0.dp else 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // RTL Right Child (first child in Row): Secondary / Back / Cancel
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentStep != DesktopWizardStep.INTRO) {
                                DesktopActionButton(
                                    text = "مرحله قبل",
                                    onClick = { handlePrevious() },
                                    icon = Icons.Outlined.ChevronRight,
                                    isSecondary = true,
                                    enabled = !isSubmitting,
                                    modifier = Modifier.testTag("wizard-prev-btn")
                                )
                            }
                        }

                        // RTL Left Child (second child in Row): Primary / Next / Finish
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentStep == DesktopWizardStep.COMPLETION) {
                                DesktopActionButton(
                                    text = "تأیید نهایی و افتتاح دوره",
                                    onClick = { handleNext() },
                                    trailingIcon = Icons.Outlined.CheckCircle,
                                    isSecondary = false,
                                    isLoading = isSubmitting,
                                    modifier = Modifier.testTag("wizard-finish-btn")
                                )
                            } else if (currentStep == DesktopWizardStep.INTRO) {
                                DesktopActionButton(
                                    text = "شروع راه‌اندازی و ثبت مشخصات گالری",
                                    onClick = { handleNext() },
                                    trailingIcon = Icons.Outlined.ChevronLeft,
                                    isSecondary = false,
                                    modifier = Modifier.testTag("wizard-next-btn")
                                )
                            } else {
                                DesktopActionButton(
                                    text = "مرحله بعد",
                                    onClick = { handleNext() },
                                    trailingIcon = Icons.Outlined.ChevronLeft,
                                    isSecondary = false,
                                    modifier = Modifier.testTag("wizard-next-btn")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
