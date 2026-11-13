package com.wortart.app.data.icons

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Xml
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlin.math.max

/** The name an icon is saved and looked up under: "Grammar Rules" becomes "grammar_rules". */
fun iconKey(name: String): String = name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

/**
 * App icons that come from a sheet (icon name in column A, a Google Drive link in column B).
 *
 * Each icon is downloaded once and kept in the app's files folder, not in the cache, so it stays on
 * the phone until the app data is cleared. When the sheet is not reachable the list that ships
 * inside the app (assets/icon_sheet.csv) is used. An icon is downloaded again only when its link
 * changes in the sheet. Until an icon is available the screens show their normal emoji or icon.
 */
class IconStore(context: Context) {

    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "icons").apply { mkdirs() }
    private val prefs = appContext.getSharedPreferences("wortart_icons", Context.MODE_PRIVATE)

    private val state = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())

    /** Loaded icons by [iconKey]. */
    val icons: StateFlow<Map<String, ImageBitmap>> = state.asStateFlow()

    /** Shows the saved icons first, then downloads new or changed ones. Nothing is ever deleted. */
    suspend fun sync() = withContext(Dispatchers.IO) {
        loadSaved()
        val entries = runCatching { fetchSheet() }.getOrNull().orEmpty().ifEmpty { bundled() }
        for ((name, link) in entries) {
            val key = iconKey(name)
            val id = driveId(link) ?: continue
            val file = File(dir, "$key.img")
            if (file.exists() && prefs.getString(key, null) == link) continue
            if (download(id, file)) {
                prefs.edit().putString(key, link).apply()
                decode(file)?.let { bitmap -> state.value = state.value + (key to bitmap) }
            }
        }
    }

    private fun loadSaved() {
        val loaded = HashMap<String, ImageBitmap>()
        dir.listFiles { f -> f.name.endsWith(".img") }?.forEach { file ->
            decode(file)?.let { loaded[file.name.removeSuffix(".img")] = it }
        }
        if (loaded.isNotEmpty()) state.value = state.value + loaded
    }

    /** Decodes a smaller copy so dozens of icons stay light on memory. */
    private fun decode(file: File): ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= TARGET_SIZE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.path, options)?.asImageBitmap()
    }

    private fun download(id: String, target: File): Boolean {
        val sources = listOf(
            "https://drive.google.com/uc?export=download&id=$id",
            // A resized copy: smaller and quicker, also works for formats the phone cannot decode.
            "https://drive.google.com/thumbnail?id=$id&sz=w512",
        )
        for (source in sources) {
            val bytes = httpGet(source) ?: continue
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0) continue // not an image (for example a Drive error page)
            val temp = File(dir, "${target.name}.tmp")
            temp.writeBytes(bytes)
            return temp.renameTo(target)
        }
        return false
    }

    private fun httpGet(address: String): ByteArray? = runCatching {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 45_000
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        connection.instanceFollowRedirects = true
        try {
            if (connection.responseCode == 200) connection.inputStream.use { it.readBytes() } else null
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    private fun bundled(): List<Pair<String, String>> = runCatching {
        parseCsv(appContext.assets.open("icon_sheet.csv").bufferedReader().use { it.readText() })
    }.getOrDefault(emptyList())

    /** The sheet as Google Sheets (CSV), or as an uploaded Excel file. */
    private fun fetchSheet(): List<Pair<String, String>>? {
        httpGet("https://docs.google.com/spreadsheets/d/$SHEET_ID/export?format=csv")?.let { bytes ->
            val text = String(bytes, Charsets.UTF_8)
            if (!text.trimStart().startsWith("<")) {
                parseCsv(text).takeIf { it.isNotEmpty() }?.let { return it }
            }
        }
        httpGet("https://drive.google.com/uc?export=download&id=$SHEET_ID")?.let { bytes ->
            parseXlsx(bytes).takeIf { it.isNotEmpty() }?.let { return it }
        }
        return null
    }

    private fun driveId(link: String): String? =
        Regex("/d/([A-Za-z0-9_-]+)").find(link)?.groupValues?.get(1)
            ?: Regex("[?&]id=([A-Za-z0-9_-]+)").find(link)?.groupValues?.get(1)

    /** Every "name, link" pair in a row: the sheet has several name and link column pairs side by side. */
    private fun parseCsv(text: String): List<Pair<String, String>> {
        val pairs = ArrayList<Pair<String, String>>()
        text.lines().forEach { line ->
            val cells = splitCsvLine(line)
            for (i in 0 until cells.size - 1) {
                val name = cells[i].trim()
                val link = cells[i + 1].trim()
                if (name.isNotEmpty() && !name.startsWith("http") && link.startsWith("http")) pairs += name to link
            }
        }
        return pairs
    }

    private fun splitCsvLine(line: String): List<String> {
        val cells = ArrayList<String>()
        val current = StringBuilder()
        var quoted = false
        for (ch in line) {
            when {
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> {
                    cells += current.toString()
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        cells += current.toString()
        return cells
    }

    private fun parseXlsx(bytes: ByteArray): List<Pair<String, String>> {
        var shared = emptyList<String>()
        var sheet: ByteArray? = null
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "xl/sharedStrings.xml" -> shared = readSharedStrings(zip.readBytes())
                    "xl/worksheets/sheet1.xml" -> sheet = zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }
        val data = sheet ?: return emptyList()

        val rows = ArrayList<Pair<String, String>>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(data), "UTF-8")
        val cells = HashMap<String, String>()
        var column = ""
        var type = ""
        var inValue = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> cells.clear()
                    "c" -> {
                        column = (parser.getAttributeValue(null, "r") ?: "").takeWhile { it.isLetter() }
                        type = parser.getAttributeValue(null, "t") ?: ""
                    }
                    "v", "t" -> inValue = true
                }
                XmlPullParser.TEXT -> if (inValue) {
                    val raw = parser.text.trim()
                    val value = if (type == "s") shared.getOrNull(raw.toIntOrNull() ?: -1).orEmpty() else raw
                    cells[column] = value
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v", "t" -> inValue = false
                    "row" -> for ((col, value) in cells) {
                        if (value.startsWith("http") && col.length == 1 && col[0] > 'A') {
                            val name = cells[(col[0] - 1).toString()].orEmpty().trim()
                            if (name.isNotEmpty() && !name.startsWith("http")) rows += name to value
                        }
                    }
                }
            }
            event = parser.next()
        }
        return rows
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
                    "si" -> strings += current.toString()
                }
            }
            event = parser.next()
        }
        return strings
    }

    private companion object {
        /** The sheet with the icon list. */
        const val SHEET_ID = "1mwH30lKEhKw4gfh9T-Hpl60O2jnAFb25"
        const val TARGET_SIZE = 256
    }
}
