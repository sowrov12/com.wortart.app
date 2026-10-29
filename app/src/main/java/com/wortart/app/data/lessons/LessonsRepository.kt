package com.wortart.app.data.lessons

import android.content.Context
import com.wortart.app.data.auth.resultOf
import com.wortart.app.domain.rules.lessons.LessonParser
import com.wortart.app.domain.rules.lessons.LessonsFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Source of the grammar lessons. */
interface LessonsRepository {
    suspend fun load(): Result<LessonsFile>
}

/** Reads assets/grammar_rules.json once and keeps it in memory. */
class AssetLessonsRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LessonsRepository {

    private val appContext = context.applicationContext

    @Volatile
    private var cached: LessonsFile? = null

    override suspend fun load(): Result<LessonsFile> = withContext(ioDispatcher) {
        val known = cached
        if (known != null) {
            Result.success(known)
        } else {
            resultOf {
                val text = appContext.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
                LessonParser.parse(text).also { cached = it }
            }
        }
    }

    companion object {
        const val ASSET_NAME = "grammar_rules.json"
    }
}
