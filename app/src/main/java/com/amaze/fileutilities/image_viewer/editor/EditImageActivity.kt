/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.image_viewer.editor

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.CustomToolbar
import com.amaze.fileutilities.image_viewer.editor.EmojiBSFragment.EmojiListener
import com.amaze.fileutilities.image_viewer.editor.StickerBSFragment.StickerListener
import com.amaze.fileutilities.image_viewer.editor.base.BaseActivity
import com.amaze.fileutilities.image_viewer.editor.filters.FilterListener
import com.amaze.fileutilities.image_viewer.editor.filters.FilterViewAdapter
import com.amaze.fileutilities.image_viewer.editor.tools.EditingToolsAdapter
import com.amaze.fileutilities.image_viewer.editor.tools.EditingToolsAdapter.OnItemSelected
import com.amaze.fileutilities.image_viewer.editor.tools.ToolType
import com.amaze.fileutilities.utilis.hideFade
import com.amaze.fileutilities.utilis.share.ShareAdapter
import com.amaze.fileutilities.utilis.share.getShareIntents
import com.amaze.fileutilities.utilis.share.showEditImageDialog
import com.amaze.fileutilities.utilis.share.showShareDialog
import com.amaze.fileutilities.utilis.showFade
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ja.burhanrashid52.photoeditor.OnPhotoEditorListener
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoEditor.OnSaveListener
import ja.burhanrashid52.photoeditor.PhotoEditorView
import ja.burhanrashid52.photoeditor.PhotoFilter
import ja.burhanrashid52.photoeditor.SaveSettings
import ja.burhanrashid52.photoeditor.TextStyleBuilder
import ja.burhanrashid52.photoeditor.ViewType
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder
import ja.burhanrashid52.photoeditor.shape.ShapeType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

class EditImageActivity :
    BaseActivity(),
    OnPhotoEditorListener,
    View.OnClickListener,
    PropertiesBSFragment.Properties,
    ShapeBSFragment.Properties,
    EmojiListener,
    StickerListener,
    OnItemSelected,
    FilterListener {

    var log: Logger = LoggerFactory.getLogger(EditImageActivity::class.java)

    var mPhotoEditor: PhotoEditor? = null
    private var mPhotoEditorView: PhotoEditorView? = null
    private var mPropertiesBSFragment: PropertiesBSFragment? = null
    private var mShapeBSFragment: ShapeBSFragment? = null
    private var mShapeBuilder: ShapeBuilder? = null
    private var mEmojiBSFragment: EmojiBSFragment? = null
    private var mStickerBSFragment: StickerBSFragment? = null
//    private var mTxtCurrentTool: TextView? = null
    private var customToolbar: CustomToolbar? = null
    private var mWonderFont: Typeface? = null
    private var mRvTools: RecyclerView? = null
    private var mRvFilters: RecyclerView? = null
    private val mFilterViewAdapter = FilterViewAdapter(this)
    private var mIsFilterVisible = false
    private var intentUri: Uri? = null

    @VisibleForTesting
    var mSaveImageUri: Uri? = null
    private var mSaveFileHelper: FileSaveHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_image)
        initViews()
        handleIntentImage(mPhotoEditorView?.source)
