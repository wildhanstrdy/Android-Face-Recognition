package com.thelazybattley.facedetection.model

import android.graphics.Bitmap

data class FaceRecognitionData(
    val rgbFrameBitmap: Bitmap,
    val croppedBitmap: Bitmap,
    val portraitBitmap: Bitmap,
    val faceBitmap: Bitmap,
    val name: String,
)
