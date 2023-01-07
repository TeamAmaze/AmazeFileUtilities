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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LowLightAnalysisDao {

    @Query("SELECT * FROM lowlightanalysis WHERE file_path=:path")
    fun findByPath(path: String): LowLightAnalysis?

    @Query("SELECT * FROM lowlightanalysis where is_low_light=1")
    fun getAllLowLight(): List<LowLightAnalysis>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(analysis: LowLightAnalysis)

    @Delete
    fun delete(user: LowLightAnalysis)

    @Query("DELETE FROM lowlightanalysis WHERE file_path like '%' || :path || '%'")
    fun deleteByPathContains(path: String)

    @Query("UPDATE lowlightanalysis SET is_low_light=0 WHERE file_path IN(:pathList)")
    fun cleanIsLowLight(pathList: List<String>)
}
