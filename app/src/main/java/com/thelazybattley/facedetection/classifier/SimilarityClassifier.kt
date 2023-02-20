package com.thelazybattley.facedetection.classifier

import android.graphics.Bitmap

interface SimilarityClassifier {

    fun register(name:String, recognition:Recognition)

    fun recognizeImage(bitmap: Bitmap,storeExtra:Boolean)
}