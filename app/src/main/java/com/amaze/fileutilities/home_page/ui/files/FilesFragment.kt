package com.amaze.fileutilities.home_page.ui.files

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.FragmentFilesBinding
import com.amaze.fileutilities.home_page.ui.MediaTypeView

class FilesFragment : Fragment() {

    private lateinit var filesViewModel: FilesViewModel
    private var _binding: FragmentFilesBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        filesViewModel =
            ViewModelProvider(this).get(FilesViewModel::class.java)

        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        filesViewModel.usedSpace.observe(viewLifecycleOwner, Observer {
            binding.usedSpace.setColorAndLabel(resources.getColor(R.color.blue), it)
            binding.freeSpace.setColorAndLabel(resources.getColor(R.color.white_translucent_2), it)
        })
        binding.imagesTab.setProgress(MediaTypeView.MediaTypeContent(50, 12))
        binding.audiosTab.setProgress(MediaTypeView.MediaTypeContent(20, 45))
        binding.videosTab.setProgress(MediaTypeView.MediaTypeContent(560, 67))
        binding.documentsTab.setProgress(MediaTypeView.MediaTypeContent(650, 23))
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}