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
interface TrialValidatorDao {

    @Query("SELECT * FROM trial")
    fun getAll(): List<Trial>

    @Query("SELECT * FROM trial WHERE subscription_status<>1001")
    fun getAllSubscribed(): List<Trial>

    @Query("SELECT * FROM trial WHERE device_id=:deviceId")
    fun findByDeviceId(deviceId: String): Trial?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(trial: Trial)

    @Delete
    fun deleteAll(vararg trial: Trial)

    @Delete
    fun delete(trial: Trial)
}
