package com.amaze.fileutilities.home_page.ui.files

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.FragmentFilesBinding
import com.amaze.fileutilities.home_page.ui.MediaTypeView
import androidx.core.graphics.ColorUtils
import com.amaze.fileutilities.utilis.FileUtils

import com.ramijemli.percentagechartview.callback.AdaptiveColorProvider

class FilesFragment : Fragment() {

    private val filesViewModel: FilesViewModel by activityViewModels()
    private var _binding: FragmentFilesBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.storagePercent.isSaveEnabled = false

        filesViewModel.run {
            internalStorageStats.observe(viewLifecycleOwner, {
                it?.run {
                    val usedSpace = FileUtils.formatStorageLength(this@FilesFragment.requireContext(), it.usedSpace!!)
                    val freeSpace = FileUtils.formatStorageLength(this@FilesFragment.requireContext(), it.freeSpace!!)
//                val totalSpace = FileUtils.formatStorageLength(this@FilesFragment.requireContext(), it.totalSpace!!)
                    binding.usedSpace.setColorAndLabel(colorProvider.provideProgressColor(it.progress.toFloat()), usedSpace)
                    binding.freeSpace.setColorAndLabel(colorProvider.provideBackgroundBarColor(it.progress.toFloat()), freeSpace)
                    binding.storagePercent.setProgress(it.progress.toFloat(), true)
                    if (it.items == 0) {
                        binding.filesAmount.text = resources.getString(R.string.num_of_files,
                            resources.getString(R.string.undetermined))
                    } else {
                        binding.filesAmount.text = resources.getString(R.string.num_of_files, it.items.toString())
                    }
                }
            })
            usedImagesSummaryTransformations.observe(viewLifecycleOwner, {
                    storageSummary ->
                storageSummary?.let {
                    val usedSpace = FileUtils.formatStorageLength(this@FilesFragment.requireContext(),
                        storageSummary.usedSpace!!)
                    binding.imagesTab.setProgress(MediaTypeView.MediaTypeContent(it.items, usedSpace,
                        it.progress))
                }
            })
            usedAudiosSummaryTransformations.observe(viewLifecycleOwner, {
                    storageSummary ->
                storageSummary?.let {
                    val usedSpace = FileUtils.formatStorageLength(this@FilesFragment.requireContext(),
                        storageSummary.usedSpace!!)
                    binding.audiosTab.setProgress(MediaTypeView.MediaTypeContent(it.items, usedSpace, it.progress))
                }
            })
            usedVideosSummaryTransformations.observe(viewLifecycleOwner, {
                    storageSummary ->
                storageSummary?.let {
                    val usedSpace = FileUtils.formatStorageLength(this@FilesFragment.requireContext(),
                        storageSummary.usedSpace!!)
                    binding.videosTab.setProgress(MediaTypeView.MediaTypeContent(it.items, usedSpace,
                        it.progress))
                }
            })
            usedDocsSummaryTransformations.observe(viewLifecycleOwner, {
                    storageSummary ->
                storageSummary?.let {
                    val usedSpace = FileUtils.formatStorageLength(this@FilesFragment.requireContext(),
                        storageSummary.usedSpace!!)
                    binding.documentsTab.setProgress(MediaTypeView.MediaTypeContent(it.items, usedSpace,
                        it.progress))
                }
            })
        }

        binding.storagePercent.setAdaptiveColorProvider(colorProvider)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    var colorProvider: AdaptiveColorProvider = object : AdaptiveColorProvider {
        override fun provideProgressColor(progress: Float): Int {
            return when {
                progress <= 25 -> {
                    resources.getColor(R.color.green)
                }
                progress <= 50 -> {
                    resources.getColor(R.color.yellow)
                }
                progress <= 75 -> {
                    resources.getColor(R.color.orange)
                }
                else -> {
                    resources.getColor(R.color.red)
                }
            }
        }

        override fun provideBackgroundColor(progress: Float): Int {
            //This will provide a bg color that is 80% darker than progress color.
//            return ColorUtils.blendARGB(provideProgressColor(progress), Color.BLACK, .8f)
            return resources.getColor(R.color.white_translucent_2)
        }

        override fun provideTextColor(progress: Float): Int {
            return resources.getColor(R.color.white)
        }

        override fun provideBackgroundBarColor(progress: Float): Int {
            return ColorUtils.blendARGB(provideProgressColor(progress), Color.BLACK, .5f)
        }
    }
}