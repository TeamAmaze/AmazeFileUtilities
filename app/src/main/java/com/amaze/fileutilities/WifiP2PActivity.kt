/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log

abstract class WifiP2PActivity : CastActivity(), WifiP2pManager.ChannelListener {

    private val wifiP2PIntentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    val manager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }

    var channel: WifiP2pManager.Channel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        channel = manager?.initialize(this, mainLooper, this)
    }

    override fun onChannelDisconnected() {
        TODO("Not yet implemented")
    }

    override fun onResume() {
        super.onResume()
        /*wifiP2PReceiver.also { receiver ->
            registerReceiver(receiver, wifiP2PIntentFilter)
        }*/
    }

    override fun onPause() {
        super.onPause()
        /*wifiP2PReceiver.also { receiver ->
            unregisterReceiver(receiver)
        }*/
    }

    fun getWifiP2PManager(): WifiP2pManager? {
        return manager
    }

    fun getWifiP2PChannel(): WifiP2pManager.Channel? {
        return channel
    }

    private val wifiP2PReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            action?.let {
                when (action) {
                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                        // Check to see if Wi-Fi is enabled and notify appropriate activity
                        when (intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)) {
                            WifiP2pManager.WIFI_P2P_STATE_ENABLED -> {
                                // Wifi P2P is enabled
//                                binding.sendButton.visibility = View.VISIBLE
                            }
                            else -> {
                                // Wi-Fi P2P is not enabled
                                /*binding.transferButton.visibility = View.GONE
                                binding.sendButton.visibility = View.GONE
                                binding.devicesParent.removeAllViews()
                                binding.searchingProgress.visibility = View.GONE
                                binding.searchingText.visibility = View.VISIBLE
                                transferViewModel.currentDeviceAddress = null
                                binding.searchingText.text = getString(R.string.enable_wifi)*/
                            }
                        }
                    }
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        // Call WifiP2pManager.requestPeers() to get a list of current peers
                        manager?.requestPeers(channel) {
                            peers: WifiP2pDeviceList? ->
                            // Handle peers list
                            Log.i(javaClass.simpleName, "Found peers: $peers")
                            /*peers?.let {
                                if (it.deviceList.isEmpty()) {
                                    binding.searchingText.text =
                                        getString(R.string.no_device_nearby)
                                } else {
                                    binding.searchingText.text = peers.deviceList.joinToString {
                                        "Name: ${it.deviceName}\n${it.deviceAddress}\n\n"
                                    }
                                    binding.devicesParent.removeAllViews()
                                    transferViewModel.currentDeviceAddress = null
                                    peers.deviceList.forEach {
                                            device ->
                                        binding.devicesParent.addView(getPeerButton(device))
                                    }
                                }
                            }*/
                        }
                    }
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        // Respond to new connection or disconnections
                    }
                    WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                        // Respond to this device's wifi state changing
                    }
                    else -> {
                    }
                }
            }
        }
    }
}
