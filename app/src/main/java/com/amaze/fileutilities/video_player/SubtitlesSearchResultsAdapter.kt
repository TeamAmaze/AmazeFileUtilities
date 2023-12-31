/*
 * Copyright (C) 2021-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.fileutilities.video_player

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.utilis.showToastInCenter

class SubtitlesSearchResultsAdapter(
    val appContext: Context,
    val listState: List<SubtitleResult>,
    private val downloadFileCallback: (String?, String?) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = mInflater.inflate(
            R.layout.subtitles_result_row_item, parent,
            false
        )
        return SubtitlesSearchResultsViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SubtitlesSearchResultsViewHolder) {
            val subtitleResult = listState[position]
            holder.movieName.text = subtitleResult.title
            holder.info.text =
                "${appContext.resources.getString(R.string.cd)}: " +
                "${subtitleResult.cdNumber ?: ""} | " +
                "${appContext.resources.getString(R.string.upload_date)}: " +
                "${subtitleResult.uploadDate?.replace("\n", "")} | " +
                "${appContext.resources.getString(R.string.rating)}: " +
                "${subtitleResult.subtitleRating ?: ""} | " +
                "${appContext.resources.getString(R.string.language)}: " +
                "${subtitleResult.language ?: ""} | " +
                "${appContext.resources.getString(R.string.uploader)}:" +
                " ${subtitleResult.uploader ?: ""} | " +
                "${appContext.resources.getString(R.string.downloads)}: " +
                "${subtitleResult.downloads ?: ""}"
            holder.parentLayout.setOnClickListener {
                if (subtitleResult.downloadId == null) {
                    appContext.showToastInCenter(
                        appContext.resources
                            .getString(R.string.choose_different_subtitle)
                    )
                } else {
                    downloadFileCallback.invoke(
                        subtitleResult.downloadId,
                        subtitleResult.downloadFileName
                    )
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return listState.size
    }

    private val mInflater: LayoutInflater
        get() = appContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    /**
     * downloadLink - in format - /en/subtitleserve/sub/5136695
     */
    data class SubtitleResult(
        var title: String? = null,
        var language: String? = null,
        var cdNumber: String? = null,
        var uploadDate: String? = null,
        var downloadId: String? = null,
        var subtitleRating: String? = null,
        var downloads: String? = null,
        var uploader: String? = null,
        var downloadFileName: String? = null,
    )

    class SubtitlesSearchResultsViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        @JvmField
        val parentLayout: RelativeLayout = view.findViewById(R.id.row_layout_parent)

        @JvmField
        val movieName: TextView = view.findViewById(R.id.movie_name) as TextView

        @JvmField
        val info: TextView = view.findViewById(R.id.info) as TextView
    }
}
