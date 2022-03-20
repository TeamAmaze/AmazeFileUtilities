/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
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
            val mimeType = intent.type
            val epubUri = intent.data
            if (epubUri == null) {
                showToastInCenter(resources.getString(R.string.unsupported_content))
            }
            log.info(
                "Loading epub from path ${epubUri?.path} " +
                    "and mimetype $mimeType"
            )
            epubModel = LocalEpubModel(uri = epubUri!!, mimeType = mimeType)
            val config: Config = Config()
                .setAllowedDirection(Config.AllowedDirection.ONLY_HORIZONTAL)
                .setDirection(Config.Direction.HORIZONTAL)
                .setFontSize(1)
                .setNightMode(false)
                .setThemeColorInt(resources.getColor(R.color.blue))
                .setShowTts(false)
            FolioReader.get()
                .setConfig(config, true)
                .openBook(epubUri.getFileFromUri(this)!!.canonicalPath)
            finish()
        }
    }
}
