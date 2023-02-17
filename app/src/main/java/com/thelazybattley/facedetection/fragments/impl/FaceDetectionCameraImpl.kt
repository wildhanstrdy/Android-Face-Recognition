package com.thelazybattley.facedetection.fragments.impl

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.thelazybattley.facedetection.MainActivity
import com.thelazybattley.facedetection.databinding.PreviewViewBinding
import com.thelazybattley.facedetection.fragments.FaceDetectionCamera
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceDetectionCameraImpl(private val context: Context) : FaceDetectionCamera {

    private lateinit var cameraSelector: CameraSelector

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .enableTracking()
        .build()


    private val faceDetection = FaceDetection.getClient(options)

    companion object {

        private const val TAG = "CameraXApp"
    }

    private lateinit var binding: PreviewViewBinding

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    override fun startCamera(
        size: Size,
        faceListener: (Rect) -> Unit,
    ) {
        binding.ivTakeImage.setOnClickListener {
            captureImage()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(/* listener = */ {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { preview ->
                    preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setTargetRotation(binding.viewFinder.display.rotation)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(/* strategy = */ ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(/* resolution = */ size)
                .build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(
                        cameraExecutor,
                        PoseDetectorImageAnalyzer(faceListener = faceListener)
                    )
                }
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    /* lifecycleOwner = */
                    context as MainActivity, /* cameraSelector = */
                    cameraSelector, /* ...useCases = */
                    preview, imageCapture, imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e(TAG, "startCamera: $e")
            }

        }, /* executor = */ ContextCompat.getMainExecutor(context))
    }


    override fun setViewBinding(binding: PreviewViewBinding) {
        this.binding = binding
    }

    private inner class PoseDetectorImageAnalyzer(
        private val faceListener: (Rect) -> Unit,
    ) : ImageAnalysis.Analyzer {

        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image =
                    InputImage.fromMediaImage(/* image = */ mediaImage, /* rotationDegrees = */
                        imageProxy.imageInfo.rotationDegrees
                    )

                faceDetection.process(image).addOnSuccessListener { faces ->
                    for (face in faces) {
                        val faceContour = face.getContour(FaceContour.FACE)?.points ?: emptyList()
                        if (faceContour.size == 36) {
                            val width = image.width
                            val right = width - face.boundingBox.right
                            val left = right - face.boundingBox.right - face.boundingBox.left
                            val rect = Rect(
                                left, face.boundingBox.top, right, face.boundingBox.bottom
                            )
                            faceListener(
                                rect
                            )
                        }
                    }
                }.addOnFailureListener {
                    Log.d(TAG, "error: $it")
                }.addOnCompleteListener {
                    imageProxy.close()
                }
            }
        }
    }

    override fun stopCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()
        cameraExecutor.shutdown()
    }

    private fun captureImage() {

    }

    override fun flipCamera() {
        if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        } else if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }
}
