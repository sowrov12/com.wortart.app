package com.wortart.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.wortart.app.data.local.entity.VocabEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for [VocabEntity].
 *
 * Declared as an abstract class (not an interface) so it can hold the
 * [upsertPreservingFavorites] transaction.
 *
 * Filter parameters are optional: pass null to skip that filter.
 */
@Dao
abstract class VocabDao {

    // ---- Writes ----

    /** Inserts new words and ignores ids that already exist. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAll(items: List<VocabEntity>)

    @Upsert
    abstract suspend fun upsertAll(items: List<VocabEntity>)

    @Query("SELECT id FROM vocab_table WHERE isFavorite = 1")
    abstract suspend fun getFavoriteIds(): List<String>

    /**
     * Inserts or updates words from a new dataset without losing the user's favorites.
     */
    @Transaction
    open suspend fun upsertPreservingFavorites(items: List<VocabEntity>) {
        val favoriteIds = getFavoriteIds().toSet()
        upsertAll(items.map { it.copy(isFavorite = it.id in favoriteIds) })
    }

    @Query("UPDATE vocab_table SET isFavorite = :isFavorite WHERE id = :id")
    abstract suspend fun setFavorite(id: String, isFavorite: Boolean)

    // ---- Reads ----

    @Query("SELECT COUNT(*) FROM vocab_table")
    abstract suspend fun count(): Int

    @Query("SELECT * FROM vocab_table WHERE id = :id")
    abstract suspend fun getById(id: String): VocabEntity?

    @Query("SELECT * FROM vocab_table ORDER BY word COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<VocabEntity>>

    @Query(
        """
        SELECT * FROM vocab_table
        WHERE (:level IS NULL OR level = :level)
          AND (:category IS NULL OR category = :category)
          AND (:topic IS NULL OR topic = :topic)
        ORDER BY word COLLATE NOCASE
        """
    )
    abstract fun observeFiltered(
        level: String? = null,
        category: String? = null,
        topic: String? = null,
    ): Flow<List<VocabEntity>>

    /**
     * Searches German, English and Bengali text.
     * [query] must already be passed through [escapeForLike].
     * Note: SQLite's LIKE is case-insensitive for ASCII only (not ä/ö/ü).
     */
    @Query(
        """
        SELECT * FROM vocab_table
        WHERE (word LIKE '%' || :query || '%' ESCAPE '\'
               OR translationEn LIKE '%' || :query || '%' ESCAPE '\'
               OR translationBn LIKE '%' || :query || '%' ESCAPE '\')
          AND (:level IS NULL OR level = :level)
          AND (:category IS NULL OR category = :category)
          AND (:topic IS NULL OR topic = :topic)
        ORDER BY word COLLATE NOCASE
        """
    )
    abstract fun search(
        query: String,
        level: String? = null,
        category: String? = null,
        topic: String? = null,
    ): Flow<List<VocabEntity>>

    @Query("SELECT * FROM vocab_table WHERE isFavorite = 1 ORDER BY word COLLATE NOCASE")
    abstract fun observeFavorites(): Flow<List<VocabEntity>>

    /** Distinct topics for the Explore category grid. */
    @Query("SELECT DISTINCT topic FROM vocab_table ORDER BY topic COLLATE NOCASE")
    abstract fun observeTopics(): Flow<List<String>>

    /**
     * Random words of the same category and level, used as wrong answer options
     * for meaning / antonym / comparative questions.
     */
    @Query(
        """
        SELECT * FROM vocab_table
        WHERE category = :category AND level = :level AND id != :excludeId
        ORDER BY RANDOM()
        LIMIT :limit
        """
    )
    abstract suspend fun getRandomDistractors(
        category: String,
        level: String,
        excludeId: String,
        limit: Int,
    ): List<VocabEntity>
}

/** Escapes `%`, `_` and `\` so user input is matched literally by [VocabDao.search]. */
fun String.escapeForLike(): String =
    replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
