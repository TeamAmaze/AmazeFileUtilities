/*
 * Copyright (C) 2021-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Utilities.
 *
 * Amaze File Utilities is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.fileutilities.utilis

import android.graphics.Bitmap
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
import kotlin.math.pow

class ImgUtils {

    companion object {

        var log: Logger = LoggerFactory.getLogger(ImgUtils::class.java)

//        private var tessBaseApi: TessBaseAPI? = null
        val wordRegex = "^[A-Za-z]*$".toRegex()

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

        fun readImage(path: String): Mat? {
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

        fun resizeImage(bitmap: Bitmap): Bitmap? {
            val mat = convertBitmapToMat(bitmap)
            if (mat == null) {
                log.warn("failure to find image for resize")
                return null
            }
            val resizeimage = resize(mat, getGenericWidth(mat), getGenericHeight(mat))
            return convertMatToBitmap(resizeimage)
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

        fun getGenericWidth(matrix: Mat): Double {
            return if (matrix.width() > matrix.height()) 620.0 else 480.0
        }

        fun getGenericHeight(matrix: Mat): Double {
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

        fun resize(matrix: Mat, width: Double, height: Double): Mat {
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
