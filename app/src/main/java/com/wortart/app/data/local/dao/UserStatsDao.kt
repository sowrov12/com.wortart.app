package com.wortart.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.wortart.app.data.local.entity.UserStats
import kotlinx.coroutines.flow.Flow

/**
 * Data access for the single-row [UserStats] table.
 * Call [insertIfAbsent] once at startup so the row always exists.
 */
@Dao
interface UserStatsDao {

    /** Emits null until the row has been created. */
    @Query("SELECT * FROM user_stats WHERE id = ${UserStats.SINGLETON_ID}")
    fun observe(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = ${UserStats.SINGLETON_ID}")
    suspend fun get(): UserStats?

    /** Creates the default row if missing; never overwrites existing progress. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(stats: UserStats = UserStats())

    @Update
    suspend fun update(stats: UserStats)

    @Upsert
    suspend fun upsert(stats: UserStats)
}
