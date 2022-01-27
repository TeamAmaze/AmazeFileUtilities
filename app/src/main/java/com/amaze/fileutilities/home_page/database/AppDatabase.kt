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
import com.amaze.fileutilities.utilis.DbConverters

@Database(entities = [MediaFileAnalysis::class, InternalStorageAnalysis::class], version = 1)
@TypeConverters(DbConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun analysisDao(): MediaFilesAnalysisDao
    abstract fun internalStorageAnalysisDao(): InternalStorageAnalysisDao

    companion object {
        private var appDatabase: AppDatabase? = null

        fun getInstance(applicationContext: Context): AppDatabase {
            if (appDatabase == null) {
                appDatabase = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java, "amaze-utils"
                ).build()
            }
            return appDatabase!!
        }
    }
}
