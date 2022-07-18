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

internal object CustomWaveformOptions {

    @JvmStatic
    fun getSampleFrom(context: Context, pathOrUrl: String): IntArray {
        return handleAmplitudaOutput(Amplituda(context).processAudio(pathOrUrl))
    }

    @JvmStatic
    fun getSampleFrom(context: Context, resource: Int, onSuccess: (IntArray) -> Unit): IntArray {
        return handleAmplitudaOutput(Amplituda(context).processAudio(resource))
    }

    @JvmStatic
    fun getSampleFrom(context: Context, uri: Uri, onSuccess: (IntArray) -> Unit): IntArray {
        return handleAmplitudaOutput(Amplituda(context).processAudio(context.uriToFile(uri)))
    }

    private fun handleAmplitudaOutput(
        amplitudaOutput: AmplitudaProcessingOutput<*>
    ): IntArray {
        val result = amplitudaOutput.get { exception: AmplitudaException ->
            exception.printStackTrace()
        }
        return result.amplitudesAsList().toTypedArray().toIntArray()
    }
}
