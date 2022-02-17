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
interface BlurAnalysisDao {

    @Query("SELECT * FROM bluranalysis WHERE file_path=:path")
    fun findByPath(path: String): BlurAnalysis?

    @Query("SELECT * FROM bluranalysis where is_blur=1")
    fun getAllBlur(): List<BlurAnalysis>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(analysis: BlurAnalysis)

    @Delete
    fun delete(user: BlurAnalysis)

    @Query("DELETE FROM bluranalysis WHERE file_path like '%' || :path || '%'")
    fun deleteByPathContains(path: String)
}
