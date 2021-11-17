package com.amaze.fileutilities.image_viewer

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.amaze.fileutilities.databinding.ImageMetadataSheetBinding
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.jpeg.JpegDirectory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File

class ImageMetadataSheet(): BottomSheetDialogFragment() {


    private var localImageModel: LocalImageModel? = null
    private var imageMetadataSheetBinding: ImageMetadataSheetBinding? = null
    private val viewBinding get() = imageMetadataSheetBinding!!

    companion object {
        const val KEY_LOCAL_IMAGE = "image"

        fun showMetadata(localImageModel: LocalImageModel, fragmentManager: FragmentManager) {
            val metadataSheet = newInstance(localImageModel)
            metadataSheet.show(fragmentManager, javaClass.simpleName)
        }

        private fun newInstance(localImageModel: LocalImageModel): ImageMetadataSheet {
            val args = Bundle()
            val fragment = ImageMetadataSheet()
            args.putParcelable(KEY_LOCAL_IMAGE, localImageModel)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localImageModel = arguments?.getParcelable(KEY_LOCAL_IMAGE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        imageMetadataSheetBinding = ImageMetadataSheetBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imageMetadataSheetBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localImageModel?.let {
            var metadata = ImageMetadataReader.readMetadata(File(it.uri.path))
            var result = ""
            metadata.directories.forEach { directory ->
                directory.tags.forEach {
                    tag ->
                    result += tag.description
                    result += "\n"
                }
                result += "\n\n"
            }
            viewBinding.metadata.text = result
        }
    }
}