/*
 * Copyright (C) 2021-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.FragmentSearchListBinding
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.home_page.ui.AggregatedMediaFileInfoObserver
import com.amaze.fileutilities.utilis.AbstractMediaFilesAdapter
import com.amaze.fileutilities.utilis.ItemsActionBarFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SearchListFragment :
    ItemsActionBarFragment(),
    AggregatedMediaFileInfoObserver,
    TextView.OnEditorActionListener,
    TextWatcher {
    private val filesViewModel: FilesViewModel by activityViewModels()
    private var _binding: FragmentSearchListBinding? = null
    private var searchEditText: AutoCompleteTextView? = null

    private var mediaFileAdapter: RecentMediaFilesAdapter? = null
    private var preloader: MediaAdapterPreloader? = null
    private var recyclerViewPreloader: RecyclerViewPreloader<String>? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private val searchQueryInput: SearchQueryInput =
        SearchQueryInput(AggregatedMediaFileInfoObserver.AggregatedMediaFiles(), SearchFilter())

    companion object {
        const val MAX_PRELOAD = 100
        const val SEARCH_THRESHOLD = 2
        const val SEARCH_HINT_THRESHOLD = 3
        const val SEARCH_HINT_RESULTS_THRESHOLD = 3
        const val FRAGMENT_TAG = "search_fragment"
    }
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    override fun getFilesModel(): FilesViewModel {
        return filesViewModel
    }

    override fun lifeCycleOwner(): LifecycleOwner {
        return viewLifecycleOwner
    }

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
        observeMediaInfoLists { isLoading, aggregatedFiles ->
            if (isLoading) {
                showLoadingViews(true)
            } else {
                aggregatedFiles?.run {
                    showLoadingViews(false)
                    showEmptyViews()
                    searchQueryInput.aggregatedMediaFiles = this
                }
            }
        }
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
        searchEditText?.requestFocus()
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
            requireActivity(),
            preloader!!,
            mutableListOf()
        ) {
            checkedSize, itemsCount, bytesFormatted ->
            val title = "$checkedSize / $itemsCount" +
                " ($bytesFormatted)"
            if (checkedSize > 0) {
                setupShowActionBar()
                setupCommonButtons()
            } else {
                hideActionBar()
            }

            val countView = getCountView()
            countView?.text = title
        }
        invalidateFilterButtons()
        binding.run {
            searchQueryInput.searchFilter.let {
                searchFilter ->
                filterImagesButton.setOnClickListener {
                    searchFilter.toggleFilterImages()
                    invalidateFilterButtons()
                }
                filterDocumentsButton.setOnClickListener {
                    searchFilter.toggleFilterDocs()
                    invalidateFilterButtons()
                }
                filterVideosButton.setOnClickListener {
                    searchFilter.toggleFilterVideos()
                    invalidateFilterButtons()
                }
                filterAudiosButton.setOnClickListener {
                    searchFilter.toggleFilterAudios()
                    invalidateFilterButtons()
                }
            }
            searchListView.addOnScrollListener(recyclerViewPreloader!!)
            searchListView.layoutManager = linearLayoutManager
            searchListView.adapter = mediaFileAdapter
            fastscroll.setRecyclerView(searchListView, 1)
        }
        return root
    }

    override fun onDestroyView() {
        searchEditText?.removeTextChangedListener(this)
        (activity as MainActivity).invalidateSearchBar(false)
        (activity as MainActivity).invalidateBottomBar(true)
        _binding = null
        super.onDestroyView()
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
                    if (searchQueryInput.aggregatedMediaFiles.mediaListsLoaded()) {
                        showLoadingViews(false)
                        filesViewModel.queryOnAggregatedMediaFiles(
                            it.text.toString(),
                            searchQueryInput
                        ).observe(
                            viewLifecycleOwner
                        ) { mediaFileInfoList ->
                            if (mediaFileInfoList != null) {
                                showLoadingViews(false)
                                if (mediaFileInfoList.size == 0) {
                                    showEmptyViews()
                                } else {
                                    binding.searchListView.scrollToPosition(0)
                                    mediaFileAdapter?.setData(mediaFileInfoList)
                                }
                            } else {
                                showLoadingViews(true)
                            }
                        }
                    } else {
                        showLoadingViews(true)
                    }
                } else {
                    if (searchQueryInput.aggregatedMediaFiles.mediaListsLoaded()) {
                        mediaFileAdapter?.setData(emptyList())
                        searchEditText?.dismissDropDown()
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
                if (searchQueryInput.aggregatedMediaFiles.mediaListsLoaded()) {
                    showLoadingViews(false)
                    filesViewModel.queryHintOnAggregatedMediaFiles(
                        query.toString(),
                        SEARCH_HINT_RESULTS_THRESHOLD,
                        searchQueryInput
                    ).observe(
                        viewLifecycleOwner
                    ) {
                        if (it != null) {
                            val adapter: ArrayAdapter<String> =
                                ArrayAdapter<String>(
                                    requireContext(),
                                    R.layout.custom_simple_selectable_list_item, it
                                )
                            searchEditText?.setAdapter(adapter)
                        }
                    }
                } else {
                    showLoadingViews(true)
                }
            } else {
                searchEditText?.dismissDropDown()
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

    override fun hideActionBarOnClick(): Boolean {
        return true
    }

    override fun getMediaFileAdapter(): AbstractMediaFilesAdapter? {
        return mediaFileAdapter
    }

    override fun getMediaListType(): Int {
        return MediaFileAdapter.MEDIA_TYPE_UNKNOWN
    }

    override fun getAllOptionsFAB(): List<FloatingActionButton> {
        return arrayListOf(
            binding.optionsButtonFab, binding.deleteButtonFab,
            binding.shareButtonFab, binding.locateFileButtonFab
        )
    }

    override fun showOptionsCallback() {
        getPlayNextButton()?.visibility = View.GONE
    }

    override fun hideOptionsCallback() {
        // do nothing
    }

    private fun showLoadingViews(doShow: Boolean) {
        binding.run {
            processingProgressView.invalidateProcessing(
                doShow, false,
                if (searchQueryInput.aggregatedMediaFiles.mediaListsLoaded()) {
                    resources.getString(R.string.loading)
                } else {
                    resources.getString(R.string.please_wait)
                }
            )
        }
    }

    private fun showEmptyViews() {
        binding.run {
            processingProgressView.invalidateProcessing(
                false, true,
                resources.getString(R.string.its_quiet_here)
            )
        }
    }

    private fun invalidateFilterButtons() {
        binding.run {
            if (searchQueryInput.searchFilter.searchFilterImages) {
                filterImagesButton.setBackgroundColor(resources.getColor(R.color.white))
                filterImagesButton.setTextColor(resources.getColor(R.color.navy_blue))
            } else {
                filterImagesButton.setBackgroundColor(
                    resources
                        .getColor(R.color.white_translucent_2)
                )
                filterImagesButton.setTextColor(resources.getColor(R.color.white))
            }
            if (searchQueryInput.searchFilter.searchFilterAudios) {
                filterAudiosButton.setBackgroundColor(resources.getColor(R.color.white))
                filterAudiosButton.setTextColor(resources.getColor(R.color.navy_blue))
            } else {
                filterAudiosButton.setBackgroundColor(
                    resources
                        .getColor(R.color.white_translucent_2)
                )
                filterAudiosButton.setTextColor(resources.getColor(R.color.white))
            }
            if (searchQueryInput.searchFilter.searchFilterVideos) {
                filterVideosButton.setBackgroundColor(resources.getColor(R.color.white))
                filterVideosButton.setTextColor(resources.getColor(R.color.navy_blue))
            } else {
                filterVideosButton.setBackgroundColor(
                    resources
                        .getColor(R.color.white_translucent_2)
                )
                filterVideosButton.setTextColor(resources.getColor(R.color.white))
            }
            if (searchQueryInput.searchFilter.searchFilterDocuments) {
                filterDocumentsButton.setBackgroundColor(resources.getColor(R.color.white))
                filterDocumentsButton.setTextColor(resources.getColor(R.color.navy_blue))
            } else {
                filterDocumentsButton.setBackgroundColor(
                    resources
                        .getColor(R.color.white_translucent_2)
                )
                filterDocumentsButton.setTextColor(resources.getColor(R.color.white))
            }
        }
    }

    data class SearchQueryInput(
        var aggregatedMediaFiles: AggregatedMediaFileInfoObserver.AggregatedMediaFiles,
        val searchFilter: SearchFilter
    )

    data class SearchFilter(
        var searchFilterImages: Boolean = true,
        var searchFilterVideos: Boolean = true,
        var searchFilterAudios: Boolean = true,
        var searchFilterDocuments: Boolean = true
    ) {
        fun toggleFilterImages() {
            searchFilterImages = !searchFilterImages
        }

        fun toggleFilterVideos() {
            searchFilterVideos = !searchFilterVideos
        }

        fun toggleFilterAudios() {
            searchFilterAudios = !searchFilterAudios
        }

        fun toggleFilterDocs() {
            searchFilterDocuments = !searchFilterDocuments
        }
    }
}
