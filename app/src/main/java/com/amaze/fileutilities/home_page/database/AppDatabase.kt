/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.amaze.fileutilities.utilis.DbConverters

@Database(
    entities = [
        ImageAnalysis::class, InternalStorageAnalysis::class, PathPreferences::class,
        BlurAnalysis::class, LowLightAnalysis::class, MemeAnalysis::class, VideoPlayerState::class,
        Trial::class, Lyrics::class
    ],
    version = 2
)
@TypeConverters(DbConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun analysisDao(): ImageAnalysisDao
    abstract fun blurAnalysisDao(): BlurAnalysisDao
    abstract fun memesAnalysisDao(): MemeAnalysisDao
    abstract fun lowLightAnalysisDao(): LowLightAnalysisDao
    abstract fun internalStorageAnalysisDao(): InternalStorageAnalysisDao
    abstract fun pathPreferencesDao(): PathPreferencesDao
    abstract fun videoPlayerStateDao(): VideoPlayerStateDao
    abstract fun trialValidatorDao(): TrialValidatorDao
    abstract fun lyricsDao(): LyricsDao

    companion object {
        private var appDatabase: AppDatabase? = null

        fun getInstance(applicationContext: Context): AppDatabase {
            if (appDatabase == null) {
                appDatabase = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java, "amaze-utils"
                ).allowMainThreadQueries()
                    .addMigrations(MIGRATION_1_2)
                    .build()
            }
            return appDatabase!!
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Lyrics` (`_id` INTEGER " +
                        "PRIMARY KEY AUTOINCREMENT NOT NULL, `file_path` TEXT NOT NULL, " +
                        "`lyrics_text` TEXT NOT NULL, `is_synced` INTEGER NOT NULL)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_Lyrics_file_path`" +
                        " ON `Lyrics` (`file_path`)"
                )
            }
        }
    }
}
