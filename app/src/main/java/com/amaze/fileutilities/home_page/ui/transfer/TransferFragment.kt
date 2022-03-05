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
import android.net.NetworkInfo
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Bundle
import android.os.Parcelable
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
import com.amaze.fileutilities.utilis.*

class TransferFragment : Fragment(), WifiP2pManager.ConnectionInfoListener, PeerListListener {

    private lateinit var transferViewModel: TransferViewModel
    private var _binding: FragmentTransferBinding? = null
    private var mainActivity: MainActivity? = null

    private var info: WifiP2pInfo? = null
    private var device: WifiP2pDevice? = null

    private val wifiP2PIntentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    companion object {
        private val RECEIVER_BASE_PATH = "AmazeFileUtils"
        private val SEND_FILE_META_SPLITTER = "/"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        transferViewModel =
            ViewModelProvider(this).get(TransferViewModel::class.java)
        mainActivity = activity as MainActivity

        _binding = FragmentTransferBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.scanButton.setOnClickListener {
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
                        transferViewModel.groupOwnerIP = null
                        binding.searchingText.visibility = View.VISIBLE
                        binding.searchingText.text =
                            "${getString(R.string.discovery_failure)} : $reasonCode"
                    }
                }
            )
        }
        binding.sendButton.setOnClickListener {
            requireContext().showFileChooserDialog {
                if (transferViewModel.peerIP == null) {
                    resetViewsOnDisconnect()
                    requireContext().showToastInCenter(
                        resources
                            .getString(R.string.failed_filename_send_reconnecct)
                    )
                    binding.searchingText.text = getString(R.string.failed_to_handshake)
                } else {
                    transferViewModel
                        .sendMessage("${it.name}$SEND_FILE_META_SPLITTER${it.length()}")
                        .observe(viewLifecycleOwner) {
                            didSendFileName ->
                            if (!didSendFileName) {
                                requireContext().showToastInCenter(
                                    resources
                                        .getString(R.string.failed_filename_send)
                                )
                            } else {
                                transferViewModel.initClientTransfer(it)
                                    .observe(viewLifecycleOwner) {
                                        progress ->
                                        invalidateTransferProgressBar(progress, it.name)
                                    }
                            }
                        }
                }
            }
        }

        binding.receiveButton.setOnClickListener {
            requireContext().getExternalStorageDirectory()?.path?.let {
                if (transferViewModel.peerIP == null) {
                    resetViewsOnDisconnect()
                    requireContext().showToastInCenter(
                        resources
                            .getString(R.string.failed_filename_receive_reconnecct)
                    )
                    binding.searchingText.text = getString(R.string.failed_to_handshake)
                } else {
                    transferViewModel.receiveMessage().observe(viewLifecycleOwner) {
                        receivedFileNameAndBytes ->
                        if (receivedFileNameAndBytes != null) {
                            val array = receivedFileNameAndBytes.split(SEND_FILE_META_SPLITTER)
                            val filePath = "$it/$RECEIVER_BASE_PATH/${array[0]}"
                            val fileLength = array[1].toLong()
                            binding.searchingText.text = getString(R.string.receiving_files)

                            transferViewModel.initServerConnection(filePath, fileLength)
                                .observe(viewLifecycleOwner) {
                                    progress ->
                                    invalidateTransferProgressBar(progress, array[0])
                                }
                        } else {
                            requireContext().showToastInCenter(
                                resources
                                    .getString(R.string.failed_filename_receive)
                            )
                        }
                    }
                }
            }
        }
        return root
    }

    private fun invalidateTransferProgressBar(progress: Long, fileName: String) {
        if (progress == -1L || progress > 100) {
            // finish sending
            binding.transferFileText.text = ""
            binding.transferProgress.progress = 0
            binding.transferInfoParent.hideFade(300)
            requireContext().showToastInCenter(resources.getString(R.string.transfer_complete))
        } else {
            binding.transferInfoParent.visibility = View.VISIBLE
            binding.transferFileText.text = fileName
            binding.transferProgress.progress = progress.toInt()
        }
    }

    override fun onResume() {
        super.onResume()
        wifiP2PReceiver.also { receiver ->
            mainActivity?.registerReceiver(receiver, wifiP2PIntentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        wifiP2PReceiver.also { receiver ->
            mainActivity?.unregisterReceiver(receiver)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val wifiP2PReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action

            action.let {
                if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {

                    // UI update to indicate wifi p2p status.
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        if (transferViewModel.isWifiP2PEnabled) {
                            binding.scanButton.visibility = View.VISIBLE

                            // set connected devices info
                        } else {
                            // Wifi Direct mode is enabled
                            transferViewModel.isWifiP2PEnabled = true
                        }
                    } else {
                        resetViewsOnDisconnect()
                    }
                    Log.d(javaClass.simpleName, "P2P state changed - $state")
                } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {

                    // request available peers from the wifi p2p manager. This is an
                    // asynchronous call and the calling activity is notified with a
                    // callback on PeerListListener.onPeersAvailable()
                    if (mainActivity?.manager != null) {
                        mainActivity?.manager!!.requestPeers(
                            mainActivity?.channel,
                            this@TransferFragment
                        )
                    }
                    Log.d(javaClass.simpleName, "P2P peers changed")
                } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {
                    if (mainActivity?.manager == null) {
                        return
                    }
                    val networkInfo = intent
                        .getParcelableExtra<Parcelable>(WifiP2pManager.EXTRA_NETWORK_INFO)
                        as NetworkInfo?
                    if (networkInfo!!.isConnected) {

                        // we are connected with the other device, request connection
                        // info to find group owner IP
                        mainActivity?.manager!!.requestConnectionInfo(
                            mainActivity?.channel,
                            this@TransferFragment
                        )
                    } else {
                        // It's a disconnect
                        resetViewsOnDisconnect()
                    }
                } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {
                    updateThisDevice(
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

    private fun resetViewsOnDisconnect() {
        transferViewModel.isWifiP2PEnabled = false
        transferViewModel.groupOwnerIP = null
        transferViewModel.selfIP = null
        transferViewModel.peerIP = null
        binding.devicesParent.removeAllViews()
        binding.searchingText.visibility = View.VISIBLE
        binding.searchingText.text = resources.getString(R.string.search_devices)
        binding.searchingProgress.visibility = View.GONE

        // Wi-Fi P2P is not enabled
        binding.scanButton.visibility = View.GONE
        binding.sendButton.visibility = View.GONE
        binding.searchingText.text = getString(R.string.enable_wifi)
        binding.deviceName.visibility = View.GONE
        binding.deviceStatus.visibility = View.GONE
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
                        // wait for group owner ip callback
                    }

                    override fun onFailure(p0: Int) {
                        requireContext().showToastInCenter(
                            "Connection " +
                                "failed with $p0"
                        )
                        resetViewsOnDisconnect()
                        binding.scanButton.visibility = View.VISIBLE
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

    override fun onConnectionInfoAvailable(p0: WifiP2pInfo?) {
        this.info = p0
        p0?.let {
            transferViewModel.groupOwnerIP = p0.groupOwnerAddress.hostAddress
            transferViewModel.selfIP = Utils.wifiIpAddress(requireContext())
            transferViewModel.initHandshake().observe(viewLifecycleOwner) {
                handshakeSuccess ->
                if (!handshakeSuccess) {
                    Log.w(javaClass.simpleName, "Handshake failed")
                    requireContext().showToastInCenter(
                        resources
                            .getString(R.string.failed_to_handshake)
                    )
                    resetViewsOnDisconnect()
                    binding.searchingText.text = getString(R.string.failed_to_handshake)
                } else {
                    Log.w(
                        javaClass.simpleName,
                        "Handshake success, " +
                            "peer ip: ${transferViewModel.peerIP}"
                    )
                    requireContext().showToastInCenter(
                        resources
                            .getString(R.string.connection_successful)
                    )
                    binding.scanButton.visibility = View.GONE
                    binding.sendButton.visibility = View.VISIBLE
                    binding.receiveButton.visibility = View.VISIBLE
                    Log.i(
                        javaClass.simpleName,
                        "Connection established with group owner" +
                            " id ${transferViewModel.groupOwnerIP}"
                    )
                }
            }
        }
    }

    override fun onPeersAvailable(peers: WifiP2pDeviceList?) {
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
                peers.deviceList.forEach {
                    device ->
                    binding.devicesParent.addView(getPeerButton(device))
                }
            }
        }
    }

    fun updateThisDevice(device: WifiP2pDevice?) {
        device?.also {
            this.device = device
            binding.deviceName.visibility = View.VISIBLE
            binding.deviceStatus.visibility = View.VISIBLE
            binding.deviceName.text = device.deviceName
            binding.deviceStatus.text = getDeviceStatus(device.status)
        }
    }

    private fun getDeviceStatus(deviceStatus: Int): String? {
        Log.d(javaClass.simpleName, "Peer status :$deviceStatus")
        return when (deviceStatus) {
            WifiP2pDevice.AVAILABLE -> "Available"
            WifiP2pDevice.INVITED -> "Invited"
            WifiP2pDevice.CONNECTED -> "Connected"
            WifiP2pDevice.FAILED -> "Failed"
            WifiP2pDevice.UNAVAILABLE -> "Unavailable"
            else -> "Unknown"
        }
    }
}
