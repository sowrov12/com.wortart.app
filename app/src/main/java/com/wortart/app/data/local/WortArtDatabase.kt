package com.wortart.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wortart.app.data.local.dao.UserStatsDao
import com.wortart.app.data.local.dao.VocabDao
import com.wortart.app.data.local.entity.UserStats
import com.wortart.app.data.local.entity.VocabEntity

@Database(
    entities = [VocabEntity::class, UserStats::class],
    version = 2,
    exportSchema = true,
)
abstract class WortArtDatabase : RoomDatabase() {

    abstract fun vocabDao(): VocabDao
    abstract fun userStatsDao(): UserStatsDao

    companion object {
        const val DATABASE_NAME = "wortart.db"

        /** Adds the flashcard set columns; favorites and statistics are kept. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf(
                    "tags", "blankDe", "blankAnswer", "blankWrong", "pz2Wrong",
                    "prefixBlankDe", "prefixBlankTitle", "antonymMeaning", "antonymWrong",
                ).forEach { column ->
                    db.execSQL("ALTER TABLE vocab_table ADD COLUMN $column TEXT")
                }
            }
        }

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
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
