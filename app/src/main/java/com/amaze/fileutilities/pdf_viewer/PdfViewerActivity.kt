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
import android.util.Log
import android.view.MotionEvent
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.PdfViewerActivityBinding
import com.amaze.fileutilities.utilis.hideFade
import com.amaze.fileutilities.utilis.showFade
import com.amaze.fileutilities.utilis.showToastInCenter
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnTapListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfDocument.Bookmark
import com.shockwave.pdfium.PdfPasswordException
import java.io.File

class PdfViewerActivity :
    PermissionActivity(),
    OnPageChangeListener,
    OnLoadCompleteListener,
    OnTapListener {

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        PdfViewerActivityBinding.inflate(layoutInflater)
    }
    private lateinit var viewModel: PdfViewerActivityViewModel
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
        if (viewModel.getPdfModel(intent) == null) {
            showToastInCenter(resources.getString(R.string.unsupported_content))
            finish()
        }
        pdfModel = viewModel.getPdfModel(intent)!!
        pdfModel.run {
            Log.i(
                javaClass.simpleName,
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
            File(pdfModel.uri.path).name
        } else {
            meta.title
        }
        viewBinding.pageNumber.text = String.format(
            "%s / %s", viewModel.pageNumber + 1,
            viewBinding.pdfView.pageCount
        )
        viewBinding.switchView.setOnClickListener {
            viewModel.nightMode = !viewModel.nightMode
            viewBinding.pdfView.setNightMode(viewModel.nightMode)
            viewBinding.pdfView.loadPages()
        }
        viewBinding.customToolbar.backButton.setOnClickListener {
            finish()
        }
        viewBinding.customToolbar.title.text = viewModel.pdfFileName
        printBookmarksTree(viewBinding.pdfView.tableOfContents, "-")
    }

    private fun showPasswordDialog(positiveCallback: (salt: String) -> Unit) {
        val inputEditTextField = EditText(this)
        inputEditTextField.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val content: Spanned?
        if (retryPassword) {
            content = Html.fromHtml(
                "<html><body>${resources
                    .getString(R.string.pdf_password_required)}<font color='red'><br>" +
                    "${resources.getString(R.string.wrong_password)}</font></body></html>"
            )
        } else {
            content = Html.fromHtml(resources.getString(R.string.pdf_password_required))
            retryPassword = !retryPassword
        }
        val dialog = AlertDialog.Builder(this).setTitle(R.string.pdf_password_title)
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
            .password(password)
            .onPageChange(this)
            .enableAnnotationRendering(true)
            .enableAntialiasing(true)
            .onLoad(this)
            .onTap(this)
            .defaultPage(viewModel.pageNumber)
            .nightMode(viewModel.nightMode)
            .scrollHandle(DefaultScrollHandle(this))
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
    }

    private fun printBookmarksTree(tree: List<Bookmark>, sep: String) {
        for (b in tree) {
            Log.e(javaClass.simpleName, String.format("%s %s, p %d", sep, b.title, b.pageIdx))
            if (b.hasChildren()) {
                printBookmarksTree(b.children, "$sep-")
            }
        }
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
            viewBinding.customToolbar.root.hideFade(ANIM_DURATION)
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
            viewBinding.customToolbar.root.showFade(ANIM_DURATION)
            viewBinding.hintsParent.showFade(ANIM_DURATION)
        }
    }
}
