package com.pabi277.messbillcheck.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonTest {

    @Test
    fun `round trips nested structures`() {
        val text = """{"a":1,"b":[true,null,"x"],"c":{"d":-4.5}}"""
        val v = JsonIo.parse(text)
        assertEquals(1L, v.long("a"))
        assertEquals(2, v.arr("b").size)
        assertEquals(true, (v.arr("b")[0] as Json.Bool).value)
        assertEquals("x", (v.arr("b")[1] as Json.Str).value)
        assertEquals("-4.5", v.obj("c")!!.raw("d"))
        assertEquals(text, JsonIo.write(v).replace(" ", ""))
    }

    @Test
    fun `big paise values never lose precision`() {
        val l = Ledger(
            schemaVersion = Ledger.SCHEMA_VERSION,
            periods = listOf(Period("p", "x", 0, 0, 9_007_199_254L, RateBasis.MEALS, 0, BillRounding.NEAREST, 1, false))
        )
        val back = LedgerJson.read(LedgerJson.write(l))
        assertEquals(9_007_199_254L, back.periods.single().rateMinor)
    }

    @Test
    fun `escapes survive a round trip`() {
        val raw = "quote \" backslash \\ newline\n tab\t Devanagari क बां unicode"
        val o = JsonIo.obj { m -> JsonIo.put(m, "v", raw) }
        val parsed = JsonIo.parse(JsonIo.write(o))
        assertEquals(raw, parsed.str("v"))
    }

    @Test
    fun `malformed documents are rejected, not half read`() {
        for (bad in listOf("{", "{\"a\":}", "[1,", "{\"a\":tru", "nul", "1 2")) {
            assertThrows(JsonException::class.java) { JsonIo.parse(bad) }
        }
    }

    @Test
    fun `unicode escapes decode`() {
        assertEquals("A", (JsonIo.parse("\"\\u0041\"") as Json.Str).value)
        assertTrue((JsonIo.parse("\"\\n\\t\"") as Json.Str).value.contains("\n"))
    }

    @Test
    fun `control characters are escaped on write`() {
        val text = JsonIo.write(Json.Str("a\u0001b"))
        assertTrue(text, text.contains("\\u0001"))
        assertEquals("a\u0001b", (JsonIo.parse(text) as Json.Str).value)
    }
}
