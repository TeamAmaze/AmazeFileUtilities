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

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object MLUtils {

    private val highAccuracyOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()
    private val faceDetector = FaceDetection.getClient(highAccuracyOpts)
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var log: Logger = LoggerFactory.getLogger(MLUtils::class.java)

    fun processImageFeatures(path: String, successCallback: (ImgUtils.ImageFeatures) -> Unit) {
        getImageFeatures(
            path
        ) { isSuccess, imageFeatures ->
            if (isSuccess) {
                var features = ImgUtils.ImageFeatures()
                imageFeatures?.run {
                    features = this
                }
                successCallback.invoke(features)
            }
        }
    }

    fun isImageMeme(
        path: String,
        callback: (isMeme: Boolean) -> Unit
    ) {
        TimeUnit.SECONDS.sleep(1L)
        extractTextFromImg(path) { isSuccess, extractedText ->
            if (isSuccess) {
                extractedText?.run {
                    for (block in textBlocks) {
                        for (line in block.lines) {
                            for (element in line.elements) {
                                val elementText = element.text
                                if (elementText.matches(ImgUtils.wordRegex) &&
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

    private fun extractTextFromImg(
        path: String,
        callback: ((isSuccess: Boolean, extractedText: Text?) -> Unit)?
    ) {
        try {
//                val image = InputImage.fromFilePath(context, uri)
            val mat = ImgUtils.readImage(path)
            if (mat == null) {
                log.warn("Failure to extract text from input image")
                callback?.invoke(false, null)
                return
            }
            val resizeimage =
                ImgUtils.resize(mat, ImgUtils.getGenericWidth(mat), ImgUtils.getGenericHeight(mat))
            val bitmap = ImgUtils.convertMatToBitmap(resizeimage)
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

    private fun getImageFeatures(
        path: String,
        callback: ((isSuccess: Boolean, imageFeatures: ImgUtils.ImageFeatures?) -> Unit)
    ) {
//            val image = InputImage.fromBitmap(bitmap, 0)
        try {
            TimeUnit.SECONDS.sleep(1L)
//                val image = InputImage.fromFilePath(context, uri)
            val mat = ImgUtils.readImage(path)
            if (mat == null) {
                log.warn("failure to find image analysis")
                callback.invoke(false, null)
                return
            }
            val resizeimage =
                ImgUtils.resize(mat, ImgUtils.getGenericWidth(mat), ImgUtils.getGenericHeight(mat))
            val bitmap = ImgUtils.convertMatToBitmap(resizeimage)
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
                            ImgUtils.ImageFeatures(
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
}
