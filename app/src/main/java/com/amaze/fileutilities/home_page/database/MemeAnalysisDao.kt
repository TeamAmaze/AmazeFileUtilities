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
interface MemeAnalysisDao {

    @Query("SELECT * FROM memeanalysis WHERE file_path=:path")
    fun findByPath(path: String): MemeAnalysis?

    @Query("SELECT * FROM memeanalysis where is_meme=1")
    fun getAllMeme(): List<MemeAnalysis>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(analysis: MemeAnalysis)

    @Delete
    fun delete(user: MemeAnalysis)

    @Query("DELETE FROM memeanalysis WHERE file_path like '%' || :path || '%'")
    fun deleteByPathContains(path: String)

    @Query("UPDATE memeanalysis SET is_meme=0 WHERE file_path IN(:pathList)")
    fun cleanIsMeme(pathList: List<String>)
}
