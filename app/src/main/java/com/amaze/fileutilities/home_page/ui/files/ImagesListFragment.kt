package com.amaze.fileutilities.home_page.ui.files

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.FragmentFilesBinding
import com.amaze.fileutilities.databinding.FragmentImagesListBinding
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.home_page.ui.MediaTypeView
import com.amaze.fileutilities.utilis.FileUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider

class ImagesListFragment: Fragment() {
    private val filesViewModel: FilesViewModel by activityViewModels()
    private var _binding: FragmentImagesListBinding? = null
    private var mediaFileAdapter: MediaFileAdapter? = null
    private var preloader: MediaAdapterPreloader? = null
    private var recyclerViewPreloader: RecyclerViewPreloader<String>? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private val MAX_PRELOAD = 100

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImagesListBinding.inflate(
            inflater, container,
            false
        )
        val root: View = binding.root
        filesViewModel.usedImagesSummaryTransformations.observe(
            viewLifecycleOwner,
            {
                    metaInfoAndSummaryPair ->
                binding.imagesListInfoText.text = resources.getString(R.string.loading)
                metaInfoAndSummaryPair?.let {
                    val metaInfoList = metaInfoAndSummaryPair.second
                    metaInfoList.run {
                        if (this.size == 0) {
                            binding.imagesListInfoText.text =
                                resources.getString(R.string.no_files)
                                binding.loadingProgress.visibility = View.GONE
                        } else {
                            binding.imagesListInfoText.visibility = View.GONE
                            binding.loadingProgress.visibility = View.GONE
                        }
                        val storageSummary = metaInfoAndSummaryPair.first
                        val usedSpace =
                            FileUtils.formatStorageLength(
                                requireContext(), storageSummary.usedSpace!!
                            )
                        /*val totalSpace = FileUtils.formatStorageLength(
                            requireContext(), storageSummary.totalSpace!!
                        )*/
                        binding.imageListHeaderView.setProgress(
                            MediaTypeView.MediaTypeContent(
                                storageSummary.items, usedSpace,
                                storageSummary.progress, "totalSpace"
                            )
                        )
                        // set list adapter
                        preloader = MediaAdapterPreloader(requireContext())
                        val sizeProvider = ViewPreloadSizeProvider<String>()
                        recyclerViewPreloader = RecyclerViewPreloader(
                            Glide.with(requireActivity()),
                            preloader!!,
                            sizeProvider,
                            MAX_PRELOAD
                        )
                        linearLayoutManager = LinearLayoutManager(context)
                        mediaFileAdapter = MediaFileAdapter(
                            requireContext(),
                            preloader!!,
                            MediaFileListSorter.SortingPreference(
                                MediaFileListSorter.GROUP_NAME,
                                MediaFileListSorter.SORT_SIZE,
                                true,
                                true
                            ),
                            this, true
                        )
                        binding.imagesListView
                            .addOnScrollListener(recyclerViewPreloader!!)
                        binding.imagesListView.layoutManager = linearLayoutManager
                        binding.imagesListView.adapter = mediaFileAdapter
                    }
                }
            }
        )
        return root
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).setCustomTitle(resources.getString(R.string.images))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}