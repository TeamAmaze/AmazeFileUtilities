/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.files

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.FragmentSearchListBinding
import com.amaze.fileutilities.home_page.MainActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider

class SearchListFragment : Fragment(), TextView.OnEditorActionListener, TextWatcher {
    private val filesViewModel: FilesViewModel by activityViewModels()
    private var _binding: FragmentSearchListBinding? = null
    private var searchEditText: AutoCompleteTextView? = null

    private var mediaFileAdapter: RecentMediaFilesAdapter? = null
    private var preloader: MediaAdapterPreloader? = null
    private var recyclerViewPreloader: RecyclerViewPreloader<String>? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var imagesMediaFilesList: ArrayList<MediaFileInfo>? = null
    private var videosMediaFilesList: ArrayList<MediaFileInfo>? = null
    private var audiosMediaFilesList: ArrayList<MediaFileInfo>? = null
    private var docsMediaFilesList: ArrayList<MediaFileInfo>? = null

    companion object {
        const val MAX_PRELOAD = 100
        const val SEARCH_THRESHOLD = 2
        const val SEARCH_HINT_THRESHOLD = 3
        const val SEARCH_HINT_RESULTS_THRESHOLD = 3
    }
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchListBinding.inflate(
            inflater, container,
            false
        )
        val root: View = binding.root
        observeMediaInfoLists()
        searchEditText = (activity as MainActivity).invalidateSearchBar(true)!!
        (activity as MainActivity).invalidateBottomBar(false)
        searchEditText?.let {
            val adapter: ArrayAdapter<String> =
                ArrayAdapter<String>(
                    requireContext(),
                    R.layout.custom_simple_selectable_list_item,
                    emptyArray()
                )
            searchEditText?.setOnEditorActionListener(this)
            searchEditText?.addTextChangedListener(this)
            searchEditText?.threshold = 0
            searchEditText?.setAdapter(adapter)
        }
        preloader = MediaAdapterPreloader(
            requireContext(),
            R.drawable.ic_outline_insert_drive_file_32
        )
        val sizeProvider = ViewPreloadSizeProvider<String>()
        recyclerViewPreloader = RecyclerViewPreloader(
            Glide.with(requireContext()),
            preloader!!,
            sizeProvider,
            MAX_PRELOAD
        )
        linearLayoutManager = LinearLayoutManager(context)
        mediaFileAdapter = RecentMediaFilesAdapter(
            requireContext(),
            preloader!!,
            mutableListOf()
        )
        binding.searchListView.addOnScrollListener(recyclerViewPreloader!!)
        binding.searchListView.layoutManager = linearLayoutManager
        binding.searchListView.adapter = mediaFileAdapter
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchEditText?.removeTextChangedListener(this)
        (activity as MainActivity).invalidateSearchBar(false)
        (activity as MainActivity).invalidateBottomBar(true)
        _binding = null
    }

    override fun onEditorAction(
        v: TextView?,
        actionId: Int,
        event: KeyEvent?
    ): Boolean {
        var handled = false
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            v?.let {
                if (it.text != null &&
                    it.text.length > SEARCH_THRESHOLD
                ) {
                    if (imagesMediaFilesList != null &&
                        videosMediaFilesList != null && audiosMediaFilesList != null &&
                        docsMediaFilesList != null
                    ) {
                        showLoadingViews(false)
                        filesViewModel.queryOnAggregatedMediaFiles(
                            it.text.toString(),
                            imagesMediaFilesList!!, videosMediaFilesList!!, audiosMediaFilesList!!,
                            docsMediaFilesList!!
                        ).observe(
                            viewLifecycleOwner,
                            {
                                mediaFileInfoList ->
                                if (mediaFileInfoList != null) {
                                    showLoadingViews(false)
                                    if (mediaFileInfoList.size == 0) {
                                        showEmptyViews()
                                    } else {
                                        mediaFileAdapter?.setData(mediaFileInfoList)
                                    }
                                } else {
                                    showLoadingViews(true)
                                }
                            }
                        )
                    } else {
                        showLoadingViews(true)
                    }
                } else {
                    if (imagesMediaFilesList != null && videosMediaFilesList != null &&
                        audiosMediaFilesList != null && docsMediaFilesList != null
                    ) {
                        mediaFileAdapter?.setData(emptyList())
                        showEmptyViews()
                    }
                }
            }
            handled = true
        }
        return handled
    }

    override fun afterTextChanged(s: Editable?) {
        s?.let {
            query ->
            if (query.toString().length > SEARCH_HINT_THRESHOLD) {
                if (imagesMediaFilesList != null &&
                    videosMediaFilesList != null && audiosMediaFilesList != null &&
                    docsMediaFilesList != null
                ) {
                    showLoadingViews(false)
                    filesViewModel.queryHintOnAggregatedMediaFiles(
                        query.toString(),
                        SEARCH_HINT_RESULTS_THRESHOLD,
                        imagesMediaFilesList!!, videosMediaFilesList!!, audiosMediaFilesList!!,
                        docsMediaFilesList!!
                    ).observe(
                        viewLifecycleOwner,
                        {
                            if (it != null) {
                                val adapter: ArrayAdapter<String> =
                                    ArrayAdapter<String>(
                                        requireContext(),
                                        R.layout.custom_simple_selectable_list_item, it
                                    )
                                searchEditText?.setAdapter(adapter)
                            }
                        }
                    )
                } else {
                    showLoadingViews(true)
                }
            } else {
                searchEditText?.setAdapter(null)
            }
        }
    }

    override fun beforeTextChanged(
        s: CharSequence?,
        start: Int,
        count: Int,
        after: Int
    ) {
        // do nothing
    }

    override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int
    ) {
        // do nothing
    }

    private fun observeMediaInfoLists() {
        filesViewModel.usedImagesSummaryTransformations
            .observe(
                viewLifecycleOwner,
                {
                    imagesPair ->
                    imagesPairObserver(imagesPair)
                }
            )
    }

    private fun imagesPairObserver(
        imagesPair:
            Pair<FilesViewModel.StorageSummary,
                ArrayList<MediaFileInfo>>?
    ) {
        if (imagesPair?.second != null) {
            showLoadingViews(false)
            filesViewModel.usedVideosSummaryTransformations
                .observe(
                    viewLifecycleOwner,
                    {
                        videosPair ->
                        videosPairObserver(videosPair, imagesPair)
                    }
                )
        } else {
            showLoadingViews(true)
        }
    }

    private fun videosPairObserver(
        videosPair:
            Pair<FilesViewModel.StorageSummary,
                ArrayList<MediaFileInfo>>?,
        imagesPair:
            Pair<FilesViewModel.StorageSummary,
                ArrayList<MediaFileInfo>>
    ) {
        if (videosPair?.second != null) {
            showLoadingViews(false)
            filesViewModel.usedAudiosSummaryTransformations
                .observe(
                    viewLifecycleOwner,
                    {
                        audiosPair ->
                        audiosPairObserver(
                            audiosPair, videosPair, imagesPair
                        )
                    }
                )
        } else {
            showLoadingViews(true)
        }
    }

    private fun audiosPairObserver(
        audiosPair:
            Pair<FilesViewModel.StorageSummary,
                ArrayList<MediaFileInfo>>?,
        videosPair:
            Pair<FilesViewModel.StorageSummary,
                ArrayList<MediaFileInfo>>,
        imagesPair:
            Pair<FilesViewModel.StorageSummary,
                ArrayList<MediaFileInfo>>
    ) {
        if (audiosPair?.second != null) {
            showLoadingViews(false)
            filesViewModel.usedDocsSummaryTransformations
                .observe(
                    viewLifecycleOwner,
                    {
                        docsPair ->
                        docsPairObserver(
                            docsPair, audiosPair, videosPair, imagesPair
                        )
                    }
                )
        } else {
            showLoadingViews(true)
        }
    }

    private fun docsPairObserver(
        docsPair:
            Pair<FilesViewModel.StorageSummary,
                ArrayList<MediaFileInfo>>?,
        audiosPair:
            Pair<FilesViewModel.StorageSummary,
                ArrayList<MediaFileInfo>>,
        videosPair:
            Pair<FilesViewModel.StorageSummary,
                ArrayList<MediaFileInfo>>,
        imagesPair:
            Pair<FilesViewModel.StorageSummary,
                ArrayList<MediaFileInfo>>
    ) {
        if (docsPair?.second != null) {
            showLoadingViews(false)
            showEmptyViews()
            imagesMediaFilesList = imagesPair.second
            videosMediaFilesList = videosPair.second
            audiosMediaFilesList = audiosPair.second
            docsMediaFilesList = docsPair.second
        } else {
            showLoadingViews(true)
        }
    }

    private fun showLoadingViews(doShow: Boolean) {
        binding.run {
            if (doShow) {
                searchListView.visibility = View.GONE
                loadingProgress.visibility = View.VISIBLE
                searchInfoText.visibility = View.VISIBLE
                searchInfoText.text = resources.getString(R.string.loading)
            } else {
                searchListView.visibility = View.VISIBLE
                loadingProgress.visibility = View.GONE
                searchInfoText.visibility = View.GONE
                emptyResultImage.visibility = View.GONE
            }
        }
    }

    private fun showEmptyViews() {
        binding.run {
            searchListView.visibility = View.GONE
            emptyResultImage.visibility = View.VISIBLE
            searchInfoText.visibility = View.VISIBLE
            searchInfoText.text = resources.getString(R.string.its_quiet_here)
        }
    }
}
