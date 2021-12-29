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

import android.R
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
import com.amaze.fileutilities.databinding.FragmentSearchListBinding
import com.amaze.fileutilities.home_page.MainActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider

class SearchListFragment: Fragment(), TextView.OnEditorActionListener, TextWatcher {
    private val filesViewModel: FilesViewModel by activityViewModels()
    private var _binding: FragmentSearchListBinding? = null
    private var searchEditText: AutoCompleteTextView? = null

    private var mediaFileAdapter: RecentMediaFilesAdapter? = null
    private var preloader: MediaAdapterPreloader? = null
    private var recyclerViewPreloader: RecyclerViewPreloader<String>? = null
    private var linearLayoutManager: LinearLayoutManager? = null

    companion object {
        const val MAX_PRELOAD = 100
        const val SEARCH_THRESHOLD = 3
        const val SEARCH_HINT_RESULTS_THRESHOLD = 5
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
        searchEditText = (activity as MainActivity).invalidateSearchBar(true)!!
        searchEditText?.let {
            val adapter: ArrayAdapter<String> =
                ArrayAdapter<String>(requireContext(),
                    R.layout.simple_selectable_list_item,
                    emptyArray())
            searchEditText?.setOnEditorActionListener(this)
            searchEditText?.addTextChangedListener(this)
            searchEditText?.threshold = SEARCH_HINT_RESULTS_THRESHOLD
            searchEditText?.setAdapter(adapter)
        }
        preloader = MediaAdapterPreloader(
            requireContext(),
            com.amaze.fileutilities.R.drawable.ic_outline_insert_drive_file_32
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
        _binding = null
    }

    override fun onEditorAction(v: TextView?, actionId: Int,
                                event: KeyEvent?): Boolean {
        var handled = false
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            v?.let {
                if (it.text != null
                    && it.text.length > SEARCH_THRESHOLD) {
                    invokeSearch { imagesList, videosList, audiosList, docsList ->
                        filesViewModel.queryOnAggregatedMediaFiles(it.text.toString(),
                            imagesList, videosList, audiosList,
                            docsList).observe(viewLifecycleOwner, {
                                mediaFileInfoList ->
                            if (mediaFileInfoList != null) {
                                mediaFileAdapter?.setData(mediaFileInfoList)
                            }
                        })
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
            if (query.toString().length > SEARCH_THRESHOLD) {
                invokeSearch { imagesList,
                               videosList,
                               audiosList,
                               docsList ->
                    filesViewModel.queryHintOnAggregatedMediaFiles(query.toString(),
                        SEARCH_HINT_RESULTS_THRESHOLD,
                        imagesList, videosList, audiosList,
                        docsList).observe(viewLifecycleOwner, {
                        if (it != null) {
                            val adapter: ArrayAdapter<String> =
                                ArrayAdapter<String>(requireContext(),
                                    R.layout.simple_selectable_list_item, it)
                            searchEditText?.setAdapter(adapter)
                        }
                    })
                }
            }
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int,
                                   count: Int, after: Int) {
        // do nothing
    }

    override fun onTextChanged(s: CharSequence?, start: Int,
                               before: Int, count: Int) {
        // do nothing
    }

    override fun onStop() {
        super.onStop()
        (activity as MainActivity).invalidateSearchBar(false)
    }

    private fun invokeSearch(callback: (imagesList: ArrayList<MediaFileInfo>,
                                videosList: ArrayList<MediaFileInfo>,
                                audiosList: ArrayList<MediaFileInfo>,
                                        docsList: ArrayList<MediaFileInfo>) -> Unit) {
        filesViewModel.usedImagesSummaryTransformations
            .observe(viewLifecycleOwner, {
                imagesPair ->
            if (imagesPair?.second != null) {
                filesViewModel.usedVideosSummaryTransformations
                    .observe(viewLifecycleOwner, {
                        videosPair ->
                    if (videosPair?.second != null) {
                        filesViewModel.usedAudiosSummaryTransformations
                            .observe(viewLifecycleOwner, {
                                audiosPair ->
                            if (audiosPair?.second != null) {
                                filesViewModel.usedDocsSummaryTransformations
                                    .observe(viewLifecycleOwner, {
                                        docsPair ->
                                    if (docsPair?.second != null) {
                                        callback.invoke(imagesPair.second,
                                            videosPair.second,
                                            audiosPair.second, docsPair.second)
                                    }
                                })
                            }
                        })
                    }
                })
            }
        })
    }
}
