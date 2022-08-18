/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.audio_player

import android.content.Context
import android.net.Uri
import com.masoudss.lib.utils.uriToFile
import linc.com.amplituda.Amplituda
import linc.com.amplituda.AmplitudaProcessingOutput
import linc.com.amplituda.exceptions.AmplitudaException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal object CustomWaveformOptions {

    var log: Logger = LoggerFactory.getLogger(CustomWaveformOptions::class.java)

    @JvmStatic
    fun getSampleFrom(context: Context, pathOrUrl: String): IntArray? {
        val processingOutput: AmplitudaProcessingOutput<String>
        try {
            processingOutput = Amplituda(context).processAudio(pathOrUrl)
        } catch (e: Exception) {
            log.warn("failed to get processing output", e)
            return null
        }
        return handleAmplitudaOutput(pathOrUrl, processingOutput)
    }

    @JvmStatic
    fun getSampleFrom(context: Context, resource: Int, onSuccess: (IntArray) -> Unit): IntArray {
        return handleAmplitudaOutput(resource.toString(), Amplituda(context).processAudio(resource))
    }

    @JvmStatic
    fun getSampleFrom(context: Context, uri: Uri, onSuccess: (IntArray) -> Unit): IntArray {
        return handleAmplitudaOutput(
            uri.toString(),
            Amplituda(context).processAudio(context.uriToFile(uri))
        )
    }

    private fun handleAmplitudaOutput(
        path: String,
        amplitudaOutput: AmplitudaProcessingOutput<*>
    ): IntArray {
        val result = amplitudaOutput.get { e: AmplitudaException ->
            log.warn("failed to get waveform data from amplituda for path {}", path, e)
        }
        return result.amplitudesAsList().toTypedArray().toIntArray()
    }
}
