package com.wortart.app.data.sheets

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/** Reads the two file kinds the word lists come in: CSV text and Excel (.xlsx) workbooks. */
object SheetFiles {

    /** CSV text into rows of cells (quotes, doubled quotes and line breaks inside quotes are handled). */
    fun parseCsv(input: String): SheetRows {
        val text = input.removePrefix("\uFEFF")
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        cell.append('"')
                        i++
                    } else {
                        quoted = false
                    }
                } else {
                    cell.append(ch)
                }
            } else if (ch == '"') {
                quoted = true
            } else if (ch == ',') {
                row.add(cell.toString())
                cell.clear()
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                row.add(cell.toString())
                cell.clear()
                rows.add(row)
                row = ArrayList()
            } else {
                cell.append(ch)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString())
            rows.add(row)
        }
        return rows
    }

    /** The first row is the header; every other row becomes a map from header name to cell. */
    fun withHeader(rows: SheetRows): List<Map<String, String>> {
        val header = rows.firstOrNull()?.map { it.trim() } ?: return emptyList()
        return rows.drop(1)
            .filter { row -> row.any { it.isNotBlank() } }
            .map { row -> header.indices.associate { index -> header[index] to row.getOrNull(index).orEmpty() } }
    }

    /** True for bytes that start like a zip file (an .xlsx workbook is one). */
    fun isWorkbook(bytes: ByteArray): Boolean =
        bytes.size > 3 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()

    /** All sheets of an .xlsx workbook, by sheet name; each sheet as rows of cells by column position. */
    fun readWorkbook(bytes: ByteArray): Map<String, SheetRows> {
        var shared = emptyList<String>()
        var workbook: ByteArray? = null
        var relations: ByteArray? = null
        val sheets = HashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    name == "xl/sharedStrings.xml" -> shared = readSharedStrings(zip.readBytes())
                    name == "xl/workbook.xml" -> workbook = zip.readBytes()
                    name == "xl/_rels/workbook.xml.rels" -> relations = zip.readBytes()
                    name.startsWith("xl/worksheets/") && name.endsWith(".xml") -> sheets[name] = zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }
        val book = workbook ?: return emptyMap()
        val rels = relations ?: return emptyMap()

        val targets = HashMap<String, String>()
        forEachTag(rels) { parser ->
            if (parser.name == "Relationship") {
                val id = parser.getAttributeValue(null, "Id")
                val target = parser.getAttributeValue(null, "Target")
                if (id != null && target != null) {
                    val clean = target.removePrefix("/")
                    targets[id] = if (clean.startsWith("xl/")) clean else "xl/$clean"
                }
            }
        }
        val result = LinkedHashMap<String, SheetRows>()
        forEachTag(book) { parser ->
            if (parser.name == "sheet") {
                val name = parser.getAttributeValue(null, "name")
                val id = parser.getAttributeValue(null, "r:id")
                val data = id?.let { targets[it] }?.let { sheets[it] }
                if (name != null && data != null) result[name] = readSheet(data, shared)
            }
        }
        return result
    }

    private fun forEachTag(bytes: ByteArray, action: (XmlPullParser) -> Unit) {
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) action(parser)
            event = parser.next()
        }
    }

    private fun readSharedStrings(bytes: ByteArray): List<String> {
        val strings = ArrayList<String>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        val current = StringBuilder()
        var inText = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "si" -> current.clear()
                    "t" -> inText = true
                }
                XmlPullParser.TEXT -> if (inText) current.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> inText = false
                    "si" -> strings.add(current.toString())
                }
            }
            event = parser.next()
        }
        return strings
    }

    /** "C5" is column 2 (counting from 0). */
    private fun columnIndex(reference: String): Int {
        var index = 0
        for (ch in reference) {
            if (!ch.isLetter()) break
            index = index * 26 + (ch.uppercaseChar() - 'A' + 1)
        }
        return index - 1
    }

    private fun readSheet(bytes: ByteArray, shared: List<String>): SheetRows {
        val rows = ArrayList<List<String>>()
        var cells = ArrayList<String>()
        var column = 0
        var type = ""
        var inValue = false
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> cells = ArrayList()
                    "c" -> {
                        column = columnIndex(parser.getAttributeValue(null, "r") ?: "")
                        type = parser.getAttributeValue(null, "t") ?: ""
                    }
                    "v", "t" -> inValue = true
                }
                XmlPullParser.TEXT -> if (inValue && column >= 0) {
                    val raw = parser.text
                    val value = if (type == "s") shared.getOrNull(raw.trim().toIntOrNull() ?: -1).orEmpty() else raw
                    while (cells.size <= column) cells.add("")
                    cells[column] = cells[column] + value
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v", "t" -> inValue = false
                    "row" -> rows.add(cells)
                }
            }
            event = parser.next()
        }
        return rows
    }
}
