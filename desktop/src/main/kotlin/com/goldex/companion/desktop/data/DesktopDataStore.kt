package com.goldex.companion.desktop.data

import com.goldex.companion.data.*
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.Karat
import com.goldex.companion.model.WageType
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.StockAdjustment
import com.goldex.companion.model.StockAdjustmentType
import com.goldex.companion.presentation.inventory.InventoryForm
import com.goldex.companion.domain.inventory.InventoryStockPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.*
import java.nio.file.StandardOpenOption.*

/** One local writer, versioned documents and atomic replacement. Never edits Android storage. */
class DesktopDataStore(val directory: Path) : PortfolioStore, SettingsStore, AutoCloseable {
    val documentPath: Path = directory.resolve("workspace.json")
    val backupPath: Path = directory.resolve("workspace.previous.json")
    private val lockChannel: FileChannel
    private val fileLock: FileLock
    private var document: JSONObject
    private val mutableSettings: MutableStateFlow<AppSettings>
    override val settings get() = mutableSettings.asStateFlow()
    // InventoryStore and PortfolioStore intentionally have different getItems return types.
    val inventory: InventoryStore = object : InventoryStore {
        override fun getItems() = inventoryItems()
        override fun addItem(item: InventoryItem) = saveInventory(item, false)
        override fun updateItem(item: InventoryItem) = saveInventory(item, true)
        override fun deleteItem(id: String) = removeInventory(id)
        override fun getAdjustments() = inventoryAdjustments()
        override fun adjustStock(adjustment: StockAdjustment) = commitAdjustment(adjustment)
    }

    init {
        Files.createDirectories(directory)
        lockChannel = FileChannel.open(directory.resolve("workspace.lock"), CREATE, WRITE)
        try {
            fileLock = checkNotNull(lockChannel.tryLock()) { "پوشهٔ اطلاعات در برنامهٔ دیگری باز است" }
            document = if (Files.exists(documentPath)) {
                require(Files.size(documentPath) <= 100_000_000) { "حجم فایل اطلاعات پشتیبانی نمی‌شود" }
                JSONObject(Files.readString(documentPath))
            } else JSONObject().put("schemaVersion", 1).put("portfolio", JSONArray()).put("settings", JSONObject())
            validate(document)
            mutableSettings = MutableStateFlow(readSettings(document))
        } catch (failure: Exception) {
            lockChannel.close()
            throw IllegalStateException("اطلاعات قابل بازکردن نیست؛ فایل اصلی و پشتیبان حفظ شده‌اند", failure)
        }
    }

    @Synchronized override fun getItems(): List<PortfolioItem> =
        PersistenceJsonCodecs.decodePortfolioItems(document.getJSONArray("portfolio").toString()).toList()

    @Synchronized override fun addItem(item: PortfolioItem) {
        val next = cloneDocument()
        val old = next.getJSONArray("portfolio")
        val encoded = JSONArray(PersistenceJsonCodecs.encodePortfolioItems(listOf(item))).getJSONObject(0)
        val replacement = JSONArray()
        var found = false
        for (index in 0 until old.length()) {
            val record = old.getJSONObject(index)
            if (record.getString("id") == item.id) {
                merge(record, encoded)
                replacement.put(record)
                found = true
            } else replacement.put(record)
        }
        if (!found) replacement.put(encoded)
        next.put("portfolio", replacement)
        commit(next)
    }

    @Synchronized override fun deleteItem(id: String) {
        val next = cloneDocument()
        val old = next.getJSONArray("portfolio")
        next.put("portfolio", JSONArray((0 until old.length()).map { old.getJSONObject(it) }.filter { it.getString("id") != id }))
        commit(next)
    }

    @Synchronized override fun loadSettings(): AppSettings = mutableSettings.value

