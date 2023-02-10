package com.thelazybattley.facedetection.ui.xml.xml

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.thelazybattley.facedetection.MainActivity
import com.thelazybattley.facedetection.R
import com.thelazybattley.facedetection.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceDetectionCameraImpl(private val context: Context) : FaceDetectionCamera,
    LifecycleEventObserver {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
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

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val TAG = "CameraXApp"
    }

    private lateinit var binding: ActivityMainBinding

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null


    override fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        binding.videoCaptureButton.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) ==
                    PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        binding.videoCaptureButton.apply {
                            text = "Stop"
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            recording?.close()
                            recording = null
                        }
                        binding.videoCaptureButton.apply {
                            text = "Start"
                            isEnabled = true
                        }
                    }
                }
            }
    }

    override fun startCamera(
        size: Size,
        faceListener: (Rect) -> Unit,
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(/* listener = */ {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { preview ->
                    preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.LOWEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            imageCapture = ImageCapture.Builder().build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(/* strategy = */ ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(/* resolution = */ size)
                .build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(cameraExecutor, PoseDetectorImageAnalyzer(faceListener = faceListener,))
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
                val image =
                    InputImage.fromMediaImage(/* image = */ mediaImage, /* rotationDegrees = */
                        imageProxy.imageInfo.rotationDegrees
                    )
                faceDetection.process(image).addOnSuccessListener { faces ->
                    for (face in faces) {
                        faceListener(
                            face.boundingBox
                        )
                        return@addOnSuccessListener
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
                binding.videoCaptureButton.setOnClickListener { captureVideo() }
                cameraExecutor = Executors.newSingleThreadExecutor()
            }
            Lifecycle.Event.ON_DESTROY -> {
                cameraExecutor.shutdown()
            }
            else -> {
                // do nothing
            }
        }
    }

    override fun stopCamera() {
    }


}
