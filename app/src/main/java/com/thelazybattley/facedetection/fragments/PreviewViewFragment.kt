package com.thelazybattley.facedetection.fragments

import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.thelazybattley.facedetection.R
import com.thelazybattley.facedetection.databinding.PreviewViewBinding
import com.thelazybattley.facedetection.fragments.impl.FaceDetectionCameraImpl

class PreviewViewFragment : Fragment(R.layout.preview_view) {

    private var _binding: PreviewViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var camera: FaceDetectionCamera

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = PreviewViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        camera = FaceDetectionCameraImpl(requireContext())
        camera.setViewBinding(binding = binding)
        var cameraStarted = false
        binding.viewFinder.viewTreeObserver.addOnGlobalLayoutListener {
            if(binding.viewFinder.height > 0 && !cameraStarted) {
                cameraStarted = true
                camera.startCamera(
                    size = Size(binding.viewFinder.width, binding.viewFinder.height),
                ) {
                    binding.facebox.setRect(rect = it)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        camera.stopCamera()
        _binding = null
    }

}
