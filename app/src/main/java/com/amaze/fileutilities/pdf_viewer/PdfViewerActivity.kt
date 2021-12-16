package com.amaze.fileutilities.pdf_viewer

import android.os.Bundle
import android.util.Log
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.databinding.PdfViewerActivityBinding
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfDocument.Bookmark
import java.io.File
import org.apache.poi.xwpf.extractor.XWPFWordExtractor

import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.FileInputStream


class PdfViewerActivity: PermissionActivity(), OnPageChangeListener, OnLoadCompleteListener {

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        PdfViewerActivityBinding.inflate(layoutInflater)
    }
    private lateinit var pdfModel: LocalPdfModel
    var pageNumber = 0
    var pdfFileName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
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
                .scrollHandle(DefaultScrollHandle(this))
                .load()
        }
    }

    override fun onPageChanged(page: Int, pageCount: Int) {
        pageNumber = page
        title = String.format("%s %s / %s", pdfFileName, page + 1, pageCount);
    }

    override fun loadComplete(nbPages: Int) {
        val meta: PdfDocument.Meta = viewBinding.pdfView.documentMeta
        pdfFileName = if (meta.title.isEmpty()) File(pdfModel.uri.path).name else meta.title
        title = String.format("%s %s / %s", pdfFileName, pageNumber + 1, viewBinding.pdfView.pageCount);
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