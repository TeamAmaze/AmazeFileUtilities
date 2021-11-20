package com.amaze.fileutilities.image_viewer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.QuickViewFragmentBinding
import com.amaze.fileutilities.databinding.VideoPlayerDialogActivityBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.chrisbanes.photoview.PhotoView

class ImageViewerFragment : Fragment(R.layout.quick_view_fragment) {

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        QuickViewFragmentBinding.inflate(layoutInflater)
    }

    companion object {
        const val VIEW_TYPE_ARGUMENT = "ImageViewerFragment.viewTypeArgument"

        /**
         * Creates a new instance of [ImageViewerFragment]
         *
         * [viewType] is the [ImageModel] that will be shown
         */
        @JvmStatic
        fun newInstance(imageModel: LocalImageModel): ImageViewerFragment {
            val arguments = Bundle().also {
                it.putParcelable(VIEW_TYPE_ARGUMENT, imageModel)
            }

            return ImageViewerFragment().also {
                it.arguments = arguments
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val constraintLayout = view.findViewById<ConstraintLayout>(R.id.frameLayout)
        val quickViewType = requireArguments().getParcelable<LocalImageModel>(VIEW_TYPE_ARGUMENT)
//        val imageView = constraintLayout.findViewById<PhotoView>(R.id.imageView)
        if (activity is ImageViewerDialogActivity) {
            viewBinding.imageView.setOnClickListener {
                activity?.finish()
                val intent = Intent(requireContext(), ImageViewerActivity::class.java).apply {
                    putExtra(VIEW_TYPE_ARGUMENT, quickViewType)
                }
                startActivity(intent)
            }
        } else if (activity is ImageViewerActivity) {
            viewBinding.imageView.setOnClickListener {
                ImageMetadataSheet.showMetadata(quickViewType!!, requireActivity().supportFragmentManager)
            }
        }
        quickViewType?.let { showImage(it) }
    }

    private fun showImage(localTypeModel: LocalImageModel) {
        Log.i(javaClass.simpleName, "Show image in fragment ${localTypeModel.uri.path} " +
                "and mimetype ${localTypeModel.mimeType}")

        viewBinding.textView.text = DocumentFile.fromSingleUri(requireContext(), localTypeModel.uri)?.name

        Glide.with(this).load(localTypeModel.uri.toString())
            .thumbnail(Glide.with(this).load(resources.getDrawable(R.drawable.about_header)))
            .into(viewBinding.imageView)
    }
}