    @Synchronized private fun inventoryItems() = PersistenceJsonCodecs.decodeInventoryItems((document.optJSONArray("inventory") ?: JSONArray()).toString()).toList()
    @Synchronized private fun inventoryAdjustments() = PersistenceJsonCodecs.decodeStockAdjustments((document.optJSONArray("stockAdjustments") ?: JSONArray()).toString()).toList()
    @Synchronized private fun saveInventory(item: InventoryItem, mustExist: Boolean) {
        val next = cloneDocument()
        val old = next.optJSONArray("inventory") ?: JSONArray()
        require((0 until old.length()).none { val record = old.getJSONObject(it); record.getString("id") != item.id && record.getString("code").equals(item.code, true) }) { "کد کالا قبلاً ثبت شده است" }
        val encoded = JSONArray(PersistenceJsonCodecs.encodeInventoryItems(listOf(item))).getJSONObject(0)
        val records = JSONArray()
        var found = false
        for (index in 0 until old.length()) {
            val record = old.getJSONObject(index)
            if (record.getString("id") == item.id) {
                require(mustExist)
                require(record.getInt("quantity") == item.quantity) { "تغییر موجودی باید از طریق ثبت گردش انجام شود" }
                merge(record, encoded); found = true
            }
            records.put(record)
        }
        require(!mustExist || found) { "کالا پیدا نشد" }
        if (!found) records.put(encoded)
        commit(next.put("inventory", records))
    }
    @Synchronized private fun removeInventory(id: String) {
        val next = cloneDocument()
        val old = next.optJSONArray("inventory") ?: JSONArray()
        require((0 until old.length()).any { old.getJSONObject(it).getString("id") == id })
        next.put("inventory", JSONArray((0 until old.length()).map { old.getJSONObject(it) }.filter { it.getString("id") != id }))
        // Stock history is an audit record and survives removal of the product.
        commit(next)
    }
    @Synchronized private fun commitAdjustment(adjustment: StockAdjustment) {
        val next = cloneDocument()
        val history = next.optJSONArray("stockAdjustments") ?: JSONArray()
        val existing = inventoryAdjustments().firstOrNull { it.id == adjustment.id }
        if (existing != null) { require(existing == adjustment) { "شناسهٔ گردش تکراری است" }; return }
        val old = next.optJSONArray("inventory") ?: JSONArray()
        val index = (0 until old.length()).firstOrNull { old.getJSONObject(it).getString("id") == adjustment.itemId }
            ?: error("کالا پیدا نشد")
        val item = inventoryItems().first { it.id == adjustment.itemId }
        require(item.title == adjustment.itemTitle)
        val quantity = InventoryStockPolicy.adjustedQuantity(item, adjustment.type, adjustment.quantityChange)
        old.getJSONObject(index).put("quantity", quantity)
        history.put(JSONArray(PersistenceJsonCodecs.encodeStockAdjustments(listOf(adjustment))).getJSONObject(0))
        commit(next.put("inventory", old).put("stockAdjustments", history))
    }
    @Synchronized fun loadInventoryVisible(): Boolean = document.optBoolean("inventoryVisible", false)
    @Synchronized fun saveInventoryVisible(visible: Boolean) { commit(cloneDocument().put("inventoryVisible", visible)) }

    @Synchronized override fun saveSettings(newSettings: AppSettings) {
        val next = cloneDocument()
        merge(next.getJSONObject("settings"), JSONObject(PersistenceJsonCodecs.encodeSettings(newSettings)))
        commit(next)
        mutableSettings.value = newSettings
    }

    @Synchronized override fun loadDarkTheme(): Boolean = document.optBoolean("darkTheme", false)
    @Synchronized override fun saveDarkTheme(enabled: Boolean) { commit(cloneDocument().put("darkTheme", enabled)) }
    @Synchronized fun loadReduceMotion(): Boolean = document.optBoolean("reduceMotion", false)
    @Synchronized fun saveReduceMotion(enabled: Boolean) { commit(cloneDocument().put("reduceMotion", enabled)) }

    @Synchronized fun cachedMarket(): MarketSnapshot? = document.optJSONObject("market")?.let(MarketJson::decode)
    @Synchronized fun saveMarket(snapshot: MarketSnapshot) { commit(cloneDocument().put("market", MarketJson.encode(snapshot))) }

    /** Export is CREATE_NEW: selecting an existing name cannot overwrite a backup. */
    @Synchronized fun exportBackup(target: Path) {
        require(target.toAbsolutePath().normalize() != documentPath.toAbsolutePath().normalize())
        FileChannel.open(target, CREATE_NEW, WRITE).use { writeFully(it, document.toString(2).toByteArray(Charsets.UTF_8)); it.force(true) }
    }

    private fun cloneDocument() = JSONObject(document.toString())
    private fun readSettings(value: JSONObject) = PersistenceJsonCodecs.decodeSettings(value.getJSONObject("settings").toString())

    private fun commit(next: JSONObject) {
        validate(next)
        val bytes = next.toString(2).toByteArray(Charsets.UTF_8)
        require(bytes.size <= 100_000_000) { "حجم اطلاعات از سقف مجاز بیشتر است" }
        // A failed write cannot publish a successful in-memory state.
        if (Files.exists(documentPath)) atomicWrite(backupPath, Files.readAllBytes(documentPath))
        atomicWrite(documentPath, bytes)
        document = next
    }

    private fun atomicWrite(target: Path, bytes: ByteArray) {
        val temporary = Files.createTempFile(directory, "workspace-", ".pending")
        try {
            FileChannel.open(temporary, WRITE, TRUNCATE_EXISTING).use { writeFully(it, bytes); it.force(true) }
            // No non-atomic fallback: preserve the existing file on unsupported filesystems.
            Files.move(temporary, target, ATOMIC_MOVE, REPLACE_EXISTING)
        } finally { Files.deleteIfExists(temporary) }
    }

    override fun close() { fileLock.release(); lockChannel.close() }

