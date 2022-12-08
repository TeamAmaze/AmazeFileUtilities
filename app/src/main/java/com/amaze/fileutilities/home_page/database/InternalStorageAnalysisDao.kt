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
interface InternalStorageAnalysisDao {

    @Query("SELECT * FROM internalstorageanalysis where is_mediastore=0")
    fun getAll(): List<InternalStorageAnalysis>

    @Query("SELECT * FROM internalstorageanalysis where depth<=:depth and is_mediastore=0")
    fun getAllShallow(depth: Int): List<InternalStorageAnalysis>

    @Query("SELECT * FROM internalstorageanalysis where is_mediastore=1")
    fun getAllMediaFiles(): List<InternalStorageAnalysis>

    @Query("SELECT * FROM internalstorageanalysis where is_empty=1")
    fun getAllEmptyFiles(): List<InternalStorageAnalysis>

    @Query("SELECT * FROM internalstorageanalysis WHERE sha256_checksum=:sha256Checksum")
    fun findBySha256Checksum(sha256Checksum: String): InternalStorageAnalysis?

    @Query(
        "SELECT * FROM internalstorageanalysis " +
            "WHERE is_mediastore = 1 and sha256_checksum=:sha256Checksum"
    )
    fun findMediaFileBySha256Checksum(sha256Checksum: String): InternalStorageAnalysis?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(analysis: InternalStorageAnalysis)

    @Delete
    fun delete(user: InternalStorageAnalysis)
}
