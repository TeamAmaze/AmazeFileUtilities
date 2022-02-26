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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.FragmentTransferBinding
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.utilis.getExternalStorageDirectory
import com.amaze.fileutilities.utilis.px
import com.amaze.fileutilities.utilis.showFileChooserDialog
import com.amaze.fileutilities.utilis.showToastInCenter

class TransferFragment : Fragment() {

    private lateinit var transferViewModel: TransferViewModel
    private var _binding: FragmentTransferBinding? = null
    private var mainActivity: MainActivity? = null

    private val wifiP2PIntentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        transferViewModel =
            ViewModelProvider(this).get(TransferViewModel::class.java)
        mainActivity = activity as MainActivity

        wifiP2PReceiver.also { receiver ->
            mainActivity?.registerReceiver(receiver, wifiP2PIntentFilter)
        }

        _binding = FragmentTransferBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.sendButton.setOnClickListener {
            binding.searchingProgress.visibility = View.VISIBLE
            binding.searchingText.visibility = View.VISIBLE
            mainActivity?.getWifiP2PManager()?.discoverPeers(
                mainActivity?.channel,
                object : WifiP2pManager.ActionListener {

                    override fun onSuccess() {
                        binding.searchingProgress.visibility = View.GONE
                        binding.searchingText.visibility = View.GONE
                    }

                    override fun onFailure(reasonCode: Int) {
                        binding.searchingProgress.visibility = View.GONE
                        transferViewModel.currentDeviceAddress = null
                        binding.searchingText.text =
                            "${getString(R.string.discovery_failure)} : $reasonCode"
                    }
                }
            )
        }
        binding.transferButton.setOnClickListener {
            requireContext().showFileChooserDialog {
                transferViewModel.initClientTransfer(it.inputStream())
            }
        }
        binding.receiveButton.setOnClickListener {
            requireContext().getExternalStorageDirectory()?.path?.let {
                binding.searchingText.text = getString(R.string.receiving_files)
                transferViewModel.initServerConnection(it)
            }
        }
        return root
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        wifiP2PReceiver.also { receiver ->
            mainActivity?.unregisterReceiver(receiver)
        }
        _binding = null
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
                                binding.sendButton.visibility = View.VISIBLE
                            }
                            else -> {
                                // Wi-Fi P2P is not enabled
                                binding.transferButton.visibility = View.GONE
                                binding.sendButton.visibility = View.GONE
                                binding.devicesParent.removeAllViews()
                                binding.searchingProgress.visibility = View.GONE
                                binding.searchingText.visibility = View.VISIBLE
                                transferViewModel.currentDeviceAddress = null
                                binding.searchingText.text = getString(R.string.enable_wifi)
                            }
                        }
                    }
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        // Call WifiP2pManager.requestPeers() to get a list of current peers
                        mainActivity?.getWifiP2PManager()?.requestPeers(mainActivity?.channel) {
                            peers: WifiP2pDeviceList? ->
                            // Handle peers list
                            Log.i(javaClass.simpleName, "Found peers: $peers")
                            peers?.let {
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
                            }
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

    private fun getPeerButton(device: WifiP2pDevice): Button {
        val button = getSelectedTextButton(device.deviceName)
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress
        button.setOnClickListener {
            mainActivity?.manager?.connect(
                mainActivity?.channel,
                config,
                object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        requireContext().showToastInCenter(
                            "Connection " +
                                "successful"
                        )
                        binding.transferButton.visibility = View.VISIBLE
                        transferViewModel.currentDeviceAddress = device.deviceAddress
                    }

                    override fun onFailure(p0: Int) {
                        requireContext().showToastInCenter(
                            "Connection " +
                                "failed with $p0"
                        )
                        transferViewModel.currentDeviceAddress = null
                    }
                }
            )
        }
        return button
    }

    private fun getSelectedTextButton(text: String): Button {
        val button = Button(context)
        setSelectButton(button)
        setParams(button)
        button.text = text
        return button
    }

    private fun setParams(button: Button) {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = 16.px.toInt()
        button.layoutParams = params
    }

    private fun setSelectButton(button: Button) {
        button.background = resources.getDrawable(R.drawable.button_curved_selected)
        button.setTextColor(resources.getColor(R.color.navy_blue))
    }
}
