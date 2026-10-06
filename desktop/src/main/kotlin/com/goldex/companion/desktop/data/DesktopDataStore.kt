package com.goldex.companion.desktop.data

import com.goldex.companion.data.*
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.Karat
import com.goldex.companion.model.WageType
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

    init {
        Files.createDirectories(directory)
        lockChannel = FileChannel.open(directory.resolve("workspace.lock"), CREATE, WRITE)
        try {
            fileLock = checkNotNull(lockChannel.tryLock()) { "پوشهٔ اطلاعات در برنامهٔ دیگری باز است" }
            document = if (Files.exists(documentPath)) {
                require(Files.size(documentPath) <= 10_000_000) { "حجم فایل اطلاعات پشتیبانی نمی‌شود" }
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
        // A failed write cannot publish a successful in-memory state.
        if (Files.exists(documentPath)) atomicWrite(backupPath, Files.readAllBytes(documentPath))
        atomicWrite(documentPath, next.toString(2).toByteArray(Charsets.UTF_8))
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
            value.optJSONObject("market")?.let(MarketJson::decode)
        }
    }
}
