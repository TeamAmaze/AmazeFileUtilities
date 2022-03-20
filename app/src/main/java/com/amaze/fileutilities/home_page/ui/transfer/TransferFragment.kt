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

import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.PermissionsActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.FragmentTransferBinding
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.utilis.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TransferFragment : Fragment(), WifiP2pManager.ConnectionInfoListener, PeerListListener {

    var log: Logger = LoggerFactory.getLogger(TransferFragment::class.java)

    private lateinit var transferViewModel: TransferViewModel
    private var _binding: FragmentTransferBinding? = null
    private var mainActivity: MainActivity? = null

    private var info: WifiP2pInfo? = null
    private var device: WifiP2pDevice? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    companion object {
        val RECEIVER_BASE_PATH = "AmazeFileUtils"
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
        binding.searchingText.visibility = View.VISIBLE
        binding.searchingText.text = resources.getString(R.string.waiting_for_permissions)
        binding.scanButton.visibility = View.GONE
        val isGranted = mainActivity?.initLocationResources(object :
                PermissionsActivity.OnPermissionGranted {
                override fun onPermissionGranted(isGranted: Boolean) {
                    if (isGranted) {
                        binding.searchingText.visibility = View.GONE
                        binding.scanButton.visibility = View.VISIBLE
                    } else {
                        locationPermissionDenied()
                    }
                }
            })
        isGranted?.also {
            binding.searchingText.visibility = View.GONE
            binding.scanButton.visibility = View.VISIBLE
        }
        checkLocationEnabled()

        binding.scanButton.setOnClickListener {
            binding.searchingProgress.visibility = View.VISIBLE
            binding.searchingText.visibility = View.VISIBLE
            binding.searchingText.text = resources.getString(R.string.searching)
            binding.scanButton.visibility = View.GONE
            binding.stopScanButton.showFade(300)
            monitorDiscoveryTime.start()
            mainActivity?.getWifiP2PManager()?.discoverPeers(
                mainActivity?.getWifiP2PChannel(),
                object : WifiP2pManager.ActionListener {

                    override fun onSuccess() {
                        // do nothing, handle when peers are found
                        log.info("Peers discovered")
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
        binding.stopScanButton.setOnClickListener {
            mainActivity?.getWifiP2PManager()?.stopPeerDiscovery(
                mainActivity?.channel,
                object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        binding.searchingText.hideFade(300)
                        binding.searchingProgress.visibility = View.GONE
                        binding.searchingText.visibility = View.GONE
                        binding.scanButton.visibility = View.VISIBLE
                        binding.stopScanButton.hideFade(300)
                    }

                    override fun onFailure(p0: Int) {
                        binding.searchingText.text = resources
                            .getString(R.string.failed_to_stop_peer_discovery)
                        binding.searchingText.visibility = View.VISIBLE
                        binding.searchingProgress.visibility = View.GONE
                        binding.searchingText.visibility = View.GONE
                        binding.stopScanButton.hideFade(300)
                    }
                }
            )
        }
        binding.disconnectButton.setOnClickListener {
            this.resetViewsOnDisconnect()
            resetNetworkGroup()
            requireContext().showToastInCenter(resources.getString(R.string.disconnected))
        }

        binding.sendButton.setOnClickListener {
            requireContext().showFileChooserDialog {
                if (transferViewModel.peerIP == null) {
                    this.resetViewsOnDisconnect()
                    requireContext().showToastInCenter(
                        resources
                            .getString(R.string.failed_filename_send_reconnecct)
                    )
                    binding.searchingText.visibility = View.VISIBLE
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
                    this.resetViewsOnDisconnect()
                    requireContext().showToastInCenter(
                        resources
                            .getString(R.string.failed_filename_receive_reconnecct)
                    )
                    binding.searchingText.text = getString(R.string.failed_to_handshake)
                    binding.scanButton.visibility = View.VISIBLE
                } else {
                    binding.receiveButton.visibility = View.GONE
                    binding.searchingText.visibility = View.VISIBLE
                    binding.searchingText.text = resources.getString(R.string.receiving_files)
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

    fun getTransferViewModel(): TransferViewModel {
        return transferViewModel
    }

    fun getViewBinding(): FragmentTransferBinding {
        return binding
    }

    private fun locationPermissionDenied() {
        binding.run {
            this@TransferFragment.resetViewsOnDisconnect()
            scanButton.visibility = View.GONE
            searchingText.text = resources.getString(R.string.location_permission_not_granted)
        }
    }

    private fun resetNetworkGroup() {
        if (!getTransferViewModel().performedRequestPeers) {
            // we didn't make any request to peers till now, disconnect existing network
            requireContext().showToastInCenter(
                resources
                    .getString(R.string.resetting_network_discovery)
            )
            mainActivity?.disconnectP2PGroup()
            getTransferViewModel().isConnectedToPeer = false
        }
    }

    val monitorDiscoveryTime = object : CountDownTimer(30000, 30000) {
        override fun onTick(millisUntilFinished: Long) {
        }

        override fun onFinish() {
            _binding?.also {
                // warn user for no device found, stop discovery
                resetNetworkGroup()
                mainActivity?.getWifiP2PManager()?.stopPeerDiscovery(
                    mainActivity?.channel,
                    object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            binding.searchingText.text = resources
                                .getString(R.string.no_devices_found)
                            this@TransferFragment.resetViewsOnDisconnect()
                        }

                        override fun onFailure(p0: Int) {
                            log.warn(
                                "Failed to stop peer discovery, " +
                                    "error code $p0"
                            )
                            binding.searchingText.text = resources
                                .getString(R.string.failed_to_stop_peer_discovery)
                            binding.searchingText.visibility = View.VISIBLE
                            binding.searchingProgress.visibility = View.VISIBLE
                        }
                    }
                )
            }
        }
    }

    private fun locationDisabled() {
        binding.run {
            this@TransferFragment.resetViewsOnDisconnect()
            scanButton.visibility = View.GONE
            enableLocationButton.visibility = View.VISIBLE
            searchingText.text = resources.getString(R.string.enable_location)
            enableLocationButton.setOnClickListener {
                checkLocationEnabled()
            }
        }
    }

    private fun checkLocationEnabled() {
        mainActivity?.isLocationEnabled(object : PermissionsActivity.OnPermissionGranted {
            override fun onPermissionGranted(isGranted: Boolean) {
                if (isGranted) {
                    binding.searchingText.visibility = View.GONE
                    binding.enableLocationButton.visibility = View.GONE
                    binding.scanButton.visibility = View.VISIBLE
                } else {
                    locationDisabled()
                }
            }
        })
    }

    private fun invalidateTransferProgressBar(progress: String, fileName: String) {
        if (progress == "-1L" || progress == "1" || progress == "done") {
            // finish sending
            binding.transferFileText.text = ""
            binding.transferBytesText.text = ""
            binding.transferProgress.visibility = View.GONE
            binding.transferInfoParent.hideFade(300)
            binding.searchingText.text = ""
            binding.searchingText.visibility = View.GONE
            binding.receiveButton.visibility = View.VISIBLE
            binding.sendReceiveParent.showFade(300)
            requireContext().showToastInCenter(resources.getString(R.string.transfer_complete))
        } else {
            binding.sendReceiveParent.hideFade(400)
            binding.transferInfoParent.visibility = View.VISIBLE
            binding.transferFileText.text = fileName
            progress.let {
                val progressArr = progress.split("/")
                val done = FileUtils.formatStorageLength(
                    requireContext(), progressArr[0].toLong()
                )
                val total = FileUtils.formatStorageLength(
                    requireContext(), progressArr[1].toLong()
                )
                binding.transferBytesText.text = "$done / $total"
            }
            binding.transferProgress.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        getTransferViewModel().clientHandshakeSocket?.close()
        getTransferViewModel().serverHandshakeSocket?.close()
        getTransferViewModel().clientTransferSocket?.close()
        getTransferViewModel().serverTransferSocket?.close()
        mainActivity?.disconnectP2PGroup()
        monitorDiscoveryTime.cancel()
        _binding = null
    }

    fun resetViewsOnDisconnect() {
        transferViewModel.isConnectedToPeer = false
        transferViewModel.groupOwnerIP = null
        transferViewModel.selfIP = null
        transferViewModel.peerIP = null

        binding.devicesParent.removeAllViews()
        binding.stopScanButton.visibility = View.GONE
        binding.searchingText.visibility = View.GONE
        binding.searchingProgress.visibility = View.GONE
        binding.sendReceiveParent.visibility = View.GONE
        binding.transferInfoParent.visibility = View.GONE
        binding.scanButton.visibility = View.VISIBLE
    }

    private fun getPeerButton(device: WifiP2pDevice): Button {
        val button = getSelectedTextButton(device.deviceName)
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress
        config.wps.setup = WpsInfo.PBC
        button.setOnClickListener {
            requireContext().showToastInCenter(resources.getString(R.string.connecting))
            mainActivity?.getWifiP2PManager()?.connect(
                mainActivity?.getWifiP2PChannel(),
                config,
                object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        // wait for group owner ip callback
                        // save device mac address vs ip so that in case we don't receive
                        // any callback to onConnectionInfoAvailable we can try to connect
                        // using previous IP
                    }

                    override fun onFailure(p0: Int) {
                        requireContext().showToastInCenter(
                            "Connection " +
                                "failed with $p0"
                        )
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
            log.info(
                "isGO: ${p0.isGroupOwner}" +
                    " owner ip: ${transferViewModel.groupOwnerIP}\n " +
                    "selfIP: ${transferViewModel.groupOwnerIP}"
            )
            binding.deviceStatus.text = "owner ip: ${transferViewModel.groupOwnerIP}\n " +
                "selfIP: ${transferViewModel.groupOwnerIP}"

            if (getTransferViewModel().isConnectedToPeer) {
                requireContext().showToastInCenter(
                    resources
                        .getString(R.string.existing_connection)
                )
            } else {
                transferViewModel.initHandshake(p0)?.observe(viewLifecycleOwner) {
                    handshakeSuccess ->
                    if (!handshakeSuccess) {
                        log.warn("Handshake failed")
                        requireContext().showToastInCenter(
                            resources
                                .getString(R.string.failed_to_handshake)
                        )
                        this.resetViewsOnDisconnect()
                        binding.searchingText.text = getString(R.string.failed_to_handshake)
                        binding.scanButton.visibility = View.VISIBLE
                    } else {
                        log.warn(
                            "Handshake success, " +
                                "peer ip: ${transferViewModel.peerIP}"
                        )
                        requireContext().showToastInCenter(
                            resources
                                .getString(R.string.connection_successful)
                        )
                        binding.scanButton.visibility = View.GONE
                        binding.devicesParent.removeAllViews()
                        binding.sendReceiveParent.showFade(200)
                        binding.deviceStatus.text = "owner ip: ${transferViewModel
                            .groupOwnerIP}\n " +
                            "selfIP: ${transferViewModel.groupOwnerIP}\npeerIp: " +
                            "${transferViewModel.peerIP}"
                        log.info(
                            "Connection established with group owner" +
                                " id ${transferViewModel.groupOwnerIP}"
                        )
                    }
                }
            }
        }
    }

    override fun onPeersAvailable(peers: WifiP2pDeviceList?) {
        // Handle peers list
        log.info("Found peers: $peers")
        peers?.let {
            binding.stopScanButton.visibility = View.GONE
            if (it.deviceList.isEmpty()) {
                binding.searchingText.text =
                    getString(R.string.no_devices_found)
            } else {
                binding.searchingText.text = peers.deviceList.joinToString {
                    "Name: ${it.deviceName}\n${it.deviceAddress}\n\n"
                }
                binding.devicesParent.removeAllViews()
                peers.deviceList.forEach {
                    device ->
                    log.info("Found peer: $device")
                    binding.devicesParent.addView(getPeerButton(device))
                }
            }
        }

        binding.searchingProgress.visibility = View.GONE
        binding.searchingText.visibility = View.GONE
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

    private fun getDeviceStatus(deviceStatus: Int): String {
        log.info("Peer status :$deviceStatus")
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
