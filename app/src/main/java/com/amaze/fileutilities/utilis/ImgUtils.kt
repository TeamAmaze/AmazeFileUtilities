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

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.pow

class ImgUtils {

    companion object {

        var log: Logger = LoggerFactory.getLogger(ImgUtils::class.java)

//        private var tessBaseApi: TessBaseAPI? = null
        private val wordRegex = "^[A-Za-z]*$".toRegex()

        fun convertMatToBitmap(input: Mat): Bitmap? {
            var bmp: Bitmap? = null
            val rgb = Mat()
            try {
                Imgproc.cvtColor(input, rgb, Imgproc.COLOR_BGR2RGB)
                bmp = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(rgb, bmp)
            } catch (e: Exception) {
                log.warn("failed to convert mat to bitmap", e)
            }
            return bmp
        }

        private fun readImage(path: String): Mat? {
            if (!path.doesFileExist()) {
                log.warn("failed to read matrix from path as file not found")
                return null
            }
            val mat = Imgcodecs.imread(path)
            if (mat.empty() || mat.height() == 0 || mat.width() == 0) {
                log.warn("failed to read matrix from path as image parameters empty")
                return null
            }
            return mat
        }

        fun convertBitmapToMat(input: Bitmap): Mat? {
            val mat = Mat()
            val bmp32: Bitmap = input.copy(Bitmap.Config.ARGB_8888, true)
            try {
                Utils.bitmapToMat(bmp32, mat)
            } catch (e: Exception) {
                log.warn("failed to convert bitmap to mat", e)
                return null
            }
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
            textRecognizer: TextRecognizer,
            path: String,
            callback: (isMeme: Boolean) -> Unit
        ) {
            TimeUnit.SECONDS.sleep(1L)
            extractTextFromImg(textRecognizer, path) { isSuccess, extractedText ->
                if (isSuccess) {
                    extractedText?.run {
                        for (block in textBlocks) {
                            for (line in block.lines) {
                                for (element in line.elements) {
                                    val elementText = element.text
                                    if (elementText.matches(wordRegex) &&
                                        elementText.length > 10 &&
                                        !elementText.contains("shot on", true)
                                    ) {
                                        callback.invoke(true)
                                        return@extractTextFromImg
                                    }
                                }
                            }
                        }
                    }
                    callback.invoke(false)
                } else {
                    callback.invoke(false)
                }
            }
        }

        fun getImageFeatures(
            faceDetector: FaceDetector,
            path: String,
            callback: ((isSuccess: Boolean, imageFeatures: ImageFeatures?) -> Unit)
        ) {
//            val image = InputImage.fromBitmap(bitmap, 0)
            try {
                TimeUnit.SECONDS.sleep(1L)
//                val image = InputImage.fromFilePath(context, uri)
                val mat = readImage(path)
                if (mat == null) {
                    log.warn("failure to find image analysis")
                    callback.invoke(false, null)
                    return
                }
                val resizeimage = resize(mat, getGenericWidth(mat), getGenericHeight(mat))
                val bitmap = convertMatToBitmap(resizeimage)
                if (bitmap != null) {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    faceDetector.process(image)
                        .addOnSuccessListener { faces ->
                            // Task completed successfully
                            // ...
                            var isSad = false
                            var isDistracted = false
                            var leftEyeOpen = false
                            var rightEyeOpen = false
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
                                        leftEyeOpen = true
                                    }
                                }
                                face.rightEyeOpenProbability?.let {
                                    if (it < 0.7) {
                                        rightEyeOpen = true
                                    }
                                }
                            }
                            mat.release()
                            resizeimage.release()
                            callback.invoke(
                                true,
                                ImageFeatures(
                                    isSad, leftEyeOpen && rightEyeOpen,
                                    isDistracted, faces.count()
                                )
                            )
                        }
                        .addOnFailureListener { e ->
                            // Task failed with an exception
                            // ...
                            log.warn("get image features failure", e)
                            mat.release()
                            resizeimage.release()
                            callback.invoke(false, null)
                        }
                } else {
                    log.warn("failed to find features of empty bitmap")
                    mat.release()
                    resizeimage.release()
                    callback.invoke(false, null)
                }
            } catch (e: Exception) {
                log.warn("Failed to check for image features due to exception", e)
                callback.invoke(false, null)
            } catch (oom: OutOfMemoryError) {
                log.warn("Failed to check for image features due to oom", oom)
                callback.invoke(false, null)
            }
        }

        fun resizeImage(bitmap: Bitmap): Bitmap? {
            val mat = convertBitmapToMat(bitmap)
            if (mat == null) {
                log.warn("failure to find image for resize")
                return null
            }
            val resizeimage = resize(mat, getGenericWidth(mat), getGenericHeight(mat))
            return convertMatToBitmap(resizeimage)
        }

        private fun extractTextFromImg(
            textRecognizer: TextRecognizer,
            path: String,
            callback: ((isSuccess: Boolean, extractedText: Text?) -> Unit)?
        ) {
            try {
//                val image = InputImage.fromFilePath(context, uri)
                val mat = readImage(path)
                if (mat == null) {
                    log.warn("Failure to extract text from input image")
                    callback?.invoke(false, null)
                    return
                }
                val resizeimage = resize(mat, getGenericWidth(mat), getGenericHeight(mat))
                val bitmap = convertMatToBitmap(resizeimage)
                if (bitmap != null) {
                    if (bitmap.width < 32 || bitmap.height < 32) {
                        log.info("skip extract text due to small image size")
                        callback?.invoke(true, null)
                        return
                    }
                    val result = textRecognizer.process(bitmap, 0)
                        .addOnSuccessListener { visionText ->
                            // Task completed successfully
                            log.debug(visionText.text)
                            mat.release()
                            resizeimage.release()
                            callback?.invoke(true, visionText)
                        }
                        .addOnFailureListener { e ->
                            // Task failed with an exception
                            // ...
                            log.warn("extract text from img failure", e)
                            mat.release()
                            resizeimage.release()
                            callback?.invoke(false, null)
                        }
                } else {
                    log.warn("Failed to extract text from empty bitmap")
                    callback?.invoke(false, null)
                }
            } catch (e: IOException) {
                log.warn("extract text from img ioexception", e)
                callback?.invoke(true, null)
                return
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

        fun isImageBlur(
            path: String
        ): Boolean? {
            return try {
                val matrix = readImage(path)
                if (matrix == null) {
                    log.warn("failure to find blur for input")
                    return false
                }
                val factor = laplace(matrix)
                if (factor == Double.MAX_VALUE) {
                    return null
                }
                if (factor < 50 && factor != 0.0) {
                    return true
                }
                return false
            } catch (e: Exception) {
                log.warn("Failed to check for blurry image", e)
                null
            } catch (oom: OutOfMemoryError) {
                log.warn("Failed to check for low light image", oom)
                null
            }
        }

        fun isImageLowLight(
            path: String
        ): Boolean? {
            return try {
                val matrix = readImage(path)
                if (matrix == null) {
                    log.warn("failure to find low light for input")
                    return false
                }
                processForLowLight(matrix)
            } catch (e: Exception) {
                log.warn("Failed to check for low light image", e)
                null
            } catch (oom: OutOfMemoryError) {
                log.warn("Failed to check for low light image", oom)
                null
            }
        }

        private fun processForLowLight(matrix: Mat): Boolean? {
            return try {
                val zerosPair = getTotalAndZeros(matrix)
                val ratio = (zerosPair.second.toDouble() / zerosPair.first.toDouble())
                matrix.release()
                return ratio >= 0.8
            } catch (e: Exception) {
                log.warn("Failed to check for low light image", e)
                null
            }
        }

        fun processPdfImg(matrix: Mat): Mat? {
            val resizeimage = resize(matrix, 4961.0, 7016.0)
            resizeimage?.let {
                val matGray = gray(resizeimage)
                val sharpen = sharpenBitmap(matGray)
                return sharpen
            }
            return null
        }

        private fun laplace(image: Mat): Double {
            val destination = Mat()
            val matGray = Mat()
            return try {
                val resizeimage = resize(image, getGenericWidth(image), getGenericHeight(image))
                if (resizeimage != null) {
                    Imgproc.cvtColor(resizeimage, matGray, Imgproc.COLOR_BGR2GRAY)
                    Imgproc.Laplacian(matGray, destination, 3)
                    val median = MatOfDouble()
                    val std = MatOfDouble()
                    Core.meanStdDev(destination, median, std)

                    resizeimage.release()
                    matGray.release()
                    destination.release()
                    image.release()

                    std[0, 0][0].pow(2.0)
                } else {
                    log.warn("Failed to check for blurry image due to empty image")
                    Double.MAX_VALUE
                }
            } catch (e: Exception) {
                log.warn("Failed to check for blurry image", e)
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

        private fun getTotalAndZeros(matrix: Mat): Pair<Int, Int> {
            try {
                val resizeimage = resize(
                    matrix, getGenericWidth(matrix),
                    getGenericHeight(matrix)
                )
                val matGray = gray(resizeimage)
                val threshold = thresholdInvert(matGray, 100.0)
                val nonZeros = Core.countNonZero(threshold)
                val total = resizeimage.width() * resizeimage.height()

                resizeimage.release()
                matGray.release()
                threshold.release()
                matrix.release()

                return Pair(total, total - nonZeros)
            } catch (e: Exception) {
                log.warn("cannot get zeros and total count from img", e)
            }
            return Pair(1, 0)
        }

        private fun thresholdInvert(matrix: Mat, intensity: Double): Mat {
            val blur = blur(matrix)
            val threshold = Mat()
            Imgproc.threshold(
                blur, threshold, intensity, 255.0,
                Imgproc.THRESH_BINARY
            )
            return threshold
        }

        fun adaptiveThresholdInvert(matrix: Mat): Mat {
            val resizeimage = resize(matrix, 620.0, 480.0)
            val matGray = gray(resizeimage)
            val blur = blur(matGray)
            val threshold = Mat()
            Imgproc.adaptiveThreshold(
                blur, threshold, 255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 3, 2.0
            )
            return threshold
        }

        private fun getGenericWidth(matrix: Mat): Double {
            return if (matrix.width() > matrix.height()) 620.0 else 480.0
        }

        private fun getGenericHeight(matrix: Mat): Double {
            return if (matrix.height() > matrix.width()) 620.0 else 480.0
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

        private fun blur(matrix: Mat): Mat {
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
