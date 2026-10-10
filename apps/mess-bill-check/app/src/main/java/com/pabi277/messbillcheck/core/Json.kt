package com.pabi277.messbillcheck.core

/**
 * A deliberately small JSON reader/writer. Chosen over a serialisation library so the app has
 * zero third-party runtime dependencies: no licence to track, no Data Safety claim to reconcile,
 * and the backup format stays readable when this app is gone.
 */
sealed class Json {
    object Null : Json()
    data class Bool(val value: Boolean) : Json()

    /** Numbers keep their literal text so paise never pass through a double. */
    data class Num(val text: String) : Json()
    data class Str(val value: String) : Json()
    data class Arr(val items: MutableList<Json> = ArrayList()) : Json()
    data class Obj(val members: MutableMap<String, Json> = LinkedHashMap()) : Json()

    fun obj(key: String): Json? = membersOf(this)[key]
    fun str(key: String, fallback: String = ""): String = (membersOf(this)[key] as? Str)?.value ?: fallback
    fun long(key: String, fallback: Long = 0L): Long =
        (membersOf(this)[key] as? Num)?.let { it.text.toLongOrNull() ?: fallback } ?: fallback

    fun int(key: String, fallback: Int = 0): Int =
        (membersOf(this)[key] as? Num)?.let { it.text.toIntOrNull() ?: fallback } ?: fallback

    fun bool(key: String, fallback: Boolean = false): Boolean =
        (membersOf(this)[key] as? Bool)?.value ?: fallback

    fun arr(key: String): List<Json> = (membersOf(this)[key] as? Arr)?.items ?: emptyList()

    /** Literal text of a number, so paise and fractions are inspected without a double. */
    fun raw(key: String): String? = (membersOf(this)[key] as? Num)?.text

    private companion object {
        fun membersOf(v: Json): Map<String, Json> = (v as? Obj)?.members ?: emptyMap()
    }
}

class JsonException(message: String) : RuntimeException(message)

object JsonIo {

    fun obj(build: (MutableMap<String, Json>) -> Unit): Json.Obj {
        val o = Json.Obj()
        build(o.members)
        return o
    }

    fun arr(items: List<Json>): Json.Arr = Json.Arr(ArrayList(items))

    fun put(members: MutableMap<String, Json>, key: String, value: String) {
        members[key] = Json.Str(value)
    }

    fun put(members: MutableMap<String, Json>, key: String, value: Long) {
        members[key] = Json.Num(value.toString())
    }

    fun put(members: MutableMap<String, Json>, key: String, value: Int) {
        members[key] = Json.Num(value.toString())
    }

    fun put(members: MutableMap<String, Json>, key: String, value: Boolean) {
        members[key] = Json.Bool(value)
    }

    fun write(value: Json): String {
        val sb = StringBuilder(256)
        write(value, sb)
        return sb.toString()
    }

    private fun write(value: Json, sb: StringBuilder) {
        when (value) {
            Json.Null -> sb.append("null")
            is Json.Bool -> sb.append(if (value.value) "true" else "false")
            is Json.Num -> sb.append(value.text)
            is Json.Str -> writeString(value.value, sb)
            is Json.Arr -> {
                sb.append('[')
                value.items.forEachIndexed { i, item ->
                    if (i > 0) sb.append(',')
                    write(item, sb)
                }
                sb.append(']')
            }
            is Json.Obj -> {
                sb.append('{')
                var first = true
                for ((key, item) in value.members) {
                    if (!first) sb.append(',')
                    first = false
                    writeString(key, sb)
                    sb.append(':')
                    write(item, sb)
                }
                sb.append('}')
            }
        }
    }

    private fun writeString(s: String, sb: StringBuilder) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                else -> if (c.code < 0x20) sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        sb.append('"')
    }

    fun parse(text: String): Json {
        val p = Parser(text)
        p.skipWs()
        val v = p.value(0)
        p.skipWs()
        if (!p.atEnd) throw JsonException("trailing characters at offset ${p.pos}")
        return v
    }

    private class Parser(val s: String) {
        var pos = 0
        val atEnd: Boolean get() = pos >= s.length

        fun skipWs() {
            while (pos < s.length && (s[pos] == ' ' || s[pos] == '\n' || s[pos] == '\t' || s[pos] == '\r')) pos++
        }

        fun value(depth: Int): Json {
            if (depth > 64) throw JsonException("json nested too deeply")
            if (atEnd) throw JsonException("unexpected end of json")
            return when (val c = s[pos]) {
                '{' -> obj(depth)
                '[' -> arr(depth)
                '"' -> Json.Str(str())
                't' -> literal("true", Json.Bool(true))
                'f' -> literal("false", Json.Bool(false))
                'n' -> literal("null", Json.Null)
                else -> number()
            }.also { skipWs() }
        }

        private fun literal(word: String, result: Json): Json {
            if (!s.startsWith(word, pos)) throw JsonException("bad literal at $pos")
            pos += word.length
            return result
        }

        private fun obj(depth: Int): Json {
            pos++ // consume {
            val o = Json.Obj()
            skipWs()
            if (pos < s.length && s[pos] == '}') {
                pos++
                return o
            }
            while (true) {
                skipWs()
                if (pos >= s.length || s[pos] != '"') throw JsonException("expected key at $pos")
                val key = str()
                skipWs()
                if (pos >= s.length || s[pos] != ':') throw JsonException("expected ':' at $pos")
                pos++
                skipWs()
                o.members[key] = value(depth + 1)
                skipWs()
                if (pos < s.length && s[pos] == ',') {
                    pos++
                    continue
                }
                if (pos < s.length && s[pos] == '}') {
                    pos++
                    return o
                }
                throw JsonException("expected ',' or '}' at $pos")
            }
        }

        private fun arr(depth: Int): Json {
            pos++ // consume [
            val a = Json.Arr()
            skipWs()
            if (pos < s.length && s[pos] == ']') {
                pos++
                return a
            }
            while (true) {
                skipWs()
                a.items.add(value(depth + 1))
                skipWs()
                if (pos < s.length && s[pos] == ',') {
                    pos++
                    continue
                }
                if (pos < s.length && s[pos] == ']') {
                    pos++
                    return a
                }
                throw JsonException("expected ',' or ']' at $pos")
            }
        }

        private fun str(): String {
            pos++ // opening quote
            val sb = StringBuilder()
            while (true) {
                if (pos >= s.length) throw JsonException("unterminated string")
                val c = s[pos]
                when (c) {
                    '"' -> {
                        pos++
                        return sb.toString()
                    }
                    '\\' -> {
                        pos++
                        if (pos >= s.length) throw JsonException("bad escape")
                        when (val e = s[pos]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 >= s.length) throw JsonException("bad unicode escape")
                                val hex = s.substring(pos + 1, pos + 5)
                                val code = hex.toIntOrNull(16) ?: throw JsonException("bad unicode escape: $hex")
                                sb.append(code.toChar())
                                pos += 4
                            }
                            else -> throw JsonException("bad escape: \\$e")
                        }
                        pos++
                    }
                    else -> {
                        sb.append(c)
                        pos++
                    }
                }
            }
        }

        private fun number(): Json {
            val start = pos
            if (pos < s.length && (s[pos] == '-' || s[pos] == '+')) pos++
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.' || s[pos] == 'e' || s[pos] == 'E' || s[pos] == '-' || s[pos] == '+')) pos++
            val text = s.substring(start, pos)
            if (text.isEmpty() || text == "-") throw JsonException("bad number at $start")
            return Json.Num(text)
        }
    }
}
