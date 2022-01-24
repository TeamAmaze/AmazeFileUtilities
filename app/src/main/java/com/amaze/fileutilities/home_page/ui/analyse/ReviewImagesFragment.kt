/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.analyse

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.FragmentReviewImagesBinding
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.home_page.database.AppDatabase
import com.amaze.fileutilities.home_page.ui.files.MediaAdapterPreloader
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.utilis.Utils
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import me.zhanghai.android.fastscroll.FastScrollerBuilder

class ReviewImagesFragment : Fragment() {

    private var _binding: FragmentReviewImagesBinding? = null
    private lateinit var viewModel: AnalyseViewModel
    private var gridLayoutManager: GridLayoutManager? = GridLayoutManager(context, 3)
    private var mediaFileAdapter: ReviewImagesAdapter? = null
    private var preloader: MediaAdapterPreloader? = null
    private var recyclerViewPreloader: RecyclerViewPreloader<String>? = null
    private val MAX_PRELOAD = 50
    private var optionsActionBar: View? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    companion object {

        private const val IS_BLUR_FRAGMENT = "is_blur"

        fun newInstance(isBlur: Boolean, fragment: Fragment) {
            val analyseFragment = ReviewImagesFragment()
            analyseFragment.apply {
                val bundle = Bundle()
                bundle.putBoolean(IS_BLUR_FRAGMENT, isBlur)
                arguments = bundle
            }

            val transaction = fragment.parentFragmentManager.beginTransaction()
            transaction.add(R.id.nav_host_fragment_activity_main, analyseFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel =
            ViewModelProvider(this).get(AnalyseViewModel::class.java)

        _binding = FragmentReviewImagesBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val isBlur: Boolean = arguments?.getBoolean(IS_BLUR_FRAGMENT)!!
        optionsActionBar = (activity as MainActivity).invalidateSelectedActionBar(true)!!
        (activity as MainActivity).invalidateBottomBar(false)

        // set list adapter
        preloader = MediaAdapterPreloader(
            requireContext(),
            R.drawable.ic_outline_image_32
        )
        val sizeProvider = ViewPreloadSizeProvider<String>()
        recyclerViewPreloader = RecyclerViewPreloader(
            Glide.with(requireActivity()),
            preloader!!,
            sizeProvider,
            MAX_PRELOAD
        )
        val appDatabase = AppDatabase.getInstance(requireContext())
        val dao = appDatabase.analysisDao()
        if (isBlur) {
            viewModel.getBlurImages(dao).observe(viewLifecycleOwner, {
                setMediaInfoList(it)
            })
        } else {
            viewModel.getMemeImages(dao).observe(viewLifecycleOwner, {
                setMediaInfoList(it)
            })
        }
        return root
    }

    private fun setMediaInfoList(mediaInfoList: MutableList<MediaFileInfo>?) {
        if (mediaInfoList == null) {
            invalidateProcessing(true)
        } else {
            invalidateProcessing(false)
            mediaFileAdapter = ReviewImagesAdapter(
                requireContext(),
                preloader!!, mediaInfoList
            ) {
                val countView = optionsActionBar?.findViewById<AppCompatTextView>(R.id.title)
                countView?.text = "$it"
            }
            binding.listView
                .addOnScrollListener(recyclerViewPreloader!!)
            Utils.setGridLayoutManagerSpan(gridLayoutManager!!, mediaFileAdapter!!)
            binding.listView.layoutManager = gridLayoutManager
            binding.listView.adapter = mediaFileAdapter
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.fastscroll.visibility = View.GONE
                FastScrollerBuilder(binding.listView).useMd2Style().build()
            } else {
                binding.fastscroll.visibility = View.VISIBLE
                binding.fastscroll.setRecyclerView(binding.listView, 1)
            }
        }
    }

    private fun invalidateProcessing(isProcessing: Boolean) {
        binding.loadingProgress.visibility = if (isProcessing) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        (activity as MainActivity).invalidateSelectedActionBar(false)
        (activity as MainActivity).invalidateBottomBar(true)
        super.onDestroyView()
    }
}
