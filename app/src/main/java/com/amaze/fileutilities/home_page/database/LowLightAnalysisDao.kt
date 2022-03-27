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
