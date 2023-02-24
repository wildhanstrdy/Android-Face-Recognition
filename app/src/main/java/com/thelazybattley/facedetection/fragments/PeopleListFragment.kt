package com.thelazybattley.facedetection.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.thelazybattley.facedetection.R
import com.thelazybattley.facedetection.adapter.PeopleListAdapter
import com.thelazybattley.facedetection.classifier.SimilarityClassifier
import com.thelazybattley.facedetection.databinding.PeopleListBinding

class PeopleListFragment: Fragment() {

    val binding get() = _binding!!
    private var _binding:PeopleListBinding? = null
    private var classifier: SimilarityClassifier? = null
    private val adapter = PeopleListAdapter()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = PeopleListBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        classifier = requireActivity() as SimilarityClassifier
        binding.rvPeople.adapter = this.adapter
        binding.rvPeople.layoutManager = LinearLayoutManager(requireContext())
        adapter.submitList(classifier?.getRegisteredPeople())
    }

    override fun onResume() {
        super.onResume()
        classifier?.let {
            adapter.submitList(it.getRegisteredPeople())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
