package com.amaze.fileutilities.utilis

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import kotlin.math.pow


class ImgUtils {

    companion object {
        private var tessBaseApi: TessBaseAPI? = null

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


        fun getTessInstance(bitmap: Bitmap, externalDirPath: String): TessBaseAPI? {
            if (tessBaseApi == null) {
                tessBaseApi = TessBaseAPI()
                try {
                    tessBaseApi!!.init(externalDirPath, "eng")
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                    return null
                }
            }
            tessBaseApi?.setImage(bitmap)
            return tessBaseApi
        }

        fun isImageMeme(path: String, externalDirPath: String): Boolean {
            val matrix = Imgcodecs.imread(path)
            val tessBaseAPI = getTessInstance(convertMatToBitmap(processPdfImgAlt(matrix))!!,
                externalDirPath)
            tessBaseAPI?.run {
                val extractedText: String? = tessBaseAPI.getUTF8Text()
                extractedText?.let {
                    for (s in extractedText.split("\n")) {
                        if (s.trim().length > 10) {
                            return true
                        }
                    }
                }
                tessBaseAPI.end()
            }
            return false
        }

        fun isImageBlur(path: String): Boolean {
            val matrix = Imgcodecs.imread(path)
            val factor = laplace(matrix)
            if (factor < 100) {
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

            Imgproc.cvtColor(image, matGray, Imgproc.COLOR_BGR2GRAY)
            Imgproc.Laplacian(matGray, destination, 3)
            val median = MatOfDouble()
            val std = MatOfDouble()
            Core.meanStdDev(destination, median, std)
            return std[0, 0][0].pow(2.0)
        }

        fun processPdfContour(matrix: Mat): Mat {
            val canny = processCanny(matrix)
            val hierarchy = Mat()
            val contours: ArrayList<MatOfPoint> = ArrayList()
            Imgproc.findContours(canny, contours, hierarchy, Imgproc.RETR_TREE,
                Imgproc.CHAIN_APPROX_SIMPLE)
            /*val color = Scalar(0.0, 0.0, 255.0)
            Imgproc.drawContours(
                matrix, contours, -1, color, 2, Imgproc.LINE_8,
                hierarchy, 2, Point()
            )*/
            return matrix
        }

        fun processPdfImgAlt(matrix: Mat): Mat {
//            val resizeimage = resize(matrix, 4961.0, 7016.0)
            val resizeimage = resize(matrix, 620.0, 480.0)
            val matGray = gray(resizeimage)
            val sharpen = sharpenBitmap(matGray)
            val threshold = Mat()
            Imgproc.threshold(sharpen, threshold, 250.0, 255.0, Imgproc.THRESH_BINARY)

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
            Imgproc.adaptiveThreshold(matrix, adaptive, 255.0,
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
            Imgproc.findContours(canny, contours, hierarchy, Imgproc.RETR_TREE,
                Imgproc.CHAIN_APPROX_SIMPLE)
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
            Imgproc.resize(matrix, resizeMat, sz, 0.0, 0.0,
                if (matrix.cols() > width || matrix.rows() > height) Imgproc.INTER_AREA else Imgproc.INTER_CUBIC)
            return resizeMat
        }

        private fun gray(matrix: Mat): Mat {
            val matGray = Mat()
            Imgproc.cvtColor(matrix, matGray, Imgproc.COLOR_BGR2GRAY)
            return matGray
        }
    }
}