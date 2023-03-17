package com.thelazybattley.facedetection.classifier

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class TFLiteObjectDetectionAPIModel(
    private val modelName: String = MODEL_NAME,
    private val context: Context,
    private val inputSize: Int = INPUT_SIZE,
    private val isQuantized: Boolean = false,
    private val useGpu: Boolean = true,
    private val useXNNPack: Boolean = true
) : SimilarityClassifier {
    companion object {
        const val OUTPUT_SIZE = 192
        const val INPUT_SIZE = 112
        const val MODEL_NAME = "mobile_face_net.tflite"

        // Float model
        private const val IMAGE_MEAN = 128.0f
        private const val IMAGE_STD = 128.0f
    }

    private lateinit var tfLite: Interpreter
    private lateinit var imgTensorProcessor: ImageProcessor

    /**
     * Ref :https://learnopencv.com/face-recognition-an-introduction-for-beginners/
     * Encoded result from an image to kartesian diagram
     * */
    private lateinit var embeddings: Array<FloatArray>
    private lateinit var imgData: ByteBuffer
    private lateinit var intValues: IntArray
    private val registeredFaces: MutableList<Person> = mutableListOf()
    fun initialize() {
        val tfLiteModel = FileUtil.loadMappedFile(context, modelName)
        val tfOptions = Interpreter.Options().apply {
            CompatibilityList().apply {
                if (isDelegateSupportedOnThisDevice) {
                    addDelegate(GpuDelegate(bestOptionsForThisDevice))
                } else {
                    numThreads = 4
                }
            }
            setUseXNNPACK(useXNNPack)
            useNNAPI = true
        }
        tfLite = Interpreter(tfLiteModel, tfOptions)
        imgTensorProcessor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(StandardizeOp())
            .build()
        val numBytesPerChannel = if (isQuantized) 1 else 4
        imgData = ByteBuffer.allocate(1 * inputSize * inputSize * 3 * numBytesPerChannel)
        imgData.order(ByteOrder.nativeOrder())
        intValues = intArrayOf(inputSize * inputSize)
    }

    override fun register(person: Person) {
        person.img?.let { input ->
            val features = featureExtraction(input)
            val newPeople = person.copy(
                featureExtracted = features
            )
            registeredFaces.add(newPeople)
            Log.d("asdf123", "registered:${person.name}||${person.featureExtracted}")
            //TODO ADD TO LOCAL STORAGE
        }
    }

    override fun recognizeImageFaceNet2(bitmap: Bitmap): Pair<Person, Float>? {
        val input = featureExtraction(bitmap)
        val distance: Float
        return if (registeredFaces.size > 0) {
            val nearest = findNearest(input)
            if (nearest != null) {
                distance = nearest.second
                val newResult = nearest.first.copy(
                    distance = distance
                )
                Log.d("asdf123", "Nearest ${nearest.first.name} and distance $distance")
                Pair(newResult, nearest.second)
            } else {
                null
            }
        } else {
            null
        }
    }

    override fun featureExtraction(bitmap: Bitmap): Array<FloatArray> {
        return extractEmbedding(bitmap)
    }

    override fun getRegisteredPeople(): List<Person> {
        return registeredFaces
    }

    private fun extractEmbedding(bitmap: Bitmap): Array<FloatArray> {
        val input = convertBitmapToBuffer(bitmap)
        val faceNetModelOutput = Array(1) { FloatArray(OUTPUT_SIZE) }
        tfLite.run(input, faceNetModelOutput)
        return faceNetModelOutput
    }

    private fun convertBitmapToBuffer(image: Bitmap): ByteBuffer {
        return imgTensorProcessor.process(TensorImage.fromBitmap(image)).buffer
    }

    private fun L2Norm(x1: FloatArray, x2: FloatArray): Float {
        var sum = 0.0f
        for (i in x1.indices) {
            val diff = x1[i] - x2[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }

    private fun cosineSimilarity(x1: FloatArray, x2: FloatArray): Float {
        val mag1 = sqrt(x1.map { it * it }.sum())
        val mag2 = sqrt(x2.map { it * it }.sum())
        val dot = x1.mapIndexed { i, xi -> xi * x2[i] }.sum()
        return dot / (mag1 * mag2)
    }

    private fun findNearest(embedded: Array<FloatArray>): Pair<Person, Float>? {
        var result: Pair<Person, Float>? = null
        registeredFaces.forEach { knownEmbedded ->
            val avgEmbedded =
                embedded.reduce { acc, arr -> FloatArray(arr.size) { i -> acc[i] + arr[i] } }
                    .map { it / embedded.size }.toFloatArray()
            val avgKnownEmbedded =
                knownEmbedded.featureExtracted.reduce { acc, arr -> FloatArray(arr.size) { i -> acc[i] + arr[i] } }
                    .map { it / knownEmbedded.featureExtracted.size }.toFloatArray()
            val distance = L2Norm(avgKnownEmbedded, avgEmbedded)
            if (result == null || distance < result!!.second) {
                result = Pair(knownEmbedded, distance)
            }
        }
        return result
    }

    class StandardizeOp : TensorOperator {

        override fun apply(p0: TensorBuffer?): TensorBuffer {
            val pixels = p0!!.floatArray
            val mean = pixels.average().toFloat()
            var std = sqrt(pixels.map { pi -> (pi - mean).pow(2) }.sum() / pixels.size.toFloat())
            std = max(std, 1f / sqrt(pixels.size.toFloat()))
            for (i in pixels.indices) {
                pixels[i] = (pixels[i] - mean) / std
            }
            val output = TensorBufferFloat.createFixedSize(p0.shape, DataType.FLOAT32)
            output.loadArray(pixels)
            return output
        }

    }
}
