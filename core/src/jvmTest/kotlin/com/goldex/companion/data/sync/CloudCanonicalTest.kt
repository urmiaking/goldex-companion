package com.goldex.companion.data.sync

import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals

class CloudCanonicalTest {
    @Test fun quotingMatchesServerWithoutOptionalEscapes() {
        assertEquals("\"۱۴۰۵/۰۸/۰۴ </script>\u2028\u2029 😀\"", SyncJson.canonical("۱۴۰۵/۰۸/۰۴ </script>\u2028\u2029 😀"))
        assertEquals("\"\\\"\\\\\\b\\f\\n\\r\\t\\u0000\"", SyncJson.canonical("\"\\\b\u000c\n\r\t\u0000"))
        assertEquals("\"\\ud800\"", SyncJson.canonical("\ud800"))
        assertEquals("\"\\udc00\"", SyncJson.canonical("\udc00"))
    }

    @Test fun objectKeysUseTheSameQuotingAndOrderAsServer() {
        assertEquals("""{"a/":"x/y","b":"</script>"}""", SyncJson.canonical(JSONObject().put("b", "</script>").put("a/", "x/y")))
    }
}
