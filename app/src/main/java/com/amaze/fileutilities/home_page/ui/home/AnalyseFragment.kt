package com.amaze.fileutilities.home_page.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.databinding.FragmentAnalyseBinding

class AnalyseFragment : Fragment() {

    private lateinit var analyseViewModel: AnalyseViewModel
    private var _binding: FragmentAnalyseBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        analyseViewModel =
            ViewModelProvider(this).get(AnalyseViewModel::class.java)

        _binding = FragmentAnalyseBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        analyseViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}