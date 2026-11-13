package com.wortart.app.data.sheets

import android.content.Context
import com.wortart.app.data.local.WortArtDatabase
import com.wortart.app.data.local.seed.VocabSeeder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Keeps the word list in step with the two sheets (the verb list and the workbook with adjectives,
 * adverbs, prepositions, conjunctions and modal verbs).
 *
 * Every start the sheets are downloaded; when something changed in them, all cards are built again
 * and replace the old ones (favorites stay). Offline, or when a sheet cannot be read, the words that
 * are on the phone are kept. Both files must be shared with "Anyone with the link".
 */
class VocabSheetSync(context: Context, private val db: WortArtDatabase) {

    private val prefs = context.applicationContext.getSharedPreferences("wortart_sheet_sync", Context.MODE_PRIVATE)

    /** Returns true when the cards were rebuilt from the sheets. */
    suspend fun sync(): Boolean = withContext(Dispatchers.IO) {
        runCatching { doSync() }.getOrDefault(false)
    }

    /** Makes the next [sync] rebuild the cards even if the sheets did not change. */
    fun forget() {
        prefs.edit().remove(KEY_SIGNATURE).apply()
    }

    private suspend fun doSync(): Boolean {
        val first = download(VERB_FILE_ID) ?: return false
        val second = download(WORKBOOK_ID) ?: return false
        val signature = "${VocabSeeder.SEED_VERSION}:${sha256(first)}${sha256(second)}"
        val dao = db.vocabDao()
        if (prefs.getString(KEY_SIGNATURE, null) == signature && dao.count() > 0) return false

        var verbs: List<Map<String, String>>? = null
        var book: Map<String, SheetRows>? = null
        for (bytes in listOf(first, second)) {
            if (SheetFiles.isWorkbook(bytes)) {
                val sheets = SheetFiles.readWorkbook(bytes)
                if (sheets.containsKey("Adverbien")) book = sheets
                else sheets.values.firstOrNull()?.let { verbs = SheetFiles.withHeader(it) }
            } else {
                verbs = SheetFiles.withHeader(SheetFiles.parseCsv(String(bytes, Charsets.UTF_8)))
            }
        }
        val verbRows = verbs ?: return false
        val sheets = book ?: return false

        fun rowsOf(name: String, fallbackIndex: Int): SheetRows =
            (sheets[name] ?: sheets.values.toList().getOrNull(fallbackIndex)).orEmpty().drop(1)

        val entities = VocabSheetImporter.build(
            SheetData(
                verbs = verbRows,
                adjectives = rowsOf("Sheet1", 0),
                adverbs = rowsOf("Adverbien", 1),
                prepositions = rowsOf("Präpositionen", 2),
                conjunctions = rowsOf("Konjunktionen", 3),
                modalVerbs = rowsOf("Modalverben", 4),
            ),
        )
        // A broken or half-empty sheet must not wipe the words on the phone.
        if (entities.size < MIN_CARDS) return false

        val favoriteWords = dao.getFavorites().map { it.word }.toSet()
        dao.replaceAll(entities.map { if (it.word in favoriteWords) it.copy(isFavorite = true) else it })
        prefs.edit().putString(KEY_SIGNATURE, signature).apply()
        return true
    }

    /** The file as bytes: first as a normal Drive download, then as a Google Sheets export. */
    private fun download(id: String): ByteArray? {
        val sources = listOf(
            "https://drive.google.com/uc?export=download&id=$id",
            "https://docs.google.com/spreadsheets/d/$id/export?format=xlsx",
            "https://docs.google.com/spreadsheets/d/$id/export?format=csv",
        )
        for (source in sources) {
            val bytes = httpGet(source) ?: continue
            if (bytes.isEmpty()) continue
            val head = String(bytes.copyOf(minOf(bytes.size, 64)), Charsets.UTF_8).trimStart().lowercase()
            if (head.startsWith("<!doctype") || head.startsWith("<html")) continue // a Drive error page
            return bytes
        }
        return null
    }

    private fun httpGet(address: String): ByteArray? = runCatching {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        try {
            if (connection.responseCode == 200) connection.inputStream.use { it.readBytes() } else null
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        /** The verb list. */
        const val VERB_FILE_ID = "1psnoyQGLPO6Ez15qOQF-w_sbHJq91l8z"

        /** The workbook with adjectives, adverbs, prepositions, conjunctions and modal verbs. */
        const val WORKBOOK_ID = "1dSPnRhaCdSXdD7g7xzUSqQiT78EK6rBJ"

        const val KEY_SIGNATURE = "signature"
        const val MIN_CARDS = 500
    }
}
