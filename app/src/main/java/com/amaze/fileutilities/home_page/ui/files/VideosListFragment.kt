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

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.util.Consumer
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.FragmentVideosListBinding
import com.amaze.fileutilities.home_page.MainActivity
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupStyles

class VideosListFragment : AbstractMediaInfoListFragment() {
    private val filesViewModel: FilesViewModel by activityViewModels()
    private var _binding: FragmentVideosListBinding? = null
    private lateinit var fileStorageSummaryAndMediaFileInfo:
        Pair<FilesViewModel.StorageSummary, List<MediaFileInfo>?>

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var preloader: MediaAdapterPreloader? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideosListBinding.inflate(
            inflater, container,
            false
        )
        val root: View = binding.root
        (requireActivity() as MainActivity).setCustomTitle(resources.getString(R.string.videos))
        (activity as MainActivity).invalidateBottomBar(false)
        filesViewModel.usedVideosSummaryTransformations().observe(
            viewLifecycleOwner
        ) { metaInfoAndSummaryPair ->
            binding.videosListInfoText.text = resources.getString(R.string.loading)
            metaInfoAndSummaryPair?.let {
                val metaInfoList = metaInfoAndSummaryPair.second
                metaInfoList.run {
                    if (this.isEmpty()) {
                        binding.videosListInfoText.text =
                            resources.getString(R.string.no_files)
                        binding.loadingProgress.visibility = View.GONE
                    } else {
                        binding.videosListInfoText.visibility = View.GONE
                        binding.loadingProgress.visibility = View.GONE
                    }
                    fileStorageSummaryAndMediaFileInfo = it
                    resetAdapter()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        binding.fastscroll.visibility = View.GONE
                        val popupStyle = Consumer<TextView> { popupView ->
                            PopupStyles.MD2.accept(popupView)
                            popupView.setTextColor(Color.BLACK)
                            popupView.setTextSize(
                                TypedValue.COMPLEX_UNIT_PX,
                                resources.getDimension(R.dimen.twenty_four_sp)
                            )
                        }
                        FastScrollerBuilder(binding.videosListView).useMd2Style()
                            .setPopupStyle(popupStyle).build()
                    } else {
                        binding.fastscroll.visibility = View.VISIBLE
                        binding.fastscroll.setRecyclerView(binding.videosListView, 1)
                    }
                }
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as MainActivity)
            .setCustomTitle(resources.getString(R.string.title_utilities))
        (activity as MainActivity).invalidateBottomBar(true)
        _binding = null
    }

    override fun getFileStorageSummaryAndMediaFileInfoPair(): Pair<FilesViewModel.StorageSummary,
        List<MediaFileInfo>?>? {
        return if (::fileStorageSummaryAndMediaFileInfo.isInitialized)
            fileStorageSummaryAndMediaFileInfo else null
    }

    override fun getMediaAdapterPreloader(): MediaAdapterPreloader {
        if (preloader == null) {
            preloader = MediaAdapterPreloader(
                requireContext(),
                R.drawable.ic_outline_video_library_32
            )
        }
        return preloader!!
    }

    override fun getRecyclerView(): RecyclerView {
        return binding.videosListView
    }

    override fun getMediaListType(): Int {
        return MediaFileAdapter.MEDIA_TYPE_VIDEO
    }

    override fun getItemPressedCallback(mediaFileInfo: MediaFileInfo) {
        // do nothing
    }
}
