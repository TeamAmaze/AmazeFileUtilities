package com.amaze.fileutilities.home_page.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AnalysisDao {

    @Query("SELECT * FROM analysis")
    fun getAll(): LiveData<List<Analysis>>

    @Insert
    fun insertAll(vararg analysis: Analysis)

    @Delete
    fun deleteAll(vararg analysis: Analysis)

    @Delete
    fun delete(user: Analysis)
}