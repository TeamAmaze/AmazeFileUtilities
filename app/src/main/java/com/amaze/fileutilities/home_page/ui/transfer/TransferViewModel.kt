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

import android.net.wifi.p2p.WifiP2pInfo
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import java.io.*
import java.net.InetAddress
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
    var isTransferInProgress = false
    var clientHandshakeSocket: Socket? = null
    var clientTransferSocket: Socket? = null
    var serverTransferSocket: ServerSocket? = null
    var serverHandshakeSocket: ServerSocket? = null

    private val handshakePort = 8989
    private val transferPort = 8989
    private val handshakeSalt = "@k%Sg4Gd9n"

    fun initHandshake(info: WifiP2pInfo): LiveData<Boolean>? {
        if (info.groupFormed && info.isGroupOwner) {
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a server thread and accepting
            // incoming connections.
            // receive handshake message
            return receiveHandshakeMessage()
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case,
            // you'll want to create a client thread that connects to the group
            // owner.
            // send handkshake message
            peerIP = groupOwnerIP
            return sendMessage(peerIP!!)
        } else {
            return null
        }
    }

    fun sendMessage(message: String): LiveData<Boolean> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            if (isTransferInProgress) {
                emit(false)
                return@liveData
            }
            try {
                clientHandshakeSocket?.also {
                    if (!it.isClosed) {
                        it.close()
                    }
                }
                clientHandshakeSocket = Socket()
                clientHandshakeSocket?.use {
                    socket ->
                    isTransferInProgress = true
                    socket.reuseAddress = true
                    socket.connect((InetSocketAddress(peerIP, handshakePort)), 5000)
                    socket.getOutputStream().use {
                        outputStream ->
                        ObjectOutputStream(outputStream).use {
                            objectOutputStream ->
                            Log.d(
                                javaClass.simpleName,
                                "Send message for filename to : " +
                                    "$peerIP"
                            )
                            objectOutputStream.writeObject(handshakeSalt + message)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emit(false)
                return@liveData
            } finally {
                isTransferInProgress = false
            }
            emit(true)
        }
    }

    fun receiveMessage(): LiveData<String?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(receiveMessageInternal()?.second)
        }
    }

    fun initClientTransfer(file: File): LiveData<Long> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            if (isTransferInProgress) {
                emit(-1)
                return@liveData
            }
            peerIP?.let {
                host ->
                var len: Int
                try {
                    clientTransferSocket?.also {
                        if (!it.isClosed) {
                            it.close()
                        }
                    }
                    clientTransferSocket = Socket()
                    clientTransferSocket?.use {
                        socket ->
                        isTransferInProgress = true
                        val buf = ByteArray(1024)
                        var progress = 0
                        val fileSize = file.length()
                        var currentTime = System.currentTimeMillis() / 1000
                        file.inputStream().use {
                            inputStream ->
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
                            socket.getOutputStream().use {
                                outputStream ->
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
                                emit(-1)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emit(-1)
                } finally {
                    isTransferInProgress = false
                }
            }
        }
    }

    fun initServerConnection(filePath: String, fileSize: Long): LiveData<Long> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            if (isTransferInProgress) {
                emit(-1)
                return@liveData
            }
            try {
                serverTransferSocket?.also {
                    if (!it.isClosed) {
                        it.close()
                    }
                }
                serverTransferSocket = ServerSocket(transferPort)
                serverTransferSocket?.use {
                    serverSocket ->
                    isTransferInProgress = true
                    /**
                     * Create a server socket.
                     */
                    /**
                     * Create a server socket.
                     */
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
                    client.getInputStream().use {
                        inputStream ->
                        FileOutputStream(f).use {
                            out ->
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
                            } catch (e: IOException) {
                                Log.w(javaClass.simpleName, e.toString())
                            }
                            emit(-1)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emit(-1)
            } finally {
                isTransferInProgress = false
            }
        }
    }

    private fun receiveHandshakeMessage(): LiveData<Boolean> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            val receivedIP = receiveMessageInternal()
            if (receivedIP == null) {
                emit(false)
            } else {
                peerIP = receivedIP.first.hostAddress
                emit(true)
            }
        }
    }

    /**
     * Returns pair to ip address and message
     */
    private fun receiveMessageInternal(): Pair<InetAddress, String>? {
        try {
            if (isTransferInProgress) {
                return null
            }
            serverHandshakeSocket?.also {
                if (!it.isClosed) {
                    it.close()
                }
            }
            serverHandshakeSocket = ServerSocket(handshakePort)
            serverHandshakeSocket?.use {
                serverSocket ->
                isTransferInProgress = true
                serverSocket.reuseAddress = true
                val client = serverSocket.accept()
                ObjectInputStream(client.getInputStream()).use {
                    objectInputStream ->
                    val incoming = objectInputStream.readObject()
                    if (incoming.javaClass == String::class.java) {
                        val incomingMessage = incoming as String
                        return if (incomingMessage.startsWith(handshakeSalt)) {
                            Log.d(
                                javaClass.simpleName,
                                "Incoming message $incomingMessage " +
                                    "from : ${client.inetAddress}"
                            )
                            Pair(
                                client.inetAddress,
                                incomingMessage
                                    .replace(handshakeSalt, "")
                            )
                        } else {
                            null
                        }
                    }
                }
                return null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isTransferInProgress = false
            return null
        } finally {
            isTransferInProgress = false
        }
        return null
    }

    private fun File.doesNotExist(): Boolean = !exists()
}
