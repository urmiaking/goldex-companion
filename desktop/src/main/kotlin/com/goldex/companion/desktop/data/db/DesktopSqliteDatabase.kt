package com.goldex.companion.desktop.data.db

import com.goldex.companion.data.*
import com.goldex.companion.data.sync.SyncJson
import com.goldex.companion.data.sync.SyncUnitOfWork
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.sync.DesktopCloudException
import com.goldex.companion.domain.inventory.InventoryStockPolicy
import com.goldex.companion.domain.onboarding.OnboardingCheckpoint
import com.goldex.companion.model.*
import com.goldex.companion.presentation.inventory.InventoryForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.UUID

data class DesktopSyncCheckpoint(
    val id: String = "main",
    val workspaceId: String = "",
    val cursor: Long = 0,
    val writerEpoch: Long = 0,
    val nextSequence: Long = 1,
    val lastSuccessAt: Long = 0,
    val uploadSnapshotId: String = "",
    val initialized: Boolean = false
)

data class DesktopSyncMetadata(
    val type: String,
    val id: String,
    val workspaceId: String = "",
    val version: Long = 0,
    val localVersion: Long = 0,
    val payload: String? = null,
    val deleted: Boolean = false
)

data class DesktopSyncOutbox(
    val operationId: String,
    val sequence: Long,
    val workspaceId: String = "",
    val payload: String,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
)

data class DesktopSyncStaging(
    val snapshotId: String,
    val page: Int,
    val payload: String
)

