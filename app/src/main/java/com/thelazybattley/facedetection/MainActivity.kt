package com.thelazybattley.facedetection

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import android.view.View
import androidx.activity.ComponentActivity
import com.thelazybattley.facedetection.databinding.ActivityMainBinding
import com.thelazybattley.facedetection.ui.xml.xml.FaceDetectionCameraImpl

class MainActivity : ComponentActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val camera = FaceDetectionCameraImpl(this)

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        camera.registerLifecycleOwner(this)
        camera.setViewBinding(binding = binding)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        var isCameraStarted = false
        binding.viewFinder.viewTreeObserver.addOnGlobalLayoutListener {
            if (binding.viewFinder.width > 0 && !isCameraStarted) {
                isCameraStarted = true
                camera.startCamera(
                    size = Size(
                        binding.viewFinder.width,
                        binding.viewFinder.height
                    ),
                    faceListener = { rect ->
                        val bitmap = binding.viewFinder.bitmap ?: return@startCamera
                        val croppedBitmap = Bitmap.createBitmap(
                            bitmap,
                            rect.left,
                            rect.top,
                            rect.right - rect.left,
                            rect.bottom - rect.top
                        )
                        binding.ivCroppedImage.setImageBitmap(croppedBitmap)
                        binding.ivCroppedImage.visibility = View.VISIBLE
                        binding.viewFinder.visibility = View.GONE
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }



}

