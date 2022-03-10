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
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import com.amaze.fileutilities.home_page.ui.transfer.TransferFragment

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
        wifiP2PReceiver.also { receiver ->
            registerReceiver(receiver, wifiP2PIntentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        wifiP2PReceiver.also { receiver ->
            unregisterReceiver(receiver)
        }
    }

    fun getWifiP2PManager(): WifiP2pManager? {
        return manager
    }

    fun getWifiP2PChannel(): WifiP2pManager.Channel? {
        return channel
    }

    abstract fun getTransferFragment(): TransferFragment?

    fun disconnectP2PGroup() {
        if (getWifiP2PManager() != null && getWifiP2PChannel() != null) {
            getWifiP2PManager()?.requestGroupInfo(
                getWifiP2PChannel()
            ) { group ->
                if (group != null && getWifiP2PManager() != null &&
                    getWifiP2PChannel() != null
                ) {
                    getWifiP2PManager()?.removeGroup(
                        getWifiP2PChannel(),
                        object : WifiP2pManager.ActionListener {
                            override fun onSuccess() {
                                Log.d(javaClass.simpleName, "removeGroup onSuccess -")
                            }

                            override fun onFailure(reason: Int) {
                                Log.d(javaClass.simpleName, "removeGroup onFailure -$reason")
                            }
                        }
                    )
                }
            }
        }
    }

    private val wifiP2PReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action

            action.let {
                getTransferFragment()?.let {
                    transferFragment ->
                    if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {

                        // UI update to indicate wifi p2p status.
                        val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                            transferFragment.getViewBinding().scanButton
                                .visibility = View.VISIBLE
                        } else {
                            transferFragment.resetViewsOnDisconnect()
                        }
                        Log.d(javaClass.simpleName, "P2P state changed - $state")
                    } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {

                        // request available peers from the wifi p2p manager. This is an
                        // asynchronous call and the calling activity is notified with a
                        // callback on PeerListListener.onPeersAvailable()
                        transferFragment.getTransferViewModel().performedRequestPeers = true
                        transferFragment.monitorDiscoveryTime.cancel()
                        getWifiP2PManager()?.requestPeers(
                            channel,
                            transferFragment
                        )
                        Log.d(javaClass.simpleName, "P2P peers changed")
                    } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {
                        if (getWifiP2PManager() == null) {
                            return
                        }
                        val networkInfo = intent
                            .getParcelableExtra<Parcelable>(WifiP2pManager.EXTRA_NETWORK_INFO)
                            as NetworkInfo?
                        if (networkInfo!!.isConnected) {

                            // we are connected with the other device, request connection
                            // info to find group owner IP
                            getWifiP2PManager()?.requestConnectionInfo(
                                channel,
                                transferFragment
                            )
                        } else {
                            // It's a disconnect
                            transferFragment.resetViewsOnDisconnect()
                        }
                    } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {
                        transferFragment.updateThisDevice(
                            intent.getParcelableExtra<Parcelable>(
                                WifiP2pManager.EXTRA_WIFI_P2P_DEVICE
                            ) as WifiP2pDevice?
                        )
                    } else {
                        // do nothing
                    }
                }
            }
        }
    }
}
