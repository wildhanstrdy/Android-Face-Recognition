package com.thelazybattley.facedetection.classifier

import android.graphics.Bitmap
import java.util.*

data class People(
    val id:String = UUID.randomUUID().toString(),
    val name:String,
    val distance:Float = -1f,
    var featureExtracted:FloatArray = floatArrayOf(),
    var img:Bitmap?=null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as People

        if (id != other.id) return false
        if (name != other.name) return false
        if (distance != other.distance) return false
        if (!featureExtracted.contentEquals(other.featureExtracted)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + distance.hashCode()
        result = 31 * result + featureExtracted.contentHashCode()
        return result
    }
}
