package com.goldex.companion.data.sync

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.goldex.companion.data.local.db.GoldexDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class CloudMigrationDeviceTest {
    @get:Rule val migration=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),GoldexDatabase::class.java,emptyList(),FrameworkSQLiteOpenHelperFactory())
    @Test fun realDeviceMigrationPreservesFinancialLongAndStableId() {
        migration.createDatabase("cloud-migration",1).apply {
            execSQL("INSERT INTO portfolio_items VALUES ('stable', 'test', 'GOLD', 0.001, 'K18', 1, NULL, 9007199254740993, '')")
            close()
        }
        migration.runMigrationsAndValidate("cloud-migration",3,true,SYNC_MIGRATION_1_2,com.goldex.companion.data.local.db.migration.SETTLEMENT_MIGRATION_2_3).apply {
            query("SELECT id,purchasePriceTotal FROM portfolio_items").use {
                check(it.moveToFirst()); assertEquals("stable",it.getString(0)); assertEquals(9007199254740993L,it.getLong(1))
            }
            close()
        }
    }
}
