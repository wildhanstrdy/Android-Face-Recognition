package com.thelazybattley.facedetection

import android.R.attr.bitmap
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thelazybattley.facedetection.classifier.Person
import com.thelazybattley.facedetection.classifier.TFLiteObjectDetectionAPIModel
import com.thelazybattley.facedetection.fragments.impl.FaceDetectionCameraImpl
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val inputTestSet = mutableMapOf<String, Bitmap>()
        val forTest = mutableMapOf<String, Bitmap>()
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val assets = appContext.assets.list("testset")
        val faceDetectionCamera = FaceDetectionCameraImpl(appContext)
        val classifier = TFLiteObjectDetectionAPIModel(context = appContext)
        classifier.initialize()
        assets?.forEach {
            val inputStream: InputStream = appContext.assets.open("testset/$it")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (it.contains("O99")) {
                forTest[it.replace("O99\\..*".toRegex(), "")] = bitmap
            } else {
                inputTestSet[it.replace("I\\..*".toRegex(), "")] = bitmap
            }
        }

        inputTestSet.toSortedMap().forEach { input ->
            Log.d("ProcessInput ", input.key)
            //Detect Face
            faceDetectionCamera.detectFace(input.value) { croppedImgInput ->
                // Input Image
                classifier.register(
                    Person(
                        name = input.key,
                        img = croppedImgInput
                    )
                )
                saveImage(appContext, croppedImgInput, input.key, true)
            }
            Thread.sleep(3000)
        }

        var avg = 0f
        var highest = Float.MIN_VALUE
        var lowest = Float.MAX_VALUE
        var correct = forTest.size
        val wrongPrediction = mutableListOf<String>()
        forTest.toSortedMap().forEach { input ->
            Log.d("ProcessOutput ", input.key)
            //Detect Face
            faceDetectionCamera.detectFace(input.value) { croppedImgInput ->
                // Input Image
                val result = classifier.recognizeImageFaceNet2(croppedImgInput)
                val person = result?.first
                val distance = result?.second
                if (distance != null) {
                    avg += distance
                }
                if (input.key != person?.name) {
                    wrongPrediction.add(input.key)
                    correct--
                }
                if (distance!! > highest) {
                    highest = distance
                }
                if (distance < lowest) {
                    lowest = distance
                }
                saveImage(appContext, croppedImgInput, input.key)
                Log.d("ResultOPT ${person?.name}", "distance: $distance")
            }
            Thread.sleep(3000)
        }

        Log.d(
            "AVG Result ",
            "${avg / forTest.size} | correctPrediction: $correct from ${forTest.size} | wrongPrediction ${wrongPrediction.joinToString()} | hi:$highest | lo:$lowest"
        )
    }

    private fun saveImage(context: Context, img: Bitmap, name: String, isInput: Boolean = false) {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            if (isInput) "InputIMG" else "OutputIMG"
        )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val fileName = "$name.jpg"

        val file = File(directory, fileName)

        try {
            FileOutputStream(file).use { outputStream ->
                img.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val contentUri = Uri.fromFile(file)
        mediaScanIntent.data = contentUri
        context.sendBroadcast(mediaScanIntent)
    }
}
