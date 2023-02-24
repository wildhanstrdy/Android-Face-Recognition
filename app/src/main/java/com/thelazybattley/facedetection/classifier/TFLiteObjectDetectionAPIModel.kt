package com.thelazybattley.facedetection.classifier

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class TFLiteObjectDetectionAPIModel(
    private val modelName:String,
    private val assetManager: AssetManager,
    private val inputSize:Int,
    private val isQuantized:Boolean
) :SimilarityClassifier{
    companion object{
        const val OUTPUT_SIZE = 192
        // Float model
        private const val IMAGE_MEAN = 128.0f
        private const val IMAGE_STD = 128.0f
    }
    private lateinit var tfLite:Interpreter
    private lateinit var tfLiteModel:MappedByteBuffer
    /**
     * Ref :https://learnopencv.com/face-recognition-an-introduction-for-beginners/
     * Encoded result from an image to kartesian diagram
    * */
    private lateinit var embeddings: Array<FloatArray>
    private lateinit var imgData:ByteBuffer
    private lateinit var intValues:IntArray
    private val registeredFaces:MutableList<Recognition> = mutableListOf()
    suspend fun initialize(){
         loadAssetFromLocal()?.let {
             tfLiteModel=it
             tfLite = Interpreter(tfLiteModel)
             val numBytesPerChannel = if(isQuantized) 1 else 4
             imgData = ByteBuffer.allocate(1 * inputSize * inputSize * 3 * numBytesPerChannel)
             imgData.order(ByteOrder.nativeOrder())
             intValues = intArrayOf(inputSize*inputSize)
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
    override fun register(recognition: Recognition) {
        registeredFaces.add(recognition)
        //TODO ADD TO LOCAL STORAGE
    }

    override fun recognizeImage(bitmap: Bitmap, storeExtra: Boolean):Recognition?{
        bitmap.getPixels(intValues,bitmap.width,bitmap.height,0,0,bitmap.width,bitmap.height)
        imgData.rewind()
        for (i in 0 until inputSize){
            for (j in 0 until inputSize){
                val pixelValue = intValues[i*inputSize*j]
                if (isQuantized){
                    // Quantized model
                    imgData.put((pixelValue shr 16 and 0xFF).toByte())
                    imgData.put((pixelValue shr 8 and 0xFF).toByte())
                    imgData.put((pixelValue and 0xFF).toByte())
                }else{
                    // Float model
                    imgData.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                }
            }
        }

        //Copy input to tensorflow
        val inputArray = arrayOf<Any>(imgData)

        // Here outputMap is changed to fit the Face Mask detector
        val outputMap: MutableMap<Int, Any> = mutableMapOf()

        embeddings = Array(1) {
            FloatArray(
                OUTPUT_SIZE
            )
        }
        outputMap[0] = embeddings
        tfLite.runForMultipleInputsOutputs(inputArray,outputMap)

        //null means there's no recognized faces
        val distance: Float
        return if(registeredFaces.size > 0){
            val nearest = findNearest(embeddings[0])
            if(nearest != null){
                distance = nearest.second
                Log.d(this.javaClass.name,"Nearest ${nearest.first} and distance $distance")
                nearest.first
            }else{
                null
            }
        }else{
            null
        }
    }

    private fun findNearest(embedded:FloatArray):Pair<Recognition,Float>?{
        var result:Pair<Recognition,Float>? = null
        registeredFaces.forEach {knownEmbedded ->
            var distance =0f
            embedded.forEachIndexed { index, value ->
                val diff = value - knownEmbedded.extra[0][index]
                distance += diff*diff
            }
            distance = sqrt(distance.toDouble()).toFloat()
            if(result == null || distance < result!!.second){
                result = Pair(knownEmbedded,distance)
            }
        }
        return result
    }
}
