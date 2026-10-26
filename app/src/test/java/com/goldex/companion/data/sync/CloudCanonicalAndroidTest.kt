package com.goldex.companion.data.sync

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CloudCanonicalAndroidTest {
    @Test fun snapshotHashMatchesServerForPersianDatesAndSlashes() {
        val records = JSONArray().put(JSONObject()
            .put("type", "ledger")
            .put("id", "test")
            .put("version", 1)
            .put("deleted", false)
            .put("payload", JSONObject().put("id", "test").put("dateTime", "۱۴۰۵/۰۸/۰۴")))
        val expected = """[{"deleted":false,"id":"test","payload":{"dateTime":"۱۴۰۵/۰۸/۰۴","id":"test"},"type":"ledger","version":1}]"""
        assertEquals(expected, SyncJson.canonical(records))
        assertEquals("cf3dd657fa57c613be3cb94668dcb33e483a0e2386cbfeadfab35c0124dcc397", SyncCoordinator.sha(SyncJson.canonical(records).toByteArray(Charsets.UTF_8)))
        assertEquals("\"</script>\u2028\u2029 😀\"", SyncJson.canonical("</script>\u2028\u2029 😀"))
        assertEquals("\"\\ud800\"", SyncJson.canonical("\ud800"))
    }
}
