package com.thelazybattley.facedetection.fragments

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import com.thelazybattley.facedetection.databinding.PreviewViewBinding

interface FaceDetectionCamera {
    fun startCamera(size: Size, faceListener: (Rect) -> Unit)
    fun setViewBinding(binding: PreviewViewBinding)
    fun stopCamera()
    fun flipCamera()
    fun captureImage(rect: Rect): Bitmap?
}
