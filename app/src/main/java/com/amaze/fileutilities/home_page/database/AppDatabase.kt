/*
 * Copyright (C) 2021-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
        Trial::class, Lyrics::class, InstalledApps::class, SimilarImagesAnalysis::class,
        SimilarImagesAnalysisMetadata::class
    ],
    exportSchema = true,
    version = 5
)
@TypeConverters(DbConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun analysisDao(): ImageAnalysisDao
    abstract fun blurAnalysisDao(): BlurAnalysisDao
    abstract fun memesAnalysisDao(): MemeAnalysisDao
    abstract fun lowLightAnalysisDao(): LowLightAnalysisDao
    abstract fun similarImagesAnalysisDao(): SimilarImagesAnalysisDao
    abstract fun similarImagesAnalysisMetadataDao(): SimilarImagesAnalysisMetadataDao
    abstract fun internalStorageAnalysisDao(): InternalStorageAnalysisDao
    abstract fun pathPreferencesDao(): PathPreferencesDao
    abstract fun videoPlayerStateDao(): VideoPlayerStateDao
    abstract fun trialValidatorDao(): TrialValidatorDao
    abstract fun installedAppsDao(): InstalledAppsDao
    abstract fun lyricsDao(): LyricsDao

    companion object {
        private var appDatabase: AppDatabase? = null

        fun getInstance(applicationContext: Context): AppDatabase {
            if (appDatabase == null) {
                appDatabase = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java, "amaze-utils"
                ).allowMainThreadQueries()
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `InstalledApps` (`_id` INTEGER " +
                        "PRIMARY KEY AUTOINCREMENT NOT NULL, `package_name` TEXT NOT NULL, " +
                        "`data_dirs` TEXT NOT NULL)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_InstalledApps_package_name`" +
                        " ON `InstalledApps` (`package_name`)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `SimilarImagesAnalysis` " +
                        "(`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                        " `histogram_checksum` TEXT NOT NULL, `files_path` TEXT NOT NULL)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_SimilarImagesAnalysis_histogram_checksum` " +
                        "ON `SimilarImagesAnalysis` (`histogram_checksum`)"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `SimilarImagesAnalysisMetadata` (`_id` " +
                        "INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `parent_path` " +
                        "TEXT NOT NULL, `file_path` TEXT NOT NULL, `blue_channel` " +
                        "TEXT NOT NULL, `green_channel` TEXT NOT NULL, `red_channel` " +
                        "TEXT NOT NULL, `datapoints` INTEGER NOT NULL, `threshold` " +
                        "INTEGER NOT NULL, `is_analysed` INTEGER NOT NULL)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_SimilarImagesAnalysisMetadata_file_path_parent_path` " +
                        "ON `SimilarImagesAnalysisMetadata` (`file_path`, `parent_path`)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `AppStorageStats` " +
                        "(`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timestamp` LONG NOT NULL, " +
                        "`package_size` LONG NOT NULL," +
                        "FOREIGN KEY(`package_id`) REFERENCES `InstalledApps`(`_id`) ON UPDATE " +
                        "CASCADE ON DELETE NO ACTION)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_AppStorageStats_package_id`" +
                        " ON `AppStorageStats` (`package_id`)"
                )
            }
        }
    }
}
