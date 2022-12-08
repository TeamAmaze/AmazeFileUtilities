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
