package com.thelazybattley.facedetection.ui.xml.xml

import android.graphics.Rect
import android.util.Size
import androidx.lifecycle.LifecycleOwner
import com.thelazybattley.facedetection.databinding.ActivityMainBinding

interface FaceDetectionCamera {
    fun captureVideo()
    fun startCamera(size: Size, faceListener: (Rect) -> Unit)
    fun getPermissions()
    fun setViewBinding(binding: ActivityMainBinding)
    fun isAllPermissionGranted(): Boolean
    fun registerLifecycleOwner(owner: LifecycleOwner)
    fun stopCamera()
}
