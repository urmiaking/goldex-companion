# قانون چینش دکمه‌های تأیید و انصراف در دیالوگ‌ها و مودال‌ها (Dialog & Modal Button Invariant)

## ۱. اصل بنیادین (Core Invariant)
در تمامی دیالوگ‌ها، پنجره‌های پاپ‌آپ، باتم‌شیت‌ها و مودال‌هایی که دارای دو دکمه عمل هستند (مانند: «ذخیره تغییرات / ثبت» و «انصراف / بستن»)، به دلیل چیدمان راست‌به‌چپ (RTL) برنامه، ترتیب قرارگیری دکمه‌ها در ردیف (`Row`) باید اکیداً به شرح زیر باشد:

1. **دکمه فرعی / انصراف (Cancel / Secondary Action)**:
   - **همیشه در سمت راست** قرار می‌گیرد (یعنی اولین فرزند در `Row`).
   - استایل: دکمه ثانویه (`isSecondary = true`).

2. **دکمه اصلی / ذخیره یا تایید (Primary Action / Save / Confirm)**:
   - **همیشه در سمت چپ** قرار می‌گیرد (یعنی دومین فرزند در `Row`).
   - استایل: دکمه طلایی شاخص (`isSecondary = false`).

## ۲. الگوی استاندارد در Jetpack Compose

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
) {
    // ۱. دکمه انصراف (در چینش RTL در سمت راست نمایش داده می‌شود)
    GoldButton(
        text = "انصراف",
        onClick = onDismiss,
        isSecondary = true,
        modifier = Modifier.weight(1f)
    )

    // ۲. دکمه ذخیره / ثبت (در چینش RTL در سمت چپ نمایش داده می‌شود)
    GoldButton(
        text = "ذخیره تغییرات",
        onClick = onSave,
        isSecondary = false,
        icon = Icons.Default.Check,
        modifier = Modifier.weight(1.5f)
    )
}
```

## ۳. دامنه شمول
این قاعده شامل تمامی کامپوننت‌های زیر و تمامی موارد جدیدی که در فازهای آتی پیاده‌سازی می‌شوند می‌گردد:
- مودال‌های تنظیمات و پروفایل زرگر (`JewelerProfileModal`، `TaxProfitModal`، `PriceSourceModal`)
- دیالوگ‌های مدیریت طرف‌حساب‌ها (`AddCustomerDialog`، `EditCustomerDialog`)
- دیالوگ‌های سفارشی ماشین‌حساب طلا (سود سفارشی، مالیات سفارشی)
- مودال‌های افزودن اقلام فاکتور تهاتر در فاز ۳
