package com.amaze.fileutilities.docx_viewer

import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebSettingsCompat.FORCE_DARK_OFF
import androidx.webkit.WebSettingsCompat.FORCE_DARK_ON
import androidx.webkit.WebViewFeature
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.DocxViewerActivityBinding
import org.zwobble.mammoth.DocumentConverter
import org.zwobble.mammoth.Result


class DocxViewerActivity: PermissionActivity() {

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        DocxViewerActivityBinding.inflate(layoutInflater)
    }
    private lateinit var docxModel: LocalDocxModel
    private lateinit var viewModel: DocxViewerActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel = ViewModelProvider(this).get(DocxViewerActivityViewModel::class.java)
        if (savedInstanceState == null) {
            val mimeType = intent.type
            val docxUri = intent.data
            Log.i(javaClass.simpleName, "Loading docx from path ${docxUri?.path} " +
                    "and mimetype $mimeType")
            docxModel = LocalDocxModel(docxUri!!, mimeType!!)
            val converter = DocumentConverter()
            val result: Result<String>? = converter.convertToHtml(docxModel.getInputStream(this))
            title = docxModel.getName(this)
            result?.let {
                val html: String = result.value // The generated HTML
                val warnings: Set<String> = result.warnings // Any warnings during conversion
                val base64 = Base64.encodeToString(html.toByteArray(), Base64.NO_PADDING)
                viewBinding.webview.also {
                    it.loadData(base64, "text/html", "base64")
                    it.settings.setSupportZoom(true);
                    it.settings.builtInZoomControls = true
                    it.settings.displayZoomControls = false
                    it.setVerticalScrollBarEnabled(true)
                    it.setHorizontalScrollBarEnabled(true)
                }
            }
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
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    viewModel.nightMode = !viewModel.nightMode
                    WebSettingsCompat.setForceDark(viewBinding.webview.settings,
                        if (viewModel.nightMode) FORCE_DARK_ON else FORCE_DARK_OFF)
                    viewBinding.webview.reload()
                }
            }
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}