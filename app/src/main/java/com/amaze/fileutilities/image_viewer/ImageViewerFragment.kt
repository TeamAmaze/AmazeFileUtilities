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
import android.text.format.Formatter
import android.view.*
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.ResourcesCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.activityViewModels
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.QuickViewFragmentBinding
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.utilis.*
import com.amaze.fileutilities.utilis.Utils.Companion.showProcessingDialog
import com.amaze.fileutilities.utilis.share.showEditImageDialog
import com.amaze.fileutilities.utilis.share.showSetAsDialog
import com.amaze.fileutilities.utilis.share.showShareDialog
import com.bumptech.glide.Glide
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.*
import com.drew.metadata.file.FileSystemDirectory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class ImageViewerFragment : AbstractMediaFragment() {

    var log: Logger = LoggerFactory.getLogger(ImageViewerFragment::class.java)

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        QuickViewFragmentBinding.inflate(layoutInflater)
    }
    private val filesViewModel: FilesViewModel by activityViewModels()

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
                    refactorSystemUi(
                        hideToolbars,
                        imageView.scale == imageView.minimumScale
                    )
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
                setupBottomBar(quickViewType)
                setupBottomSheetBehaviour()
                setupPropertiesSheet(quickViewType)
                /*var result = "\n"
                metadata.directories.forEach { directory ->
                    val dirName = directory.name
                    result += dirName + "\n"
                    directory.tags.forEach {
                        tag ->
                        result += tag.description
                        result += "\n"
                    }
                    result += "\n\n"
                }*/

