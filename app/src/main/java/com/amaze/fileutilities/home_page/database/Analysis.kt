package com.amaze.fileutilities.home_page.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Analysis(@PrimaryKey(autoGenerate = true)
                    @ColumnInfo(name = "_id")
                    val uid: Int?,
                    @ColumnInfo(name = "file_path") val filePath: String,
                    @ColumnInfo(name = "is_blur") val isBlur: Boolean,
                    @ColumnInfo(name = "is_meme") val isMeme: Boolean)