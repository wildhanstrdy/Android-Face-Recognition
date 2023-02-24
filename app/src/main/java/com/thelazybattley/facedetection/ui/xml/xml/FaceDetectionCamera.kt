package com.thelazybattley.facedetection.ui.xml.xml

import android.graphics.Rect
import android.util.Size
import androidx.lifecycle.LifecycleOwner
import com.thelazybattley.facedetection.databinding.PreviewViewBinding

interface FaceDetectionCamera {
    fun startCamera(size: Size, faceListener: (Rect) -> Unit)
    fun setViewBinding(binding: PreviewViewBinding)
    fun stopCamera()
}
