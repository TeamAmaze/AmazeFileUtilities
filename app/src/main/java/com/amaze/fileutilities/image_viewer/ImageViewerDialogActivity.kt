package com.amaze.fileutilities.image_viewer

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.ImageViewerDialogActivityBinding
import com.amaze.fileutilities.databinding.VideoPlayerDialogActivityBinding

class ImageViewerDialogActivity: PermissionActivity() {

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ImageViewerDialogActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        if (savedInstanceState == null) {
            val mimeType = intent.type
            val imageUri = intent.data
            Log.i(javaClass.simpleName, "Loading image from path ${imageUri?.path} " +
                    "and mimetype $mimeType")
            val bundle = bundleOf(ImageViewerFragment.VIEW_TYPE_ARGUMENT
                    to LocalImageModel(uri = imageUri!!, mimeType = mimeType!!))
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<ImageViewerFragment>(R.id.fragment_container_view, args = bundle)
            }
        }
    }
}