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

import androidx.room.*

@Dao
interface MediaFilesAnalysisDao {

    @Query("SELECT * FROM mediafileanalysis")
    fun getAll(): List<MediaFileAnalysis>

    @Query("SELECT * FROM mediafileanalysis where is_blur=1")
    fun getAllBlur(): List<MediaFileAnalysis>

    @Query("SELECT * FROM mediafileanalysis where is_meme=1")
    fun getAllMeme(): List<MediaFileAnalysis>

    @Query("SELECT * FROM mediafileanalysis WHERE file_path=:path")
    fun findByPath(path: String): MediaFileAnalysis?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(analysis: MediaFileAnalysis)

    @Delete
    fun deleteAll(vararg analysis: MediaFileAnalysis)

    @Delete
    fun delete(user: MediaFileAnalysis)
}
