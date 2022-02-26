/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.transfer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class TransferViewModel : ViewModel() {

    var currentDeviceAddress: String? = null

    fun initClientTransfer(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.Default) {
            currentDeviceAddress?.let {
                host ->
                val port: Int
                var len: Int
                val socket = Socket()
                val buf = ByteArray(1024)
                try {
                    /**
                     * Create a client socket with the host,
                     * port, and timeout information.
                     */
                    socket.bind(null)
                    socket.connect((InetSocketAddress(host, 8888)), 5000)

                    /**
                     * Create a byte stream from a JPEG file and pipe it to the output stream
                     * of the socket. This data is retrieved by the server device.
                     */
                    val outputStream = socket.getOutputStream()
                    while (inputStream.read(buf).also { len = it } != -1) {
                        outputStream.write(buf, 0, len)
                    }
                    outputStream.close()
                    inputStream.close()
                } catch (e: FileNotFoundException) {
                    // catch logic
                    e.printStackTrace()
                } catch (e: IOException) {
                    // catch logic
                    e.printStackTrace()
                } finally {
                    /**
                     * Clean up any open sockets when done
                     * transferring or if an exception occurred.
                     */
                    socket.takeIf { it.isConnected }?.apply {
                        close()
                    }
                }
            }
        }
    }

    fun initServerConnection(basePath: String) {
        viewModelScope.launch(Dispatchers.Default) {
            /**
             * Create a server socket.
             */
            val serverSocket = ServerSocket(8888)
            /**
             * Wait for client connections. This call blocks until a
             * connection is accepted from a client.
             */
            val client = serverSocket.accept()
            /**
             * If this code is reached, a client has connected and transferred data
             * Save the input stream from the client as a JPEG file
             */
            val f = File("$basePath/AmazeUtils/wifip2pshared-${System.currentTimeMillis()}.jpg")
            val dirs = File(f.parent)

            dirs.takeIf { it.doesNotExist() }?.apply {
                mkdirs()
            }
            f.createNewFile()
            val inputstream = client.getInputStream()
            copyFile(inputstream, FileOutputStream(f))
            serverSocket.close()
            f.absolutePath
        }
    }

    private fun copyFile(inputStream: InputStream, out: OutputStream): Boolean {
        val buf = ByteArray(1024)
        var len: Int
        try {
            while (inputStream.read(buf).also { len = it } != -1) {
                out.write(buf, 0, len)
            }
            out.close()
            inputStream.close()
        } catch (e: IOException) {
            Log.d(javaClass.simpleName, e.toString())
            return false
        }
        return true
    }

    private fun File.doesNotExist(): Boolean = !exists()
}
