package com.thelazybattley.facedetection.classifier

import android.graphics.Bitmap

interface SimilarityClassifier {

    fun register(person:Person)

    fun recognizeImageFaceNet2(bitmap: Bitmap):Pair<Person,Float>?
    fun extractFaceEmbedding(bitmap: Bitmap):Array<FloatArray>

    fun getRegisteredPeople():List<Person>
}
