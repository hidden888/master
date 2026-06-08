package com.iptv.player.data.remote.xmltv

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import javax.inject.Inject

/** A single `<programme>` entry parsed from an XMLTV feed. */
data class ParsedProgram(
    val channelId: String,
    val title: String,
    val description: String,
    val startUtc: Long,
    val endUtc: Long,
)

/**
 * Streams an XMLTV document with [XmlPullParser] and emits one [ParsedProgram] per `<programme>`.
 * XMLTV times look like `20240115140000 +0000` (offset optional).
 */
class XmltvParser @Inject constructor() {

    fun parse(input: InputStream): List<ParsedProgram> {
        val result = ArrayList<ParsedProgram>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        var channelId: String? = null
        var start = 0L
        var stop = 0L
        var title = StringBuilder()
        var desc = StringBuilder()
        var current: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "programme" -> {
                        channelId = parser.getAttributeValue(null, "channel")
                        start = parseXmltvTime(parser.getAttributeValue(null, "start"))
                        stop = parseXmltvTime(parser.getAttributeValue(null, "stop"))
                        title = StringBuilder()
                        desc = StringBuilder()
                    }
                    "title" -> current = "title"
                    "desc" -> current = "desc"
                }

                XmlPullParser.TEXT -> when (current) {
                    "title" -> title.append(parser.text)
                    "desc" -> desc.append(parser.text)
                }

                XmlPullParser.END_TAG -> when (parser.name) {
                    "title", "desc" -> current = null
                    "programme" -> {
                        val id = channelId
                        if (id != null && stop > start) {
                            result.add(
                                ParsedProgram(
                                    channelId = id,
                                    title = title.toString().trim(),
                                    description = desc.toString().trim(),
                                    startUtc = start,
                                    endUtc = stop,
                                ),
                            )
                        }
                    }
                }
            }
            event = parser.next()
        }
        return result
    }

    /** Parses `yyyyMMddHHmmss[ ±hhmm]` into epoch millis (UTC). */
    internal fun parseXmltvTime(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val trimmed = raw.trim()
        val digits = trimmed.takeWhile { it.isDigit() }
        if (digits.length < 14) return 0L
        return try {
            val year = digits.substring(0, 4).toInt()
            val month = digits.substring(4, 6).toInt()
            val day = digits.substring(6, 8).toInt()
            val hour = digits.substring(8, 10).toInt()
            val minute = digits.substring(10, 12).toInt()
            val second = digits.substring(12, 14).toInt()

            // Days since epoch via a civil-calendar algorithm (UTC, no TimeZone allocation).
            val y = if (month <= 2) year - 1 else year
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val doy = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            val days = era * 146097L + doe - 719468L
            var millis = ((days * 24 + hour) * 3600L + minute * 60L + second) * 1000L

            // Apply the "+hhmm" / "-hhmm" offset if present (convert local time to UTC).
            val offsetPart = trimmed.drop(digits.length).trim()
            if (offsetPart.length >= 5 && (offsetPart[0] == '+' || offsetPart[0] == '-')) {
                val sign = if (offsetPart[0] == '-') -1 else 1
                val offHour = offsetPart.substring(1, 3).toInt()
                val offMin = offsetPart.substring(3, 5).toInt()
                millis -= sign * (offHour * 3600L + offMin * 60L) * 1000L
            }
            millis
        } catch (_: Exception) {
            0L
        }
    }
}
