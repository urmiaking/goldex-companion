package com.goldex.companion.data

import android.content.Context
import android.content.SharedPreferences
import com.goldex.companion.model.Customer
import com.goldex.companion.model.LedgerDirection
import com.goldex.companion.model.LedgerEntryType
import com.goldex.companion.model.LedgerTransaction

class CustomerRepository(context: Context) : CustomerStore {
    private val prefs: SharedPreferences = context.getSharedPreferences("goldex_customers_prefs", Context.MODE_PRIVATE)
    private val txPrefs: SharedPreferences = context.getSharedPreferences("goldex_ledger_transactions_prefs", Context.MODE_PRIVATE)

    override fun getCustomers(): List<Customer> {
        val json = prefs.getString("customers_json", null)
        if (json.isNullOrBlank()) {
            val defaults = listOf(
                Customer(
                    id = "cust_sarraf",
                    name = "حاج محمود صراف",
                    phone = "۰۹۱۲۱۱۱۸۸۹۹",
                    nationalId = "۰۰۱۲۳۴۵۶۷۸",
                    role = "بنکدار شمش و آبشده",
                    cityOrMarket = "بازار بزرگ تهران",
                    accountCode = "۴۰۲۹۱",
                    goldDebtGrams = 254.320,
                    cashDebtTomans = 45_000_000L,
                    isVerified = true,
                    lastActivityTime = "۲ ساعت پیش",
                    note = "همکار تایید شده و بنکدار رسمی صنف طلا و جواهر تهران"
                ),
                Customer(
                    id = "cust_aria",
                    name = "کارگاه زرگری آریا",
                    phone = "۰۹۱۴۲۲۲۳۳۴۴",
                    nationalId = "۱۳۷۰۹۸۷۶۵۴",
                    role = "کارگاه النگو و دستبند ریخته‌گری",
                    cityOrMarket = "بازار تاریخی تبریز",
                    accountCode = "۱۰۴۸۲",
                    goldDebtGrams = -64.200,
                    cashDebtTomans = -12_500_000L,
                    isVerified = true,
                    lastActivityTime = "دیروز",
                    note = "تولیدکننده تخصصی سرویس و النگوهای سبک کارتیه"
                ),
                Customer(
                    id = "cust_ghaem",
                    name = "جواهری قائم (برادران رضایی)",
                    phone = "۰۹۱۲۷۷۷۶۶۵۵",
                    nationalId = "۰۰۹۸۷۶۵۴۳۲",
                    role = "فروشگاه لوکس طلا و برلیان",
                    cityOrMarket = "مجتمع میلاد نور تهران",
                    accountCode = "۳۰۵۷۱",
                    goldDebtGrams = 118.750,
                    cashDebtTomans = 85_000_000L,
                    isVerified = true,
                    lastActivityTime = "۳ روز پیش",
                    note = "تهاتر هفتگی شمش با مصنوعات النگو و ست برلیان"
                ),
                Customer(
                    id = "cust_negin",
                    name = "بنکداری نگین مشهد (حاج قاسم)",
                    phone = "۰۹۱۵۵۵۵۴۴۳۳",
                    nationalId = "۰۹۴۱۲۳۴۵۶۷",
                    role = "کیفی و بنکدار طلا",
                    cityOrMarket = "بازار رضا مشهد",
                    accountCode = "۵۰۱۹۴",
                    goldDebtGrams = 0.0,
                    cashDebtTomans = 0L,
                    isVerified = false,
                    lastActivityTime = "۱ هفته پیش",
                    note = "تسویه منظم نقدی و تهاتر شمش ۲۴ عیار بدون مانده معوق"
                ),
                Customer(
                    id = "cust_default_1",
                    name = "حاج احمد کریمی",
                    phone = "۰۹۱۲۳۴۵۶۷۸۹",
                    nationalId = "۰۰۱۲۳۴۵۶۷۸",
                    role = "بنکدار و همکار بازار",
                    cityOrMarket = "بازار بزرگ تهران",
                    accountCode = "۲۰۱۴۵",
                    goldDebtGrams = 32.450,
                    cashDebtTomans = 18_000_000L,
                    isVerified = true,
                    lastActivityTime = "۵ روز پیش",
                    note = "مشتری قدیمی بازار تهران"
                ),
                Customer(
                    id = "cust_default_2",
                    name = "خانم مهندس صادقی",
                    phone = "۰۹۱۴۱۱۱۱۲۲۳",
                    nationalId = "۱۳۷۰۹۸۷۶۵۴",
                    role = "مشتری عادی",
                    cityOrMarket = "سعادت آباد تهران",
                    accountCode = "۶۰۳۱۸",
                    goldDebtGrams = 0.0,
                    cashDebtTomans = 0L,
                    isVerified = true,
                    lastActivityTime = "۲ هفته پیش",
                    note = "سفارش ست و نیم‌ست عروس"
                )
            )
            saveCustomers(defaults)
            seedInitialTransactions()
            return defaults
        }
        return PersistenceJsonCodecs.decodeCustomers(json)
    }

    override fun addCustomer(customer: Customer) {
        val list = getCustomers().toMutableList()
        list.add(0, customer)
        saveCustomers(list)
    }

    override fun updateCustomer(customer: Customer) {
        val list = getCustomers().map { if (it.id == customer.id) customer else it }
        saveCustomers(list)
    }

    override fun deleteCustomer(id: String) {
        val list = getCustomers().filter { it.id != id }
        saveCustomers(list)
    }

