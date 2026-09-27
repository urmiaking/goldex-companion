package com.goldex.companion.data.sync

import com.goldex.companion.data.PersistenceJsonCodecs
import com.goldex.companion.model.Customer
import com.goldex.companion.model.BarterInvoice
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
}
