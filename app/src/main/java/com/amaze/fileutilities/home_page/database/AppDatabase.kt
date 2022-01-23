package com.amaze.fileutilities.home_page.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Analysis::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun analysisDao(): AnalysisDao

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