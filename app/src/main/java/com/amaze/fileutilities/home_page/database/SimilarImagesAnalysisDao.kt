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
interface SimilarImagesAnalysisDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(imagesAnalysis: SimilarImagesAnalysis)

    @Query("SELECT * FROM similarimagesanalysis WHERE histogram_checksum=:histogramChecksum")
    fun findByHistogramChecksum(histogramChecksum: String): SimilarImagesAnalysis?

    @Query("SELECT * FROM similarimagesanalysis")
    fun getAll(): List<SimilarImagesAnalysis>

    @Delete
    fun delete(imagesAnalysis: SimilarImagesAnalysis)

    @Query("DELETE FROM similarimagesanalysis")
    fun deleteAll()
}