    companion object {
        fun defaultDirectory(): Path = Path.of(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "Qirato", "Desktop")
        private fun writeFully(channel: FileChannel, bytes: ByteArray) {
            val buffer = ByteBuffer.wrap(bytes)
            while (buffer.hasRemaining()) channel.write(buffer)
        }
        private fun merge(target: JSONObject, source: JSONObject) { source.keySet().forEach { target.put(it, source.get(it)) } }

        fun validate(value: JSONObject) {
            require(value.get("schemaVersion").toString() == "1") { "نسخهٔ فایل اطلاعات پشتیبانی نمی‌شود" }
            val settings = value.getJSONObject("settings")
            for ((field, names) in listOf("priceSource" to PriceSource.values().map { it.name }, "defaultWageType" to WageType.values().map { it.name })) {
                require(!settings.has(field) || settings.getString(field) in names) { "تنظیم ناشناخته در فایل" }
            }
            val decodedSettings = PersistenceJsonCodecs.decodeSettings(settings.toString())
            listOf(decodedSettings.defaultProfitPercent, decodedSettings.defaultTaxPercent).forEach {
                require(it.toDoubleOrNull()?.let { number -> number.isFinite() && number in 0.0..100.0 } == true)
            }
            val portfolio = value.getJSONArray("portfolio")
            require(portfolio.length() <= 10_000) { "تعداد دارایی‌ها پشتیبانی نمی‌شود" }
            val ids = mutableSetOf<String>()
            for (index in 0 until portfolio.length()) {
                val record = portfolio.getJSONObject(index)
                require(record.getString("id").isNotBlank() && ids.add(record.getString("id"))) { "شناسهٔ تکراری یا نامعتبر" }
                require(record.getString("title").isNotBlank())
                require(record.getString("category") in PortfolioCategory.values().map { it.name })
                require(!record.has("karat") || record.getString("karat") in Karat.values().map { it.name })
                require(!record.has("coinType") || record.isNull("coinType") || record.optString("coinType").isBlank() || record.getString("coinType") in CoinType.values().map { it.name })
                val cost = record.opt("purchasePriceTotal")?.toString() ?: "0"
                require(cost.toLongOrNull()?.let { it in 0..1_000_000_000_000_000L } == true)
                if (record.getString("category") == "GOLD") {
                    val weight = record.getDouble("weightGrams")
                    require(weight.isFinite() && weight > 0 && weight <= 1_000_000)
                } else require((record.opt("quantity")?.toString() ?: "1").toIntOrNull()?.let { it in 1..1_000_000 } == true)
            }
            require(PersistenceJsonCodecs.decodePortfolioItems(portfolio.toString()).size == portfolio.length())
            val inventory = value.optJSONArray("inventory") ?: JSONArray().also { require(!value.has("inventory")) }
            require(inventory.length() <= 10_000)
            val inventoryIds = mutableSetOf<String>()
            val codes = mutableSetOf<String>()
            for (index in 0 until inventory.length()) {
                val record = inventory.getJSONObject(index)
                require(record.getString("id").isNotBlank() && inventoryIds.add(record.getString("id")))
                require(record.getString("code").isNotBlank() && codes.add(record.getString("code").lowercase()))
                require(record.getString("category") in InventoryCategory.values().filter { it != InventoryCategory.ALL }.map { it.name })
                require(record.getString("karat") in Karat.values().map { it.name })
                require(record.getString("wageType") in WageType.values().map { it.name })
                val item = PersistenceJsonCodecs.decodeInventoryItems(JSONArray().put(record).toString()).single()
                require(InventoryForm.validate(InventoryForm.from(item)).isEmpty())
                // Do not accept codec defaults for malformed numeric persisted fields.
                for (field in listOf("grossWeightGrams", "stoneWeightGrams", "wageValue", "profitPercent", "taxPercent")) require(record.get(field) is Number && record.getDouble(field).isFinite())
                for (field in listOf("quantity", "customKaratValue", "createdAt")) require(record.get(field).toString().toLongOrNull() != null)
                require(item.imageUrl.length <= 300_000)
                if (item.imageUrl.isNotBlank()) require(InventoryPhoto.isValid(item.imageUrl))
            }
            require(PersistenceJsonCodecs.decodeInventoryItems(inventory.toString()).size == inventory.length())
            val adjustments = value.optJSONArray("stockAdjustments") ?: JSONArray().also { require(!value.has("stockAdjustments")) }
            require(adjustments.length() <= 100_000)
            val movementIds = mutableSetOf<String>()
            for (index in 0 until adjustments.length()) {
                val record = adjustments.getJSONObject(index)
                require(record.getString("id").isNotBlank() && movementIds.add(record.getString("id")))
                require(record.getString("itemId").isNotBlank() && record.getString("itemTitle").isNotBlank())
                require(record.getString("type") in StockAdjustmentType.values().map { it.name })
                require(record.get("quantityChange").toString().toIntOrNull()?.let { it in 1..1_000_000 } == true)
                require(record.get("weightGrams") is Number && record.getDouble("weightGrams").let { it.isFinite() && it in 0.0..1_000_000_000_000.0 })
                require(record.get("timestamp").toString().toLongOrNull()?.let { it >= 0 } == true)
                require(record.getString("reason").isNotBlank() && record.getString("reason").length <= 200)
                require(record.optString("note").length <= 1000)
            }
            require(PersistenceJsonCodecs.decodeStockAdjustments(adjustments.toString()).size == adjustments.length())
            value.optJSONObject("market")?.let(MarketJson::decode)
        }
    }
}
