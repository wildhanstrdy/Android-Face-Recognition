package com.thelazybattley.facedetection.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.thelazybattley.facedetection.databinding.CroppedFaceDialogFragmentBinding

class CroppedFaceDialogFragment(
    private val bitmap: Bitmap,
    private val inputNameCallback: (String) -> Unit,
) : DialogFragment() {

    private var _binding: CroppedFaceDialogFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = CroppedFaceDialogFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivImage.setImageBitmap(bitmap)
        binding.mbOk.setOnClickListener {
            val text = binding.tfName.text
            inputNameCallback(text.toString())
            dismiss()
        }
    }
}
