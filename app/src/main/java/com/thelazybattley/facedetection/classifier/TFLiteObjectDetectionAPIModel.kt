package com.thelazybattley.facedetection.classifier

import android.content.res.AssetManager
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.InterpreterApi
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class TFLiteObjectDetectionAPIModel(
    private val modelName:String,
    private val assetManager: AssetManager
) :SimilarityClassifier{
    companion object{
        const val OUTPUT_SIZE = 192
    }
    private lateinit var tfLite:Interpreter
    private lateinit var tfLiteModel:MappedByteBuffer


    private lateinit var embeddings: Array<FloatArray>
    private val registered:HashMap<String,Recognition> = hashMapOf()
    suspend fun initialize(){
         loadAssetFromLocal()?.let {
             tfLiteModel=it
             tfLite = Interpreter(tfLiteModel)

         }?.run {
             throw Exception("Failed to initialized model")
         }
    }
    private fun loadAssetFromLocal():MappedByteBuffer?{
        val assetDesc = assetManager.openFd(modelName)
        val inputStream = FileInputStream(assetDesc.fileDescriptor)
        val startOffset: Long = assetDesc.startOffset
        val declaredLength: Long = assetDesc.declaredLength
        return inputStream.channel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength)
    }
    override fun register(name: String, recognition: Recognition) {
        registered[name] = recognition
    }

    override fun recognizeImage(bitmap: Bitmap, storeExtra: Boolean) {

    }

    private fun findNearest(embedded:FloatArray):Pair<String,Float>?{
        var result:Pair<String,Float>? = null
        registered.forEach { (name, knownEmbedded) ->
            var distance =0f
            embedded.forEachIndexed { index, value ->
                val diff = value - knownEmbedded.extra[0][index]
                distance += diff*diff
            }
            distance = sqrt(distance.toDouble()).toFloat()
            if(result == null || distance < result!!.second){
                result = Pair(name,distance)
            }
        }
        return result
    }
}