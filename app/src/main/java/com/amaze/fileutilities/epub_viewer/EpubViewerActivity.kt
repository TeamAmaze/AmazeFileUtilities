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

package com.amaze.fileutilities.epub_viewer

import android.os.Bundle
import com.amaze.fileutilities.PermissionsActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.EpubViewerActivityBinding
import com.amaze.fileutilities.utilis.getFileFromUri
import com.amaze.fileutilities.utilis.showToastInCenter
import com.folioreader.Config
import com.folioreader.FolioReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EpubViewerActivity : PermissionsActivity() {

    var log: Logger = LoggerFactory.getLogger(EpubViewerActivity::class.java)

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        EpubViewerActivityBinding.inflate(layoutInflater)
    }
    private lateinit var epubModel: LocalEpubModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        if (savedInstanceState == null) {
            if (intent == null) {
                showToastInCenter(resources.getString(R.string.unsupported_content))
                return
            }
            val mimeType = intent.type
            val epubUri = intent.data
            if (epubUri == null) {
                showToastInCenter(resources.getString(R.string.unsupported_content))
                return
            }
            log.info(
                "Loading epub from path ${epubUri.path} " +
                    "and mimetype $mimeType"
            )
            epubModel = LocalEpubModel(uri = epubUri, mimeType = mimeType)
            val filePathFromUri = epubUri.getFileFromUri()
            if (filePathFromUri != null) {
                val config: Config = Config()
                    .setAllowedDirection(Config.AllowedDirection.ONLY_HORIZONTAL)
                    .setDirection(Config.Direction.HORIZONTAL)
                    .setFontSize(1)
                    .setNightMode(false)
                    .setThemeColorInt(resources.getColor(R.color.blue))
                    .setShowTts(false)
                FolioReader.get()
                    .setConfig(config, true)
                    .openBook(filePathFromUri.canonicalPath)
                finish()
            } else {
                showToastInCenter(resources.getString(R.string.unsupported_content))
                return
            }
        }
    }
}
