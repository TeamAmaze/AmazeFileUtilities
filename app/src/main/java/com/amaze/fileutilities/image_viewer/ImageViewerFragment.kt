/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.image_viewer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.res.ResourcesCompat
import androidx.documentfile.provider.DocumentFile
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.QuickViewFragmentBinding
import com.amaze.fileutilities.utilis.AbstractMediaFragment
import com.amaze.fileutilities.utilis.getFileFromUri
import com.bumptech.glide.Glide

class ImageViewerFragment : AbstractMediaFragment() {

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        QuickViewFragmentBinding.inflate(layoutInflater)
    }

    private var hideToolbars = false

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

    override fun getRootLayout(): View {
        return viewBinding.root
    }

    override fun getToolbarLayout(): View {
        return viewBinding.customToolbar.root
    }

    override fun getBottomBarLayout(): View {
        return viewBinding.customBottomBar
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
            viewBinding.run {
                imageView.setOnClickListener {
                    hideToolbars = !hideToolbars
                    refactorSystemUi(hideToolbars)
                }
                customToolbar.root.visibility = View.VISIBLE
                customBottomBar.visibility = View.VISIBLE
                customToolbar.title.text = DocumentFile.fromSingleUri(
                    requireContext(),
                    quickViewType!!.uri
                )?.name ?: quickViewType.uri.getFileFromUri(requireContext())?.name
                customToolbar.backButton.setOnClickListener {
                    requireActivity().onBackPressed()
                }
                customBottomBar.addButton(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_outline_info_32, requireActivity().theme
                    )!!
                ) {
                    ImageMetadataSheet.showMetadata(
                        quickViewType,
                        requireActivity().supportFragmentManager
                    )
                }
            }
        }
        quickViewType?.let { showImage(it) }
    }

    private fun showImage(localTypeModel: LocalImageModel) {
        Log.i(
            javaClass.simpleName,
            "Show image in fragment ${localTypeModel.uri.path} " +
                "and mimetype ${localTypeModel.mimeType}"
        )

        Glide.with(this).load(localTypeModel.uri.toString())
            .thumbnail(
                Glide.with(this).load(
                    resources.getDrawable(R.drawable.ic_outline_image_32)
                )
            )
            .into(viewBinding.imageView)
    }
}
