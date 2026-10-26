package com.wortart.app.data.repository

import com.wortart.app.data.local.dao.VocabDao
import com.wortart.app.data.local.dao.escapeForLike
import com.wortart.app.data.local.entity.VocabEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class VocabRepositoryImpl(
    private val dao: VocabDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : VocabRepository {

    override fun observeWords(filter: VocabFilter): Flow<List<VocabEntity>> {
        val query = filter.query.trim()
        val source = if (query.isEmpty()) {
            dao.observeFiltered(filter.level, filter.category, filter.topic)
        } else {
            dao.search(query.escapeForLike(), filter.level, filter.category, filter.topic)
        }
        return source.distinctUntilChanged().flowOn(ioDispatcher)
    }

    override fun observeFavorites(): Flow<List<VocabEntity>> =
        dao.observeFavorites().distinctUntilChanged().flowOn(ioDispatcher)

    override fun observeTopics(): Flow<List<String>> =
        dao.observeTopics().distinctUntilChanged().flowOn(ioDispatcher)

    override suspend fun getWord(id: String): VocabEntity? =
        withContext(ioDispatcher) { dao.getById(id) }

    override suspend fun wordCount(): Int =
        withContext(ioDispatcher) { dao.count() }

    override suspend fun setFavorite(id: String, isFavorite: Boolean) =
        withContext(ioDispatcher) { dao.setFavorite(id, isFavorite) }

    override suspend fun getDistractors(card: VocabEntity, count: Int): List<VocabEntity> =
        withContext(ioDispatcher) {
            dao.getRandomDistractors(
                category = card.category,
                level = card.level,
                excludeId = card.id,
                limit = count,
            )
        }
}
