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
interface ImageAnalysisDao {

    @Query("SELECT * FROM imageanalysis")
    fun getAll(): List<ImageAnalysis>

    @Query("SELECT * FROM imageanalysis where is_blur=1")
    fun getAllBlur(): List<ImageAnalysis>

    @Query("SELECT * FROM imageanalysis where is_low_light=1")
    fun getAllLowLight(): List<ImageAnalysis>

    @Query("SELECT * FROM imageanalysis where is_meme=1")
    fun getAllMeme(): List<ImageAnalysis>

    @Query("SELECT * FROM imageanalysis where is_sleeping=1")
    fun getAllSleeping(): List<ImageAnalysis>

    @Query("SELECT * FROM imageanalysis where is_distracted=1")
    fun getAllDistracted(): List<ImageAnalysis>

    @Query("SELECT * FROM imageanalysis where is_sad=1")
    fun getAllSad(): List<ImageAnalysis>

    @Query("SELECT * FROM imageanalysis where face_count=1")
    fun getAllSelfie(): List<ImageAnalysis>

    @Query("SELECT * FROM imageanalysis where face_count>1")
    fun getAllGroupPic(): List<ImageAnalysis>

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
