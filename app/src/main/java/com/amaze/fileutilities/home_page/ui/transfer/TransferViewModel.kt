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
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.TimeUnit

class TransferViewModel : ViewModel() {

    var log: Logger = LoggerFactory.getLogger(TransferViewModel::class.java)

    // group owner ip known to both devices
    var groupOwnerIP: String? = null
    // own ip
    var selfIP: String? = null
    // other device's ip, only in case self ip and group owner ip is different
    var peerIP: String? = null
    var isConnectedToPeer: Boolean = false
    var isTransferInProgress = false
    var clientHandshakeSocket: Socket? = null
    var clientTransferSocket: Socket? = null
    var serverTransferSocket: ServerSocket? = null
    var serverHandshakeSocket: ServerSocket? = null
    // needed in case we're connected to a rogue wifi direct already and aren't peers changed action
    var performedRequestPeers: Boolean = false

    private val handshakePort = 8787
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
            if (isTransferInProgress && false) {
                emit(false)
            } else {
                try {
                    if (message == peerIP) {
                        // sleep for few seconds so that we're sure handshake receiver is available
                        isConnectedToPeer = true
                        TimeUnit.SECONDS.sleep(3L)
                    }
                    clientHandshakeSocket?.also {
                        if (!it.isClosed) {
                            it.close()
                        }
                    }
                    isTransferInProgress = true
                    clientHandshakeSocket = Socket()
                    clientHandshakeSocket?.use {
                        socket ->
                        socket.reuseAddress = true
                        socket.connect((InetSocketAddress(peerIP, handshakePort)), 5000)
                        socket.getOutputStream().use {
                            outputStream ->
                            ObjectOutputStream(outputStream).use {
                                objectOutputStream ->
                                log.debug(
                                    "Send message for filename to : " +
                                        "$peerIP"
                                )
                                objectOutputStream.writeObject(handshakeSalt + message)
                            }
                        }
                    }
                    emit(true)
                } catch (e: Exception) {
                    log.error("failed to send message", e)
                    emit(false)
                } finally {
                    isTransferInProgress = false
                }
            }
        }
    }

    fun receiveMessage(): LiveData<String?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(receiveMessageInternal()?.second)
        }
    }

    fun initClientTransfer(file: File): LiveData<String> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            if (isTransferInProgress && false) {
                emit("done")
            } else {
                peerIP?.let {
                    host ->
                    var len: Int
                    try {
                        clientTransferSocket?.also {
                            if (!it.isClosed) {
                                it.close()
                            }
                        }
                        isTransferInProgress = true
                        TimeUnit.SECONDS.sleep(3L)
                        clientTransferSocket = Socket()
                        clientTransferSocket?.use {
                            socket ->
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
                                socket.connect(
                                    (InetSocketAddress(host, transferPort)),
                                    30000
                                )

                                /**
                                 * Create a byte stream from a file and pipe it to the output stream
                                 * of the socket. This data is retrieved by the server device.
                                 */
                                socket.getOutputStream().use {
                                    outputStream ->
                                    log.info(
                                        "Start sending " +
                                            "file to host : $host"
                                    )
                                    while (inputStream.read(buf).also { len = it } != -1) {
                                        outputStream.write(buf, 0, len)
                                        progress += len
                                        if (System.currentTimeMillis() / 1000 - currentTime > 3) {
                                            currentTime = System.currentTimeMillis() / 1000
                                            emit(("$progress/$fileSize"))
                                        }
                                    }
                                    emit(("$progress/$fileSize"))
                                    emit("done")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        log.error("failed to init client transfer", e)
                        emit("done")
                    } finally {
                        isTransferInProgress = false
                    }
                }
            }
        }
    }

    fun initServerConnection(filePath: String, fileSize: Long): LiveData<String> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            if (isTransferInProgress && false) {
                emit("done")
            } else {
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
                                log.info(
                                    "Start receiving file to host" +
                                        " : $peerIP"
                                )
                                try {
                                    while (inputStream.read(buf).also { len = it } != -1) {
                                        out.write(buf, 0, len)
                                        progress += len
                                        if (System.currentTimeMillis() / 1000 - currentTime > 3) {
                                            currentTime = System.currentTimeMillis() / 1000
                                            emit(("$progress/$fileSize"))
                                        }
                                    }
                                    emit(("$progress/$fileSize"))
                                } catch (e: IOException) {
                                    log.warn("failed to receive file to host", e)
                                }
                                emit("done")
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.error("failed to init server connection", e)
                    emit("done")
                } finally {
                    isTransferInProgress = false
                }
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
                isConnectedToPeer = true
                emit(true)
            }
        }
    }

    /**
     * Returns pair to ip address and message
     */
    private fun receiveMessageInternal(): Pair<InetAddress, String>? {
        try {
            if (isTransferInProgress && false) {
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
//                serverSocket.soTimeout = 30000
                val client = serverSocket.accept()
                ObjectInputStream(client.getInputStream()).use {
                    objectInputStream ->
                    val incoming = objectInputStream.readObject()
                    if (incoming.javaClass == String::class.java) {
                        val incomingMessage = incoming as String
                        return if (incomingMessage.startsWith(handshakeSalt)) {
                            log.debug(
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
            log.warn("failed to receive message", e)
            return null
        } finally {
            isTransferInProgress = false
        }
        return null
    }

    private fun File.doesNotExist(): Boolean = !exists()
}
