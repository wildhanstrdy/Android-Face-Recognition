package com.thelazybattley.facedetection.fragments.impl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import com.google.mlkit.vision.common.InputImage
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
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .enableTracking()
        .build()


    private val faceDetection = FaceDetection.getClient(options)

    companion object {

        private const val TAG = "CameraXApp"
    }

    private val binding get() = _binding!!
    private var _binding: PreviewViewBinding? = null

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    override fun startCamera(
        size: Size,
        faceListener: (Rect) -> Unit,
    ) {
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
                        PoseDetectorImageAnalyzer(
                            faceListener = faceListener,
                            size = size,
                        )
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
        this._binding = binding
    }

    private inner class PoseDetectorImageAnalyzer(
        private val size: Size,
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
                    if (faces.isEmpty()) {
                        faceListener(Rect())
                    }
                    for (face in faces) {
                        val flipMatrix = Matrix().apply {
                            setScale(-1f, 1f)
                        }
                        val mirroredRect = Rect(face.boundingBox)
                        val rectF = RectF(mirroredRect)
                        flipMatrix.mapRect(rectF)
                        rectF.round(mirroredRect)
                        rectF.left = size.width + rectF.left
                        rectF.right = size.width + rectF.right

                        if (rectF.left < 0) {
                            rectF.left = 0f
                        }
                        if (rectF.right > size.width) {
                            rectF.right = size.width.toFloat()
                        }
                        if (rectF.bottom > size.height) {
                            rectF.bottom = size.height.toFloat()
                        }
                        if (rectF.top < 0) {
                            rectF.top = 0f
                        }

                        faceListener(
                            rectF.toRect()
                        )
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

    override fun captureImage(
        rect: Rect,
    ): Bitmap? {
        if(rect.isEmpty) {
            return null
        }
        val bitmap = binding.viewFinder.bitmap
        if (bitmap != null) {
            return Bitmap.createBitmap(
                bitmap,
                rect.left, rect.top, rect.width(), rect.height(),
            )
        }
        return null
    }

    override fun unbind() {
        _binding = null
    }

    override fun detectFace(image: Bitmap, successCrop: (bitmap: Bitmap) -> Unit) {
        faceDetection.process(InputImage.fromBitmap(image, 0)).addOnSuccessListener { faces ->
            for (face in faces) {
               val rect = face.boundingBox
                successCrop(
                    Bitmap.createBitmap(
                        image,
                        rect.left, rect.top, rect.width(), rect.height(),
                    )
                )
            }
        }.addOnFailureListener {
            Log.d(TAG, "error: $it")
        }.addOnCompleteListener {
            Log.d(TAG, "complete: $it")
        }
    }

    override fun flipCamera() {
        if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        } else if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }
}
