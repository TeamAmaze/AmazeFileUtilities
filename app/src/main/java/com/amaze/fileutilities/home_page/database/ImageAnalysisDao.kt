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
interface ImageAnalysisDao {

    @Query("SELECT * FROM imageanalysis")
    fun getAll(): List<ImageAnalysis>

    @Query("SELECT * FROM imageanalysis WHERE is_sleeping=1")
    fun getAllSleeping(): List<ImageAnalysis>

    @Query("UPDATE imageanalysis SET is_sleeping=0 WHERE file_path IN(:pathList)")
    fun cleanIsSleeping(pathList: List<String>)

    @Query("SELECT * FROM imageanalysis WHERE is_distracted=1")
    fun getAllDistracted(): List<ImageAnalysis>

    @Query("UPDATE imageanalysis SET is_distracted=0 WHERE file_path IN(:pathList)")
    fun cleanIsDistracted(pathList: List<String>)

    @Query("SELECT * FROM imageanalysis WHERE is_sad=1")
    fun getAllSad(): List<ImageAnalysis>

    @Query("UPDATE imageanalysis SET is_sad=0 WHERE file_path IN(:pathList)")
    fun cleanIsSad(pathList: List<String>)

    @Query("SELECT * FROM imageanalysis WHERE face_count=1")
    fun getAllSelfie(): List<ImageAnalysis>

    @Query("UPDATE imageanalysis SET face_count=0 WHERE file_path IN(:pathList)")
    fun cleanIsSelfie(pathList: List<String>)

    @Query("SELECT * FROM imageanalysis WHERE face_count>1")
    fun getAllGroupPic(): List<ImageAnalysis>

    @Query("UPDATE imageanalysis SET face_count=0 WHERE file_path IN(:pathList)")
    fun cleanIsGroupPic(pathList: List<String>)

    @Query("SELECT * FROM imageanalysis WHERE file_path=:path")
    fun findByPath(path: String): ImageAnalysis?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(analysis: ImageAnalysis)

    @Delete
    fun deleteAll(vararg analysis: ImageAnalysis)

    @Delete
    fun delete(user: ImageAnalysis)

    @Query("DELETE FROM imageanalysis WHERE file_path like '%' || :path || '%'")
    fun deleteByPathContains(path: String)
}
