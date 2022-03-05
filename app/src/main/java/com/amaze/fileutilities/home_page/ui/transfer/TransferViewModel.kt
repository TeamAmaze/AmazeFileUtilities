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
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class TransferViewModel : ViewModel() {

    // group owner ip known to both devices
    var groupOwnerIP: String? = null
    // own ip
    var selfIP: String? = null
    // other device's ip, only in case self ip and group owner ip is different
    var peerIP: String? = null
    var isWifiP2PEnabled: Boolean = false

    val handshakePort = 8989
    val transferPort = 8787
    private val handshakeSalt = "@k%Sg4Gd9n"

    fun initHandshake(): LiveData<Boolean> {
        return if (!selfIP.equals(groupOwnerIP)) {
            // send handkshake message
            peerIP = groupOwnerIP
            sendMessage(selfIP!!)
        } else {
            // receive handshake message
            receiveHandshakeMessage()
        }
    }

    fun sendMessage(message: String): LiveData<Boolean> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            try {
                val socket = Socket()
                socket.reuseAddress = true
                socket.connect((InetSocketAddress(groupOwnerIP, handshakePort)), 5000)
                val outputStream = socket.getOutputStream()
                val objectOutputStream = ObjectOutputStream(outputStream)
                Log.d(javaClass.simpleName, "Send message for filename to : $groupOwnerIP")
                objectOutputStream.writeObject(handshakeSalt + message)
                objectOutputStream.close()
                outputStream.close()
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
                emit(false)
            }
            emit(true)
        }
    }

    fun receiveMessage(): LiveData<String?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(receiveMessageInternal())
        }
    }

    private fun receiveHandshakeMessage(): LiveData<Boolean> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            val receivedIP = receiveMessageInternal()
            if (receivedIP == null) {
                emit(false)
            } else {
                peerIP = receivedIP
                emit(true)
            }
        }
    }

    private fun receiveMessageInternal(): String? {
        try {
            val serverSocket = ServerSocket(handshakePort)
            serverSocket.reuseAddress = true
            val client = serverSocket.accept()
            val objectInputStream = ObjectInputStream(client.getInputStream())
            val incoming = objectInputStream.readObject()
            if (incoming.javaClass == String::class.java) {
                val incomingMessage = incoming as String
                if (incomingMessage.startsWith(handshakeSalt)) {
                    Log.d(javaClass.simpleName, "Incoming message from : " + client.inetAddress)
                    return incomingMessage.replace(handshakeSalt, "")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return null
    }

    fun initClientTransfer(file: File): LiveData<Long> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            peerIP?.let {
                host ->
                var len: Int
                val socket = Socket()
                val buf = ByteArray(1024)
                var progress = 0
                val fileSize = file.length()
                var currentTime = System.currentTimeMillis() / 1000
                val inputStream: InputStream = file.inputStream()
                try {
                    /**
                     * Create a client socket with the host,
                     * port, and timeout information.
                     */
                    socket.bind(null)
                    socket.connect((InetSocketAddress(host, transferPort)), 5000)

                    /**
                     * Create a byte stream from a file and pipe it to the output stream
                     * of the socket. This data is retrieved by the server device.
                     */
                    val outputStream = socket.getOutputStream()
                    Log.i(javaClass.simpleName, "Start sending file to host : $host")
                    while (inputStream.read(buf).also { len = it } != -1) {
                        outputStream.write(buf, 0, len)
                        progress += len
                        if (System.currentTimeMillis() / 1000 - currentTime > 3) {
                            currentTime = System.currentTimeMillis() / 1000
                            emit((progress * 100) / fileSize)
                        }
                    }
                    emit((progress * 100) / fileSize)
                    outputStream.close()
                    inputStream.close()
                    emit(-1)
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

    fun initServerConnection(filePath: String, fileSize: Long): LiveData<Long> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            /**
             * Create a server socket.
             */
            /**
             * Create a server socket.
             */
            val serverSocket = ServerSocket(transferPort)
            /**
             * Wait for client connections. This call blocks until a
             * connection is accepted from a client.
             */
            /**
             * Wait for client connections. This call blocks until a
             * connection is accepted from a client.
             */
            val client = serverSocket.accept()
            /**
             * If this code is reached, a client has connected and transferred data
             * Save the input stream from the client as a JPEG file
             */
            /**
             * If this code is reached, a client has connected and transferred data
             * Save the input stream from the client as a JPEG file
             */
            val f = File(filePath)
            val dirs = File(f.parent)

            dirs.takeIf { it.doesNotExist() }?.apply {
                mkdirs()
            }
            f.createNewFile()
            val inputStream = client.getInputStream()
            val out = FileOutputStream(f)

            val buf = ByteArray(1024)
            var len: Int
            var progress = 0
            var currentTime = System.currentTimeMillis() / 1000
            Log.i(javaClass.simpleName, "Start receiving file to host : $peerIP")
            try {
                while (inputStream.read(buf).also { len = it } != -1) {
                    out.write(buf, 0, len)
                    progress += len
                    if (System.currentTimeMillis() / 1000 - currentTime > 3) {
                        currentTime = System.currentTimeMillis() / 1000
                        emit((progress * 100) / fileSize)
                    }
                }
                emit((progress * 100) / fileSize)
                out.close()
                inputStream.close()
            } catch (e: IOException) {
                Log.w(javaClass.simpleName, e.toString())
            }
            serverSocket.close()
            emit(-1)
        }
    }

    private fun File.doesNotExist(): Boolean = !exists()
}
