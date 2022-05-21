/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.pdf_viewer

import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.InputType
import android.text.Spanned
import android.view.MotionEvent
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.PermissionsActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.PdfViewerActivityBinding
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.utilis.*
import com.amaze.fileutilities.utilis.share.showShareDialog
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnTapListener
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfDocument.Bookmark
import com.shockwave.pdfium.PdfPasswordException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class PdfViewerActivity :
    PermissionsActivity(),
    OnPageChangeListener,
    OnLoadCompleteListener,
    OnTapListener {

    var log: Logger = LoggerFactory.getLogger(PdfViewerActivity::class.java)

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        PdfViewerActivityBinding.inflate(layoutInflater)
    }
    private lateinit var viewModel: PdfViewerActivityViewModel
    private lateinit var filesViewModel: FilesViewModel
    private lateinit var pdfModel: LocalPdfModel
    private var isToolbarVisible = true
    private var retryPassword = false

    companion object {
        const val ANIM_DURATION = 500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel = ViewModelProvider(this).get(PdfViewerActivityViewModel::class.java)
        filesViewModel = ViewModelProvider(this).get(FilesViewModel::class.java)
        if (viewModel.getPdfModel(intent) == null) {
            showToastInCenter(resources.getString(R.string.unsupported_content))
            finish()
        }
        pdfModel = viewModel.getPdfModel(intent)!!
        pdfModel.run {
            log.info(
                "Loading pdf from path ${this.uri.path} " +
                    "and mimetype ${this.mimeType}"
            )
            openPdf(this.uri, null)
        }
    }

    override fun onPageChanged(page: Int, pageCount: Int) {
        viewModel.pageNumber = page
        viewBinding.pageNumber.text = String.format("%s / %s", page + 1, pageCount)
    }

    override fun loadComplete(nbPages: Int) {
        val meta: PdfDocument.Meta = viewBinding.pdfView.documentMeta
        viewModel.pdfFileName = if (meta.title.isEmpty()) {
            pdfModel.uri.getFileFromUri()?.name
        } else {
            meta.title
        }
        viewBinding.pageNumber.text = String.format(
            "%s / %s", viewModel.pageNumber + 1,
            viewBinding.pdfView.pageCount
        )
        viewBinding.switchView.setOnClickListener {
            switchView()
        }
        viewBinding.customToolbar.setBackButtonClickListener { finish() }
        viewBinding.customToolbar.setTitle(viewModel.pdfFileName ?: "pdf")
        viewBinding.customToolbar.setOverflowPopup(R.menu.pdf_activity) { item ->
            when (item!!.itemId) {
                R.id.info -> {
                    showInfoDialog()
                }
                R.id.invert_colors -> {
                    switchView()
                }
                R.id.bookmarks -> {
                    showBookmarksDialog(viewBinding.pdfView.tableOfContents, "-")
                }
                R.id.share -> {
                    var processed = false
                    pdfModel.uri.getFileFromUri().let {
                        file ->
                        if (file == null) {
                            showToastInCenter(getString(R.string.failed_to_share))
                            return@let
                        }
                        val mediaFile = MediaFileInfo.fromFile(
                            file,
                            MediaFileInfo.ExtraInfo(
                                MediaFileInfo.MEDIA_TYPE_DOCUMENT,
                                null, null, null
                            )
                        )
                        filesViewModel.getShareMediaFilesAdapter(listOf(mediaFile))
                            .observe(this) { shareAdapter ->
                                if (shareAdapter == null) {
                                    if (processed) {
                                        showToastInCenter(
                                            this.resources.getString(R.string.failed_to_share)
                                        )
                                    } else {
                                        showToastInCenter(
                                            resources
                                                .getString(R.string.please_wait)
                                        )
                                        processed = true
                                    }
                                } else {
                                    showShareDialog(
                                        this, this.layoutInflater,
                                        shareAdapter
                                    )
                                }
                            }
                    }
                }
            }
            true
        }
    }

    private fun switchView() {
        viewModel.nightMode = !viewModel.nightMode
        viewBinding.pdfView.setNightMode(viewModel.nightMode)
        if (viewModel.nightMode) {
            viewBinding.hintsParent.background = ResourcesCompat.getDrawable(
                resources,
                R.drawable.button_curved_unselected, theme
            )
            viewBinding.switchView.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_outline_light_mode_32, theme
                )
            )
        } else {
            viewBinding.hintsParent.background = ResourcesCompat.getDrawable(
                resources,
                R.drawable.background_curved_dark_2, theme
            )
            viewBinding.switchView.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_outline_dark_mode_32, theme
                )
            )
        }
        viewBinding.pdfView.loadPages()
    }

    private fun showPasswordDialog(positiveCallback: (salt: String) -> Unit) {
        val inputEditTextField = EditText(this)
        inputEditTextField.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val content: Spanned?
        if (retryPassword) {
            content = Html.fromHtml(
                "<html><body>" +
                    "${resources
                        .getString(R.string.pdf_password_required)}" +
                    "<font color='red'><br>" +
                    "${resources.getString(R.string.wrong_password)}</font></body></html>"
            )
        } else {
            content = Html.fromHtml(resources.getString(R.string.pdf_password_required))
            retryPassword = !retryPassword
        }
        val dialog = AlertDialog.Builder(this, R.style.Custom_Dialog_Dark)
            .setTitle(R.string.pdf_password_title)
            .setMessage(content)
            .setView(inputEditTextField)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                val salt = inputEditTextField.text.toString()
                positiveCallback.invoke(salt)
                dialog.dismiss()
            }
            .setNegativeButton(
                R.string.cancel
            ) { _, _ -> finish() }
            .create()
        dialog.show()
    }

    private fun openPdf(uri: Uri, password: String?) {
        viewBinding.pdfView.fromUri(uri).defaultPage(0)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .password(password)
            .onPageChange(this)
            .enableAnnotationRendering(true)
            .enableAntialiasing(true)
            .onLoad(this)
            .onTap(this)
            .defaultPage(viewModel.pageNumber)
            .nightMode(viewModel.nightMode)
            .spacing(32.dp.toInt())
            .pageFitPolicy(FitPolicy.BOTH)
            .scrollHandle(PdfScrollHandle(this))
            .onError { t ->
                t?.let {
                    if (t is PdfPasswordException) {
                        showPasswordDialog {
                            openPdf(uri, it)
                        }
                    } else {
                        this@PdfViewerActivity.showToastInCenter(
                            resources
                                .getString(R.string.unsupported_content)
                        )
                    }
                }
            }
            .load()
        // TODO: provide option to use better quality with pdfView.useBestQuality
    }

    /*private fun extractText(page: Int) {
        try {
            val pdfiumCore = PdfiumCore(this)
            val pd = this.contentResolver.openFileDescriptor(pdfModel.uri, "r")
            val pdfDocument: PdfDocument = pdfiumCore.newDocument(pd)
            pdfiumCore.openPage(pdfDocument, page)
            val width: Int = pdfiumCore.getPageWidthPoint(pdfDocument, page)
            val height: Int = pdfiumCore.getPageHeightPoint(pdfDocument, page)

            // ARGB_8888 - best quality, high memory usage, higher possibility of OutOfMemoryError
            // RGB_565 - little worse quality, twice less memory usage
            val bitmap: Bitmap = Bitmap.createBitmap(
                width, height,
                Bitmap.Config.ARGB_8888
            )
            pdfiumCore.renderPageBitmap(
                pdfDocument, bitmap, page, 0, 0,
                width, height
            )
            viewModel.getCurrentPageText(bitmap, externalCacheDir!!.path).observe(this) {
                if (it == null) {
                    showToastInCenter(resources.getString(R.string.analysing))
                } else {
                    Log.e(javaClass.simpleName, "CUSTOM:\n$it")
                    showToastInCenter(resources.getString(R.string.page_copied_to_clipboard))
                    Utils.copyToClipboard(this, it)
                }
            }
            pdfiumCore.closeDocument(pdfDocument) // important!
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }*/

    private fun showInfoDialog() {
        var dialogMessage = ""
        viewBinding.pdfView.documentMeta.let {
            meta ->
            if (meta.title.isNotBlank()) {
                dialogMessage += "${resources.getString(R.string.title)}: ${meta.author}" + "\n"
            }
            if (meta.author.isNotBlank()) {
                dialogMessage += "${resources.getString(R.string.author)}: ${meta.author}" + "\n"
            }
            if (meta.subject.isNotBlank()) {
                dialogMessage += "${resources.getString(R.string.subject)}: ${meta.subject}" + "\n"
            }
            if (meta.keywords.isNotBlank()) {
                dialogMessage += "${resources.getString(R.string.keywords)}: " +
                    "${meta.keywords}" + "\n"
            }
            if (meta.creator.isNotBlank()) {
                dialogMessage += "${resources.getString(R.string.creator)}:" +
                    " ${meta.creator}" + "\n"
            }
            if (meta.creationDate.isNotBlank()) {
                dialogMessage += "${resources.getString(R.string.creation_date)}:" +
                    " ${meta.creationDate}" + "\n"
            }
            if (meta.producer.isNotBlank()) {
                dialogMessage += "${resources.getString(R.string.producer)}: " +
                    "${meta.producer}" + "\n"
            }
            if (meta.modDate.isNotBlank()) {
                dialogMessage += "${resources.getString(R.string.modification_date)}:" +
                    " ${meta.modDate}" + "\n"
            }
        }
        val builder: AlertDialog.Builder = this.let {
            AlertDialog.Builder(it, R.style.Custom_Dialog_Dark)
        }
        builder.setMessage(dialogMessage)
            .setTitle(R.string.information)
            .setNegativeButton(R.string.close) { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun showBookmarksDialog(tree: List<Bookmark>, sep: String) {
        val bookmarksText = getBookmarksTree(tree, sep)
        val builder: AlertDialog.Builder = this.let {
            AlertDialog.Builder(it, R.style.Custom_Dialog_Dark)
        }
        builder.setMessage(bookmarksText)
            .setTitle(R.string.bookmarks)
            .setNegativeButton(R.string.close) { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun getBookmarksTree(tree: List<Bookmark>, sep: String): String {
        var curr = "\n"
        for (b in tree) {
            log.info(String.format("%s %s, p %d", sep, b.title, b.pageIdx))
            if (b.hasChildren()) {
                curr += getBookmarksTree(b.children, "$sep-")
            }
        }
        return curr
    }

    override fun onTap(e: MotionEvent?): Boolean {
        isToolbarVisible = !isToolbarVisible
        refactorSystemUi(isToolbarVisible)
        return true
    }

    private fun refactorSystemUi(hide: Boolean) {
        if (hide) {
            WindowInsetsControllerCompat(
                window,
                viewBinding.root
            ).let {
                controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat
                    .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            viewBinding.customToolbar.hideFade(ANIM_DURATION)
            viewBinding.hintsParent.hideFade(ANIM_DURATION)
        } else {
            WindowInsetsControllerCompat(
                window,
                viewBinding.root
            ).let {
                controller ->
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat
                    .BEHAVIOR_SHOW_BARS_BY_TOUCH
            }
            viewBinding.customToolbar.showFade(ANIM_DURATION)
            viewBinding.hintsParent.showFade(ANIM_DURATION)
        }
    }
}
