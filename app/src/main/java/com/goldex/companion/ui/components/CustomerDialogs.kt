package com.goldex.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.model.Customer
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.LocalGoldExColors

val CustomerIconVector: ImageVector = ImageVector.Builder(
    name = "CustomerIcon",
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

val SearchIconVector: ImageVector = ImageVector.Builder(
    name = "SearchIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(15.5f, 14f)
        horizontalLineTo(14.71f)
        lineTo(14.43f, 13.73f)
        curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
        curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
        curveTo(5.91f, 3f, 3f, 5.91f, 3f, 9.5f)
        curveTo(3f, 13.09f, 5.91f, 16f, 9.5f, 16f)
        curveTo(11.11f, 16f, 12.59f, 15.41f, 13.73f, 14.43f)
        lineTo(14f, 14.71f)
        verticalLineTo(15.5f)
        lineTo(19f, 20.49f)
        lineTo(20.49f, 19f)
        lineTo(15.5f, 14f)
        close()
        moveTo(9.5f, 14f)
        curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
        curveTo(5f, 7.01f, 7.01f, 5f, 9.5f, 5f)
        curveTo(11.99f, 5f, 14f, 7.01f, 14f, 9.5f)
        curveTo(14f, 11.99f, 11.99f, 14f, 9.5f, 14f)
        close()
    }
}.build()

@Composable
fun CustomerPickerDialog(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    onSelectCustomer: (Customer?) -> Unit,
    onAddNewCustomerClick: () -> Unit,
    onDeleteCustomer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalGoldExColors.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery) ||
            it.nationalId.contains(searchQuery)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.goldBorder),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.goldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CustomerIconVector,
                                    contentDescription = null,
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "مدیریت و انتخاب مشتری",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = "${PersianNumberFormatter.toPersianDigits(customers.size.toString())} مشتری ثبت‌شده",
                                    fontSize = 10.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = colors.textSecondary
                            )
                        }
                    }

                    // Quick Actions Row: Add New Customer & Anonymous Cash Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAddNewCustomerClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.goldPrimary,
                                contentColor = if (colors.isDark) Color(0xFF0A0B0E) else Color.White
                            ),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "مشتری جدید", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                onSelectCustomer(null)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                            border = androidx.compose.foundation.BorderStroke(0.7.dp, colors.border),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Text(text = "مشتری عمومی (نقدی)", fontSize = 11.sp)
                        }
                    }

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("جستجو بر اساس نام، شماره تلفن یا کد ملی...", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = SearchIconVector,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "پاک کردن",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceElevated,
                            unfocusedContainerColor = colors.surfaceElevated,
                            focusedBorderColor = colors.goldPrimary,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textMain,
                            unfocusedTextColor = colors.textMain,
                            cursorColor = colors.goldPrimary
                        )
                    )

                    // Customer List
                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "مشتری‌ای یافت نشد",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textMuted
                                )
                                Text(
                                    text = "جهت صدور فاکتور می‌توانید مشتری جدید ثبت کنید.",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredList, key = { it.id }) { customer ->
                                val isSelected = selectedCustomer?.id == customer.id
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) colors.goldContainer.copy(alpha = 0.35f) else colors.surfaceElevated,
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 1.dp else 0.5.dp,
                                        if (isSelected) colors.goldPrimary else colors.border
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectCustomer(customer)
                                            onDismiss()
                                        }
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
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Avatar circle with initial letter
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) colors.goldPrimary else colors.surfaceVariant)
                                                    .border(0.5.dp, colors.goldBorder, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = customer.name.firstOrNull()?.toString() ?: "م",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else colors.goldPrimary
                                                )
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = customer.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textMain
                                                )
                                                if (customer.phone.isNotBlank()) {
                                                    Text(
                                                        text = PersianNumberFormatter.toPersianDigits(customer.phone),
                                                        fontSize = 11.sp,
                                                        color = colors.textSecondary
                                                    )
                                                }
                                                if (customer.note.isNotBlank()) {
                                                    Text(
                                                        text = customer.note,
                                                        fontSize = 10.sp,
                                                        color = colors.textMuted,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onDeleteCustomer(customer.id) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف مشتری",
                                                    tint = colors.errorRed.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
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
        }
    }
}

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onSaveCustomer: (Customer) -> Unit
) {
    val colors = LocalGoldExColors.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nationalId by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.goldBorder),
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ثبت مشتری جدید",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "انصراف",
                                tint = colors.textSecondary
                            )
                        }
                    }

                    // Name (Required)
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            hasError = false
                        },
                        label = { Text("نام و نام‌خانوادگی خریدار *", fontSize = 11.sp) },
                        isError = hasError,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceElevated,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.goldPrimary,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textMain,
                            unfocusedTextColor = colors.textMain
                        )
                    )
                    if (hasError) {
                        Text(
                            text = "وارد کردن نام مشتری الزامی است",
                            fontSize = 10.sp,
                            color = colors.errorRed
                        )
                    }

                    // Phone
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = PersianNumberFormatter.toEnglishDigits(it).filter { c -> c.isDigit() || c == '+' } },
                        label = { Text("شماره همراه (اختیاری)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceElevated,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.goldPrimary,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textMain,
                            unfocusedTextColor = colors.textMain
                        )
                    )

                    // National ID
                    OutlinedTextField(
                        value = nationalId,
                        onValueChange = { nationalId = PersianNumberFormatter.toEnglishDigits(it).filter { c -> c.isDigit() } },
                        label = { Text("کد ملی / شناسه اقتصادی (اختیاری)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceElevated,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.goldPrimary,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textMain,
                            unfocusedTextColor = colors.textMain
                        )
                    )

                    // Note
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("توضیحات و یادداشت (اختیاری)", fontSize = 11.sp) },
                        singleLine = false,
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceElevated,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.goldPrimary,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textMain,
                            unfocusedTextColor = colors.textMain
                        )
                    )

                    // Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text(text = "انصراف", fontSize = 12.sp, color = colors.textSecondary)
                        }

                        Button(
                            onClick = {
                                if (name.trim().isBlank()) {
                                    hasError = true
                                } else {
                                    val customer = Customer(
                                        name = name.trim(),
                                        phone = phone.trim(),
                                        nationalId = nationalId.trim(),
                                        note = note.trim()
                                    )
                                    onSaveCustomer(customer)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.goldPrimary,
                                contentColor = if (colors.isDark) Color(0xFF0A0B0E) else Color.White
                            ),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text(text = "ثبت و انتخاب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