//                    viewBinding.metadata.text = result
            }
        }
        quickViewType?.let { showImage(it) }
    }

    private fun setupPropertiesSheet(quickViewType: LocalImageModel) {
        quickViewType.let {
            val file = it.uri.getFileFromUri(requireContext())
            file?.let {
                file ->
                val metadata = ImageMetadataReader.readMetadata(file)
                viewBinding.run {
                    val exifSubDirectory = metadata
                        .getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
                    val descriptor = ExifSubIFDDescriptor(exifSubDirectory)
                    val fileSystemDirectory = metadata
                        .getFirstDirectoryOfType(FileSystemDirectory::class.java)
                    val gpsDirectory = metadata
                        .getFirstDirectoryOfType(GpsDirectory::class.java)
                    val gpsDescriptor = GpsDescriptor(gpsDirectory)

                    imageMetadataLayout.fileName.text =
                        "${resources.getString(R.string.file_name)}: ${file.name}"
                    var widthAndHeight = ""
                    if (exifSubDirectory != null && descriptor != null) {
                        descriptor.exifImageWidthDescription.let {
                            width ->
                            if (!width.isNullOrEmpty()) {
                                descriptor.exifImageHeightDescription.let {
                                    height ->
                                    if (!height.isNullOrEmpty()) {
                                        widthAndHeight =
                                            "${width.replace(" pixels", "")}" +
                                            "x${height.replace(
                                                " pixels",
                                                ""
                                            )} | "
                                    }
                                }
                            }
                        }
                    }
                    imageMetadataLayout.fileSize.text =
                        "${resources.getString(R.string.size)}: " +
                        "$widthAndHeight${Formatter.formatFileSize(
                            requireContext(),
                            file.length()
                        )}"
                    imageMetadataLayout.fileLastModified.text =
                        "${resources.getString(R.string.date)}: " +
                        "${Date(file.lastModified())}"
                    imageMetadataLayout.filePath.text =
                        "${resources.getString(R.string.path)}: ${it.uri.path ?: file.path}"
                    /*else if (fileSystemDirectory != null) {
                        viewBinding.imageMetadataLayout.fileName.text =
                            fileSystemDirectory.tags.toMutableList()[0].toString()
                        viewBinding.imageMetadataLayout.fileSize.text =
                            Formatter.formatFileSize(
                                requireContext(),
                                fileSystemDirectory.tags.toMutableList()[1].toString().toLong()
                            )

                        imageMetadataLayout.fileLastModified.text =
                            "${resources.getString(R.string.date)}: " +
                                    "${fileSystemDirectory.tags.toMutableList()[2]
                                    }"
                    }*/

                    val exifDirectory = metadata
                        .getFirstDirectoryOfType(ExifIFD0Directory::class.java)
                    if (exifDirectory != null) {
                        exifDirectory.getString(ExifIFD0Directory.TAG_MODEL).let {
                            property ->
                            if (!property.isNullOrEmpty()) {
                                imageMetadataLayout.model.visibility = View.VISIBLE
                                imageMetadataLayout.lensInfoParent.visibility = View.VISIBLE
                                imageMetadataLayout.model.text =
                                    "${resources.getString(R.string.model)}: $property"
                            } else {
                                imageMetadataLayout.model.visibility = View.GONE
                            }
                        }
                        exifDirectory.getString(ExifIFD0Directory.TAG_MAKE).let {
                            property ->
                            if (!property.isNullOrEmpty()) {
                                imageMetadataLayout.make.visibility = View.VISIBLE
                                imageMetadataLayout.lensInfoParent.visibility = View.VISIBLE
                                imageMetadataLayout.make.text =
                                    "${resources.getString(R.string.make)}: $property"
                            } else {
                                imageMetadataLayout.make.visibility = View.GONE
                            }
                        }
                    }
                    if (exifSubDirectory != null && descriptor != null) {
                        descriptor.apertureValueDescription.let {
                            property ->
                            if (!property.isNullOrEmpty()) {
                                imageMetadataLayout.aperture.visibility = View.VISIBLE
                                imageMetadataLayout.lensInfoParent.visibility = View.VISIBLE
                                imageMetadataLayout.aperture.text =
                                    "${resources.getString(R.string.aperture)}: $property"
                            } else {
                                imageMetadataLayout.aperture.visibility = View.GONE
                            }
                        }
                        descriptor.shutterSpeedDescription.let {
                            property ->
                            if (!property.isNullOrEmpty()) {
                                imageMetadataLayout.shutterTime.visibility = View.VISIBLE
                                imageMetadataLayout.lensInfoParent.visibility = View.VISIBLE
                                imageMetadataLayout.shutterTime.text =
                                    "${resources.getString(R.string.shutter_time)}: $property"
                            } else {
                                imageMetadataLayout.shutterTime.visibility = View.GONE
                            }
                        }
                        descriptor.isoEquivalentDescription.let {
                            property ->
                            if (!property.isNullOrEmpty()) {
                                imageMetadataLayout.iso.visibility = View.VISIBLE
                                imageMetadataLayout.lensInfoParent.visibility = View.VISIBLE
                                imageMetadataLayout.iso.text =
                                    "${resources.getString(R.string.iso)}: $property"
                            } else {
                                imageMetadataLayout.iso.visibility = View.GONE
                            }
                        }
                    }
                    if (gpsDirectory != null && gpsDescriptor != null) {
                        gpsDescriptor.gpsLatitudeDescription.let {
                            latitude ->
                            gpsDescriptor.gpsLongitudeDescription.let {
                                longitude ->
                                if (!longitude.isNullOrEmpty() && !latitude.isNullOrEmpty()) {
                                    imageMetadataLayout.longitude.visibility = View.VISIBLE
                                    imageMetadataLayout.gpsInfoParent.visibility = View.VISIBLE
                                    imageMetadataLayout.longitude.text =
                                        "${resources.getString(R.string.longitude)}: $longitude"
                                    imageMetadataLayout.lat.visibility = View.VISIBLE
                                    imageMetadataLayout.lat.text =
                                        "${resources.getString(R.string.latitude)}: $latitude"
                                    imageMetadataLayout.openInMapsImage.setOnClickListener {
                                        Utils.openInMaps(requireContext(), latitude, longitude)
                                    }
                                    imageMetadataLayout.openInMapsText.setOnClickListener {
                                        Utils.openInMaps(requireContext(), latitude, longitude)
                                    }
                                } else {
                                    imageMetadataLayout.longitude.visibility = View.GONE
                                    imageMetadataLayout.lat.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupBottomSheetBehaviour() {
        viewBinding.run {
            val params: CoordinatorLayout.LayoutParams = viewBinding.layoutBottomSheet
                .layoutParams as CoordinatorLayout.LayoutParams
            val behavior = params.behavior as DragDismissBottomSheetBehaviour

            imageViewSmall.setOnClickListener {
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }

            val sHeight = Utils.getScreenHeight(requireActivity().windowManager)
            val imageSmallHeight = sHeight / 3
            val bottomSheetHeight = (sHeight * 2) / 3
            viewBinding.imageViewSmall.layoutParams.height = imageSmallHeight
            viewBinding.layoutBottomSheet.layoutParams.height = bottomSheetHeight

            viewBinding.layoutBottomSheet.visibility = View.VISIBLE
            behavior.addBottomSheetCallback(bottomSheetCallback)
            behavior.setProxyView(imageView, {
                if (!hideToolbars) {
                    hideToolbars = true
                    // disable viewpager swipe while swipe to dismiss gesture
                    (activity as ImageViewerActivity).getViewpager().isUserInputEnabled = false
                    refactorSystemUi(hideToolbars, true)
                }
            }, {
                if (hideToolbars) {
                    hideToolbars = false
                    (activity as ImageViewerActivity).getViewpager().isUserInputEnabled = true
                    refactorSystemUi(hideToolbars, true)
                }
            })
            layoutBottomSheet.setOnClickListener {
                if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }

    private fun setupBottomBar(localImageModel: LocalImageModel?) {
        viewBinding.run {
            customBottomBar.addButton(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_round_edit_32, requireActivity().theme
                )!!,
                resources.getString(R.string.edit)
            ) {
                localImageModel?.let {
                    showEditImageDialog(
                        localImageModel.uri,
                        this@ImageViewerFragment.requireContext()
                    )
                }
            }
            customBottomBar.addButton(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_twotone_share_32, requireActivity().theme
                )!!,
                resources.getString(R.string.share)
            ) {
                var processed = false
                localImageModel?.let {
                    filesViewModel.getShareMediaFilesAdapterFromUriList(
                        Collections
                            .singletonList(it.uri)
                    )
                        .observe(viewLifecycleOwner) {
                            shareAdapter ->
                            if (shareAdapter == null) {
                                if (processed) {
                                    requireActivity().showToastInCenter(
                                        this@ImageViewerFragment.resources
                                            .getString(R.string.failed_to_share)
                                    )
                                } else {
                                    requireActivity()
                                        .showToastInCenter(
                                            resources
                                                .getString(R.string.please_wait)
                                        )
                                    processed = true
                                }
                            } else {
                                showShareDialog(
                                    requireActivity(),
                                    this@ImageViewerFragment.layoutInflater, shareAdapter
                                )
                            }
                        }
                }
            }
            customBottomBar.addButton(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_baseline_open_in_new_24, requireActivity().theme
                )!!,
                resources.getString(R.string.set_as)
            ) {
                localImageModel?.let {
                    showSetAsDialog(localImageModel.uri, this@ImageViewerFragment.requireContext())
                }
            }
            customBottomBar.addButton(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_round_delete_outline_32, requireActivity().theme
                )!!,
                resources.getString(R.string.delete)
            ) {
                localImageModel!!.uri
                    .getFileFromUri(requireContext())?.let {
                        file ->
                        val toDelete = Collections.singletonList(
                            MediaFileInfo.fromFile(
                                file,
                                MediaFileInfo.ExtraInfo(
                                    MediaFileInfo.MEDIA_TYPE_UNKNOWN,
                                    null, null, null
                                )
                            )
                        )
                        val progressDialogBuilder = requireContext()
                            .showProcessingDialog(layoutInflater, "")
                        val progressDialog = progressDialogBuilder.create()
                        val summaryDialogBuilder = Utils
                            .buildDeleteSummaryDialog(requireContext()) {
                                progressDialog.show()
                                filesViewModel.deleteMediaFiles(toDelete)
                                    .observe(viewLifecycleOwner) {
                                        progressDialog
                                            .findViewById<TextView>(R.id.please_wait_text)?.text =
                                            resources.getString(R.string.deleted_progress)
                                                .format(it.first, toDelete.size)
                                        if (it.second == toDelete.size) {
                                            // delete deleted data from observables in fileviewmodel
                                    /*filesViewModel.usedImagesSummaryTransformations
                                        .observe(viewLifecycleOwner) {
                                            summary ->
                                        if (summary != null) {
                                            filesViewModel.deleteMediaFilesFromList(summary.second,
                                                toDelete)
                                        }
                                    }*/

                                            // deletion complete, no need to check analysis data to remove
                                            // as it will get deleted lazily while loading analysis lists
                                            requireContext().showToastOnBottom(
                                                resources
                                                    .getString(R.string.successfully_deleted)
                                            )
                                            requireActivity().finish()
                                            progressDialog.dismiss()
                                        }
                                    }
                            }
                        val summaryDialog = summaryDialogBuilder.create()
                        summaryDialog.show()
                        filesViewModel.getMediaFileListSize(toDelete).observe(viewLifecycleOwner) {
                            sizeRaw ->
                            val size = Formatter.formatFileSize(requireContext(), sizeRaw)
                            summaryDialog.setMessage(
                                resources
                                    .getString(R.string.delete_files_message).format(
                                        toDelete.size,
                                        size
                                    )
                            )
                        }
                    }
            }
        }
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
                if (slideOffset == 1f) {
                    viewBinding.imageView.visibility = View.GONE
                } else {
                    viewBinding.imageView.visibility = View.VISIBLE
                    viewBinding.sheetUpArrow.visibility = View.INVISIBLE
                    if ((activity as ImageViewerActivity).getViewpager().isUserInputEnabled) {
                        (activity as ImageViewerActivity).getViewpager().isUserInputEnabled = false
                    }
                }
            } else {
                viewBinding.imageViewSmall.visibility = View.GONE
                viewBinding.sheetUpArrow.visibility = View.VISIBLE
                (activity as ImageViewerActivity).getViewpager().isUserInputEnabled = true
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

    private fun showImageProcessingText() {

        /*val matrix = Imgcodecs.imread(it.uri.getFileFromUri(requireContext())!!.path)
        val factor = ImgUtils.laplace(matrix)
        log.info("Found laplace of image: $factor")*/

        /*val recognizer = TextRecognition
            .getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val imgPath = quickViewType.uri.getFileFromUri(requireContext())!!.path
        val image = InputImage.fromBitmap(
            BitmapFactory.decodeFile(imgPath),
            0
        )
//                    result += "Laplacian variance: $factor\n"
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

        *//*val thres = ImgUtils.thresholdInvert(ImgUtils.convertBitmapToMat(BitmapFactory
                        .decodeFile(imgPath)))*//*
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
                    }*/
    }
}
