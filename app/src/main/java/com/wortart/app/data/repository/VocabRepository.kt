package com.wortart.app.data.repository

import com.wortart.app.data.local.entity.VocabEntity
import kotlinx.coroutines.flow.Flow

/**
 * Single entry point for vocabulary data. ViewModels depend on this interface,
 * not on Room, so the data source can be swapped or faked in tests.
 */
interface VocabRepository {

    /** Words matching [filter], sorted by German word. Re-emits when the data changes. */
    fun observeWords(filter: VocabFilter = VocabFilter()): Flow<List<VocabEntity>>

    fun observeFavorites(): Flow<List<VocabEntity>>

    /** Distinct topics for the Explore grid. */
    fun observeTopics(): Flow<List<String>>

    suspend fun getWord(id: String): VocabEntity?

    suspend fun wordCount(): Int

    suspend fun setFavorite(id: String, isFavorite: Boolean)

    /**
     * Random words of the same category and level as [card], used as wrong answer
     * options. May return fewer than [count] items if the dataset is small.
     */
    suspend fun getDistractors(card: VocabEntity, count: Int = 3): List<VocabEntity>
}
