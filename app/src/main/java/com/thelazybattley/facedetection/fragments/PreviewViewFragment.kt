package com.thelazybattley.facedetection.fragments

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.thelazybattley.facedetection.R
import com.thelazybattley.facedetection.classifier.People
import com.thelazybattley.facedetection.classifier.SimilarityClassifier
import com.thelazybattley.facedetection.databinding.PreviewViewBinding
import com.thelazybattley.facedetection.fragments.impl.FaceDetectionCameraImpl

class PreviewViewFragment : Fragment() {

    companion object{
        private const val SCAN_FACE = "SCAN_FACE"
        fun initialize(scan:Boolean = false): PreviewViewFragment {
            return PreviewViewFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(SCAN_FACE,scan)
                }
            }
        }
    }

    private var _binding: PreviewViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var camera: FaceDetectionCamera
    lateinit var similarityClassifier: SimilarityClassifier

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        camera = FaceDetectionCameraImpl(requireContext())
        _binding = PreviewViewBinding.inflate(inflater, container, false)
        camera.setViewBinding(binding = binding)
        binding.viewFinder.viewTreeObserver.addOnGlobalLayoutListener(treeListener)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        similarityClassifier = requireActivity() as SimilarityClassifier
        binding.ivTakeImage.setOnClickListener {
            val croppedBitmap = camera.captureImage(rect = binding.facebox.getRect()!!)
                ?: return@setOnClickListener
            val rgbFrameBitmap = binding.viewFinder.bitmap ?: return@setOnClickListener
            val portraitBitmap = rotateBitmap(croppedBitmap)
            if (requireArguments().getBoolean(SCAN_FACE,false)) {
                val result = similarityClassifier.recognizeImageFaceNet2(croppedBitmap)
                val person = result?.first
                Log.d("asdf123","Result : Name: ${result?.first?.name}|| Dist ${result?.first?.distance}")
                if(person!=null && person.distance<1){
                    Toast.makeText(requireContext(),"Name: ${person.name} || Similarity:${person.distance}",Toast.LENGTH_SHORT).show()
                }
            }else{
                savePic(croppedBitmap)
            }
        }
    }

    private val treeListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            camera.startCamera(
                size = Size(binding.viewFinder.width, binding.viewFinder.height),
            ) { rect ->
                try{
                    binding.facebox.setRect(rect = rect)

                }catch (_:java.lang.NullPointerException){

                }
            }
            binding.viewFinder.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    }

    private fun savePic(bitmap: Bitmap){
        if (binding.facebox.getRect() != null) {
            CroppedFaceDialogFragment(
                bitmap = bitmap,
                inputNameCallback = {
                    similarityClassifier.register(
                        People(
                            name = it,
                            img = bitmap
                        )
                    )
                }
            ).show(this.parentFragmentManager, CroppedFaceDialogFragment::class.simpleName)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        camera.stopCamera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        camera.unbind()
        _binding = null
    }

    private fun rotateBitmap(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        val rotation = binding.facebox.rotation
        println("Test: $rotation")
        matrix.postRotate(90f)

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 112,112, true)

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
