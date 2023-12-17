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

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Date

@RunWith(AndroidJUnit4::class)
class StorageStatToAppNameDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var storageStatToAppNameDao: StorageStatToAppNameDao
    private lateinit var appStorageStatsDao: AppStorageStatsDao
    private lateinit var installedAppsDao: InstalledAppsDao
    private lateinit var appEntry: InstalledApps

    private val appName = "testApp"
    private val appName2 = "testApp2"

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        storageStatToAppNameDao = db.storageStatsPerAppDao()
        appStorageStatsDao = db.appStorageStatsDao()
        installedAppsDao = db.installedAppsDao()
        installedAppsDao.insert(InstalledApps(appName, listOf()))
        installedAppsDao.insert(InstalledApps(appName2, listOf()))
        val entry = installedAppsDao.findByPackageName(appName)
        Assert.assertNotNull(entry)
        appEntry = entry!!
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun findAllTest() {
        val range = LongRange(0, 3)
        val appNames = listOf(appName, appName2)
        for (i in range) {
            for (name in appNames) {
                appStorageStatsDao.insert(name, Date(i), i)
            }
        }

        val allStorageStatToAppName = storageStatToAppNameDao.findAll()
        Assert.assertEquals(
            "Did not return correct number of entries but $allStorageStatToAppName",
            range.count() * 2,
            allStorageStatToAppName.size
        )

        for (i in range) {
            for (name in appNames) {
                Assert.assertNotNull(
                    "",
                    allStorageStatToAppName.find {
                        it.packageName == name && it.packageSize == i && it.timestamp == Date(i)
                    }
                )
            }
        }
    }

    @Test
    @Throws(IOException::class)
    fun findByPackageNameTest() {
        val range = LongRange(0, 3)
        val appNames = listOf(appName, appName2)
        for (i in range) {
            for (name in appNames) {
                appStorageStatsDao.insert(name, Date(i), i)
            }
        }

        val storageStats = storageStatToAppNameDao.findByPackageName(appNames.last())
        Assert.assertTrue(
            "Returned storage stats that were not associated to ${appNames.last()}: $storageStats",
            storageStats.all { it.packageName == appNames.last() }
        )
        Assert.assertEquals(
            "Did not return expected number of entries: $storageStats",
            range.count(),
            storageStats.size
        )
        for (i in range) {
            Assert.assertNotNull(
                "Did not find expected entry with packageSize $i and timestamp ${Date(i)}: " +
                    "$storageStats",
                storageStats.find {
                    it.packageSize == i && it.timestamp == Date(i)
                }
            )
        }
    }

    @Test
    @Throws(IOException::class)
    fun findOldestInPeriodTest() {
        val range = LongProgression.fromClosedRange(0, 30, 10)
        val appNames = listOf(appName, appName2)
        for (i in range) {
            for (name in appNames) {
                appStorageStatsDao.insert(name, Date(i), i)
            }
        }
        val periodStart = Date(4)
        val periodEnd = Date(43)
        val entry = storageStatToAppNameDao.findOldestWithinPeriod(
            appNames.first(), periodStart, periodEnd
        )
        Assert.assertNotNull(entry)
        val statToAppName = entry!!
        Assert.assertEquals(appNames.first(), statToAppName.packageName)
        Assert.assertEquals(Date(10), statToAppName.timestamp)
    }
}
