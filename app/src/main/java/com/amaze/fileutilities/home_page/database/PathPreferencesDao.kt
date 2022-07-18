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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PathPreferencesDao {

    @Query("SELECT * FROM pathpreferences")
    fun getAll(): List<PathPreferences>

    @Query("SELECT * FROM pathpreferences WHERE feature=:feature")
    fun findByFeature(feature: Int): List<PathPreferences>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(analysis: PathPreferences)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(analysis: List<PathPreferences>)

    @Delete
    fun delete(pathPreferences: PathPreferences)
}
