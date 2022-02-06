/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.utilis

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.amaze.fileutilities.home_page.database.PathPreferences
import com.amaze.fileutilities.utilis.Utils.Companion.containsInPreferences
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.lang.Exception
import java.util.concurrent.TimeUnit
import kotlin.math.pow

class ImgUtils {

    companion object {
//        private var tessBaseApi: TessBaseAPI? = null
        private val wordRegex = "^[A-Za-z]*$".toRegex()

        fun convertMatToBitmap(input: Mat): Bitmap? {
            var bmp: Bitmap? = null
            val rgb = Mat()
            Imgproc.cvtColor(input, rgb, Imgproc.COLOR_BGR2RGB)
            try {
                bmp = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(rgb, bmp)
            } catch (e: CvException) {
                e.printStackTrace()
                Log.e(javaClass.simpleName, e.message!!)
            }
            return bmp
        }

        fun convertBitmapToMat(input: Bitmap): Mat {
            val mat = Mat()
            val bmp32: Bitmap = input.copy(Bitmap.Config.ARGB_8888, true)
            Utils.bitmapToMat(bmp32, mat)
            return mat
        }

        /*fun getTessInstance(bitmap: Bitmap, externalDirPath: String): TessBaseAPI? {
            if (tessBaseApi == null) {
                tessBaseApi = TessBaseAPI()
                try {
                    tessBaseApi!!.init(externalDirPath, "eng")
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                    return null
                }
            }
            tessBaseApi?.clear()
            tessBaseApi?.setImage(bitmap)
            return tessBaseApi
        }*/

        fun isImageMeme(
            context: Context,
            textRecognizer: TextRecognizer,
            uri: Uri,
            pathPreferences: List<PathPreferences>,
            memesProcessed: Int,
            callback: (isMeme: Boolean) -> Unit
        ) {
            TimeUnit.SECONDS.sleep(1L)
            if (!containsInPreferences(
                    uri.path!!,
                    pathPreferences, true
                ) || memesProcessed > 10000
            ) {
                callback.invoke(false)
                return
            }
            extractTextFromImg(context, textRecognizer, uri) { isSuccess, extractedText ->
                if (isSuccess) {
                    extractedText?.run {
                        for (block in textBlocks) {
                            for (line in block.lines) {
                                for (element in line.elements) {
                                    val elementText = element.text
                                    if (elementText.matches(wordRegex) && elementText.length > 4) {
                                        callback.invoke(true)
                                        return@extractTextFromImg
                                    }
                                }
                            }
                        }
                        callback.invoke(false)
                    }
                } else {
                    callback.invoke(false)
                }
            }
        }

        fun getImageFeatures(
            context: Context,
            faceDetector: FaceDetector,
            uri: Uri,
            featuresProcessed: Int,
            pathPreferences: List<PathPreferences>,
            callback: ((isSuccess: Boolean, imageFeatures: ImageFeatures?) -> Unit)
        ) {
            TimeUnit.SECONDS.sleep(1L)
            if (!containsInPreferences(uri.path!!, pathPreferences, true) ||
                featuresProcessed > 10000
            ) {
                callback.invoke(false, null)
                return
            }
//            val image = InputImage.fromBitmap(bitmap, 0)
            val image = InputImage.fromFilePath(context, uri)
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    // Task completed successfully
                    // ...
                    var isSad = false
                    var isDistracted = false
                    var isSleeping = false
                    faces.forEach {
                        face ->
                        face.smilingProbability?.let {
                            if (it < 0.7) {
                                isSad = true
                            }
                        }
                        if (face.headEulerAngleX > 36 || face.headEulerAngleX < -36 ||
                            face.headEulerAngleY > 36 || face.headEulerAngleY < -36 ||
                            face.headEulerAngleZ > 36 || face.headEulerAngleZ < -36
                        ) {
                            isDistracted = true
                        }
                        face.leftEyeOpenProbability?.let {
                            if (it < 0.7) {
                                isSleeping = true
                            }
                        }
                        face.rightEyeOpenProbability?.let {
                            if (it < 0.7) {
                                isSleeping = true
                            }
                        }
                    }
                    callback.invoke(
                        true,
                        ImageFeatures(isSad, isSleeping, isDistracted, faces.count())
                    )
                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    // ...
                    e.printStackTrace()
                    callback.invoke(false, null)
                }
        }

