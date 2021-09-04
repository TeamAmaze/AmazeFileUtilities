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
import com.bumptech.glide.Glide

class ImageViewerFragment : Fragment(R.layout.quick_view_fragment) {

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val constraintLayout = view.findViewById<ConstraintLayout>(R.id.frameLayout)
        val quickViewType = requireArguments().getParcelable<LocalImageModel>(VIEW_TYPE_ARGUMENT)
        constraintLayout.setOnClickListener {
            activity?.finish()
            val intent = Intent(requireContext(), ImageViewerActivity::class.java).apply {
                putExtra(VIEW_TYPE_ARGUMENT, quickViewType)
            }
            startActivity(intent)
        }
        quickViewType?.let { showImage(it, constraintLayout) }
    }

    private fun showImage(localTypeModel: LocalImageModel, constraintLayout: ConstraintLayout) {
        Log.i(javaClass.simpleName, "Show image in fragment ${localTypeModel.uri.path} " +
                "and mimetype ${localTypeModel.mimeType}")

        val textView = constraintLayout.findViewById<TextView>(R.id.textView)
        textView.text = DocumentFile.fromSingleUri(requireContext(), localTypeModel.uri)?.name

        val imageView = constraintLayout.findViewById<ImageView>(R.id.imageView)
        Glide.with(this).load(localTypeModel.uri.toString()).into(imageView)
    }
}