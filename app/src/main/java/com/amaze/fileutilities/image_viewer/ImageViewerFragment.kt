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
import android.view.*
import androidx.core.content.res.ResourcesCompat
import androidx.documentfile.provider.DocumentFile
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.QuickViewFragmentBinding
import com.amaze.fileutilities.utilis.AbstractMediaFragment
import com.amaze.fileutilities.utilis.getFileFromUri
import com.bumptech.glide.Glide
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.klinker.android.drag_dismiss.DragDismissIntentBuilder

class ImageViewerFragment : AbstractMediaFragment() {

    var log: Logger = LoggerFactory.getLogger(ImageViewerFragment::class.java)

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
                val intent = Intent(requireContext(), ImageViewerActivity::class.java)
                intent.setDataAndType(quickViewType?.uri, quickViewType?.mimeType)
                if (!quickViewType?.uri?.authority.equals(requireContext().packageName, true)) {
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                // add elastic activity properties
                DragDismissIntentBuilder(context)
                    .setTheme(DragDismissIntentBuilder.Theme.DARK)	// LIGHT (default), DARK, BLACK, DAY_NIGHT, SYSTEM_DEFAULT
//                    .setPrimaryColorResource(R.color.colorPrimary)	// defaults to a semi-transparent black
                    .setShowToolbar(false) // defaults to true
                    .setFullscreenOnTablets(false) // defaults to false, tablets will have padding on each side
                    .setDragElasticity(DragDismissIntentBuilder.DragElasticity.XXLARGE) // Larger elasticities will make it easier to dismiss.
                    .setDrawUnderStatusBar(false) // defaults to false. Change to true if you don't want me to handle the content margin for the Activity. Does not apply to the RecyclerView Activities
                    .build(intent)
                startActivity(intent)
                activity?.finish()
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
        log.info(
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
