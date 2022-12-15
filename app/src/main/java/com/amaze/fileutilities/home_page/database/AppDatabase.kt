/*
 * Copyright (C) 2021-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Utilities.
 *
 * Amaze File Utilities is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
