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

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentManager
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.ImageMetadataSheetBinding
import com.amaze.fileutilities.utilis.ImgUtils
import com.amaze.fileutilities.utilis.getFileFromUri
import com.drew.imaging.ImageMetadataReader
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.opencv.imgcodecs.Imgcodecs
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ImageMetadataSheet : BottomSheetDialogFragment() {

    var log: Logger = LoggerFactory.getLogger(ImageMetadataSheet::class.java)

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
        imageMetadataSheetBinding!!.root.background = ResourcesCompat.getDrawable(
            resources,
            R.drawable.background_curved, requireActivity().theme
        )
        return viewBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imageMetadataSheetBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localImageModel?.let {
            val metadata = ImageMetadataReader.readMetadata(it.uri.getFileFromUri(requireContext()))
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

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val imgPath = localImageModel!!.uri.getFileFromUri(requireContext())!!.path
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
                    "\nRatio: ${(zeros.second.toDouble() / zeros.first.toDouble()).toDouble()}"
                result += "\n\n"
            }
        }
    }
}
