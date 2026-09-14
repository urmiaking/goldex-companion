package com.goldex.companion.ui.wizard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun WizardProfileStep(
    profileState: WizardProfileState,
    onProfileChange: (WizardProfileState) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step Header Stepper
        WizardStepHeader(currentStep = WizardStep.PROFILE)

        // Section Narrative Title
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.goldPrimary)
                )
                Text(
                    text = "مشخصات بنکداری و گالری طلا",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
            }
            Text(
                text = "این مشخصات در سربرگ فاکتورهای رسمی، اسناد معاملات آبشده و پیش‌فاکتورهای چاپی مشتریان درج خواهند شد.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textSecondary,
                lineHeight = 18.sp
            )
        }

        // Logo Upload Box Card
        LuxuryCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.goldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = WizardStorefront,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "نشان تجاری و آرم واحد صنفی",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )

                Text(
                    text = "نمایش خودکار آرم طلافروشی در سربرگ و مهر رسمی اسناد",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textMuted
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = WizardCheckCircle,
                            contentDescription = null,
                            tint = colors.profitGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "نشان پیش‌فرض زرگری فعال است",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.profitGreen
                        )
                    }
                }
            }
        }

        // Form Fields
        LuxuryCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gallery Name
            GoldInputField(
                value = profileState.galleryName,
                onValueChange = { onProfileChange(profileState.copy(galleryName = it)) },
                label = "نام تجاری طلافروشی / بنکداری *",
                leadingIcon = {
                    Icon(
                        imageVector = WizardDomain,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                keyboardType = KeyboardType.Text,
                useThousandsSeparator = false,
                modifier = Modifier.fillMaxWidth()
            )

            // Manager Name
            GoldInputField(
                value = profileState.managerName,
                onValueChange = { onProfileChange(profileState.copy(managerName = it)) },
                label = "نام و نام خانوادگی مدیر مسئول (صاحب پروانه) *",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                keyboardType = KeyboardType.Text,
                useThousandsSeparator = false,
                modifier = Modifier.fillMaxWidth()
            )

            // Union License Code
            GoldInputField(
                value = profileState.unionCode,
                onValueChange = { onProfileChange(profileState.copy(unionCode = it)) },
                label = "شماره پروانه صنف طلا و جواهر *",
                leadingIcon = {
                    Icon(
                        imageVector = WizardSecurity,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingText = "اتحادیه",
                keyboardType = KeyboardType.Text,
                useThousandsSeparator = false,
                modifier = Modifier.fillMaxWidth()
            )

            // Phone
            GoldInputField(
                value = profileState.phone,
                onValueChange = { onProfileChange(profileState.copy(phone = it)) },
                label = "شماره تماس واحد تجاری *",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                keyboardType = KeyboardType.Phone,
                useThousandsSeparator = false,
                modifier = Modifier.fillMaxWidth()
            )

            // Address
            GoldInputField(
                value = profileState.address,
                onValueChange = { onProfileChange(profileState.copy(address = it)) },
                label = "نشانی واحد صنفی (جهت درج در فاکتور رسمی) *",
                leadingIcon = {
                    Icon(
                        imageVector = WizardApartment,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                keyboardType = KeyboardType.Text,
                useThousandsSeparator = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Navigation Action Row (RTL: Secondary/Back on Right, Primary/Next on Left)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // First Child in Row -> Right side in RTL
            GoldButton(
                text = "بازگشت",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                isSecondary = true,
                onClick = onBack,
                modifier = Modifier.weight(1f)
            )

            // Second Child in Row -> Left side in RTL
            GoldButton(
                text = "تأیید و گام بعدی",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onNext,
                modifier = Modifier.weight(2f)
            )
        }
    }
}
