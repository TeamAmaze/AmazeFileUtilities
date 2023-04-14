/*
 * Copyright (C) 2021-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.core.TermCriteria
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.PriorityQueue
import kotlin.math.pow
import kotlin.math.roundToInt

class ImgUtils {

    companion object {

        var log: Logger = LoggerFactory.getLogger(ImgUtils::class.java)

        const val DATAPOINTS = 7
        const val THRESHOLD = 100
        const val ASSERT_DATAPOINTS = 5
        const val PIXEL_POSITION_NORMALIZE_FACTOR = 10
        const val PIXEL_INTENSITY_NORMALIZE_FACTOR = 30

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

        fun imgChannels(
            path: String
        ): Boolean? {
            return try {
                val matrix = readImage(path)
                if (matrix == null) {
                    log.warn("failure to find low light for input")
                    return false
                }
                val resizeimage = resize(
                    matrix, getGenericWidth(matrix),
                    getGenericHeight(matrix)
                )
                /*Imgproc.cvtColor(input, rgb, Imgproc.COLOR_BGR2RGB)
                Imgproc.compareHist()
                for (i in 0 until resizeimage.height()) {
                    for (j in 0 until resizeimage.width()) {
                        val pixelVal = resizeimage.get(i, j)
                    }
                }*/
                /*val threshold = thresholdWithoutBlur(resizeimage, 120.0)
                val outputRGB = Mat()
                Imgproc.cvtColor(threshold, outputRGB, Imgproc.COLOR_BGR2RGB)
                val outputChannels = arrayListOf<Double>()
                for (k in 0 until outputRGB.channels()) {
                    outputChannels.add(0.0)
                }
                for (i in 0 until outputRGB.width()) {
                    for (j in 0 until outputRGB.height()) {
                        val pixelVal = outputRGB.get(i, j)
                        if (!pixelVal.all { _v -> _v == 255.0 || _v == 0.0 }) {
                            for (k in 0 until outputRGB.channels()) {
                                outputChannels[k] += pixelVal[k]
                            }
                        }
                    }
                }*/
                /*val orb: ORB = ORB.__fromPtr__(1)
                val bfMatcher = BFMatcher()
                bfMatcher
                orb.detectAndCompute()*/
                resizeimage.convertTo(resizeimage, CvType.CV_32F)
                val data: Mat = resizeimage.reshape(1, resizeimage.total().toInt())

                val K = 2
                val bestLabels = Mat()
                val criteria = TermCriteria()
                val attempts = 10
                val flags = Core.KMEANS_PP_CENTERS
                val centers = Mat()
                val compactness: Double =
                    Core.kmeans(data, K, bestLabels, criteria, attempts, flags, centers)
                var draw = Mat(resizeimage.total().toInt(), 1, CvType.CV_32FC3)
                val colors = centers.reshape(3, K)
                for (i in 0 until K) {
                    val mask = Mat() // a mask for each cluster label
                    Core.compare(bestLabels, Scalar(i.toDouble()), mask, Core.CMP_EQ)
                    val col =
                        colors.row(i) // can't use the Mat directly with setTo() (see #19100)
                    val d = col[0, 0] // can't create Scalar directly from get(), 3 vs 4 elements
                    draw.setTo(Scalar(d[0], d[1], d[2]), mask)
                }
                draw = draw.reshape(3, resizeimage.rows())
                draw.convertTo(draw, CvType.CV_8U)

                processForLowLight(matrix)
            } catch (e: Exception) {
                log.warn("Failed to check for low light image", e)
                null
            } catch (oom: OutOfMemoryError) {
                log.warn("Failed to check for low light image", oom)
                null
            }
        }

        fun getHistogram(inputPath: String, widthPx: Double, heightPx: Double): Bitmap? {
            return try {
                val matrix = readImage(inputPath)
                if (matrix == null) {
                    log.warn("failure to find input for histogram for path {}", inputPath)
                    return null
                }
                val histograms = processHistogram(matrix, heightPx)
                val histMatBitmap = Mat(
                    Size(256.0, heightPx), CvType.CV_8UC4,
                    Scalar(
                        0.0, 0.0,
                        0.0, 0.0
                    )
                )
                /*val colorsBgr =
                    arrayOf(Scalar(0.0, 0.0, 200.0, 255.0),
                        Scalar(0.0, 200.0, 0.0, 255.0),
                        Scalar(200.0, 0.0, 0.0, 255.0))*/
                histograms.forEachIndexed { index, mat ->
                    for (j in 0 until 256) {
                        /*val p1 = Point(
                            (binWidth * (j - 1)).toDouble(), (heightPx - Math.round(
                                mat.get(j - 1, 0)[0]
                            ))
                        )
                        val p2 =
                            Point(
                                (binWidth * j).toDouble(),
                                (heightPx - Math.round(mat.get(j, 0)[0]))
                            )
                        Imgproc.line(histMatBitmap, p1, p2, colorsBgr.get(index), 4, 16, 0)*/
                        val heightCalc = heightPx - Math.round(mat.get(j, 0)[0])
                        for (pt in heightPx.toInt() - 1 downTo heightCalc.toInt()) {
                            val existingChannel1 = histMatBitmap.get(pt, j)[0]
                            val existingChannel2 = histMatBitmap.get(pt, j)[1]
                            val existingChannel3 = histMatBitmap.get(pt, j)[2]
                            when (index) {
                                0 ->
                                    histMatBitmap.put(
                                        pt, j,
                                        existingChannel1, existingChannel2, 200.0, 0.0
                                    )
                                1 ->
                                    histMatBitmap.put(
                                        pt, j,
                                        existingChannel1, 200.0, existingChannel3, 0.0
                                    )
                                2 ->
                                    histMatBitmap.put(
                                        pt, j,
                                        200.0, existingChannel2, existingChannel3, 0.0
                                    )
                            }
                        }
                    }
                }
                val resultBitmap = Mat()
                Imgproc.resize(histMatBitmap, resultBitmap, Size(widthPx, heightPx))

                val resultBitmapFiltered = Mat(
                    resultBitmap.rows(), resultBitmap.cols(),
                    resultBitmap.type()
                )
                Imgproc.medianBlur(resultBitmap, resultBitmapFiltered, 19)

                val histBitmap = Bitmap.createBitmap(
                    resultBitmapFiltered.cols(),
                    resultBitmapFiltered.rows(),
                    Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(resultBitmapFiltered, histBitmap)
                resultBitmapFiltered.release()
                resultBitmap.release()
                histMatBitmap.release()
                histograms.forEach { it.release() }
                matrix.release()
                return histBitmap
            } catch (e: Exception) {
                log.warn("Failed to get histogram for {}", inputPath, e)
                null
            } catch (oom: OutOfMemoryError) {
                log.warn("Failed to get histogram for {}", inputPath, oom)
                null
            }
        }

        fun getHistogramChannelsWithPeaks(inputPath: String): List<List<Pair<Int, Int>>>? {
            return try {
                val matrix = readImage(inputPath)
                if (matrix == null) {
                    log.warn("failure to find input for histogram for path {}", inputPath)
                    return null
                }
                val histograms = processHistogram(
                    matrix,
                    THRESHOLD.toDouble()
                )

                val priorityQueueBlue = PriorityQueue<Pair<Int, Int>>(
                    DATAPOINTS
                ) { o1, o2 -> o1.second.compareTo(o2.second) }
                val priorityQueueGreen = PriorityQueue<Pair<Int, Int>>(
                    DATAPOINTS
                ) { o1, o2 -> o1.second.compareTo(o2.second) }
                val priorityQueueRed = PriorityQueue<Pair<Int, Int>>(
                    DATAPOINTS
                ) { o1, o2 -> o1.second.compareTo(o2.second) }

                histograms.forEachIndexed { index, mat ->
                    for (j in 0 until 256) {
                        val channelCurrentLevel = mat.get(j, 0)[0].roundToInt()
                        when (index) {
                            0 -> {
                                if (j > DATAPOINTS - 1) {
                                    priorityQueueBlue.remove()
                                }
                                priorityQueueBlue.add(Pair(j, channelCurrentLevel))
                            }
                            1 -> {
                                if (j > DATAPOINTS - 1) {
                                    priorityQueueGreen.remove()
                                }
                                priorityQueueGreen.add(Pair(j, channelCurrentLevel))
                            }
                            2 -> {
                                if (j > DATAPOINTS - 1) {
                                    priorityQueueRed.remove()
                                }
                                priorityQueueRed.add(Pair(j, channelCurrentLevel))
                            }
                        }
                    }
                }
                val blueTopValues: MutableList<Pair<Int, Int>> = mutableListOf()
                val greenTopValues: MutableList<Pair<Int, Int>> = mutableListOf()
                val redTopValues: MutableList<Pair<Int, Int>> = mutableListOf()
                while (!priorityQueueBlue.isEmpty()) {
                    priorityQueueBlue.remove()?.let {
                        blueTopValues.add(it)
                    }
                }
                while (!priorityQueueGreen.isEmpty()) {
                    priorityQueueGreen.remove()?.let {
                        greenTopValues.add(it)
                    }
                }
                while (!priorityQueueRed.isEmpty()) {
                    priorityQueueRed.remove()?.let {
                        redTopValues.add(it)
                    }
                }
                histograms.forEach { it.release() }
                matrix.release()
                return mutableListOf(blueTopValues, greenTopValues, redTopValues)
            } catch (e: Exception) {
                log.warn("Failed to process similar images histogram for {}", inputPath, e)
                null
            } catch (oom: OutOfMemoryError) {
                log.warn("Failed to process similar images histogram for {}", inputPath, oom)
                null
            }
        }

        private fun processHistogram(inputMat: Mat, heightPx: Double): List<Mat> {
            val bHist = Mat()
            val gHist = Mat()
            val rHist = Mat()
            val bgrPlane = ArrayList<Mat>(3)
            Core.split(inputMat, bgrPlane)
            Imgproc.calcHist(
                arrayListOf(bgrPlane[0]), MatOfInt(0), Mat(), bHist,
                MatOfInt(256), MatOfFloat(0f, 256f), false
            )
            Imgproc.calcHist(
                arrayListOf(bgrPlane[1]), MatOfInt(0), Mat(), gHist,
                MatOfInt(256), MatOfFloat(0f, 256f), false
            )
            Imgproc.calcHist(
                arrayListOf(bgrPlane[2]), MatOfInt(0), Mat(), rHist,
                MatOfInt(256), MatOfFloat(0f, 256f), false
            )
            Core.normalize(
                bHist, bHist, heightPx,
                0.0, Core.NORM_INF
            )
            Core.normalize(
                gHist, gHist, heightPx,
                0.0, Core.NORM_INF
            )
            Core.normalize(
                rHist, rHist, heightPx,
                0.0, Core.NORM_INF
            )
            return arrayListOf(bHist, gHist, rHist)
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

        fun getHistogramChecksum(
            blueChannelMap: Map<Int, Int>,
            greenChannelMap: Map<Int, Int>,
            redChannelMap: Map<Int, Int>,
            parentPath: String
        ): String {
            var blueChannelPosSum = 0
            var blueChannelIntensitySum = 0
            var greenChannelPosSum = 0
            var greenChannelIntensitySum = 0
            var redChannelPosSum = 0
            var redChannelIntensitySum = 0
            blueChannelMap.forEach {
                blueChannelPosSum += it.key
                blueChannelIntensitySum += it.value
            }
            greenChannelMap.forEach {
                greenChannelPosSum += it.key
                greenChannelIntensitySum += it.value
            }
            redChannelMap.forEach {
                redChannelPosSum += it.key
                redChannelIntensitySum += it.value
            }
            val checksumRaw = "$parentPath/$blueChannelPosSum:${blueChannelIntensitySum}_" +
                "$greenChannelPosSum:${greenChannelIntensitySum}_" +
                "$redChannelPosSum:$redChannelIntensitySum"
            return com.amaze.fileutilities.utilis.Utils.getMd5ForString(checksumRaw)
        }

        private fun thresholdWithoutBlur(matrix: Mat, intensity: Double): Mat {
            val threshold = Mat()
            Imgproc.threshold(
                matrix, threshold, intensity, 255.0,
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
