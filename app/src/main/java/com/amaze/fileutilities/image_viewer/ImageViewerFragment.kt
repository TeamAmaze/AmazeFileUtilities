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
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.ResourcesCompat
import androidx.documentfile.provider.DocumentFile
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.QuickViewFragmentBinding
import com.amaze.fileutilities.utilis.*
import com.bumptech.glide.Glide
import com.drew.imaging.ImageMetadataReader
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.opencv.imgcodecs.Imgcodecs
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
        return viewBinding.layoutBottomSheet
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
        val quickViewType = requireArguments().getParcelable<LocalImageModel>(VIEW_TYPE_ARGUMENT)
        if (activity is ImageViewerDialogActivity) {
            viewBinding.layoutBottomSheet.visibility = View.GONE
            viewBinding.imageView.setOnClickListener {
                val intent = Intent(requireContext(), ImageViewerActivity::class.java)
                intent.setDataAndType(quickViewType?.uri, quickViewType?.mimeType)
                if (!quickViewType?.uri?.authority.equals(
                        requireContext()
                            .packageName,
                        true
                    )
                ) {
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
                activity?.finish()
            }
        } else if (activity is ImageViewerActivity) {
            viewBinding.run {
                imageView.setOnClickListener {
                    hideToolbars = !hideToolbars
                    refactorSystemUi(hideToolbars)
                }
                frameLayout.setProxyView(imageView)
                customToolbar.root.visibility = View.VISIBLE
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
                customBottomBar.addButton(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_twotone_share_32, requireActivity().theme
                    )!!
                ) {
                    ImageMetadataSheet.showMetadata(
                        quickViewType,
                        requireActivity().supportFragmentManager
                    )
                }
                customBottomBar.addButton(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_round_delete_outline_32, requireActivity().theme
                    )!!
                ) {
                    ImageMetadataSheet.showMetadata(
                        quickViewType,
                        requireActivity().supportFragmentManager
                    )
                }

                val sHeight = Utils.getScreenHeight(requireActivity().windowManager)
                val imageSmallHeight = sHeight / 3
                val bottomSheetHeight = (sHeight * 2) / 3
                viewBinding.imageViewSmall.layoutParams.height = imageSmallHeight
                viewBinding.layoutBottomSheet.layoutParams.height = bottomSheetHeight

                viewBinding.layoutBottomSheet.visibility = View.VISIBLE
                val params: CoordinatorLayout.LayoutParams = viewBinding.layoutBottomSheet
                    .layoutParams as CoordinatorLayout.LayoutParams
                val behavior = params.behavior as DragDismissBottomSheetBehaviour
                behavior.addBottomSheetCallback(bottomSheetCallback)
                behavior.setProxyView(imageView, {
                    if (!hideToolbars) {
                        hideToolbars = true
                        refactorSystemUi(hideToolbars)
                    }
                }, {
                    if (hideToolbars) {
                        hideToolbars = false
                        refactorSystemUi(hideToolbars)
                    }
                })
                viewBinding.layoutBottomSheet.setOnClickListener {
                    if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    } else {
                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }

                quickViewType.let {
                    val metadata = ImageMetadataReader.readMetadata(
                        it.uri
                            .getFileFromUri(requireContext())
                    )
                    val matrix = Imgcodecs.imread(it.uri.getFileFromUri(requireContext())!!.path)
                    val factor = ImgUtils.laplace(matrix)
                    log.info("Found laplace of image: $factor")

                    var result = "\n"
                    result += "Laplacian variance: $factor\n"
                    metadata.directories.forEach { directory ->
                        directory.tags.forEach {
                            tag ->
                            result += tag.description
                            result += "\n"
                        }
                        result += "\n\n"
                    }

                    val recognizer = TextRecognition
                        .getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val imgPath = quickViewType.uri.getFileFromUri(requireContext())!!.path
                    val image = InputImage.fromBitmap(
                        BitmapFactory.decodeFile(imgPath),
                        0
                    )
                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            // Task completed successfully

                            result += "\n\n\n------ Extracted text --------\n\n"
                            result += visionText.text
                            result += "\n\n"
                            log.info("CUSTOM:\n${visionText.text}")
                            viewBinding.metadata.text = result
                        }
                        .addOnFailureListener { e ->
                            // Task failed with an exception
                            // ...
                            log.warn("failed to recognize image", e)
                        }

                    /*val thres = ImgUtils.thresholdInvert(ImgUtils.convertBitmapToMat(BitmapFactory
                        .decodeFile(imgPath)))*/
                    ImgUtils.convertBitmapToMat(
                        BitmapFactory
                            .decodeFile(imgPath)
                    )?.let {
                        mat ->
                        val zeros = ImgUtils.getTotalAndZeros(mat)

//            val bitmap = ImgUtils.convertMatToBitmap(thres)
                        result += "\n\n\n------ Dark areas --------\n\n"
                        result += "Total: ${zeros.first}\nZeros: ${zeros.second}" +
                            "\nRatio: ${(
                                zeros.second.toDouble() /
                                    zeros.first.toDouble()
                                ).toDouble()}"
                        result += "\n\n"
                    }

                    viewBinding.metadata.text = result
                }
            }
        }
        quickViewType?.let { showImage(it) }
    }

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            /*if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                binding.bottomSheetSmall.visibility = View.GONE
            }*/
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            viewBinding.imageView.alpha = 1 - slideOffset
            if (slideOffset != 0f) {
                viewBinding.imageViewSmall.visibility = View.VISIBLE
                viewBinding.imageViewSmall.alpha = slideOffset
            } else {
                viewBinding.imageViewSmall.visibility = View.GONE
            }
//            viewBinding.bottomSheetBig.alpha = slideOffset
//            viewBinding.layoutBottomSheet.alpha = slideOffset
        }
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
        Glide.with(this).load(localTypeModel.uri.toString())
            .thumbnail(
                Glide.with(this).load(
                    resources.getDrawable(R.drawable.ic_outline_image_32)
                )
            )
            .into(viewBinding.imageViewSmall)
    }
}