//        mWonderFont = Typeface.createFromAsset(assets, "beyond_wonderland.ttf")
        mPropertiesBSFragment = PropertiesBSFragment()
        mEmojiBSFragment = EmojiBSFragment()
        mStickerBSFragment = StickerBSFragment()
        mShapeBSFragment = ShapeBSFragment()
        mStickerBSFragment?.setStickerListener(this)
        mEmojiBSFragment?.setEmojiListener(this)
        mPropertiesBSFragment?.setPropertiesChangeListener(this)
        mShapeBSFragment?.setPropertiesChangeListener(this)
        val llmTools = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mRvTools?.layoutManager = llmTools
        mRvTools?.adapter = EditingToolsAdapter(this, getToolsAdapterList())
        val llmFilters = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mRvFilters?.layoutManager = llmFilters
        mRvFilters?.adapter = mFilterViewAdapter

        // NOTE(lucianocheng): Used to set integration testing parameters to PhotoEditor
        val pinchTextScalable = intent.getBooleanExtra(PINCH_TEXT_SCALABLE_INTENT_KEY, true)

        // Typeface mTextRobotoTf = ResourcesCompat.getFont(this, R.font.roboto_medium);
        // Typeface mEmojiTypeFace = Typeface.createFromAsset(getAssets(), "emojione-android.ttf");
        mPhotoEditor = mPhotoEditorView?.run {
            PhotoEditor.Builder(this@EditImageActivity, this)
                .setPinchTextScalable(pinchTextScalable) // set flag to make text scalable when pinch
                // .setDefaultTextTypeface(mTextRobotoTf)
                // .setDefaultEmojiTypeface(mEmojiTypeFace)
                .build() // build photo editor sdk
        }
        mPhotoEditor?.setOnPhotoEditorListener(this)

        // Set Image Dynamically
        mSaveFileHelper = FileSaveHelper(this)
    }

    private fun handleIntentImage(source: ImageView?) {
        if (intent == null) {
            return
        }
        when (intent.action) {
            Intent.ACTION_EDIT, ACTION_NEXTGEN_EDIT -> {
                try {
                    val uri = intent.data
                    intentUri = uri
                    /*val bitmap = MediaStore.Images.Media.getBitmap(
                        contentResolver, uri
                    )
                    source?.setImageBitmap(bitmap)*/
                    Glide.with(this).load(uri.toString()).thumbnail(
                        Glide.with(this)
                            .load(
                                resources.getDrawable(R.drawable.ic_outline_image_32)
                            )
                    ).into(source!!)
                } catch (e: IOException) {
                    log.warn("failed to display image in editor", e)
                }
            }
            else -> {
                val imageUri = intent.data
                if (imageUri != null) {
//                        source?.setImageURI(imageUri)
                    intentUri = imageUri
                    Glide.with(this).load(imageUri.toString()).thumbnail(
                        Glide.with(this)
                            .load(
                                resources.getDrawable(R.drawable.ic_outline_image_32)
                            )
                    ).into(source!!)
                }
            }
        }
    }

    private fun initViews() {
        mPhotoEditorView = findViewById(R.id.photoEditorView)
        customToolbar = findViewById(R.id.custom_toolbar)
        mRvTools = findViewById(R.id.rvConstraintTools)
        mRvFilters = findViewById(R.id.rvFilterView)

        val imgUndo: ImageView = findViewById(R.id.imgUndo)
        imgUndo.setOnClickListener(this)
        val imgRedo: ImageView = findViewById(R.id.imgRedo)
        imgRedo.setOnClickListener(this)
        val imgSave: ImageView = findViewById(R.id.imgSave)
        imgSave.setOnClickListener(this)
        val imgShare: ImageView = findViewById(R.id.imgShare)
        imgShare.setOnClickListener(this)
        customToolbar?.setTitle(getString(R.string.image_editor))
        customToolbar?.setBackButtonClickListener {
            onBackPressed()
        }
        customToolbar?.addActionButton(resources.getDrawable(R.drawable.ic_round_edit_32)) {
            if (intentUri != null) {
                showEditImageDialog(intentUri!!, this)
            } else {
                showSnackbar(getString(R.string.operation_failed))
            }
        }
        customToolbar?.addActionButton(resources.getDrawable(R.drawable.ic_camera)) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST)
        }
        customToolbar?.addActionButton(resources.getDrawable(R.drawable.ic_gallery)) {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(
                    intent,
                    getString(R.string.select_picture)
                ),
                PICK_REQUEST
            )
        }
    }

    override fun onEditTextChangeListener(rootView: View?, text: String?, colorCode: Int) {
        val textEditorDialogFragment =
            TextEditorDialogFragment.show(this, text.toString(), colorCode)
        textEditorDialogFragment.setOnTextEditorListener(object :
                TextEditorDialogFragment.TextEditorListener {
                override fun onDone(inputText: String?, colorCode: Int) {
                    val styleBuilder = TextStyleBuilder()
                    styleBuilder.withTextColor(colorCode)
                    if (rootView != null) {
                        mPhotoEditor?.editText(rootView, inputText, styleBuilder)
                    }
//                mTxtCurrentTool?.setText(R.string.label_text)
                    customToolbar?.setTitle(getString(R.string.label_text))
                }
            })
    }

    override fun onAddViewListener(viewType: ViewType?, numberOfAddedViews: Int) {
        log.debug(
            TAG,
            "onAddViewListener() called with: viewType = [$viewType], " +
                "numberOfAddedViews = [$numberOfAddedViews]"
        )
    }

    override fun onRemoveViewListener(viewType: ViewType?, numberOfAddedViews: Int) {
        log.debug(
            TAG,
            "onRemoveViewListener() called with: viewType = [$viewType], " +
                "numberOfAddedViews = [$numberOfAddedViews]"
        )
    }

    override fun onStartViewChangeListener(viewType: ViewType?) {
        log.debug(TAG, "onStartViewChangeListener() called with: viewType = [$viewType]")
    }

    override fun onStopViewChangeListener(viewType: ViewType?) {
        log.debug(TAG, "onStopViewChangeListener() called with: viewType = [$viewType]")
    }

    override fun onTouchSourceImage(event: MotionEvent?) {
        log.debug(TAG, "onTouchView() called with: event = [$event]")
    }

    @SuppressLint("NonConstantResourceId", "MissingPermission")
    override fun onClick(view: View) {
        when (view.id) {
            R.id.imgUndo -> mPhotoEditor?.undo()
            R.id.imgRedo -> mPhotoEditor?.redo()
            R.id.imgSave -> saveImage({}, {})
            R.id.imgShare -> shareImage()
        }
    }

    private fun shareImage() {
        var shareAdapter: ShareAdapter?
        if (mSaveImageUri != null) {
            shareAdapter = getShareIntents(arrayListOf(mSaveImageUri!!), this)
            showShareDialog(
                this, this.layoutInflater,
                shareAdapter!!
            )
        } else {
            saveImage({ uri ->
                if (uri != null) {
                    shareAdapter = getShareIntents(arrayListOf(uri), this)
                    showShareDialog(
                        this, this.layoutInflater,
                        shareAdapter!!
                    )
                }
            }, {
                showSnackbar(getString(R.string.failed_to_share))
            })
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    private fun saveImage(successCallback: (Uri?) -> Unit, failureCallback: () -> Unit) {
        val fileName = System.currentTimeMillis().toString() + ".png"
        val hasStoragePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (hasStoragePermission || FileSaveHelper.isSdkHigherThan28()) {
            showLoading(getString(R.string.saving))
            mSaveFileHelper?.createFile(
                fileName,
                object : FileSaveHelper.OnFileCreateResult {

                    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
                    override fun onFileCreateResult(
                        created: Boolean,
                        filePath: String?,
                        error: String?,
                        uri: Uri?
                    ) {
                        if (created && filePath != null) {
                            val saveSettings = SaveSettings.Builder()
                                .setClearViewsEnabled(true)
                                .setTransparencyEnabled(true)
                                .build()

                            mPhotoEditor?.saveAsFile(
                                filePath,
                                saveSettings,
                                object : OnSaveListener {
                                    override fun onSuccess(imagePath: String) {
                                        mSaveFileHelper?.notifyThatFileIsNowPubliclyAvailable(
                                            contentResolver
                                        )
                                        hideLoading()
                                        showSnackbar(getString(R.string.image_saved))
                                        mSaveImageUri = uri
//                                    Glide.with(this@EditImageActivity).load(uri)
//                                        .into(mPhotoEditorView!!.source)
//                                    mPhotoEditorView?.source?.setImageURI(mSaveImageUri)
                                        successCallback.invoke(uri)
                                    }

                                    override fun onFailure(exception: Exception) {
                                        hideLoading()
                                        showSnackbar(getString(R.string.failed_image_save))
                                        failureCallback.invoke()
                                    }
                                }
                            )
                        } else {
                            hideLoading()
                            error?.let { showSnackbar(error) }
                        }
                    }
                }
            )
        } else {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    // TODO(lucianocheng): Replace onActivityResult with Result API from Google
    //                     See https://developer.android.com/training/basics/intents/result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST -> {
                    mPhotoEditor?.clearAllViews()
                    val photo = data?.extras?.get("data") as Bitmap?
//                    mPhotoEditorView?.source?.setImageBitmap(photo)
                    Glide.with(this).load(photo).into(mPhotoEditorView!!.source)
                }
                PICK_REQUEST -> try {
                    mPhotoEditor?.clearAllViews()
                    val uri = data?.data
                    val bitmap = MediaStore.Images.Media.getBitmap(
                        contentResolver, uri
                    )
//                    mPhotoEditorView?.source?.setImageBitmap(bitmap)
                    Glide.with(this).load(uri).into(mPhotoEditorView!!.source)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onColorChanged(colorCode: Int) {
        mPhotoEditor?.setShape(mShapeBuilder?.withShapeColor(colorCode))
        customToolbar?.setTitle(getString(R.string.label_brush))
    }

    override fun onOpacityChanged(opacity: Int) {
        mPhotoEditor?.setShape(mShapeBuilder?.withShapeOpacity(opacity))
        customToolbar?.setTitle(getString(R.string.label_brush))
    }

    override fun onShapeSizeChanged(shapeSize: Int) {
        mPhotoEditor?.setShape(mShapeBuilder?.withShapeSize(shapeSize.toFloat()))
        customToolbar?.setTitle(getString(R.string.label_brush))
    }

    override fun onShapePicked(shapeType: ShapeType?) {
        mPhotoEditor?.setShape(mShapeBuilder?.withShapeType(shapeType))
    }

    override fun onEmojiClick(emojiUnicode: String?) {
        mPhotoEditor?.addEmoji(emojiUnicode)
        customToolbar?.setTitle(getString(R.string.emoji))
    }

    override fun onStickerClick(bitmap: Bitmap?) {
        mPhotoEditor?.addImage(bitmap)
        customToolbar?.setTitle(getString(R.string.label_sticker))
    }

    @SuppressLint("MissingPermission")
    override fun isPermissionGranted(isGranted: Boolean, permission: String?) {
        if (isGranted) {
            saveImage({}, {})
        }
    }

    @SuppressLint("MissingPermission")
    private fun showSaveDialog() {
        val builder = AlertDialog.Builder(this, R.style.Custom_Dialog_Dark)
        builder.setMessage(getString(R.string.msg_save_image))
        builder.setPositiveButton(getString(R.string.save)) { _: DialogInterface?,
            _: Int ->
            saveImage({}, {})
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog: DialogInterface,
            _: Int ->
            dialog.dismiss()
        }
        builder.setNeutralButton(getString(R.string.discard)) { _: DialogInterface?,
            _: Int ->
            finish()
        }
        builder.create().show()
    }

    override fun onFilterSelected(photoFilter: PhotoFilter?) {
        mPhotoEditor?.setFilterEffect(photoFilter)
    }

    var isRotateApplied = false
    var isFlipHApplied = false
    var isFlipVApplied = false
    override fun onToolSelected(toolType: ToolType?) {
        when (toolType) {
            ToolType.SHAPE -> {
                mPhotoEditor?.setBrushDrawingMode(true)
                mShapeBuilder = ShapeBuilder()
                mPhotoEditor?.setShape(mShapeBuilder)
                customToolbar?.setTitle(getString(R.string.label_shape))
                showBottomSheetDialogFragment(mShapeBSFragment)
            }
            ToolType.TEXT -> {
                val textEditorDialogFragment = TextEditorDialogFragment.show(this)
                textEditorDialogFragment.setOnTextEditorListener(object :
                        TextEditorDialogFragment.TextEditorListener {
                        override fun onDone(inputText: String?, colorCode: Int) {
                            val styleBuilder = TextStyleBuilder()
                            styleBuilder.withTextColor(colorCode)
                            mPhotoEditor?.addText(inputText, styleBuilder)
                            customToolbar?.setTitle(getString(R.string.label_text))
                        }
                    })
            }
            ToolType.ERASER -> {
                mPhotoEditor?.brushEraser()
                customToolbar?.setTitle(getString(R.string.label_eraser_mode))
            }
            ToolType.FILTER -> {
                customToolbar?.setTitle(getString(R.string.label_filter))
                showFilter(true)
            }
            ToolType.ROTATE -> {
                isRotateApplied = !isRotateApplied
                mPhotoEditor?.setFilterEffect(
                    if (isRotateApplied) {
                        PhotoFilter.ROTATE
                    } else {
                        PhotoFilter.NONE
                    }
                )
//                applyBitmapOperation { rotate(it) }
            }
            ToolType.RESIZE -> {
            }
            ToolType.FLIP_HORIZONTAL -> {
                isFlipHApplied = !isFlipHApplied
                mPhotoEditor?.setFilterEffect(
                    if (isFlipHApplied) {
                        PhotoFilter.FLIP_HORIZONTAL
                    } else {
                        PhotoFilter.NONE
                    }
                )
//                applyBitmapOperation { flipHorizontal(it) }
            }
            ToolType.FLIP_VERTICAL -> {
                isFlipVApplied = !isFlipVApplied
                mPhotoEditor?.setFilterEffect(
                    if (isFlipVApplied) {
                        PhotoFilter.FLIP_VERTICAL
                    } else {
                        PhotoFilter.NONE
                    }
                )
//                applyBitmapOperation { flipVertical(it) }
            }
            ToolType.EMOJI -> showBottomSheetDialogFragment(mEmojiBSFragment)
            ToolType.STICKER -> showBottomSheetDialogFragment(mStickerBSFragment)
        }
    }

    private fun showBottomSheetDialogFragment(fragment: BottomSheetDialogFragment?) {
        if (fragment == null || fragment.isAdded) {
            return
        }
        fragment.show(supportFragmentManager, fragment.tag)
    }

    private fun showFilter(isVisible: Boolean) {
        mIsFilterVisible = isVisible
        if (isVisible) {
            mRvTools?.hideFade(300)
            mRvFilters?.showFade(300)
        } else {
            mRvFilters?.hideFade(300)
            mRvTools?.showFade(300)
        }
    }

    private fun getToolsAdapterList(): ArrayList<EditingToolsAdapter.ToolModel> {
        val toolsList = arrayListOf<EditingToolsAdapter.ToolModel>()
        toolsList.add(
            EditingToolsAdapter.ToolModel(
                getString(R.string.shape),
                R.drawable.ic_oval,
                ToolType.SHAPE
            )
        )
        toolsList.add(
            EditingToolsAdapter.ToolModel(
                getString(R.string.text), R.drawable.ic_text,
                ToolType.TEXT
            )
        )
        toolsList.add(
            EditingToolsAdapter.ToolModel(
                getString(R.string.eraser), R.drawable.ic_eraser,
                ToolType.ERASER
            )
        )
        toolsList.add(
            EditingToolsAdapter.ToolModel(
                getString(R.string.filter), R.drawable.ic_photo_filter,
                ToolType.FILTER
            )
        )
        toolsList.add(
            EditingToolsAdapter.ToolModel(
                getString(R.string.rotate),
                R.drawable.ic_baseline_rotate_right_24,
                ToolType.ROTATE
            )
        )
        toolsList.add(
            EditingToolsAdapter.ToolModel(
                getString(R.string.resize), R.drawable.ic_round_crop_24,
                ToolType.RESIZE
            )
        )
        toolsList.add(
            EditingToolsAdapter.ToolModel(
                getString(R.string.flip_horizontal), R.drawable.ic_round_flip_24,
                ToolType.FLIP_HORIZONTAL
            )
        )
        toolsList.add(
            EditingToolsAdapter.ToolModel(
                getString(R.string.flip_vertical), R.drawable.ic_round_flip_v_24,
                ToolType.FLIP_VERTICAL
            )
        )
        toolsList.add(
            EditingToolsAdapter.ToolModel(
                getString(R.string.emoji), R.drawable.ic_insert_emoticon,
                ToolType.EMOJI
            )
        )
        toolsList.add(
            EditingToolsAdapter.ToolModel(
                getString(R.string.sticker), R.drawable.ic_sticker,
                ToolType.STICKER
            )
        )
        return toolsList
    }

    override fun onBackPressed() {
        val isCacheEmpty = mPhotoEditor?.isCacheEmpty
            ?: throw IllegalArgumentException("isCacheEmpty Expected")

        if (mIsFilterVisible) {
            showFilter(false)
            customToolbar?.setTitle(getString(R.string.image_editor))
        } else if (!isCacheEmpty) {
            showSaveDialog()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private val TAG = EditImageActivity::class.java.simpleName
        const val FILE_PROVIDER_AUTHORITY = "com.amaze.fileutilities"
        private const val CAMERA_REQUEST = 52
        private const val PICK_REQUEST = 53
        const val ACTION_NEXTGEN_EDIT = "action_nextgen_edit"
        const val PINCH_TEXT_SCALABLE_INTENT_KEY = "PINCH_TEXT_SCALABLE"
    }
}
