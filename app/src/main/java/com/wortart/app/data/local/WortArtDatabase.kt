package com.wortart.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wortart.app.data.local.dao.UserStatsDao
import com.wortart.app.data.local.dao.VocabDao
import com.wortart.app.data.local.entity.UserStats
import com.wortart.app.data.local.entity.VocabEntity

@Database(
    entities = [VocabEntity::class, UserStats::class],
    version = 1,
    exportSchema = true,
)
abstract class WortArtDatabase : RoomDatabase() {

    abstract fun vocabDao(): VocabDao
    abstract fun userStatsDao(): UserStatsDao

    companion object {
        const val DATABASE_NAME = "wortart.db"

        @Volatile
        private var instance: WortArtDatabase? = null

        fun getInstance(context: Context): WortArtDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WortArtDatabase::class.java,
                    DATABASE_NAME,
                )
                    // Task 1.2 will add .createFromAsset("databases/wortart_seed.db") here.
                    .build()
                    .also { instance = it }
            }
    }
}