        private fun extractTextFromImg(
            context: Context,
            textRecognizer: TextRecognizer,
            uri: Uri,
            callback: ((isSuccess: Boolean, extractedText: Text?) -> Unit)?
        ) {
            val image = InputImage.fromFilePath(context, uri)
            val result = textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Task completed successfully
                    Log.d(javaClass.simpleName, visionText.text)
                    callback?.invoke(true, visionText)
                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    // ...
                    e.printStackTrace()
                    callback?.invoke(false, null)
                }
        }

        /*fun isImageMeme(path: String, externalDirPath: String): Boolean {
            try {
                val matrix = Imgcodecs.imread(path)
                val tessBaseAPI = getTessInstance(
                    convertMatToBitmap(processPdfImgAlt(matrix))!!,
                    externalDirPath
                )
                tessBaseAPI?.run {
                    val extractedText: String? = tessBaseAPI.getUTF8Text()
                    extractedText?.let {
                        for (sentence in extractedText.split("\n")) {
                            val words = sentence.split(" ")
                            for (word in words) {
                                if (word.matches(wordRegex) && word.length > 4) {
                                    return true
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
            return false
        }*/

        /*fun extractText(path: String, externalDirPath: String): String {
            val matrix = Imgcodecs.imread(path)
            val tessBaseAPI = getTessInstance(
                convertMatToBitmap(processPdfImgAlt(matrix))!!,
                externalDirPath
            )
            tessBaseAPI?.run {
                val extractedText: String? = tessBaseAPI.getUTF8Text()
                return extractedText!!
            }
            return ""
        }*/

        fun isImageBlur(path: String, pathPreferences: List<PathPreferences>): Boolean {
            if (!containsInPreferences(path, pathPreferences, true)) {
                return false
            }
            val matrix = Imgcodecs.imread(path)
            val factor = laplace(matrix)
            if (factor < 100 && factor != 0.0) {
                return true
            }
            return false
        }

        fun processPdfImg(matrix: Mat): Mat {
            val resizeimage = resize(matrix, 4961.0, 7016.0)
            val matGray = gray(resizeimage)
            val sharpen = sharpenBitmap(matGray)
            return sharpen
        }

        fun laplace(image: Mat): Double {
            val destination = Mat()
            val matGray = Mat()
            return try {
                Imgproc.cvtColor(image, matGray, Imgproc.COLOR_BGR2GRAY)
                Imgproc.Laplacian(matGray, destination, 3)
                val median = MatOfDouble()
                val std = MatOfDouble()
                Core.meanStdDev(destination, median, std)
                std[0, 0][0].pow(2.0)
            } catch (e: Exception) {
                e.printStackTrace()
                Double.MAX_VALUE
            }
        }

        fun processPdfContour(matrix: Mat): Mat {
            val canny = processCanny(matrix)
            val hierarchy = Mat()
            val contours: ArrayList<MatOfPoint> = ArrayList()
            Imgproc.findContours(
                canny, contours, hierarchy, Imgproc.RETR_TREE,
                Imgproc.CHAIN_APPROX_SIMPLE
            )
            /*val color = Scalar(0.0, 0.0, 255.0)
            Imgproc.drawContours(
                matrix, contours, -1, color, 2, Imgproc.LINE_8,
                hierarchy, 2, Point()
            )*/
            return matrix
        }

        private fun processPdfImgAlt(matrix: Mat): Mat {
//            val resizeimage = resize(matrix, 4961.0, 7016.0)
            val resizeimage = resize(matrix, 620.0, 480.0)
            val matGray = gray(resizeimage)
            val sharpen = sharpenBitmap(matGray)
            val threshold = Mat()
            Imgproc.threshold(
                sharpen, threshold, 250.0, 255.0,
                Imgproc.THRESH_BINARY
            )

//            val blur = blur(matGray)
//            val canny = processCanny(matGray)
//            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(9.0, 9.0))
//            val dilate = Mat()
//            Imgproc.dilate(adaptive, dilate, kernel, Point(-1.0, -1.0), 4)
            /*val hierarchy = Mat()
            val contours: ArrayList<MatOfPoint> = ArrayList()
            Imgproc.findContours(canny, contours, hierarchy, Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE)
            for (c in contours) {
                val area = Imgproc.contourArea(c)
                val rect = Imgproc.boundingRect(c)

                if (rect.y >= 600 && rect.x <= 1000) {
                    if (area > 10000) {
                        Imgproc.rectangle(canny, Point(rect.x.toDouble(), rect.y.toDouble()),
                            Point(2200.0, (rect.y+rect.height).toDouble()), Scalar(0.0,
                                0.0, 255.0)
                        )
//                        line_items_coordinates.append([(x,y), (2200, y+h)])
                    }
                }
                if (rect.y >= 2400 && rect.x<= 2000) {
                    Imgproc.rectangle(canny, Point(rect.x.toDouble(), rect.y.toDouble()),
                        Point(2200.0, (rect.y+rect.height).toDouble()), Scalar(0.0,
                            0.0, 255.0))
//                    line_items_coordinates.append([(x,y), (2200, y+h)])
                }
            }*/
            return threshold
        }

        fun adaptiveThreshold(matrix: Mat): Mat {
            val adaptive = Mat()
            Imgproc.adaptiveThreshold(
                matrix, adaptive, 255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, 2.0
            )
            return adaptive
        }

        private fun processCanny(matrix: Mat): Mat {
            val edges = Mat()
//            val resize = resize(matrix, 620.0, 480.0)
//            val gray = gray(resize)
//            val blur = blur(gray)
            Imgproc.Canny(matrix, edges, 50.0, 300.0)
            return edges
        }

        fun processContour(matrix: Mat): Mat {
            val canny = processCanny(matrix)
            val hierarchy = Mat()
            val contours: ArrayList<MatOfPoint> = ArrayList()
            Imgproc.findContours(
                canny, contours, hierarchy, Imgproc.RETR_TREE,
                Imgproc.CHAIN_APPROX_SIMPLE
            )
            val color = Scalar(0.0, 0.0, 255.0)
            Imgproc.drawContours(
                matrix, contours, -1, color, 2, Imgproc.LINE_8,
                hierarchy, 2, Point()
            )
            return matrix
        }

        fun bilateralFilter(matrix: Mat): Mat {
            val result = Mat()
            Imgproc.bilateralFilter(matrix, result, 13, 15.0, 15.0)
            return result
        }

        fun blur(matrix: Mat): Mat {
            val result = Mat()
            Imgproc.blur(matrix, result, Size(9.0, 9.0))
            return result
        }

        private fun sharpenBitmap(matrix: Mat): Mat {
            val destination = Mat(matrix.rows(), matrix.cols(), matrix.type())
            Imgproc.GaussianBlur(matrix, destination, Size(0.0, 0.0), 10.0)
            Core.addWeighted(matrix, 1.5, destination, -0.5, 0.0, destination)
            return destination
        }

        private fun resize(matrix: Mat, width: Double, height: Double): Mat {
            val resizeMat = Mat()
            val sz = Size(width, height)
            Imgproc.resize(
                matrix, resizeMat, sz, 0.0, 0.0,
                if (matrix.cols() > width || matrix.rows() > height) Imgproc.INTER_AREA
                else Imgproc.INTER_CUBIC
            )
            return resizeMat
        }

        private fun gray(matrix: Mat): Mat {
            val matGray = Mat()
            Imgproc.cvtColor(matrix, matGray, Imgproc.COLOR_BGR2GRAY)
            return matGray
        }
    }

    data class ImageFeatures(
        val isSad: Boolean = false,
        val isSleeping: Boolean = false,
        val isDistracted: Boolean = false,
        val facesCount: Int = 0
    ) {
        fun featureDetected(): Boolean {
            return isSleeping || isSad || isDistracted || facesCount > 0
        }
    }
}
