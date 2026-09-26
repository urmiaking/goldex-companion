# ADR 0001: انتقال لایه ماندگاری داده به دیتابیس رابطه‌ای (Room/KMP) و معماری چندسکویی (Multiplatform)

## وضعیت (Status)
**پیشنهاد شده (Proposed)** / نقشه راه معماری (Architectural Blueprint)

## بستر و انگیزه (Context)
پروژه «قیراط» (GoldEx Companion) در حال حاضر در فاز گذار از MVP به سمت پروداکشن واقعی و پایدار قرار دارد.
در وضعیت فعلی:
1. داده‌های اصلی برنامه (مشتریان، فاکتورهای تهاتری، تراکنش‌های دفتر حساب، موجودی انبار، پورتفولیو و تنظیمات) به صورت رشته‌های متنی JSON درون `SharedPreferences` ذخیره و بازیابی می‌شوند.
2. خواندن و نوشتن از طریق انکود/دیکود دسته‌جمعی کل آرایه‌ها انجام می‌شود که با افزایش حجم تراکنش‌ها و اقلام، مصرف رم (RAM) و لگ‌های رابط کاربری (I/O روی ترد اصلی) را به همراه خواهد داشت.
3. در حال حاضر امکان تراکنش‌های امن و اتمیک (ACID) بین فاکتورها و دفتر معین وجود ندارد (نوشتن همزمان در دو فایل SharedPreferences جداگانه با ریسک ناهماهنگی در زمان کرش مواجه است).
4. پروژه به زودی علاوه بر اندروید، نیازمند توسعه روی پلتفرم‌های **iOS**، **ویندوز (Desktop)** و **وب (Web)** خواهد بود.

## اصول معماری هدف (Core Architectural Principles)
1. **Separation of Concerns (SoC)**: تفکیک دقیق لایه‌ها (UI، Domain، Data/Infrastructure).
2. **SOLID Principles**:
   - **SRP**: کلاس‌های انتتی دیتابیس جدا از مدل‌های دامنه کسب‌وکار (Domain Models) هستند.
   - **OCP**: منطق کسب‌وکار روی اینترفیس‌ها کار می‌کند و افزودن پلتفرم وب یا ویندوز، کدهای محاسباتی و بیزنس لاجیک را تغییر نمی‌دهد.
   - **LSP**: پیاده‌سازی‌های مختلف مخازن داده (Room، SQLite، حافظه تستی) رفتار یکسانی از دیدگاه قرارداد ارائه می‌دهند.
   - **ISP**: تفکیک اینترفیس‌های بزرگ به اینترفیس‌های خرد و تخصصی.
   - **DIP**: لایه‌های بالا (Use Cases و ViewModels) به ابسترکشن‌ها وابسته هستند، نه به درایور دیتابیس یا اندروید.
3. **Ports & Adapters (Hexagonal Architecture)**: قراردادهای مخزن داده (Repository Ports) در لایه دامنه تعریف شده و آداپتورهای دیتابیس (Room Adapters) در لایه زیرساخت پیاده می‌شوند.
4. **تضمین عدم تخریب داده (Zero Data Loss)**: مهاجرت از SharedPreferences به دیتابیس از طریق یک فرآیند خودکار و اتمیک با حفظ داده‌های قدیمی انجام می‌پذیرد.

## ساختار لایه‌بندی چندسکویی (Target Multiplatform Layers)

```text
┌─────────────────────────────────────────────────────────────┐
│                   Presentation Layer                        │
│   Compose Multiplatform (Android, Desktop/Win, iOS, Web)   │
│             ViewModels (StateFlow, UiState)                 │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                           │
│             (Pure Kotlin - 100% Agnostic)                   │
│   - Domain Models (Customer, BarterInvoice, Karat, Money)   │
│   - Use Cases (GoldCalculations, LedgerSync, Deletion)      │
│   - Repository Contracts / Ports (Flow, suspend)            │
└──────────────────────────────▲──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 Data / Infrastructure Layer                 │
│   - Database Adapters: Room KMP / SQLite / SQLDelight       │
│   - DAOs & Database Entities (CustomerEntity, etc.)         │
│   - Mappers (Entity <-> Domain Model)                       │
│   - Migration Worker (SharedPreferences JSON -> SQLite)     │
│   - Network Adapters (Ktor Client / Multi-provider HTTP)   │
└─────────────────────────────────────────────────────────────┘
```

## تصمیمات کلیدی (Key Decisions)

1. **مدل‌های دامنه (Domain Entities) مستقل از دیتابیس هستند**:
   هیچ انوتیشن دیتابیسی (`@Entity`, `@PrimaryKey`, `@ColumnInfo`) نباید وارد مدل‌های دامین شود. لایه Data انتتی‌های اختصاصی خود را داشته و از طریق Mappers نگاشت دوطرفه انجام می‌دهد.
2. **قراردادهای ریپازیتوری غیرهمگام (Reactive & Asynchronous)**:
   متدهای همگام و بلاک‌کننده (مانند `fun getCustomers(): List<Customer>`) به `fun observeCustomers(): Flow<List<Customer>>` و توابع `suspend` تبدیل می‌شوند.
3. **موتور دیتابیس پیشنهادی**:
   - برای فاز تولید اندروید و سپس Desktop/iOS: استفاده از **Room Multiplatform (نسخه 2.7+)** که اکنون رسماً از KMP پشتیبانی می‌کند.
   - برای وب: اتصال از طریق معماری Ports به یک لایه ذخیره‌سازی ابری/REST یا IndexedDB بدون تغییر در لایه Domain.
4. **فرآیند تضمین سلامت مهاجرت داده**:
   در زمان اولین اجرای نسخه جدید، یک `DataMigrationManager` بررسی می‌کند که آیا انتقال انجام شده یا خیر. در صورت نیاز، کل داده‌های JSON موجود در SharedPreferences با استفاده از `PersistenceJsonCodecs` دیکود شده و درون یک تراکنش اتمیک SQLite درج می‌شوند و فایل‌های قبلی به عنوان نسخه پشتیبان امن حفظ می‌گردند.
