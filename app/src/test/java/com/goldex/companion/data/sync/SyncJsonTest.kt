package com.goldex.companion.data.sync

import com.goldex.companion.data.PersistenceJsonCodecs
import com.goldex.companion.model.Customer
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.CraftedGoldItem
import java.math.BigDecimal
import org.json.*
import org.junit.Assert.*
import org.junit.Test

class SyncJsonTest {
    @Test fun moneyAboveJavascriptSafeIntegerAndWeightsRoundTripExactly() {
        val customer=Customer(id="stable",name="آزمون",createdAt=1234,cashDebtTomans=Long.MAX_VALUE,goldDebtGrams=0.001)
        val wire=SyncJson.record(customer)
        assertEquals(Long.MAX_VALUE.toString(),wire.getString("cashDebtTomans"))
        assertEquals("0.001",wire.getString("goldDebtGrams"))
        assertEquals(customer,PersistenceJsonCodecs.decodeCustomers(JSONArray().put(wire).toString()).single())
    }
    @Test fun oneChangedNameProducesOnlyNamePatch() {
        val old=SyncJson.record(Customer(id="id",name="قبل",createdAt=1,cashDebtTomans=123))
        val fresh=JSONObject(old.toString()).put("name","بعد")
        val patch=SyncJson.diff(old,fresh)
        assertEquals("patch",patch.getString("action"))
        assertEquals(setOf("name"),patch.getJSONObject("fields").keys().asSequence().toSet())
        assertEquals(SyncJson.canonical(fresh),SyncJson.canonical(SyncJson.apply(old,patch)))
    }
    @Test fun nullMissingAndRemovedFieldsHaveDistinctMeanings() {
        val old=JSONObject().put("id","id").put("note","text").put("phone","09")
        val fresh=JSONObject().put("id","id").put("note",JSONObject.NULL)
        val patch=SyncJson.diff(old,fresh)
        assertTrue(patch.getJSONObject("fields").isNull("note"))
        assertEquals("phone",patch.getJSONArray("remove").getString(0))
        val result=SyncJson.apply(old,patch)!!
        assertTrue(result.isNull("note")); assertFalse(result.has("phone"))
    }
    @Test fun embeddedInvoiceCustomerSnapshotPreservesAllHistoricalFields() {
        val customer=Customer(id="old",name="historical",createdAt=1234,cashDebtTomans=Long.MAX_VALUE,goldDebtGrams=0.001,accountCode="42",cityOrMarket="city")
        val invoice=BarterInvoice(id="inv",createdAt=100,customer=customer,thirdPartyCustomer=customer)
        val decoded=PersistenceJsonCodecs.decodeBarterInvoices(JSONArray().put(SyncJson.record(invoice)).toString()).single()
        assertEquals(invoice,decoded)
    }
    @Test fun classicInvoiceAlsoPreservesCustomerHistoryAndUnknownFieldsSurviveEdit() {
        val customer=Customer(id="old",name="historical",createdAt=1234,cashDebtTomans=Long.MAX_VALUE)
        val invoice=com.goldex.companion.model.Invoice(id="classic",createdAt=1,customer=customer)
        assertEquals(invoice,PersistenceJsonCodecs.decodeInvoices(JSONArray().put(SyncJson.record(invoice)).toString()).single())
        val old=SyncJson.record(customer).put("futureField",JSONObject().put("value","untouched"))
        val fresh=SyncJson.preserveUnknown("customer",old,SyncJson.record(customer.copy(name="new")))!!
        assertEquals("untouched",fresh.getJSONObject("futureField").getString("value"))
        assertEquals(setOf("name"),SyncJson.diff(old,fresh).getJSONObject("fields").keys().asSequence().toSet())
    }
    @Test(expected=IllegalArgumentException::class) fun nonFiniteDecimalIsRejected() { SyncJson.numericStrings(Double.NaN) }

    @Test fun largeDoublesInSalesItemsDoNotUseScientificNotation() {
        val crafted = CraftedGoldItem(
            id = "item1",
            title = "دستبند طلا",
            grossWeight = 12.0,
            netWeight = 12.0,
            spotPrice = 24920800L,
            wageInput = 11.0,
            wageAmount = 32895456.0,
            profitPercent = 7.0,
            profitAmount = 23236153.92,
            taxPercent = 9.0,
            taxAmount = 5051844.8928,
            rawGoldValue = 299049600.0,
            totalPayable = 360233054.8128,
            equivalent18kWeight = 12.0
        )
        val invoice = BarterInvoice(
            id = "inv1",
            createdAt = 1000L,
            salesItems = listOf(crafted)
        )
        val record = SyncJson.record(invoice)
        val itemObj = record.getJSONArray("salesItems").getJSONObject(0)
        assertEquals("299049600", itemObj.getString("rawGoldValue"))
        assertEquals("32895456", itemObj.getString("wageAmount"))

        // Simulate cloud restore activation decode loop
        val encoded = JSONArray().put(record).toString()
        val decoded = PersistenceJsonCodecs.decodeBarterInvoices(encoded).single()
        val again = SyncJson.record(decoded)
        again.keys().forEach { key ->
            if (record.has(key)) {
                assertEquals("Key $key must match canonically", SyncJson.canonical(record.get(key)), SyncJson.canonical(again.get(key)))
            }
        }
    }

    @Test fun scientificNotationAndBigDecimalsAreNormalizedToPlainStrings() {
        assertEquals("299049600", SyncJson.numericStrings(BigDecimal("2.990496E+8")))
        assertEquals("299049600", SyncJson.numericStrings("2.990496E+8"))
        assertEquals("39041920", SyncJson.numericStrings("3.904192E+7"))
        assertEquals("1112692620", SyncJson.numericStrings("1.11269262E+9"))
        assertEquals("299049600", SyncJson.numericStrings(299049600L))
    }
}
