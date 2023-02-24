package com.thelazybattley.facedetection.fragments

import android.graphics.Bitmap
import android.graphics.Matrix
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
        binding.ivTakeImage.setOnClickListener {
            if (binding.facebox.getRect() != null) {
                val croppedBitmap = camera.captureImage(rect = binding.facebox.getRect()!!)
                    ?: return@setOnClickListener
                val rgbFrameBitmap = binding.viewFinder.bitmap ?: return@setOnClickListener
                val portraitBitmap = rotateBitmap(croppedBitmap)
                CroppedFaceDialogFragment(
                    bitmap = croppedBitmap,
                    inputNameCallback = {
                        println("Test: $it")
                    }
                ).show(this.parentFragmentManager, CroppedFaceDialogFragment::class.simpleName)

            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        camera = FaceDetectionCameraImpl(requireContext())
        camera.setViewBinding(binding = binding)
        var cameraStarted = false
        binding.viewFinder.viewTreeObserver.addOnGlobalLayoutListener {
            if (binding.viewFinder.height > 0 && !cameraStarted) {
                cameraStarted = true
                camera.startCamera(
                    size = Size(binding.viewFinder.width, binding.viewFinder.height),
                ) { rect ->
                    binding.facebox.setRect(rect = rect)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        camera.stopCamera()
        _binding = null
    }

    private fun rotateBitmap(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        val rotation = binding.facebox.rotation
        println("Test: $rotation")
        matrix.postRotate(90f)

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true)

        return Bitmap.createBitmap(
            scaledBitmap,
            0,
            0,
            scaledBitmap.width,
            scaledBitmap.height,
            matrix,
            true
        )
    }
}