    override fun getTransactions(customerId: String): List<LedgerTransaction> {
        val json = txPrefs.getString("transactions_json", null)
        if (json.isNullOrBlank()) {
            seedInitialTransactions()
            val seededJson = txPrefs.getString("transactions_json", null)
            return PersistenceJsonCodecs.decodeLedgerTransactions(seededJson).filter { it.customerId == customerId }
        }
        val all = PersistenceJsonCodecs.decodeLedgerTransactions(json)
        return all.filter { it.customerId == customerId }
    }

    override fun addTransaction(transaction: LedgerTransaction) {
        val json = txPrefs.getString("transactions_json", null)
        val all = (PersistenceJsonCodecs.decodeLedgerTransactions(json)).toMutableList()
        all.add(0, transaction)
        txPrefs.edit().putString("transactions_json", PersistenceJsonCodecs.encodeLedgerTransactions(all)).apply()
    }

    override fun updateTransaction(transaction: LedgerTransaction) {
        val json = txPrefs.getString("transactions_json", null)
        val all = PersistenceJsonCodecs.decodeLedgerTransactions(json).map {
            if (it.id == transaction.id) transaction else it
        }
        txPrefs.edit().putString("transactions_json", PersistenceJsonCodecs.encodeLedgerTransactions(all)).apply()
    }

    override fun deleteTransaction(id: String) {
        val json = txPrefs.getString("transactions_json", null)
        val all = PersistenceJsonCodecs.decodeLedgerTransactions(json).filter { it.id != id }
        txPrefs.edit().putString("transactions_json", PersistenceJsonCodecs.encodeLedgerTransactions(all)).apply()
    }

    private fun seedInitialTransactions() {
        val existing = txPrefs.getString("transactions_json", null)
        if (!existing.isNullOrBlank()) return

        val seed = listOf(
            LedgerTransaction(
                id = "tx_1",
                customerId = "cust_sarraf",
                documentNumber = "۹۸۴۱",
                title = "فروش دستبند کارتیه و سرویس مارکیز",
                dateTime = "۱۲ شهریور ۱۴۰۳ • ساعت ۱۱:۴۵",
                type = LedgerEntryType.GOLD_WEIGHT,
                direction = LedgerDirection.PAY,
                goldCategory = "مصنوعات",
                scaleWeightGrams = 18.500,
                karat = 750,
                equivalent750WeightGrams = 18.500,
                tagBadge = "فاکتور رسمی",
                note = "فی مظنه پایه طلا: ۱۸٬۵۶۰٬۰۰۰ تومان",
                resultingGoldBalance = 254.320,
                resultingCashBalance = 45_000_000L
            ),
            LedgerTransaction(
                id = "tx_2",
                customerId = "cust_sarraf",
                documentNumber = "۹۸۰۳",
                title = "دریافت طلای آبشده انگ‌دار",
                dateTime = "۸ شهریور ۱۴۰۳ • ساعت ۱۶:۲۰",
                type = LedgerEntryType.GOLD_WEIGHT,
                direction = LedgerDirection.RECEIVE,
                goldCategory = "آبشده",
                scaleWeightGrams = 50.000,
                karat = 745,
                equivalent750WeightGrams = 49.660,
                angNumber = "۸۷۲",
                labName = "آزمایشگاه پارس",
                tagBadge = "آزمایشگاه پارس",
                note = "وزن ۵۰٫۰۰۰ گرم • عیار ۷۴۵ • انگ ۸۷۲",
                resultingGoldBalance = 235.820,
                resultingCashBalance = 45_000_000L
            ),
            LedgerTransaction(
                id = "tx_3",
                customerId = "cust_sarraf",
                documentNumber = "۹۷۵۵",
                title = "واریز حواله ساتنا / پایا",
                dateTime = "۵ شهریور ۱۴۰۳ • ساعت ۱۰:۱۵",
                type = LedgerEntryType.CASH_RIAL,
                direction = LedgerDirection.RECEIVE,
                amountTomans = 80_000_000L,
                paymentMethod = "حواله بانکی / پایا",
                destinationBank = "بانک تجارت",
                trackingCode = "SAT-9812700431",
                tagBadge = "بانک تجارت",
                note = "کد رهگیری شبا: SAT-9812700431",
                resultingGoldBalance = 285.480,
                resultingCashBalance = 45_000_000L
            ),
            LedgerTransaction(
                id = "tx_4",
                customerId = "cust_sarraf",
                documentNumber = "۹۶۴۰",
                title = "خرید شمش ۲۴ عیار (تهاتر وزنی)",
                dateTime = "۲۸ مرداد ۱۴۰۳ • ساعت ۱۸:۰۰",
                type = LedgerEntryType.GOLD_WEIGHT,
                direction = LedgerDirection.RECEIVE,
                goldCategory = "سکه و شمش",
                scaleWeightGrams = 31.100,
                karat = 999,
                equivalent750WeightGrams = 41.425,
                tagBadge = "تهاتر ارزی",
                note = "شمش ۱ انس سوئیسی پمپ با تحویل فیزیکی در خزانه",
                resultingGoldBalance = 285.480,
                resultingCashBalance = 125_000_000L
            )
        )
        txPrefs.edit().putString("transactions_json", PersistenceJsonCodecs.encodeLedgerTransactions(seed)).apply()
    }

    private fun saveCustomers(customers: List<Customer>) {
        prefs.edit().putString("customers_json", PersistenceJsonCodecs.encodeCustomers(customers)).apply()
    }
}

