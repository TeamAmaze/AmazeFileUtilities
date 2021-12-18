package com.amaze.fileutilities.pdf_viewer

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.PdfViewerActivityBinding
import com.amaze.fileutilities.image_viewer.ImageViewerViewModel
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfDocument.Bookmark
import java.io.File


class PdfViewerActivity: PermissionActivity(), OnPageChangeListener, OnLoadCompleteListener {

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        PdfViewerActivityBinding.inflate(layoutInflater)
    }
    private lateinit var viewModel: PdfViewerActivityViewModel
    private lateinit var pdfModel: LocalPdfModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel = ViewModelProvider(this).get(PdfViewerActivityViewModel::class.java)
        if (savedInstanceState == null) {
            val mimeType = intent.type
            val pdfUri = intent.data
            Log.i(javaClass.simpleName, "Loading pdf from path ${pdfUri?.path} " +
                    "and mimetype $mimeType")
            pdfModel = LocalPdfModel(uri = pdfUri!!, mimeType = mimeType!!)
            viewBinding.pdfView.fromUri(pdfUri).defaultPage(0)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .nightMode(viewModel.nightMode)
                .scrollHandle(DefaultScrollHandle(this))
                .load()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.pdf_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.also {
            it.findItem(R.id.invert_colors).isChecked = viewModel.nightMode
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.invert_colors -> {
                viewModel.nightMode = !viewModel.nightMode
                viewBinding.pdfView.setNightMode(viewModel.nightMode)
                item.isChecked = viewModel.nightMode
                viewBinding.pdfView.loadPages()
            }
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPageChanged(page: Int, pageCount: Int) {
        viewModel.pageNumber = page
        title = String.format("%s %s / %s", viewModel.pdfFileName, page + 1, pageCount);
    }

    override fun loadComplete(nbPages: Int) {
        val meta: PdfDocument.Meta = viewBinding.pdfView.documentMeta
        viewModel.pdfFileName = if (meta.title.isEmpty()) File(pdfModel.uri.path).name else meta.title
        title = String.format("%s %s / %s", viewModel.pdfFileName, viewModel.pageNumber + 1,
            viewBinding.pdfView.pageCount);
        printBookmarksTree(viewBinding.pdfView.tableOfContents, "-")
    }

    private fun printBookmarksTree(tree: List<Bookmark>, sep: String) {
        for (b in tree) {
            Log.e(javaClass.simpleName, String.format("%s %s, p %d", sep, b.title, b.pageIdx))
            if (b.hasChildren()) {
                printBookmarksTree(b.children, "$sep-")
            }
        }
    }
}