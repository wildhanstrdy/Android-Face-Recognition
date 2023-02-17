package com.thelazybattley.facedetection.ui.xml.xml

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.View
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.thelazybattley.facedetection.MainActivity
import com.thelazybattley.facedetection.databinding.ActivityMainBinding
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceDetectionCameraImpl(private val context: Context) : FaceDetectionCamera,
    LifecycleEventObserver {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .enableTracking()
        .build()


    private val faceDetection = FaceDetection.getClient(options)

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

        private const val TAG = "CameraXApp"
    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    override fun startCamera(
        size: Size,
        faceListener: (Rect) -> Unit,
    ) {
        binding.retry.visibility = View.GONE
        binding.viewFinder.visibility = View.VISIBLE
        binding.ivCroppedImage.visibility = View.GONE
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(/* listener = */ {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { preview ->
                    preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
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
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

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

    override fun getPermissions() {
        ActivityCompat.requestPermissions(
            context as MainActivity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )
    }

    override fun setViewBinding(binding: ActivityMainBinding) {
        this.binding = binding
    }

    private inner class PoseDetectorImageAnalyzer(
        private val faceListener: (Rect) -> Unit,
    ) : ImageAnalysis.Analyzer {

        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                println("Test: ${imageProxy.imageInfo.rotationDegrees}")
                val image = InputImage.fromMediaImage(/* image = */ mediaImage, /* rotationDegrees = */imageProxy.imageInfo.rotationDegrees)

                faceDetection.process(image).addOnSuccessListener { faces ->
                    for (face in faces) {
                        val faceContour = face.getContour(FaceContour.FACE)?.points ?: emptyList()
                        if (faceContour.size == 36) {
                            faceListener(
                                face.boundingBox
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

    override fun isAllPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            /* context = */ context, /* permission = */ it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun registerLifecycleOwner(owner: LifecycleOwner) {
        owner.lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                if (!isAllPermissionGranted()) {
                    ActivityCompat.requestPermissions(
                        context as MainActivity,
                        REQUIRED_PERMISSIONS,
                        REQUEST_CODE_PERMISSIONS
                    )
                }
                cameraExecutor = Executors.newSingleThreadExecutor()
            }
            Lifecycle.Event.ON_DESTROY -> {
                stopCamera()
                cameraExecutor.shutdown()
            }
            else -> {
                // do nothing
            }
        }
    }

    override fun stopCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()
    }
}
