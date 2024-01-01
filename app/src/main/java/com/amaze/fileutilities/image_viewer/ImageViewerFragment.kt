/*
 * Copyright (C) 2021-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Utilities.
 *
 * Amaze File Utilities is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.fileutilities.image_viewer

import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.QuickViewFragmentBinding
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.image_viewer.editor.EditImageActivity
import com.amaze.fileutilities.utilis.*
import com.amaze.fileutilities.utilis.Utils.Companion.showProcessingDialog
import com.amaze.fileutilities.utilis.share.showSetAsDialog
import com.amaze.fileutilities.utilis.share.showShareDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.*
import com.drew.metadata.file.FileSystemDirectory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.Date

class ImageViewerFragment : AbstractMediaFragment() {

    var log: Logger = LoggerFactory.getLogger(ImageViewerFragment::class.java)
    private var _binding: QuickViewFragmentBinding? = null
    private val filesViewModel: FilesViewModel by activityViewModels()
    private lateinit var viewModel: ImageFragmentViewModel

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
        return _binding?.root!!
    }

    override fun getToolbarLayout(): View {
        return _binding?.customToolbar?.root!!
    }

    override fun getBottomBarLayout(): View {
        return _binding?.layoutBottomSheet!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = QuickViewFragmentBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ImageFragmentViewModel::class.java)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val quickViewType = requireArguments().getParcelable<LocalImageModel>(VIEW_TYPE_ARGUMENT)
        if (quickViewType == null) {
            requireContext().showToastInCenter(getString(R.string.operation_failed))
            requireActivity().onBackPressed()
            return
        }
        if (activity is ImageViewerDialogActivity) {
            _binding?.layoutBottomSheet?.visibility = View.GONE
            _binding?.imageView?.setOnClickListener {
                val intent = Intent(requireContext(), ImageViewerActivity::class.java)
                intent.setDataAndType(quickViewType.uri, quickViewType.mimeType)
                if (!quickViewType.uri.authority.equals(
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
            _binding?.run {
                imageView.setOnClickListener {
                    hideToolbars = !hideToolbars
                    refactorSystemUi(
                        hideToolbars,
                        imageView.scale == imageView.minimumScale
                    )
                }
                frameLayout.setProxyView(imageView)
                customToolbar.root.visibility = View.VISIBLE
                customToolbar.title.text = quickViewType.uri.getDocumentFileFromUri(
                    requireContext()
                )?.name ?: quickViewType.uri.getFileFromUri(
                    requireContext()
                )?.name
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
        showImage(quickViewType)
    }

    private fun setupPropertiesSheet(quickViewType: LocalImageModel) {
        quickViewType.let {
            val file = it.uri.getFileFromUri(requireContext())
            file?.let {
                file ->
                _binding?.run {

                    imageMetadataLayout.fileName.text =
                        "${resources.getString(R.string.file_name)}: \n${file.name}\n"

                    imageMetadataLayout.fileSize.text =
                        "${resources.getString(R.string.size)}: \n" +
                        "${Formatter.formatFileSize(
                            requireContext(),
                            file.length()
                        )}\n"
                    imageMetadataLayout.fileLastModified.text =
                        "${resources.getString(R.string.date)}: \n" +
                        "${Date(file.lastModified())}\n"
                    imageMetadataLayout.filePath.text =
                        "${resources.getString(R.string.path)}: \n${it.uri.path ?: file.path}\n"
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

                    try {
                        val metadata = ImageMetadataReader.readMetadata(file)

                        val exifSubDirectory = metadata
                            .getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
                        val descriptor = ExifSubIFDDescriptor(exifSubDirectory)
                        val fileSystemDirectory = metadata
                            .getFirstDirectoryOfType(FileSystemDirectory::class.java)
                        val gpsDirectory = metadata
                            .getFirstDirectoryOfType(GpsDirectory::class.java)
                        val gpsDescriptor = GpsDescriptor(gpsDirectory)

                        var widthAndHeight = ""
                        if (exifSubDirectory != null && descriptor != null) {
                            descriptor.exifImageWidthDescription.let {
                                width ->
                                if (!width.isNullOrEmpty()) {
                                    descriptor.exifImageHeightDescription.let {
                                        height ->
                                        if (!height.isNullOrEmpty()) {
                                            widthAndHeight =
                                                "${width.replace(
                                                " pixels",
                                                ""
                                            )}" +
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
                            "${resources.getString(R.string.size)}: \n" +
                            "$widthAndHeight${Formatter.formatFileSize(
                                requireContext(),
                                file.length()
                            )}\n"

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
                            gpsDescriptor.gpsLatitudeDescription.let { latitude ->
                                gpsDescriptor.gpsLongitudeDescription.let { longitude ->
                                    if (!longitude.isNullOrEmpty() &&
                                        !latitude.isNullOrEmpty()
                                    ) {
                                        imageMetadataLayout.longitude.visibility = View.VISIBLE
                                        imageMetadataLayout.gpsInfoParent.visibility = View.VISIBLE
                                        imageMetadataLayout.longitude.text =
                                            "${resources.getString(R.string.longitude)}: " +
                                            "$longitude"
                                        imageMetadataLayout.lat.visibility = View.VISIBLE
                                        imageMetadataLayout.lat.text =
                                            "${resources.getString(R.string.latitude)}: " +
                                            "$latitude"
                                        imageMetadataLayout.openInMapsImage
                                            .setOnClickListener {
                                                Utils.openInMaps(
                                                    requireContext(), latitude,
                                                    longitude
                                                )
                                            }
                                        imageMetadataLayout.openInMapsText
                                            .setOnClickListener {
                                                Utils.openInMaps(
                                                    requireContext(), latitude,
                                                    longitude
                                                )
                                            }
                                    } else {
                                        imageMetadataLayout.longitude.visibility = View.GONE
                                        imageMetadataLayout.lat.visibility = View.GONE
                                    }
                                }
                            }
                        }
                        imageMetadataLayout.loadHistogramButton.setOnClickListener {
                            viewModel.loadHistogram(
                                file.path,
                                imageMetadataLayout.histogramInfoParent.width.toDouble(),
                                resources
                            )
                                .observe(viewLifecycleOwner) {
                                    bitmap ->
                                    if (bitmap != null) {
                                        imageMetadataLayout.histogramInfo.visibility =
                                            View.VISIBLE
                                        imageMetadataLayout.histogramLoadingBar.visibility =
                                            View.GONE
                                        imageMetadataLayout.histogramInfo.setImageBitmap(bitmap)
                                    } else {
                                        imageMetadataLayout.loadHistogramButton.visibility =
                                            View.GONE
                                        imageMetadataLayout.histogramLoadingBar.visibility =
                                            View.VISIBLE
                                    }
                                }
                        }
                    } catch (e: Exception) {
                        log.warn("failed to parse image metadata", e)
                    }
                }
            }
        }
    }

    private fun setupBottomSheetBehaviour() {
        _binding?.run {
            val params: CoordinatorLayout.LayoutParams = layoutBottomSheet
                .layoutParams as CoordinatorLayout.LayoutParams
            val behavior = params.behavior as DragDismissBottomSheetBehaviour

            imageViewSmall.setOnClickListener {
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }

            val sHeight = Utils.getScreenHeight(requireActivity().windowManager)
            val imageSmallHeight = sHeight / 3
            val bottomSheetHeight = (sHeight * 2) / 3
            imageViewSmall.layoutParams.height = imageSmallHeight
            layoutBottomSheet.layoutParams.height = bottomSheetHeight

            layoutBottomSheet.visibility = View.VISIBLE
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
        _binding?.run {
            customBottomBar.addButton(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_round_edit_32, requireActivity().theme
                )!!,
                resources.getString(R.string.edit)
            ) {
                localImageModel?.let {
                    val intent = Intent(requireContext(), EditImageActivity::class.java)
                    intent.setDataAndType(it.uri, it.mimeType)
                    if (!it.uri.authority.equals(
                            requireContext()
                                .packageName,
                            true
                        )
                    ) {
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(intent)
                    /*showEditImageDialog(
                        localImageModel.uri,
                        this@ImageViewerFragment.requireContext()
                    )*/
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
                    showSetAsDialog(
                        localImageModel.uri,
                        this@ImageViewerFragment.requireContext()
                    )
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
                                deletePermanently ->
                                progressDialog.show()
                                if (deletePermanently) {
                                    filesViewModel.deleteMediaFiles(toDelete)
                                        .observe(viewLifecycleOwner) {
                                            deleteProgressCallback(it, progressDialog, toDelete)
                                        }
                                } else {
                                    filesViewModel.moveToTrashBin(toDelete)
                                        .observe(viewLifecycleOwner) {
                                            deleteProgressCallback(it, progressDialog, toDelete)
                                        }
                                }
                            }
                        val summaryDialog = summaryDialogBuilder.create()
                        summaryDialog.show()
                        filesViewModel.getMediaFileListSize(toDelete)
                            .observe(viewLifecycleOwner) {
                                sizeRaw ->
                                if (summaryDialog.isShowing) {
                                    val size = Formatter.formatFileSize(requireContext(), sizeRaw)
                                    summaryDialog
                                        .findViewById<TextView>(R.id.dialog_summary)?.text =
                                        resources
                                            .getString(R.string.delete_files_message).format(
                                                toDelete.size,
                                                size
                                            )
                                }
                            }
                    }
            }
        }
    }

    private fun deleteProgressCallback(
        progressPair: Pair<Int, Int>,
        progressDialog: AlertDialog,
        toDelete: List<MediaFileInfo>
    ) {
        progressDialog
            .findViewById<TextView>(R.id.please_wait_text)?.text =
            resources.getString(R.string.deleted_progress)
                .format(progressPair.first, toDelete.size)
        if (progressPair.second == toDelete.size) {
            // delete deleted data from observables in fileviewmodel
            filesViewModel.usedImagesSummaryTransformations()
                .observe(viewLifecycleOwner) {
                    pair ->
                    if (pair != null) {
                        filesViewModel.deleteMediaFilesFromList(
                            pair.second,
                            toDelete
                        )
                    }
                }

            // reset interal storage stats so that we recalculate storage remaining
            filesViewModel.internalStorageStatsLiveData = null
            filesViewModel.resetTrashBinConfig()

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

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            /*if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                binding.bottomSheetSmall.visibility = View.GONE
            }*/
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            _binding?.run {
                imageView.alpha = 1 - slideOffset
                if (slideOffset != 0f) {
                    imageViewSmall.visibility = View.VISIBLE
                    imageViewSmall.alpha = slideOffset
                    if (slideOffset == 1f) {
                        imageView.visibility = View.GONE
                    } else {
                        imageView.visibility = View.VISIBLE
                        sheetUpArrow.visibility = View.INVISIBLE
                        if ((activity as ImageViewerActivity).getViewpager()
                            .isUserInputEnabled
                        ) {
                            (activity as ImageViewerActivity).getViewpager()
                                .isUserInputEnabled = false
                        }
                    }
                } else {
                    imageViewSmall.visibility = View.GONE
                    sheetUpArrow.visibility = View.VISIBLE
                    (activity as ImageViewerActivity).getViewpager().isUserInputEnabled = true
                }
//            viewBinding.bottomSheetBig.alpha = slideOffset
//            viewBinding.layoutBottomSheet.alpha = slideOffset
            }
        }
    }

    private fun showImage(localTypeModel: LocalImageModel) {
        log.info(
            "Show image in fragment ${localTypeModel.uri.path} " +
                "and mimetype ${localTypeModel.mimeType}"
        )

        var glide = Glide.with(this).load(localTypeModel.uri.toString())
            .thumbnail(
                Glide.with(this)
                    .load(
                        resources.getDrawable(R.drawable.ic_outline_image_32)
                    )
            )
        if (activity is ImageViewerDialogActivity) {
            glide = glide.transform(RoundedCorners(24.px.toInt()))
        } else {
            val paletteEnabled = requireContext().getAppCommonSharedPreferences()
                .getBoolean(
                    PreferencesConstants.KEY_ENABLE_IMAGE_PALETTE,
                    PreferencesConstants.DEFAULT_PALETTE_EXTRACT
                )
            if (paletteEnabled) {
                glide = glide.addListener(paletteListener)
            }
        }
        _binding?.let {
            glide.into(it.imageView)
            Glide.with(this).load(localTypeModel.uri.toString())
                .thumbnail(
                    Glide.with(this).load(
                        resources.getDrawable(R.drawable.ic_outline_image_32)
                    )
                )
                .into(it.imageViewSmall)
        }
    }

    private val paletteListener: RequestListener<Drawable>
        get() = object : RequestListener<Drawable> {
            val metadataLayout = _binding?.imageMetadataLayout
            val fileInfoParent = metadataLayout?.fileInfoParent
            val lensInfoParent = metadataLayout?.lensInfoParent
            val gpsInfoParent = metadataLayout?.gpsInfoParent

            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                // do nothing
                log.warn("failed to load image", e)
                _binding?.layoutBottomSheet?.background?.setColorFilter(
                    resources.getColor(R.color.navy_blue_alt_3),
                    PorterDuff.Mode.SRC_ATOP
                )
                if (fileInfoParent?.isVisible == true) {
                    fileInfoParent.background?.setColorFilter(
                        resources.getColor(R.color.navy_blue_alt),
                        PorterDuff.Mode.SRC_ATOP
                    )
                }
                if (lensInfoParent?.isVisible == true) {
                    lensInfoParent.background?.setColorFilter(
                        resources.getColor(R.color.navy_blue_alt),
                        PorterDuff.Mode.SRC_ATOP
                    )
                }
                if (gpsInfoParent?.isVisible == true) {
                    gpsInfoParent.background?.setColorFilter(
                        resources.getColor(R.color.navy_blue_alt),
                        PorterDuff.Mode.SRC_ATOP
                    )
                }
                _binding?.customToolbar?.customToolbarRoot?.background?.setColorFilter(
                    resources.getColor(R.color.translucent_toolbar),
                    PorterDuff.Mode.SRC_ATOP
                )
                _binding?.customToolbar?.customToolbarRoot?.invalidate()
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                resource.let {
                    filesViewModel.getPaletteColors(it)
                        .observe(this@ImageViewerFragment) {
                            colorPair ->
                            if (colorPair != null) {
                                _binding?.layoutBottomSheet?.background?.setColorFilter(
                                    colorPair.first,
                                    PorterDuff.Mode.SRC_ATOP
                                )
                                if (fileInfoParent?.isVisible == true) {
                                    fileInfoParent.background?.setColorFilter(
                                        colorPair.second,
                                        PorterDuff.Mode.SRC_ATOP
                                    )
                                }
                                if (lensInfoParent?.isVisible == true) {
                                    lensInfoParent.background?.setColorFilter(
                                        colorPair.second,
                                        PorterDuff.Mode.SRC_ATOP
                                    )
                                }
                                if (gpsInfoParent?.isVisible == true) {
                                    gpsInfoParent.background?.setColorFilter(
                                        colorPair.second,
                                        PorterDuff.Mode.SRC_ATOP
                                    )
                                }
                                if (metadataLayout?.histogramInfoParent?.isVisible == true) {
                                    metadataLayout.histogramInfoParent
                                        .background?.setColorFilter(
                                            colorPair.second,
                                            PorterDuff.Mode.SRC_ATOP
                                        )
                                }
                                _binding?.customToolbar?.customToolbarRoot?.background
                                    ?.setColorFilter(
                                        colorPair.first,
                                        PorterDuff.Mode.SRC_ATOP
                                    )
                                _binding?.customToolbar?.customToolbarRoot?.invalidate()
                            }
                        }
                }
                return false
            }
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
