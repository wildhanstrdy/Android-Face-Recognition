package com.thelazybattley.facedetection.classifier

import android.graphics.Bitmap

interface SimilarityClassifier {

    fun register(people:People)

    fun recognizeImage(bitmap: Bitmap,storeExtra:Boolean):People?
    fun recognizeImageFaceNet2(bitmap: Bitmap):Pair<People,Float>?
    fun featureExtraction(bitmap: Bitmap):FloatArray

    fun getRegisteredPeople():List<People>
}
