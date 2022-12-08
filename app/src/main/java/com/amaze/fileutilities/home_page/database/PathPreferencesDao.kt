/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
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