data class DesktopSyncConflict(
    val operationId: String,
    val localPayload: String,
    val remotePayload: String,
    val code: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Robust relational SQLite database engine for Windows Desktop (Qirato).
 * Provides ACID transactional guarantees, foreign key enforcement, WAL mode,
 * and high-speed indexed persistence implementing the shared repository contracts.
 */
class DesktopSqliteDatabase(val dbFile: Path) : AutoCloseable, SyncUnitOfWork,
    PortfolioStore, SettingsStore, OnboardingCheckpoint {

    private val connection: Connection
    private val inTransaction = ThreadLocal.withInitial { false }
    private val mutableSettings: MutableStateFlow<AppSettings>
    override val settings: StateFlow<AppSettings> get() = mutableSettings.asStateFlow()

    private data class PendingMutation(val type: String, val id: String, val payload: JSONObject?)
    private val pendingMutations = ThreadLocal<LinkedHashMap<String, PendingMutation>?>()
    var isSyncApplying = false
    var onSyncCommit: (() -> Unit)? = null

    init {
        Files.createDirectories(dbFile.parent)
        val url = "jdbc:sqlite:${dbFile.toAbsolutePath()}"
        connection = DriverManager.getConnection(url)
        configurePragmas()
        createSchema()
        mutableSettings = MutableStateFlow(loadSettings())
    }

    private fun configurePragmas() {
        connection.createStatement().use { stmt ->
            stmt.execute("PRAGMA journal_mode = WAL;")
            stmt.execute("PRAGMA synchronous = NORMAL;")
            stmt.execute("PRAGMA foreign_keys = ON;")
            stmt.execute("PRAGMA busy_timeout = 5000;")
        }
    }

    private fun createSchema() {
        connection.createStatement().use { stmt ->
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS metadata (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                );
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS system_settings (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                );
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS portfolio_items (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    category TEXT NOT NULL,
                    weight_grams REAL NOT NULL DEFAULT 0.0,
                    karat TEXT NOT NULL DEFAULT 'K18',
                    quantity INTEGER NOT NULL DEFAULT 1,
                    coin_type TEXT,
                    purchase_price_total INTEGER NOT NULL DEFAULT 0,
                    purchase_date TEXT NOT NULL DEFAULT '',
                    payload_json TEXT NOT NULL
                );
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS inventory_items (
                    id TEXT PRIMARY KEY,
                    code TEXT NOT NULL UNIQUE,
                    title TEXT NOT NULL,
                    category TEXT NOT NULL,
                    custom_category_id TEXT,
                    location TEXT NOT NULL,
                    gross_weight_grams REAL NOT NULL DEFAULT 0.0,
                    stone_weight_grams REAL NOT NULL DEFAULT 0.0,
                    karat TEXT NOT NULL DEFAULT 'K18',
                    custom_karat_value INTEGER NOT NULL DEFAULT 750,
                    workshop TEXT NOT NULL DEFAULT '',
                    wage_type TEXT NOT NULL DEFAULT 'PERCENTAGE',
                    wage_percent REAL NOT NULL DEFAULT 0.0,
                    wage_value REAL NOT NULL DEFAULT 0.0,
                    profit_percent REAL NOT NULL DEFAULT 7.0,
                    tax_percent REAL NOT NULL DEFAULT 9.0,
                    rfid_tag TEXT NOT NULL DEFAULT '',
                    quantity INTEGER NOT NULL DEFAULT 1,
                    image_url TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL,
                    payload_json TEXT NOT NULL
                );
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS stock_adjustments (
                    id TEXT PRIMARY KEY,
                    item_id TEXT NOT NULL,
                    item_title TEXT NOT NULL,
                    type TEXT NOT NULL DEFAULT 'CHARGE',
                    quantity_change INTEGER NOT NULL DEFAULT 1,
                    weight_grams REAL NOT NULL DEFAULT 0.0,
                    reason TEXT NOT NULL,
                    note TEXT NOT NULL DEFAULT '',
                    timestamp INTEGER NOT NULL,
                    payload_json TEXT NOT NULL
                );
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS inventory_options (
                    id TEXT PRIMARY KEY,
                    type TEXT NOT NULL,
                    name TEXT NOT NULL
                );
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    phone TEXT NOT NULL DEFAULT '',
                    national_id TEXT NOT NULL DEFAULT '',
                    note TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL,
                    role TEXT NOT NULL DEFAULT 'بنکدار و همکار بازار',
                    gold_debt_grams REAL NOT NULL DEFAULT 0.0,
                    cash_debt_tomans INTEGER NOT NULL DEFAULT 0,
                    account_code TEXT NOT NULL DEFAULT '',
                    is_verified INTEGER NOT NULL DEFAULT 1,
                    city_or_market TEXT NOT NULL DEFAULT 'بازار بزرگ تهران',
                    last_activity_time TEXT NOT NULL DEFAULT 'امروز',
                    payload_json TEXT NOT NULL
                );
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS deleted_customers (
                    id TEXT PRIMARY KEY,
                    payload_json TEXT NOT NULL,
                    deleted_at INTEGER NOT NULL
                );
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ledger_transactions (
                    id TEXT PRIMARY KEY,
                    customer_id TEXT NOT NULL,
                    document_number TEXT NOT NULL DEFAULT '',
                    title TEXT NOT NULL DEFAULT '',
                    date_time TEXT NOT NULL DEFAULT '',
                    type TEXT NOT NULL DEFAULT 'GOLD_WEIGHT',
                    direction TEXT NOT NULL DEFAULT 'RECEIVE',
                    gold_category TEXT NOT NULL DEFAULT 'آبشده',
                    scale_weight_grams REAL NOT NULL DEFAULT 0.0,
                    karat INTEGER NOT NULL DEFAULT 750,
                    equivalent_750_weight_grams REAL NOT NULL DEFAULT 0.0,
                    ang_number TEXT NOT NULL DEFAULT '',
                    lab_name TEXT NOT NULL DEFAULT '',
                    amount_tomans INTEGER NOT NULL DEFAULT 0,
                    payment_method TEXT NOT NULL DEFAULT '',
                    destination_bank TEXT NOT NULL DEFAULT '',
                    tracking_code TEXT NOT NULL DEFAULT '',
                    note TEXT NOT NULL DEFAULT '',
                    tag_badge TEXT NOT NULL DEFAULT '',
                    resulting_gold_balance REAL NOT NULL DEFAULT 0.0,
                    resulting_cash_balance INTEGER NOT NULL DEFAULT 0,
                    timestamp INTEGER NOT NULL,
                    invoice_id TEXT,
                    invoice_number TEXT,
                    is_auto_generated INTEGER NOT NULL DEFAULT 0,
                    settlement_json TEXT NOT NULL DEFAULT '',
                    payload_json TEXT NOT NULL
                );
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS invoices (
                    id TEXT PRIMARY KEY,
                    invoice_number TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    payload_json TEXT NOT NULL
                );
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS barter_invoices (
                    id TEXT PRIMARY KEY,
                    invoice_number TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    customer_id TEXT,
                    spot_price_18k INTEGER NOT NULL DEFAULT 0,
                    debt_basis TEXT NOT NULL DEFAULT '',
                    sync_with_ledger INTEGER NOT NULL DEFAULT 1,
                    payload_json TEXT NOT NULL
                );
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS market_cache (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    snapshot_json TEXT NOT NULL,
                    updated_at INTEGER NOT NULL
                );
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sync_checkpoint (
                    id TEXT PRIMARY KEY,
                    workspace_id TEXT NOT NULL DEFAULT '',
                    cursor INTEGER NOT NULL DEFAULT 0,
                    writer_epoch INTEGER NOT NULL DEFAULT 0,
                    next_sequence INTEGER NOT NULL DEFAULT 1,
                    last_success_at INTEGER NOT NULL DEFAULT 0,
                    upload_snapshot_id TEXT NOT NULL DEFAULT '',
                    initialized INTEGER NOT NULL DEFAULT 0
                );
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sync_metadata (
                    type TEXT NOT NULL,
                    id TEXT NOT NULL,
                    workspace_id TEXT NOT NULL DEFAULT '',
                    version INTEGER NOT NULL DEFAULT 0,
                    local_version INTEGER NOT NULL DEFAULT 0,
                    payload_json TEXT,
                    deleted INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (type, id)
                );
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sync_outbox (
                    operation_id TEXT PRIMARY KEY,
                    sequence INTEGER NOT NULL,
                    workspace_id TEXT NOT NULL DEFAULT '',
                    payload_json TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    created_at INTEGER NOT NULL
                );
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sync_staging (
                    snapshot_id TEXT NOT NULL,
                    page INTEGER NOT NULL,
                    payload_json TEXT NOT NULL,
                    PRIMARY KEY (snapshot_id, page)
                );
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sync_conflicts (
                    operation_id TEXT PRIMARY KEY,
                    local_payload_json TEXT NOT NULL,
                    remote_payload_json TEXT NOT NULL,
                    code TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                );
            """.trimIndent())

            // Indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone);")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_customers_code ON customers(account_code);")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ledger_customer ON ledger_transactions(customer_id);")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ledger_invoice ON ledger_transactions(invoice_id);")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ledger_timestamp ON ledger_transactions(timestamp);")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_inventory_code ON inventory_items(code);")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_inventory_category ON inventory_items(category);")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_adjustments_item ON stock_adjustments(item_id);")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_options_type ON inventory_options(type);")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_barter_invoice_num ON barter_invoices(invoice_number);")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_barter_created_at ON barter_invoices(created_at);")
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sync_outbox_seq ON sync_outbox(sequence);")
        }
    }

    @Synchronized
    override fun <T> transaction(action: () -> T): T {
        if (inTransaction.get()) {
            return action()
        }
        inTransaction.set(true)
        val originalAutoCommit = connection.autoCommit
        connection.autoCommit = false
        pendingMutations.set(linkedMapOf())
        try {
            val result = action()
            val pending = pendingMutations.get()
            pendingMutations.remove()
            if (!isSyncApplying && pending != null && pending.isNotEmpty()) {
                flushMutations(pending)
            }
            connection.commit()
            runCatching { onSyncCommit?.invoke() }
            return result
        } catch (t: Throwable) {
            pendingMutations.remove()
            runCatching { connection.rollback() }
            throw t
        } finally {
            connection.autoCommit = originalAutoCommit
            inTransaction.set(false)
        }
    }

    private fun recordMutation(type: String, id: String, payload: JSONObject?) {
        if (isSyncApplying) return
        val map = pendingMutations.get()
        if (map != null) {
            map["$type:$id"] = PendingMutation(type, id, payload)
        } else {
            transaction {
                recordMutation(type, id, payload)
            }
        }
    }

    private fun flushMutations(pending: Map<String, PendingMutation>) {
        val cp = checkpoint()
        val changes = JSONArray()
        pending.values.forEach { p ->
            val old = metadata(p.type, p.id)
            val fresh = SyncJson.preserveUnknown(p.type, old?.payload?.let(::JSONObject), p.payload)
            if ((old?.payload != null && fresh != null && SyncJson.canonical(JSONObject(old.payload)) == SyncJson.canonical(fresh)) ||
                (old?.payload == null && fresh == null)) return@forEach
            val delta = SyncJson.diff(old?.payload?.let(::JSONObject), fresh)
            val version = (old?.localVersion ?: 0) + 1
            delta.put("type", p.type).put("id", p.id).put("baseVersion", version - 1).put("localVersion", version).put("localPayload", fresh ?: JSONObject.NULL)
            changes.put(delta)
            saveMetadata(DesktopSyncMetadata(p.type, p.id, cp.workspaceId, old?.version ?: 0, version, fresh?.toString(), fresh == null))
        }
        if (changes.length() == 0) return
        val operationId = UUID.randomUUID().toString()
        val payload = JSONObject().put("changes", changes).toString()
        enqueueOutbox(DesktopSyncOutbox(operationId, cp.nextSequence, cp.workspaceId, payload))
        saveCheckpoint(cp.copy(nextSequence = cp.nextSequence + 1))
    }

    // -------------------------------------------------------------------------
    // PortfolioStore Implementation
    // -------------------------------------------------------------------------

    @Synchronized
    override fun getItems(): List<PortfolioItem> {
        val list = mutableListOf<PortfolioItem>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT payload_json FROM portfolio_items ORDER BY rowid ASC").use { rs ->
                while (rs.next()) {
                    val json = rs.getString("payload_json")
                    list.addAll(PersistenceJsonCodecs.decodePortfolioItems("[$json]"))
                }
            }
        }
        return list
    }

    @Synchronized
    override fun addItem(item: PortfolioItem): Unit {
        transaction {
            val encoded = JSONArray(PersistenceJsonCodecs.encodePortfolioItems(listOf(item))).getJSONObject(0)
            connection.prepareStatement("""
                INSERT INTO portfolio_items (id, title, category, weight_grams, karat, quantity, coin_type, purchase_price_total, purchase_date, payload_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    title = excluded.title,
                    category = excluded.category,
                    weight_grams = excluded.weight_grams,
                    karat = excluded.karat,
                    quantity = excluded.quantity,
                    coin_type = excluded.coin_type,
                    purchase_price_total = excluded.purchase_price_total,
                    purchase_date = excluded.purchase_date,
                    payload_json = excluded.payload_json;
            """.trimIndent()).use { stmt ->
                stmt.setString(1, item.id)
                stmt.setString(2, item.title)
                stmt.setString(3, item.category.name)
                stmt.setDouble(4, item.weightGrams)
                stmt.setString(5, item.karat.name)
                stmt.setInt(6, item.quantity)
                stmt.setString(7, item.coinType?.name)
                stmt.setLong(8, item.purchasePriceTotal)
                stmt.setString(9, item.purchaseDate)
                stmt.setString(10, encoded.toString())
                stmt.executeUpdate()
            }
            recordMutation("portfolio", item.id, SyncJson.record(item))
        }
    }

    @Synchronized
    override fun deleteItem(id: String): Unit {
        transaction {
            connection.prepareStatement("DELETE FROM portfolio_items WHERE id = ?").use { stmt ->
                stmt.setString(1, id)
                stmt.executeUpdate()
            }
            recordMutation("portfolio", id, null)
        }
    }

    // -------------------------------------------------------------------------
    // SettingsStore & Options Implementation
    // -------------------------------------------------------------------------

    @Synchronized
    override fun loadSettings(): AppSettings {
        val json = getSystemSetting("app_settings")
        return if (json != null) {
            PersistenceJsonCodecs.decodeSettings(json)
        } else {
            AppSettings()
        }
    }

    @Synchronized
    override fun saveSettings(newSettings: AppSettings) = transaction {
        val encoded = PersistenceJsonCodecs.encodeSettings(newSettings)
        setSystemSetting("app_settings", encoded)
        mutableSettings.value = newSettings
        recordMutation("businessSettings", "business", SyncJson.business(newSettings))
    }

    @Synchronized
    override fun loadDarkTheme(): Boolean =
        getSystemSetting("dark_theme")?.toBooleanStrictOrNull() ?: false

    @Synchronized
    override fun saveDarkTheme(enabled: Boolean) = transaction {
        setSystemSetting("dark_theme", enabled.toString())
    }

    @Synchronized
    fun loadReduceMotion(): Boolean =
        getSystemSetting("reduce_motion")?.toBooleanStrictOrNull() ?: false

    @Synchronized
    fun saveReduceMotion(enabled: Boolean) = transaction {
        setSystemSetting("reduce_motion", enabled.toString())
    }

    @Synchronized
    fun loadInventoryVisible(): Boolean =
        getSystemSetting("inventory_visible")?.toBooleanStrictOrNull() ?: false

    @Synchronized
    fun saveInventoryVisible(visible: Boolean) = transaction {
        setSystemSetting("inventory_visible", visible.toString())
    }

    @Synchronized
    override fun hasSeededOpeningInventory(): Boolean =
        getSystemSetting("opening_inventory_seeded")?.toBooleanStrictOrNull() ?: false

    @Synchronized
    override fun markOpeningInventorySeeded() = transaction {
        setSystemSetting("opening_inventory_seeded", "true")
    }

    private fun getSystemSetting(key: String): String? {
        connection.prepareStatement("SELECT value FROM system_settings WHERE key = ?").use { stmt ->
            stmt.setString(1, key)
            stmt.executeQuery().use { rs ->
                return if (rs.next()) rs.getString("value") else null
            }
        }
    }

    private fun setSystemSetting(key: String, value: String) {
        connection.prepareStatement("""
            INSERT INTO system_settings (key, value) VALUES (?, ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value;
        """.trimIndent()).use { stmt ->
            stmt.setString(1, key)
            stmt.setString(2, value)
            stmt.executeUpdate()
        }
    }

    // -------------------------------------------------------------------------
    // InventoryStore & Inventory Extensions
    // -------------------------------------------------------------------------

    val inventoryStore: InventoryStore = object : InventoryStore {
        override fun getItems(): List<InventoryItem> = this@DesktopSqliteDatabase.getInventoryItems()
        override fun addItem(item: InventoryItem) { saveInventoryItem(item, mustExist = false) }
        override fun updateItem(item: InventoryItem) { saveInventoryItem(item, mustExist = true) }
        override fun deleteItem(id: String) { this@DesktopSqliteDatabase.deleteInventoryItem(id) }
        override fun getAdjustments(): List<StockAdjustment> = this@DesktopSqliteDatabase.getStockAdjustments()
        override fun adjustStock(adjustment: StockAdjustment) { this@DesktopSqliteDatabase.commitStockAdjustment(adjustment) }
    }

    @Synchronized
    fun getInventoryItems(): List<InventoryItem> {
        val list = mutableListOf<InventoryItem>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT payload_json FROM inventory_items ORDER BY rowid ASC").use { rs ->
                while (rs.next()) {
                    val json = rs.getString("payload_json")
                    list.addAll(PersistenceJsonCodecs.decodeInventoryItems("[$json]"))
                }
            }
        }
        return list
    }

    @Synchronized
    fun getStockAdjustments(): List<StockAdjustment> {
        val list = mutableListOf<StockAdjustment>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT payload_json FROM stock_adjustments ORDER BY timestamp DESC").use { rs ->
                while (rs.next()) {
                    val json = rs.getString("payload_json")
                    list.addAll(PersistenceJsonCodecs.decodeStockAdjustments("[$json]"))
                }
            }
        }
        return list
    }

    @Synchronized
    fun loadInventoryOptions(): InventoryOptions {
        val categories = mutableListOf<InventoryOption>()
        val locations = mutableListOf<InventoryOption>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT id, type, name FROM inventory_options").use { rs ->
                while (rs.next()) {
                    val opt = InventoryOption(rs.getString("id"), rs.getString("name"))
                    if (rs.getString("type") == DesktopInventoryOptions.CATEGORIES) {
                        categories.add(opt)
                    } else {
                        locations.add(opt)
                    }
                }
            }
        }

        val categoryIds = mutableMapOf<String, String>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT id, custom_category_id FROM inventory_items WHERE custom_category_id IS NOT NULL AND custom_category_id != ''").use { rs ->
                while (rs.next()) {
                    categoryIds[rs.getString("id")] = rs.getString("custom_category_id")
                }
            }
        }
        return InventoryOptions(categories, locations, categoryIds)
    }

    @Synchronized
    fun addInventoryOption(key: String, name: String): InventoryOption = transaction {
        require(key == DesktopInventoryOptions.CATEGORIES || key == DesktopInventoryOptions.LOCATIONS)
        val clean = name.trim()
        require(clean.isNotEmpty() && clean.length <= 50)
        val options = loadInventoryOptions()
        val existing = if (key == DesktopInventoryOptions.CATEGORIES) options.categories else options.locations
        existing.firstOrNull { it.name == clean }?.let { return@transaction it }
        val option = InventoryOption(UUID.randomUUID().toString(), clean)
        connection.prepareStatement("INSERT INTO inventory_options (id, type, name) VALUES (?, ?, ?)").use { stmt ->
            stmt.setString(1, option.id)
            stmt.setString(2, key)
            stmt.setString(3, option.name)
            stmt.executeUpdate()
        }
        option
    }

    @Synchronized
    fun nextInventoryCode(): String {
        val storedSequence = getSystemSetting("inventory_code_sequence")?.toIntOrNull() ?: 999_999
        return DesktopInventoryOptions.nextCode(getInventoryItems(), storedSequence)
    }

    @Synchronized
    fun getPragma(name: String): String? {
        connection.createStatement().use { stmt ->
            stmt.executeQuery("PRAGMA $name;").use { rs ->
                return if (rs.next()) rs.getString(1) else null
            }
        }
    }

    @Synchronized
    fun saveInventoryProduct(item: InventoryItem, isNew: Boolean, categoryId: String?): InventoryItem = transaction {
        val saved = if (isNew) item.copy(code = nextInventoryCode()) else item
        saveInventoryItem(
            item = saved,
            mustExist = !isNew,
            categoryId = categoryId,
            updateCategory = true,
            generatedCode = if (isNew) saved.code.toIntOrNull() else null
        )
        saved
    }

    @Synchronized
    internal fun saveInventoryItem(
        item: InventoryItem,
        mustExist: Boolean,
        categoryId: String? = null,
        updateCategory: Boolean = false,
        generatedCode: Int? = null
    ): Unit = transaction {
        if (generatedCode != null) {
            setSystemSetting("inventory_code_sequence", generatedCode.toString())
        }

        connection.prepareStatement("SELECT id FROM inventory_items WHERE LOWER(code) = LOWER(?) AND id != ?").use { stmt ->
            stmt.setString(1, item.code)
            stmt.setString(2, item.id)
            stmt.executeQuery().use { rs ->
                require(!rs.next()) { "کد کالا قبلاً ثبت شده است" }
            }
        }

        var existingPayload: JSONObject? = null
        var existingQuantity = item.quantity
        var existingCategoryId: String? = null
        connection.prepareStatement("SELECT payload_json, quantity, custom_category_id FROM inventory_items WHERE id = ?").use { stmt ->
            stmt.setString(1, item.id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    existingPayload = JSONObject(rs.getString("payload_json"))
                    existingQuantity = rs.getInt("quantity")
                    existingCategoryId = rs.getString("custom_category_id")
                }
            }
        }

        require(!mustExist || existingPayload != null) { "کالا پیدا نشد" }
        if (existingPayload != null) {
            require(existingQuantity == item.quantity) { "تغییر موجودی باید از طریق ثبت گردش انجام شود" }
        }

        val encoded = JSONArray(PersistenceJsonCodecs.encodeInventoryItems(listOf(item))).getJSONObject(0)
        existingPayload?.let { mergeJson(it, encoded) }
        val targetJson = existingPayload ?: encoded

        val finalCategoryId = if (updateCategory) categoryId else (categoryId ?: existingCategoryId)
        if (updateCategory) {
            targetJson.put("customCategoryId", finalCategoryId ?: JSONObject.NULL)
        }

        connection.prepareStatement("""
            INSERT INTO inventory_items (
                id, code, title, category, custom_category_id, location,
                gross_weight_grams, stone_weight_grams, karat, custom_karat_value,
                workshop, wage_type, wage_percent, wage_value, profit_percent,
                tax_percent, rfid_tag, quantity, image_url, created_at, payload_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                code = excluded.code,
                title = excluded.title,
                category = excluded.category,
                custom_category_id = excluded.custom_category_id,
                location = excluded.location,
                gross_weight_grams = excluded.gross_weight_grams,
                stone_weight_grams = excluded.stone_weight_grams,
                karat = excluded.karat,
                custom_karat_value = excluded.custom_karat_value,
                workshop = excluded.workshop,
                wage_type = excluded.wage_type,
                wage_percent = excluded.wage_percent,
                wage_value = excluded.wage_value,
                profit_percent = excluded.profit_percent,
                tax_percent = excluded.tax_percent,
                rfid_tag = excluded.rfid_tag,
                quantity = excluded.quantity,
                image_url = excluded.image_url,
                created_at = excluded.created_at,
                payload_json = excluded.payload_json;
        """.trimIndent()).use { stmt ->
            stmt.setString(1, item.id)
            stmt.setString(2, item.code)
            stmt.setString(3, item.title)
            stmt.setString(4, item.category.name)
            stmt.setString(5, finalCategoryId)
            stmt.setString(6, item.location)
            stmt.setDouble(7, item.grossWeightGrams)
            stmt.setDouble(8, item.stoneWeightGrams)
            stmt.setString(9, item.karat.name)
            stmt.setInt(10, item.customKaratValue)
            stmt.setString(11, item.workshop)
            stmt.setString(12, item.wageType.name)
            stmt.setDouble(13, item.wagePercent)
            stmt.setDouble(14, item.wageValue)
            stmt.setDouble(15, item.profitPercent)
            stmt.setDouble(16, item.taxPercent)
            stmt.setString(17, item.rfidTag)
            stmt.setInt(18, item.quantity)
            stmt.setString(19, item.imageUrl)
            stmt.setLong(20, item.createdAt)
            stmt.setString(21, targetJson.toString())
            stmt.executeUpdate()
        }
        recordMutation("inventory", item.id, SyncJson.record(item))
    }

    @Synchronized
    private fun deleteInventoryItem(id: String) = transaction {
        connection.prepareStatement("SELECT id FROM inventory_items WHERE id = ?").use { stmt ->
            stmt.setString(1, id)
            stmt.executeQuery().use { rs ->
                require(rs.next()) { "کالا پیدا نشد" }
            }
        }
        connection.prepareStatement("DELETE FROM inventory_items WHERE id = ?").use { stmt ->
            stmt.setString(1, id)
            stmt.executeUpdate()
        }
        recordMutation("inventory", id, null)
    }

    @Synchronized
    private fun commitStockAdjustment(adjustment: StockAdjustment) = transaction {
        connection.prepareStatement("SELECT payload_json FROM stock_adjustments WHERE id = ?").use { stmt ->
            stmt.setString(1, adjustment.id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    val existing = PersistenceJsonCodecs.decodeStockAdjustments("[${rs.getString("payload_json")}]").single()
                    require(existing == adjustment) { "شناسه گردش تکراری است" }
                    return@transaction
                }
            }
        }

        var item: InventoryItem? = null
        var itemJson: JSONObject? = null
        connection.prepareStatement("SELECT payload_json FROM inventory_items WHERE id = ?").use { stmt ->
            stmt.setString(1, adjustment.itemId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    val jsonStr = rs.getString("payload_json")
                    item = PersistenceJsonCodecs.decodeInventoryItems("[$jsonStr]").single()
                    itemJson = JSONObject(jsonStr)
                }
            }
        }
        val foundItem = checkNotNull(item) { "کالا پیدا نشد" }
        require(foundItem.title == adjustment.itemTitle)

        val newQuantity = InventoryStockPolicy.adjustedQuantity(foundItem, adjustment.type, adjustment.quantityChange)
        itemJson?.put("quantity", newQuantity)

        connection.prepareStatement("UPDATE inventory_items SET quantity = ?, payload_json = ? WHERE id = ?").use { stmt ->
            stmt.setInt(1, newQuantity)
            stmt.setString(2, itemJson.toString())
            stmt.setString(3, foundItem.id)
            stmt.executeUpdate()
        }

        val adjustmentEncoded = JSONArray(PersistenceJsonCodecs.encodeStockAdjustments(listOf(adjustment))).getJSONObject(0)
        connection.prepareStatement("""
            INSERT INTO stock_adjustments (id, item_id, item_title, type, quantity_change, weight_grams, reason, note, timestamp, payload_json)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.setString(1, adjustment.id)
            stmt.setString(2, adjustment.itemId)
            stmt.setString(3, adjustment.itemTitle)
            stmt.setString(4, adjustment.type.name)
            stmt.setInt(5, adjustment.quantityChange)
            stmt.setDouble(6, adjustment.weightGrams)
            stmt.setString(7, adjustment.reason)
            stmt.setString(8, adjustment.note)
            stmt.setLong(9, adjustment.timestamp)
            stmt.setString(10, adjustmentEncoded.toString())
            stmt.executeUpdate()
        }
        recordMutation("inventory", foundItem.id, SyncJson.record(foundItem.copy(quantity = newQuantity)))
        recordMutation("stockAdjustment", adjustment.id, SyncJson.record(adjustment))
    }

    // -------------------------------------------------------------------------
    // CustomerStore Implementation
    // -------------------------------------------------------------------------

    val customerStore: CustomerStore = object : CustomerStore {
        override fun getCustomers(): List<Customer> = this@DesktopSqliteDatabase.getCustomers()
        override fun addCustomer(customer: Customer) { this@DesktopSqliteDatabase.addCustomer(customer) }
        override fun updateCustomer(customer: Customer) { this@DesktopSqliteDatabase.updateCustomer(customer) }
        override fun deleteCustomer(id: String) { this@DesktopSqliteDatabase.deleteCustomer(id) }
        override fun getTransactions(customerId: String): List<LedgerTransaction> = this@DesktopSqliteDatabase.getTransactions(customerId)
        override fun addTransaction(transaction: LedgerTransaction) { this@DesktopSqliteDatabase.addTransaction(transaction) }
        override fun updateTransaction(transaction: LedgerTransaction) { this@DesktopSqliteDatabase.updateTransaction(transaction) }
        override fun deleteTransaction(id: String) { this@DesktopSqliteDatabase.deleteTransaction(id) }
        override fun getTransactionsByInvoiceId(invoiceId: String): List<LedgerTransaction> = this@DesktopSqliteDatabase.getTransactionsByInvoiceId(invoiceId)
        override fun getTransactionsByInvoiceIds(invoiceIds: List<String>): List<LedgerTransaction> = this@DesktopSqliteDatabase.getTransactionsByInvoiceIds(invoiceIds)
        override fun deleteTransactionsByInvoiceId(invoiceId: String) { this@DesktopSqliteDatabase.deleteTransactionsByInvoiceId(invoiceId) }
    }

    @Synchronized
    fun getCustomers(): List<Customer> {
        val list = mutableListOf<Customer>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT payload_json FROM customers ORDER BY created_at ASC").use { rs ->
                while (rs.next()) {
                    val json = rs.getString("payload_json")
                    list.addAll(PersistenceJsonCodecs.decodeCustomers("[$json]"))
                }
            }
        }
        return list
    }

    @Synchronized
    fun addCustomer(customer: Customer) = transaction {
        connection.prepareStatement("SELECT id FROM customers WHERE id = ?").use { stmt ->
            stmt.setString(1, customer.id)
            stmt.executeQuery().use { rs ->
                require(!rs.next()) { "شناسه طرف‌حساب تکراری است" }
            }
        }
        val encoded = JSONObject(PersistenceJsonCodecs.encodeCustomerString(customer))
        saveCustomerRecord(customer, encoded)
    }

    @Synchronized
    fun updateCustomer(customer: Customer) = transaction {
        var existingJson: JSONObject? = null
        connection.prepareStatement("SELECT payload_json FROM customers WHERE id = ?").use { stmt ->
            stmt.setString(1, customer.id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) existingJson = JSONObject(rs.getString("payload_json"))
            }
        }
        requireNotNull(existingJson) { "رکورد یافت نشد" }
        val fresh = JSONObject(PersistenceJsonCodecs.encodeCustomerString(customer))
        mergeJson(existingJson!!, fresh)
        saveCustomerRecord(customer, existingJson!!)
    }

    private fun saveCustomerRecord(customer: Customer, encoded: JSONObject) {
        val cleanJson = JSONObject(PersistenceJsonCodecs.encodeCustomerString(customer))
        mergeJson(cleanJson, encoded)
        connection.prepareStatement("""
            INSERT INTO customers (
                id, name, phone, national_id, note, created_at, role,
                gold_debt_grams, cash_debt_tomans, account_code, is_verified,
                city_or_market, last_activity_time, payload_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                name = excluded.name,
                phone = excluded.phone,
                national_id = excluded.national_id,
                note = excluded.note,
                created_at = excluded.created_at,
                role = excluded.role,
                gold_debt_grams = excluded.gold_debt_grams,
                cash_debt_tomans = excluded.cash_debt_tomans,
                account_code = excluded.account_code,
                is_verified = excluded.is_verified,
                city_or_market = excluded.city_or_market,
                last_activity_time = excluded.last_activity_time,
                payload_json = excluded.payload_json;
        """.trimIndent()).use { stmt ->
            stmt.setString(1, customer.id)
            stmt.setString(2, customer.name)
            stmt.setString(3, customer.phone)
            stmt.setString(4, customer.nationalId)
            stmt.setString(5, customer.note)
            stmt.setLong(6, customer.createdAt)
            stmt.setString(7, customer.role)
            stmt.setDouble(8, customer.goldDebtGrams)
            stmt.setLong(9, customer.cashDebtTomans)
            stmt.setString(10, customer.accountCode)
            stmt.setInt(11, if (customer.isVerified) 1 else 0)
            stmt.setString(12, customer.cityOrMarket)
            stmt.setString(13, customer.lastActivityTime)
            stmt.setString(14, cleanJson.toString())
            stmt.executeUpdate()
        }
        recordMutation("customer", customer.id, SyncJson.record(customer))
    }

    @Synchronized
    fun deleteCustomer(id: String) = transaction {
        var payload: String? = null
        var goldDebt = 0.0
        var cashDebt = 0L
        connection.prepareStatement("SELECT payload_json, gold_debt_grams, cash_debt_tomans FROM customers WHERE id = ?").use { stmt ->
            stmt.setString(1, id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    payload = rs.getString("payload_json")
                    goldDebt = rs.getDouble("gold_debt_grams")
                    cashDebt = rs.getLong("cash_debt_tomans")
                }
            }
        }
        if (payload == null) return@transaction
        require(kotlin.math.abs(goldDebt) <= 1e-10 && cashDebt == 0L) {
            "پیش از حذف طرف‌حساب، مانده حساب را تسویه کنید"
        }

        connection.prepareStatement("INSERT OR REPLACE INTO deleted_customers (id, payload_json, deleted_at) VALUES (?, ?, ?)").use { stmt ->
            stmt.setString(1, id)
            stmt.setString(2, payload)
            stmt.setLong(3, System.currentTimeMillis())
            stmt.executeUpdate()
        }
        connection.prepareStatement("DELETE FROM customers WHERE id = ?").use { stmt ->
            stmt.setString(1, id)
            stmt.executeUpdate()
        }
        recordMutation("customer", id, null)
    }

    @Synchronized
    fun getTransactions(customerId: String): List<LedgerTransaction> {
        val list = mutableListOf<LedgerTransaction>()
        connection.prepareStatement("""
            SELECT payload_json FROM ledger_transactions
            WHERE customer_id = ?
            ORDER BY timestamp DESC, date_time DESC
        """.trimIndent()).use { stmt ->
            stmt.setString(1, customerId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val json = rs.getString("payload_json")
                    list.addAll(PersistenceJsonCodecs.decodeLedgerTransactions("[$json]"))
                }
            }
        }
        return list
    }

    @Synchronized
    fun addTransaction(tx: LedgerTransaction) = transaction {
        connection.prepareStatement("SELECT id FROM ledger_transactions WHERE id = ?").use { stmt ->
            stmt.setString(1, tx.id)
            stmt.executeQuery().use { rs ->
                require(!rs.next()) { "شناسه سند تکراری است" }
            }
        }
        connection.prepareStatement("SELECT id FROM customers WHERE id = ?").use { stmt ->
            stmt.setString(1, tx.customerId)
            stmt.executeQuery().use { rs ->
                require(rs.next()) { "طرف‌حساب یافت نشد" }
            }
        }
        val encoded = JSONArray(PersistenceJsonCodecs.encodeLedgerTransactions(listOf(tx))).getJSONObject(0)
        saveTransactionRecord(tx, encoded)
    }

    @Synchronized
    fun updateTransaction(tx: LedgerTransaction) = transaction {
        var existingJson: JSONObject? = null
        var existingCustId: String? = null
        connection.prepareStatement("SELECT customer_id, payload_json FROM ledger_transactions WHERE id = ?").use { stmt ->
            stmt.setString(1, tx.id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    existingCustId = rs.getString("customer_id")
                    existingJson = JSONObject(rs.getString("payload_json"))
                }
            }
        }
        val oldJson = checkNotNull(existingJson) { "سند یافت نشد" }
        require(existingCustId == tx.customerId) { "طرف‌حساب سند قابل تغییر نیست" }

        val fresh = JSONArray(PersistenceJsonCodecs.encodeLedgerTransactions(listOf(tx))).getJSONObject(0)
        mergeJson(oldJson, fresh)
        saveTransactionRecord(tx, oldJson)
    }

    private fun saveTransactionRecord(tx: LedgerTransaction, encoded: JSONObject) {
        val cleanJson = JSONArray(PersistenceJsonCodecs.encodeLedgerTransactions(listOf(tx))).getJSONObject(0)
        mergeJson(cleanJson, encoded)
        connection.prepareStatement("""
            INSERT INTO ledger_transactions (
                id, customer_id, document_number, title, date_time, type, direction,
                gold_category, scale_weight_grams, karat, equivalent_750_weight_grams,
                ang_number, lab_name, amount_tomans, payment_method, destination_bank,
                tracking_code, note, tag_badge, resulting_gold_balance, resulting_cash_balance,
                timestamp, invoice_id, invoice_number, is_auto_generated, settlement_json, payload_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                customer_id = excluded.customer_id,
                document_number = excluded.document_number,
                title = excluded.title,
                date_time = excluded.date_time,
                type = excluded.type,
                direction = excluded.direction,
                gold_category = excluded.gold_category,
                scale_weight_grams = excluded.scale_weight_grams,
                karat = excluded.karat,
                equivalent_750_weight_grams = excluded.equivalent_750_weight_grams,
                ang_number = excluded.ang_number,
                lab_name = excluded.lab_name,
                amount_tomans = excluded.amount_tomans,
                payment_method = excluded.payment_method,
                destination_bank = excluded.destination_bank,
                tracking_code = excluded.tracking_code,
                note = excluded.note,
                tag_badge = excluded.tag_badge,
                resulting_gold_balance = excluded.resulting_gold_balance,
                resulting_cash_balance = excluded.resulting_cash_balance,
                timestamp = excluded.timestamp,
                invoice_id = excluded.invoice_id,
                invoice_number = excluded.invoice_number,
                is_auto_generated = excluded.is_auto_generated,
                settlement_json = excluded.settlement_json,
                payload_json = excluded.payload_json;
        """.trimIndent()).use { stmt ->
            stmt.setString(1, tx.id)
            stmt.setString(2, tx.customerId)
            stmt.setString(3, tx.documentNumber)
            stmt.setString(4, tx.title)
            stmt.setString(5, tx.dateTime)
            stmt.setString(6, tx.type.name)
            stmt.setString(7, tx.direction.name)
            stmt.setString(8, tx.goldCategory)
            stmt.setDouble(9, tx.scaleWeightGrams)
            stmt.setInt(10, tx.karat)
            stmt.setDouble(11, tx.equivalent750WeightGrams)
            stmt.setString(12, tx.angNumber)
            stmt.setString(13, tx.labName)
            stmt.setLong(14, tx.amountTomans)
            stmt.setString(15, tx.paymentMethod)
            stmt.setString(16, tx.destinationBank)
            stmt.setString(17, tx.trackingCode)
            stmt.setString(18, tx.note)
            stmt.setString(19, tx.tagBadge)
            stmt.setDouble(20, tx.resultingGoldBalance)
            stmt.setLong(21, tx.resultingCashBalance)
            stmt.setLong(22, tx.timestamp)
            stmt.setString(23, tx.invoiceId)
            stmt.setString(24, tx.invoiceNumber)
            stmt.setInt(25, if (tx.isAutoGenerated) 1 else 0)
            stmt.setString(26, tx.settlement?.let { PersistenceJsonCodecs.encodeLedgerSettlement(it) } ?: "")
            stmt.setString(27, cleanJson.toString())
            stmt.executeUpdate()
        }
        recordMutation("ledger", tx.id, SyncJson.record(tx))
    }

    @Synchronized
    fun deleteTransaction(id: String) = transaction {
        connection.prepareStatement("DELETE FROM ledger_transactions WHERE id = ?").use { stmt ->
            stmt.setString(1, id)
            stmt.executeUpdate()
        }
        recordMutation("ledger", id, null)
    }

    @Synchronized
    fun getTransactionsByInvoiceId(invoiceId: String): List<LedgerTransaction> {
        val list = mutableListOf<LedgerTransaction>()
        connection.prepareStatement("SELECT payload_json FROM ledger_transactions WHERE invoice_id = ?").use { stmt ->
            stmt.setString(1, invoiceId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val json = rs.getString("payload_json")
                    list.addAll(PersistenceJsonCodecs.decodeLedgerTransactions("[$json]"))
                }
            }
        }
        return list
    }

    @Synchronized
    fun getTransactionsByInvoiceIds(invoiceIds: List<String>): List<LedgerTransaction> {
        if (invoiceIds.isEmpty()) return emptyList()
        val unique = invoiceIds.distinct()
        val placeholders = unique.joinToString(",") { "?" }
        val list = mutableListOf<LedgerTransaction>()
        connection.prepareStatement("SELECT payload_json FROM ledger_transactions WHERE invoice_id IN ($placeholders)").use { stmt ->
            unique.forEachIndexed { index, id -> stmt.setString(index + 1, id) }
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val json = rs.getString("payload_json")
                    list.addAll(PersistenceJsonCodecs.decodeLedgerTransactions("[$json]"))
                }
            }
        }
        return list
    }

    @Synchronized
    fun deleteTransactionsByInvoiceId(invoiceId: String) = transaction {
        val txs = getTransactionsByInvoiceId(invoiceId)
        connection.prepareStatement("DELETE FROM ledger_transactions WHERE invoice_id = ?").use { stmt ->
            stmt.setString(1, invoiceId)
            stmt.executeUpdate()
        }
        txs.forEach { recordMutation("ledger", it.id, null) }
    }

    // -------------------------------------------------------------------------
    // InvoiceStore Implementation
    // -------------------------------------------------------------------------

    val invoiceStore: InvoiceStore = object : InvoiceStore {
        override fun getInvoices(): List<Invoice> = this@DesktopSqliteDatabase.getInvoices()
        override fun saveInvoice(invoice: Invoice) { this@DesktopSqliteDatabase.saveInvoice(invoice) }
        override fun deleteInvoice(id: String) { this@DesktopSqliteDatabase.deleteInvoice(id) }
        override fun getBarterInvoices(): List<BarterInvoice> = this@DesktopSqliteDatabase.getBarterInvoices()
        override fun saveBarterInvoice(invoice: BarterInvoice) { this@DesktopSqliteDatabase.saveBarterInvoice(invoice) }
        override fun deleteBarterInvoice(id: String) { this@DesktopSqliteDatabase.deleteBarterInvoice(id) }
    }

    @Synchronized
    fun getInvoices(): List<Invoice> {
        val list = mutableListOf<Invoice>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT payload_json FROM invoices ORDER BY created_at DESC").use { rs ->
                while (rs.next()) {
                    val json = rs.getString("payload_json")
                    list.addAll(PersistenceJsonCodecs.decodeInvoices("[$json]"))
                }
            }
        }
        return list
    }

    @Synchronized
    fun saveInvoice(invoice: Invoice) = transaction {
        var existingJson: JSONObject? = null
        connection.prepareStatement("SELECT payload_json FROM invoices WHERE id = ?").use { stmt ->
            stmt.setString(1, invoice.id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) existingJson = JSONObject(rs.getString("payload_json"))
            }
        }
        val encoded = JSONArray(PersistenceJsonCodecs.encodeInvoices(listOf(invoice))).getJSONObject(0)
        existingJson?.let { mergeJson(it, encoded) }
        val target = existingJson ?: encoded

        connection.prepareStatement("""
            INSERT INTO invoices (id, invoice_number, created_at, payload_json)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                invoice_number = excluded.invoice_number,
                created_at = excluded.created_at,
                payload_json = excluded.payload_json;
        """.trimIndent()).use { stmt ->
            stmt.setString(1, invoice.id)
            stmt.setString(2, invoice.invoiceNumber)
            stmt.setLong(3, invoice.createdAt)
            stmt.setString(4, target.toString())
            stmt.executeUpdate()
        }
        recordMutation("invoice", invoice.id, SyncJson.record(invoice))
    }

    @Synchronized
    fun deleteInvoice(id: String) = transaction {
        connection.prepareStatement("DELETE FROM invoices WHERE id = ?").use { stmt ->
            stmt.setString(1, id)
            stmt.executeUpdate()
        }
        recordMutation("invoice", id, null)
    }

    @Synchronized
    fun getBarterInvoices(): List<BarterInvoice> {
        val list = mutableListOf<BarterInvoice>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT payload_json FROM barter_invoices ORDER BY created_at DESC").use { rs ->
                while (rs.next()) {
                    val json = rs.getString("payload_json")
                    list.addAll(PersistenceJsonCodecs.decodeBarterInvoices("[$json]"))
                }
            }
        }
        return list
    }

    @Synchronized
    fun saveBarterInvoice(invoice: BarterInvoice) = transaction {
        var existingJson: JSONObject? = null
        connection.prepareStatement("SELECT payload_json FROM barter_invoices WHERE id = ?").use { stmt ->
            stmt.setString(1, invoice.id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) existingJson = JSONObject(rs.getString("payload_json"))
            }
        }
        val encoded = JSONArray(PersistenceJsonCodecs.encodeBarterInvoices(listOf(invoice))).getJSONObject(0)
        existingJson?.let { mergeJson(it, encoded) }
        val target = existingJson ?: encoded

        connection.prepareStatement("""
            INSERT INTO barter_invoices (id, invoice_number, created_at, customer_id, spot_price_18k, debt_basis, sync_with_ledger, payload_json)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                invoice_number = excluded.invoice_number,
                created_at = excluded.created_at,
                customer_id = excluded.customer_id,
                spot_price_18k = excluded.spot_price_18k,
                debt_basis = excluded.debt_basis,
                sync_with_ledger = excluded.sync_with_ledger,
                payload_json = excluded.payload_json;
        """.trimIndent()).use { stmt ->
            stmt.setString(1, invoice.id)
            stmt.setString(2, invoice.invoiceNumber)
            stmt.setLong(3, invoice.createdAt)
            stmt.setString(4, invoice.customer?.id)
            stmt.setLong(5, invoice.spotPrice18k)
            stmt.setString(6, invoice.debtBasis?.name ?: "")
            stmt.setInt(7, if (invoice.syncWithLedger) 1 else 0)
            stmt.setString(8, target.toString())
            stmt.executeUpdate()
        }
        recordMutation("barterInvoice", invoice.id, SyncJson.record(invoice))
    }

    @Synchronized
    fun deleteBarterInvoice(id: String) = transaction {
        connection.prepareStatement("DELETE FROM barter_invoices WHERE id = ?").use { stmt ->
            stmt.setString(1, id)
            stmt.executeUpdate()
        }
        recordMutation("barterInvoice", id, null)
    }

    // -------------------------------------------------------------------------
    // Market Cache
    // -------------------------------------------------------------------------

    @Synchronized
    fun cachedMarket(): MarketSnapshot? {
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT snapshot_json FROM market_cache WHERE id = 1").use { rs ->
                if (rs.next()) {
                    return MarketJson.decode(JSONObject(rs.getString("snapshot_json")))
                }
            }
        }
        return null
    }

    @Synchronized
    fun saveMarket(snapshot: MarketSnapshot) = transaction {
        val json = MarketJson.encode(snapshot).toString()
        connection.prepareStatement("""
            INSERT INTO market_cache (id, snapshot_json, updated_at) VALUES (1, ?, ?)
            ON CONFLICT(id) DO UPDATE SET snapshot_json = excluded.snapshot_json, updated_at = excluded.updated_at;
        """.trimIndent()).use { stmt ->
            stmt.setString(1, json)
            stmt.setLong(2, System.currentTimeMillis())
            stmt.executeUpdate()
        }
    }

    // -------------------------------------------------------------------------
    // Metadata & Migration Support
    // -------------------------------------------------------------------------

    @Synchronized
    fun getMetadata(key: String): String? {
        connection.prepareStatement("SELECT value FROM metadata WHERE key = ?").use { stmt ->
            stmt.setString(1, key)
            stmt.executeQuery().use { rs ->
                return if (rs.next()) rs.getString("value") else null
            }
        }
    }

    @Synchronized
    fun setMetadata(key: String, value: String) = transaction {
        connection.prepareStatement("""
            INSERT INTO metadata (key, value) VALUES (?, ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value;
        """.trimIndent()).use { stmt ->
            stmt.setString(1, key)
            stmt.setString(2, value)
            stmt.executeUpdate()
        }
    }

    /** Export full contents into an Android / Desktop compatible JSON document. */
    @Synchronized
    fun exportToJsonObject(): JSONObject {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("settings", JSONObject(PersistenceJsonCodecs.encodeSettings(loadSettings())))
        root.put("darkTheme", loadDarkTheme())
        root.put("reduceMotion", loadReduceMotion())
        root.put("inventoryVisible", loadInventoryVisible())
        root.put("openingInventorySeeded", hasSeededOpeningInventory())
        getSystemSetting("inventory_code_sequence")?.toIntOrNull()?.let { root.put("inventoryCodeSequence", it) }

        val portfolioArray = JSONArray()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT payload_json FROM portfolio_items ORDER BY rowid ASC").use { rs ->
                while (rs.next()) portfolioArray.put(JSONObject(rs.getString("payload_json")))
            }
        }
        root.put("portfolio", portfolioArray)

        val inventoryArray = JSONArray()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT payload_json FROM inventory_items ORDER BY rowid ASC").use { rs ->
                while (rs.next()) inventoryArray.put(JSONObject(rs.getString("payload_json")))
            }
        }
        root.put("inventory", inventoryArray)

        val adjustmentsArray = JSONArray()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT payload_json FROM stock_adjustments ORDER BY rowid ASC").use { rs ->
                while (rs.next()) adjustmentsArray.put(JSONObject(rs.getString("payload_json")))
            }
        }
        root.put("stockAdjustments", adjustmentsArray)

        val options = loadInventoryOptions()
        if (options.categories.isNotEmpty()) {
            root.put(DesktopInventoryOptions.CATEGORIES, JSONArray().apply {
                options.categories.forEach { put(JSONObject().put("id", it.id).put("name", it.name)) }
            })
        }
        if (options.locations.isNotEmpty()) {
            root.put(DesktopInventoryOptions.LOCATIONS, JSONArray().apply {
                options.locations.forEach { put(JSONObject().put("id", it.id).put("name", it.name)) }
            })
        }

        val customersArray = JSONArray()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT payload_json FROM customers ORDER BY created_at ASC").use { rs ->
                while (rs.next()) customersArray.put(JSONObject(rs.getString("payload_json")))
            }
        }
        root.put("customers", customersArray)

        val deletedCustArray = JSONArray()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT payload_json FROM deleted_customers ORDER BY deleted_at ASC").use { rs ->
                while (rs.next()) deletedCustArray.put(JSONObject(rs.getString("payload_json")))
            }
        }
        root.put("deletedCustomers", deletedCustArray)

        val ledgerArray = JSONArray()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT payload_json FROM ledger_transactions ORDER BY timestamp ASC, rowid ASC").use { rs ->
                while (rs.next()) ledgerArray.put(JSONObject(rs.getString("payload_json")))
            }
        }
        root.put("ledgerTransactions", ledgerArray)

        val invoicesArray = JSONArray()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT payload_json FROM invoices ORDER BY created_at ASC").use { rs ->
                while (rs.next()) invoicesArray.put(JSONObject(rs.getString("payload_json")))
            }
        }
        root.put("invoices", invoicesArray)

        val barterArray = JSONArray()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT payload_json FROM barter_invoices ORDER BY created_at ASC").use { rs ->
                while (rs.next()) barterArray.put(JSONObject(rs.getString("payload_json")))
            }
        }
        root.put("barterInvoices", barterArray)

        cachedMarket()?.let { root.put("market", MarketJson.encode(it)) }
        return root
    }

    // -------------------------------------------------------------------------
    // Sync Storage & Multi-Device Coordination Implementation
    // -------------------------------------------------------------------------

    @Synchronized
    fun checkpoint(): DesktopSyncCheckpoint {
        connection.prepareStatement("SELECT id, workspace_id, cursor, writer_epoch, next_sequence, last_success_at, upload_snapshot_id, initialized FROM sync_checkpoint WHERE id = 'main'").use { stmt ->
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return DesktopSyncCheckpoint(
                        id = rs.getString("id"),
                        workspaceId = rs.getString("workspace_id"),
                        cursor = rs.getLong("cursor"),
                        writerEpoch = rs.getLong("writer_epoch"),
                        nextSequence = rs.getLong("next_sequence"),
                        lastSuccessAt = rs.getLong("last_success_at"),
                        uploadSnapshotId = rs.getString("upload_snapshot_id"),
                        initialized = rs.getInt("initialized") == 1
                    )
                }
            }
        }
        val defaultCp = DesktopSyncCheckpoint()
        saveCheckpoint(defaultCp)
        return defaultCp
    }

    @Synchronized
    fun saveCheckpoint(cp: DesktopSyncCheckpoint) {
        connection.prepareStatement("""
            INSERT INTO sync_checkpoint (id, workspace_id, cursor, writer_epoch, next_sequence, last_success_at, upload_snapshot_id, initialized)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                workspace_id = excluded.workspace_id,
                cursor = excluded.cursor,
                writer_epoch = excluded.writer_epoch,
                next_sequence = excluded.next_sequence,
                last_success_at = excluded.last_success_at,
                upload_snapshot_id = excluded.upload_snapshot_id,
                initialized = excluded.initialized;
        """.trimIndent()).use { stmt ->
            stmt.setString(1, cp.id)
            stmt.setString(2, cp.workspaceId)
            stmt.setLong(3, cp.cursor)
            stmt.setLong(4, cp.writerEpoch)
            stmt.setLong(5, cp.nextSequence)
            stmt.setLong(6, cp.lastSuccessAt)
            stmt.setString(7, cp.uploadSnapshotId)
            stmt.setInt(8, if (cp.initialized) 1 else 0)
            stmt.executeUpdate()
        }
    }

    @Synchronized
    fun metadata(type: String, id: String): DesktopSyncMetadata? {
        connection.prepareStatement("SELECT type, id, workspace_id, version, local_version, payload_json, deleted FROM sync_metadata WHERE type = ? AND id = ?").use { stmt ->
            stmt.setString(1, type)
            stmt.setString(2, id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return DesktopSyncMetadata(
                        type = rs.getString("type"),
                        id = rs.getString("id"),
                        workspaceId = rs.getString("workspace_id"),
                        version = rs.getLong("version"),
                        localVersion = rs.getLong("local_version"),
                        payload = rs.getString("payload_json"),
                        deleted = rs.getInt("deleted") == 1
                    )
                }
            }
        }
        return null
    }

    @Synchronized
    fun saveMetadata(meta: DesktopSyncMetadata) {
        connection.prepareStatement("""
            INSERT INTO sync_metadata (type, id, workspace_id, version, local_version, payload_json, deleted)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(type, id) DO UPDATE SET
                workspace_id = excluded.workspace_id,
                version = excluded.version,
                local_version = excluded.local_version,
                payload_json = excluded.payload_json,
                deleted = excluded.deleted;
        """.trimIndent()).use { stmt ->
            stmt.setString(1, meta.type)
            stmt.setString(2, meta.id)
            stmt.setString(3, meta.workspaceId)
            stmt.setLong(4, meta.version)
            stmt.setLong(5, meta.localVersion)
            stmt.setString(6, meta.payload)
            stmt.setInt(7, if (meta.deleted) 1 else 0)
            stmt.executeUpdate()
        }
    }

    @Synchronized
    fun allMetadata(): List<DesktopSyncMetadata> {
        val list = mutableListOf<DesktopSyncMetadata>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT type, id, workspace_id, version, local_version, payload_json, deleted FROM sync_metadata").use { rs ->
                while (rs.next()) {
                    list.add(DesktopSyncMetadata(
                        type = rs.getString("type"),
                        id = rs.getString("id"),
                        workspaceId = rs.getString("workspace_id"),
                        version = rs.getLong("version"),
                        localVersion = rs.getLong("local_version"),
                        payload = rs.getString("payload_json"),
                        deleted = rs.getInt("deleted") == 1
                    ))
                }
            }
        }
        return list
    }

    @Synchronized
    fun clearMetadata() {
        connection.createStatement().use { it.execute("DELETE FROM sync_metadata;") }
    }

    @Synchronized
    fun firstOutbox(): DesktopSyncOutbox? {
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT operation_id, sequence, workspace_id, payload_json, status, created_at FROM sync_outbox WHERE status = 'PENDING' ORDER BY sequence ASC LIMIT 1").use { rs ->
                if (rs.next()) {
                    return DesktopSyncOutbox(
                        operationId = rs.getString("operation_id"),
                        sequence = rs.getLong("sequence"),
                        workspaceId = rs.getString("workspace_id"),
                        payload = rs.getString("payload_json"),
                        status = rs.getString("status"),
                        createdAt = rs.getLong("created_at")
                    )
                }
            }
        }
        return null
    }

    @Synchronized
    fun allOutbox(): List<DesktopSyncOutbox> {
        val list = mutableListOf<DesktopSyncOutbox>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT operation_id, sequence, workspace_id, payload_json, status, created_at FROM sync_outbox ORDER BY sequence ASC").use { rs ->
                while (rs.next()) {
                    list.add(DesktopSyncOutbox(
                        operationId = rs.getString("operation_id"),
                        sequence = rs.getLong("sequence"),
                        workspaceId = rs.getString("workspace_id"),
                        payload = rs.getString("payload_json"),
                        status = rs.getString("status"),
                        createdAt = rs.getLong("created_at")
                    ))
                }
            }
        }
        return list
    }

    @Synchronized
    fun enqueueOutbox(item: DesktopSyncOutbox) {
        connection.prepareStatement("""
            INSERT INTO sync_outbox (operation_id, sequence, workspace_id, payload_json, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(operation_id) DO UPDATE SET
                sequence = excluded.sequence,
                workspace_id = excluded.workspace_id,
                payload_json = excluded.payload_json,
                status = excluded.status,
                created_at = excluded.created_at;
        """.trimIndent()).use { stmt ->
            stmt.setString(1, item.operationId)
            stmt.setLong(2, item.sequence)
            stmt.setString(3, item.workspaceId)
            stmt.setString(4, item.payload)
            stmt.setString(5, item.status)
            stmt.setLong(6, item.createdAt)
            stmt.executeUpdate()
        }
    }

    @Synchronized
    fun acknowledgeOutbox(operationId: String) {
        connection.prepareStatement("DELETE FROM sync_outbox WHERE operation_id = ?").use { stmt ->
            stmt.setString(1, operationId)
            stmt.executeUpdate()
        }
    }

    @Synchronized
    fun clearOutbox() {
        connection.createStatement().use { it.execute("DELETE FROM sync_outbox;") }
    }

    @Synchronized
    fun outboxCount(): Int {
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT COUNT(*) FROM sync_outbox WHERE status = 'PENDING'").use { rs ->
                return if (rs.next()) rs.getInt(1) else 0
            }
        }
    }

    @Synchronized
    fun stageSnapshot(staging: DesktopSyncStaging) {
        connection.prepareStatement("""
            INSERT INTO sync_staging (snapshot_id, page, payload_json)
            VALUES (?, ?, ?)
            ON CONFLICT(snapshot_id, page) DO UPDATE SET payload_json = excluded.payload_json;
        """.trimIndent()).use { stmt ->
            stmt.setString(1, staging.snapshotId)
            stmt.setInt(2, staging.page)
            stmt.setString(3, staging.payload)
            stmt.executeUpdate()
        }
    }

    @Synchronized
    fun stagedSnapshot(snapshotId: String): List<DesktopSyncStaging> {
        val list = mutableListOf<DesktopSyncStaging>()
        connection.prepareStatement("SELECT snapshot_id, page, payload_json FROM sync_staging WHERE snapshot_id = ? ORDER BY page ASC").use { stmt ->
            stmt.setString(1, snapshotId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    list.add(DesktopSyncStaging(
                        snapshotId = rs.getString("snapshot_id"),
                        page = rs.getInt("page"),
                        payload = rs.getString("payload_json")
                    ))
                }
            }
        }
        return list
    }

    @Synchronized
    fun clearStagedSnapshot(snapshotId: String) {
        connection.prepareStatement("DELETE FROM sync_staging WHERE snapshot_id = ?").use { stmt ->
            stmt.setString(1, snapshotId)
            stmt.executeUpdate()
        }
    }

    @Synchronized
    fun getAllTransactions(): List<LedgerTransaction> {
        val list = mutableListOf<LedgerTransaction>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT payload_json FROM ledger_transactions ORDER BY timestamp ASC, rowid ASC").use { rs ->
                while (rs.next()) {
                    list.addAll(PersistenceJsonCodecs.decodeLedgerTransactions("[${rs.getString("payload_json")}]"))
                }
            }
        }
        return list
    }

    @Synchronized
    fun currentRecords(): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        fun add(type: String, values: List<Any>) {
            values.forEach { value ->
                val payload = SyncJson.record(value)
                result.add(
                    JSONObject()
                        .put("type", type)
                        .put("id", payload.getString("id"))
                        .put("payload", payload)
                        .put("version", 1)
                        .put("deleted", false)
                )
            }
        }
        add("customer", getCustomers())
        add("ledger", getAllTransactions())
        add("invoice", getInvoices())
        add("barterInvoice", getBarterInvoices())
        add("inventory", getInventoryItems())
        add("stockAdjustment", getStockAdjustments())
        add("portfolio", getItems())
        val settingsPayload = SyncJson.business(loadSettings())
        result.add(
            JSONObject()
                .put("type", "businessSettings")
                .put("id", "business")
                .put("payload", settingsPayload)
                .put("version", 1)
                .put("deleted", false)
        )
        return result
    }

    @Synchronized
    fun applyCloudRecord(type: String, id: String, payload: JSONObject?) {
        val wasApplying = isSyncApplying
        isSyncApplying = true
        try {
            if (payload == null) {
                when (type) {
                    "customer" -> connection.prepareStatement("DELETE FROM customers WHERE id = ?").use { it.setString(1, id); it.executeUpdate() }
                    "ledger" -> deleteTransaction(id)
                    "invoice" -> deleteInvoice(id)
                    "barterInvoice" -> deleteBarterInvoice(id)
                    "inventory" -> connection.prepareStatement("DELETE FROM inventory_items WHERE id = ?").use { it.setString(1, id); it.executeUpdate() }
                    "stockAdjustment" -> connection.prepareStatement("DELETE FROM stock_adjustments WHERE id = ?").use { it.setString(1, id); it.executeUpdate() }
                    "portfolio" -> deleteItem(id)
                    else -> throw DesktopCloudException("INVALID_RECORD")
                }
                return
            }
            require(payload.getString("id") == id)
            val encoded = JSONArray().put(payload).toString()
            when (type) {
                "customer" -> {
                    val customer = PersistenceJsonCodecs.decodeCustomers(encoded).single()
                    saveCustomerRecord(customer, payload)
                }
                "ledger" -> {
                    val tx = PersistenceJsonCodecs.decodeLedgerTransactions(encoded).single()
                    saveTransactionRecord(tx, payload)
                }
                "invoice" -> {
                    val invoice = PersistenceJsonCodecs.decodeInvoices(encoded).single()
                    saveInvoice(invoice)
                }
                "barterInvoice" -> {
                    val invoice = PersistenceJsonCodecs.decodeBarterInvoices(encoded).single()
                    saveBarterInvoice(invoice)
                }
                "inventory" -> {
                    val item = PersistenceJsonCodecs.decodeInventoryItems(encoded).single()
                    val categoryId = payload.optString("customCategoryId").ifBlank { null }
                    saveInventoryItem(item, mustExist = false, categoryId = categoryId, updateCategory = true)
                }
                "stockAdjustment" -> {
                    val adj = PersistenceJsonCodecs.decodeStockAdjustments(encoded).single()
                    connection.prepareStatement("""
                        INSERT INTO stock_adjustments (id, item_id, item_title, type, quantity_change, weight_grams, reason, note, timestamp, payload_json)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(id) DO UPDATE SET
                            item_id = excluded.item_id,
                            item_title = excluded.item_title,
                            type = excluded.type,
                            quantity_change = excluded.quantity_change,
                            weight_grams = excluded.weight_grams,
                            reason = excluded.reason,
                            note = excluded.note,
                            timestamp = excluded.timestamp,
                            payload_json = excluded.payload_json;
                    """.trimIndent()).use { stmt ->
                        stmt.setString(1, adj.id)
                        stmt.setString(2, adj.itemId)
                        stmt.setString(3, adj.itemTitle)
                        stmt.setString(4, adj.type.name)
                        stmt.setInt(5, adj.quantityChange)
                        stmt.setDouble(6, adj.weightGrams)
                        stmt.setString(7, adj.reason)
                        stmt.setString(8, adj.note)
                        stmt.setLong(9, adj.timestamp)
                        stmt.setString(10, payload.toString())
                        stmt.executeUpdate()
                    }
                }
                "portfolio" -> {
                    val item = PersistenceJsonCodecs.decodePortfolioItems(encoded).single()
                    addItem(item)
                }
                "businessSettings" -> {
                    val currentSettings = loadSettings()
                    val updated = SyncJson.applyBusiness(currentSettings, payload)
                    saveSettings(updated)
                }
            }
        } finally {
            isSyncApplying = wasApplying
        }
    }

    @Synchronized
    fun prepareUpload(snapshotId: String, workspace: String, epoch: Long): List<DesktopSyncStaging> = transaction {
        val cp = checkpoint()
        require(cp.workspaceId.isBlank() || cp.workspaceId == workspace)
        val records = currentRecords()
        clearMetadata()
        clearOutbox()
        records.forEach { r ->
            saveMetadata(DesktopSyncMetadata(
                type = r.getString("type"),
                id = r.getString("id"),
                workspaceId = workspace,
                version = 1,
                localVersion = 1,
                payload = r.getJSONObject("payload").toString(),
                deleted = false
            ))
        }
        records.chunked(100).forEachIndexed { page, list ->
            stageSnapshot(DesktopSyncStaging(snapshotId, page, JSONArray(list).toString()))
        }
        if (records.isEmpty()) {
            stageSnapshot(DesktopSyncStaging(snapshotId, 0, "[]"))
        }
        saveCheckpoint(cp.copy(
            workspaceId = workspace,
            writerEpoch = epoch,
            nextSequence = 1,
            uploadSnapshotId = snapshotId
        ))
        stagedSnapshot(snapshotId)
    }

    @Synchronized
    fun activateSnapshot(snapshotId: String, workspace: String, revision: Long, epoch: Long, sequence: Long, expectedNextSequence: Long? = null) = transaction {
        if (expectedNextSequence != null && checkpoint().nextSequence != expectedNextSequence) throw DesktopCloudException("LOCAL_CHANGED_DURING_RESTORE")
        val pages = stagedSnapshot(snapshotId)
        val records = pages.flatMap { p ->
            val a = JSONArray(p.payload)
            (0 until a.length()).map { a.getJSONObject(it) }
        }
        require(records.map { it.getString("type") + ":" + it.getString("id") }.distinct().size == records.size)

        val tables = listOf("barter_invoices", "invoices", "ledger_transactions", "customers", "inventory_items", "stock_adjustments", "portfolio_items")
        connection.createStatement().use { stmt ->
            for (table in tables) {
                stmt.execute("DELETE FROM $table;")
            }
        }
        clearMetadata()
        clearOutbox()

        records.forEach { r ->
            val type = r.getString("type")
            val id = r.getString("id")
            val deleted = r.getBoolean("deleted")
            val version = r.getLong("version")
            val payload = if (deleted) null else r.getJSONObject("payload")
            if (!deleted) {
                applyCloudRecord(type, id, payload)
            }
            saveMetadata(DesktopSyncMetadata(type, id, workspace, version, version, payload?.toString(), deleted))
        }

        saveCheckpoint(DesktopSyncCheckpoint(
            id = "main",
            workspaceId = workspace,
            cursor = revision,
            writerEpoch = epoch,
            nextSequence = sequence + 1,
            initialized = true
        ))
        clearStagedSnapshot(snapshotId)
        setSystemSetting("opening_inventory_seeded", "true")
    }

    /** SQLite creates a consistent, independent copy including pending sync and checkpoints. */
    @Synchronized
    fun backupBeforeCloudRestore(target: Path) {
        Files.createDirectories(target.parent)
        require(!Files.exists(target))
        connection.prepareStatement("VACUUM INTO ?").use { statement ->
            statement.setString(1, target.toAbsolutePath().toString())
            statement.execute()
        }
    }

    @Synchronized
    fun applyPull(result: JSONObject) = transaction {
        val groups = result.getJSONArray("groups")
        for (g in 0 until groups.length()) {
            val changes = groups.getJSONObject(g).getJSONArray("changes")
            for (i in 0 until changes.length()) {
                val c = changes.getJSONObject(i)
                val type = c.getString("type")
                val id = c.getString("id")
                val version = c.getLong("version")
                val old = metadata(type, id)
                if (old != null && version <= old.version) continue
                if (old != null && old.localVersion > old.version) throw DesktopCloudException("VERSION_CONFLICT", 409)
                val fresh = SyncJson.apply(old?.payload?.let(::JSONObject), c)
                applyCloudRecord(type, id, fresh)
                saveMetadata(DesktopSyncMetadata(type, id, checkpoint().workspaceId, version, version, fresh?.toString(), fresh == null))
            }
        }
        saveCheckpoint(checkpoint().copy(cursor = result.getLong("cursor")))
    }

    @Synchronized
    fun acknowledge(item: DesktopSyncOutbox, result: JSONObject) = transaction {
        require(result.getString("operationId") == item.operationId)
        val sent = JSONObject(item.payload).getJSONArray("changes")
        val ack = result.getJSONArray("changes")
        require(ack.length() == sent.length())
        for (i in 0 until sent.length()) {
            val c = sent.getJSONObject(i)
            val accepted = ack.getJSONObject(i)
            require(c.getString("type") == accepted.getString("type") && c.getString("id") == accepted.getString("id"))
            require(c.getLong("localVersion") == accepted.getLong("version"))
            val old = metadata(c.getString("type"), c.getString("id")) ?: error("Missing metadata for ${c.getString("type")}:${c.getString("id")}")
            saveMetadata(old.copy(version = accepted.getLong("version")))
        }
        acknowledgeOutbox(item.operationId)
    }

    fun clearAllData() {
        connection.createStatement().use { stmt ->
            stmt.execute("PRAGMA foreign_keys = OFF;")
            val tables = listOf(
                "barter_invoices", "invoices", "ledger_transactions",
                "deleted_customers", "customers", "inventory_options",
                "stock_adjustments", "inventory_items", "portfolio_items",
                "system_settings", "metadata", "market_cache",
                "sync_metadata", "sync_outbox", "sync_staging", "sync_conflicts", "sync_checkpoint"
            )
            for (table in tables) {
                stmt.execute("DELETE FROM $table;")
            }
            stmt.execute("PRAGMA foreign_keys = ON;")
        }
        mutableSettings.value = AppSettings()
    }

    override fun close() {
        if (!connection.isClosed) {
            connection.close()
        }
    }

    companion object {
        fun open(dbFile: Path): DesktopSqliteDatabase = DesktopSqliteDatabase(dbFile)

        internal fun mergeJson(target: JSONObject, source: JSONObject) {
            source.keySet().forEach { key ->
                val old = target.optJSONObject(key)
                val fresh = source.optJSONObject(key)
                if (old != null && fresh != null) {
                    mergeJson(old, fresh)
                } else if (target.optJSONArray(key) != null && source.optJSONArray(key) != null) {
                    val previous = target.getJSONArray(key)
                    val replacement = source.getJSONArray(key)
                    val byId = (0 until previous.length()).mapNotNull { previous.optJSONObject(it) }
                        .filter { it.has("id") }.associateBy { it.getString("id") }
                    for (i in 0 until replacement.length()) {
                        val encoded = replacement.optJSONObject(i) ?: continue
                        val retained = byId[encoded.optString("id")] ?: continue
                        mergeJson(retained, encoded)
                        replacement.put(i, retained)
                    }
                    target.put(key, replacement)
                } else {
                    target.put(key, source.get(key))
                }
            }
        }
    }
}
