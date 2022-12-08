/*
 * Copyright (C) 2021-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Utilities.
 *
 * Amaze File Utilities is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.fileutilities.home_page.ui.transfer

import android.content.Intent
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.amaze.fileutilities.PermissionsActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.FragmentTransferBinding
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.utilis.FileUtils
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.getExternalStorageDirectory
import com.amaze.fileutilities.utilis.hideFade
import com.amaze.fileutilities.utilis.px
import com.amaze.fileutilities.utilis.showFade
import com.amaze.fileutilities.utilis.showFileChooserDialog
import com.amaze.fileutilities.utilis.showToastInCenter
import com.amaze.fileutilities.utilis.showToastOnBottom
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.abs

class TransferFragment : Fragment(), WifiP2pManager.ConnectionInfoListener, PeerListListener {

    var log: Logger = LoggerFactory.getLogger(TransferFragment::class.java)

    private val viewModel: TransferViewModel by activityViewModels()
    private var _binding: FragmentTransferBinding? = null
    private var mainActivity: MainActivity? = null

    private var info: WifiP2pInfo? = null
    private var device: WifiP2pDevice? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    companion object {
        val RECEIVER_BASE_PATH = "AmazeFileUtils"
        val NO_MEDIA = ".nomedia"
        val ID_LOG = "_id.log"
        private val SEND_FILE_META_SPLITTER = "/"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mainActivity = activity as MainActivity

        _binding = FragmentTransferBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.searchingText.visibility = View.VISIBLE
        binding.searchingText.text = resources.getString(R.string.waiting_for_permissions)
        binding.scanButton.visibility = View.GONE

        val wifiManager = (
            mainActivity!!.applicationContext
                .getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager
            )
        if (!wifiManager.isWifiEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                mainActivity!!.startActivity(intent)
            } else {
                wifiManager.isWifiEnabled = true
            }
            binding.searchingText.text = resources.getString(R.string.enable_wifi_to_continue)
            requireActivity().showToastOnBottom(
                resources
                    .getString(R.string.enable_wifi_to_continue)
            )
        } else {
            val isGranted = mainActivity?.initLocationResources(object :
                    PermissionsActivity.OnPermissionGranted {
                    override fun onPermissionGranted(isGranted: Boolean) {
                        if (isGranted) {
                            initScreenComponents()
                        } else {
                            locationPermissionDenied()
                        }
                    }
                })
            isGranted?.also {
                initScreenComponents()
            }
            checkLocationEnabled()
            setupButtonClicks()
        }
        return root
    }

    fun initScreenComponents() {
        if (viewModel.isConnectedToPeer) {
            initAfterHandshake()

            log.info(
                "Already connect with" +
                    " id ${viewModel.groupOwnerIP} my ip " +
                    "${viewModel.selfIP}"
            )
        } else {
            _binding?.run {
                devicesParent.removeAllViews()
                searchingText.visibility = View.VISIBLE
                scanButton.visibility = View.VISIBLE
                searchingText.text = resources.getString(R.string.start_scan_both_devices)
            }
        }
    }

    fun getTransferViewModel(): TransferViewModel {
        return viewModel
    }

    fun getViewBinding(): FragmentTransferBinding {
        return binding
    }

    private fun locationPermissionDenied() {
        _binding?.run {
            this@TransferFragment.resetViewsOnDisconnect()
            scanButton.visibility = View.GONE
            searchingText.text = resources.getString(R.string.location_permission_not_granted)
        }
    }

    fun resetNetworkGroup() {
        // we didn't make any request to peers till now, disconnect existing network
        requireContext().showToastInCenter(
            resources
                .getString(R.string.resetting_network_discovery)
        )
        mainActivity?.disconnectP2PGroup()
        getTransferViewModel().isConnectedToPeer = false
    }

    val monitorDiscoveryTime = object : CountDownTimer(30000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            _binding?.searchingText?.visibility = View.VISIBLE
            _binding?.searchingText?.text = resources.getString(R.string.searching_timer)
                .format(abs(millisUntilFinished / 1000))
        }

        override fun onFinish() {
            _binding?.also {
                // warn user for no device found, stop discovery
                if (!viewModel.performedRequestPeers) {
                    resetNetworkGroup()
                    resetViewsOnDisconnect()
                    viewModel.performedRequestPeers = true
                }
                binding.searchingText.visibility = View.VISIBLE
                mainActivity?.getWifiP2PManager()?.stopPeerDiscovery(
                    mainActivity?.channel,
                    object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            requireActivity().showToastOnBottom(
                                resources
                                    .getString(R.string.no_devices_found)
                            )
                            binding.searchingText.text = resources
                                .getString(R.string.start_scan_both_devices)
                            binding.searchingText.visibility = View.VISIBLE
                        }

                        override fun onFailure(p0: Int) {
                            log.warn(
                                "Failed to stop peer discovery, " +
                                    "error code $p0"
                            )
                            requireActivity().showToastOnBottom(
                                resources
                                    .getString(R.string.failed_to_stop_peer_discovery)
                            )
                            binding.searchingText.text =
                                getString(R.string.start_scan_both_devices)
                            binding.searchingText.visibility = View.VISIBLE
                            binding.searchingProgress.visibility = View.GONE
                            binding.stopScanButton.visibility = View.GONE
                            binding.scanButton.visibility = View.VISIBLE
                        }
                    }
                )
            }
        }
    }

    private fun setupButtonClicks() {
        _binding?.run {
            scanButton.setOnClickListener {
                searchingProgress.visibility = View.VISIBLE
                searchingText.visibility = View.VISIBLE
                searchingText.text = resources.getString(R.string.searching)
                scanButton.visibility = View.GONE
                stopScanButton.showFade(300)
                monitorDiscoveryTime.start()
                mainActivity?.getWifiP2PManager()?.discoverPeers(
                    mainActivity?.getWifiP2PChannel(),
                    object : WifiP2pManager.ActionListener {

                        override fun onSuccess() {
                            // do nothing, handle when peers are found
                            log.info("Peers discovered")
                        }

                        override fun onFailure(reasonCode: Int) {
                            searchingProgress.visibility = View.GONE
                            monitorDiscoveryTime.cancel()
                            viewModel.groupOwnerIP = null
                            viewModel.peerIP = null
                            viewModel.selfIP = null
                            viewModel.isConnectedToPeer = false
                            searchingText.visibility = View.VISIBLE
                            searchingText.text = resources
                                .getString(R.string.connected_send_receive_hint)
                            requireActivity()
                                .showToastOnBottom(
                                    "${getString(R.string.discovery_failure)} " +
                                        ": $reasonCode"
                                )
                            initScreenComponents()
                            stopScanButton.visibility = View.GONE
                            scanButton.visibility = View.VISIBLE
                        }
                    }
                )
            }
            stopScanButton.setOnClickListener {
                mainActivity?.getWifiP2PManager()?.stopPeerDiscovery(
                    mainActivity?.channel,
                    object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            searchingText.hideFade(300)
                            searchingProgress.visibility = View.GONE
                            searchingText.visibility = View.VISIBLE
                            searchingText.text = resources
                                .getString(R.string.start_scan_both_devices)
                            deviceStatus.visibility = View.GONE
                            deviceName.visibility = View.GONE
                            scanButton.visibility = View.VISIBLE
                            stopScanButton.hideFade(300)
                            monitorDiscoveryTime.cancel()
                            initConnectionTimer.cancel()
                        }

                        override fun onFailure(p0: Int) {
                            searchingText.text = resources
                                .getString(R.string.failed_to_stop_peer_discovery)
                            searchingText.visibility = View.VISIBLE
                            searchingProgress.visibility = View.GONE
                            searchingText.visibility = View.GONE
                            stopScanButton.hideFade(300)
                            monitorDiscoveryTime.cancel()
                            initConnectionTimer.cancel()
                        }
                    }
                )
            }
            disconnectButton.setOnClickListener {
                resetViewsOnDisconnect()
                resetNetworkGroup()
                requireContext().showToastInCenter(resources.getString(R.string.disconnected))
            }

            sendButton.setOnClickListener {
                requireContext().showFileChooserDialog {
                    if (viewModel.peerIP == null) {
                        resetViewsOnDisconnect()
                        resetNetworkGroup()
                        requireContext().showToastInCenter(
                            resources
                                .getString(R.string.failed_filename_send_reconnecct)
                        )
                    } else if (viewModel.isTransferInProgress) {
                        requireContext().showToastInCenter(
                            resources
                                .getString(R.string.transfer_in_progress_title)
                        )
                    } else {
                        sendReceiveParent.hideFade(400)
                        initConnectionTimer.start()
                        viewModel
                            .sendMessage("${it.name}$SEND_FILE_META_SPLITTER${it.length()}")
                            .observe(viewLifecycleOwner) {
                                didSendFileName ->
                                if (!didSendFileName) {
                                    requireContext().showToastInCenter(
                                        resources
                                            .getString(R.string.failed_filename_send)
                                    )
                                    resetViewsOnDisconnect()
                                    initConnectionTimer.cancel()
                                } else {
                                    var lastProgress = 0L
                                    viewModel.initClientTransfer(it)
                                        .observe(viewLifecycleOwner) {
                                            progress ->
                                            initConnectionTimer.cancel()
                                            invalidateTransferProgressBar(
                                                progress, lastProgress,
                                                it.name
                                            )
                                            lastProgress = progress.split("/")[0].toLong()
                                        }
                                }
                            }
                    }
                }
            }

            transferCancel.setOnClickListener {
                viewModel.serverTransferSocket?.close()
                viewModel.clientTransferSocket?.close()
                requireActivity().showToastOnBottom("Transfer cancelled")
                initTransferFinishViews()
            }

            receiveStopButton.setOnClickListener {
                viewModel.serverHandshakeSocket?.close()
                sendButton.visibility = View.VISIBLE
                receiveButton.visibility = View.VISIBLE
                receiveStopButton.visibility = View.GONE
                searchingText.text = resources.getString(R.string.connected_send_receive_hint)
            }

            receiveButton.setOnClickListener {
                requireContext().getExternalStorageDirectory()?.path?.let {
                    if (viewModel.peerIP == null) {
                        resetViewsOnDisconnect()
                        requireContext().showToastInCenter(
                            resources
                                .getString(R.string.failed_filename_receive_reconnecct)
                        )
                        searchingText.text = getString(R.string.failed_to_handshake)
                        scanButton.visibility = View.VISIBLE
                    } else if (viewModel.isTransferInProgress) {
                        requireContext().showToastInCenter(
                            resources
                                .getString(R.string.transfer_in_progress_title)
                        )
                    } else {
                        receiveButton.visibility = View.GONE
                        receiveStopButton.visibility = View.VISIBLE
                        sendButton.visibility = View.GONE
                        searchingText.visibility = View.VISIBLE
                        searchingText.text = resources.getString(R.string.receiving_files)
                        viewModel.receiveMessage().observe(viewLifecycleOwner) {
                            receivedFileNameAndBytes ->
                            if (receivedFileNameAndBytes != null) {
                                val array = receivedFileNameAndBytes.split(SEND_FILE_META_SPLITTER)
                                val filePath = "$it/$RECEIVER_BASE_PATH/${array[0]}"
                                val fileLength = array[1].toLong()
                                searchingText.visibility = View.VISIBLE
                                searchingText.text = getString(R.string.receiving_files)
                                var lastProgress = 0L
                                viewModel.initServerConnection(filePath, fileLength)
                                    .observe(viewLifecycleOwner) {
                                        progress ->
                                        invalidateTransferProgressBar(
                                            progress, lastProgress,
                                            array[0]
                                        )
                                        lastProgress = progress.split("/")[0].toLong()
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
        }
    }

    private fun locationDisabled() {
        _binding?.run {
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
                    initScreenComponents()
                } else {
                    locationDisabled()
                }
            }
        })
    }

    private fun initTransferFinishViews() {
        _binding?.run {
            transferFileText.text = ""
            transferBytesText.text = ""
            transferProgress.visibility = View.GONE
            transferInfoParent.hideFade(300)
            searchingText.text = resources.getString(R.string.connected_send_receive_hint)
            searchingText.visibility = View.VISIBLE
            searchingProgress.visibility = View.GONE
            receiveStopButton.visibility = View.GONE
            sendButton.visibility = View.VISIBLE
            receiveButton.visibility = View.VISIBLE
            sendReceiveParent.showFade(300)
        }
    }

    private fun invalidateTransferProgressBar(
        progress: String,
        lastProgress: Long,
        fileName: String
    ) {
        if (progress == "-1L" || progress == "1" || progress == "done") {
            // finish sending
            initTransferFinishViews()
            requireContext().showToastInCenter(resources.getString(R.string.transfer_complete))
        } else {
            _binding?.run {
                searchingProgress.visibility = View.GONE
                searchingText.visibility = View.GONE
                sendReceiveParent.hideFade(400)
                transferInfoParent.visibility = View.VISIBLE
                transferFileText.text = fileName
                transferProgress.visibility = View.VISIBLE
                progress.let {
                    val progressArr = progress.split("/")
                    val done = FileUtils.formatStorageLength(
                        requireContext(), progressArr[0].toLong()
                    )
                    val total = FileUtils.formatStorageLength(
                        requireContext(), progressArr[1].toLong()
                    )
                    val bytesPerSec = (progressArr[0].toLong() - lastProgress) / 4
                    val speed = FileUtils.formatStorageLength(
                        requireContext(), bytesPerSec
                    )
                    transferBytesText.text = "$done / $total ($speed/s)"
                    transferProgress.max = progressArr[1].toInt()
                    transferProgress.progress = progressArr[0].toInt()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun warnTransferInProgress(leaveCallback: () -> Unit) {
        val builder = AlertDialog.Builder(requireActivity(), R.style.Custom_Dialog_Dark)
        builder.setTitle(resources.getString(R.string.transfer_in_progress_title))
            .setMessage(resources.getString(R.string.transfer_in_progress_summary))
            .setCancelable(false)
            .setPositiveButton(
                resources.getString(R.string.leave)
            ) { dialog, _ ->
                leaveCallback.invoke()
                dialog.dismiss()
            }
            .setNegativeButton(
                resources.getString(R.string.stay)
            ) { dialog, _ ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    fun resetViewsOnDisconnect() {
        viewModel.isConnectedToPeer = false
        viewModel.isTransferInProgress = false
        viewModel.groupOwnerIP = null
        viewModel.selfIP = null
        viewModel.peerIP = null
        viewModel.serverHandshakeSocket?.close()
        viewModel.clientHandshakeSocket?.close()
        viewModel.serverTransferSocket?.close()
        viewModel.clientTransferSocket?.close()

        _binding?.run {
            devicesParent.removeAllViews()
            stopScanButton.visibility = View.GONE
            searchingText.visibility = View.VISIBLE
            searchingText.text = resources.getString(R.string.start_scan_both_devices)
            deviceStatus.visibility = View.GONE
            deviceName.visibility = View.GONE
            searchingProgress.visibility = View.GONE
            sendReceiveParent.visibility = View.GONE
            transferInfoParent.visibility = View.GONE
            scanButton.visibility = View.VISIBLE
        }
    }

    val initConnectionTimer = object : CountDownTimer(30000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            if (!viewModel.isConnectedToPeer || !viewModel.isTransferInProgress) {
                _binding?.run {
                    devicesParent.removeAllViews()
                    scanButton.visibility = View.GONE
                    searchingProgress.visibility = View.VISIBLE
                    searchingText.visibility = View.VISIBLE
                    searchingText.text = resources
                        .getString(R.string.connecting_please_wait)
                        .format(abs(millisUntilFinished / 1000))
                }
            }
        }

        override fun onFinish() {
            if (!viewModel.isConnectedToPeer || !viewModel.isTransferInProgress) {
                log.warn("Handshake connection timeout")
                failedToHandshake()
            }
        }
    }

    private fun failedToHandshake() {
        log.warn("Handshake failed")
        requireContext().showToastInCenter(
            resources
                .getString(R.string.failed_to_handshake)
        )
        this.resetViewsOnDisconnect()
    }

    private fun getPeerButton(device: WifiP2pDevice): Button {
        val button = getSelectedTextButton(device.deviceName)
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress
        config.wps.setup = WpsInfo.PBC
        button.setOnClickListener {
            requireContext().showToastInCenter(resources.getString(R.string.connecting))
            _binding?.run {
                searchingText.visibility = View.VISIBLE
                searchingProgress.visibility = View.VISIBLE
                scanButton.visibility = View.GONE
                devicesParent.removeAllViews()
            }
            initConnectionTimer.start()
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
            viewModel.groupOwnerIP = p0.groupOwnerAddress.hostAddress
            viewModel.selfIP = Utils.wifiIpAddress(requireContext())
            log.info(
                "isGO: ${p0.isGroupOwner}" +
                    " owner ip: ${viewModel.groupOwnerIP}\n " +
                    "selfIP: ${viewModel.groupOwnerIP}"
            )

            if (getTransferViewModel().isConnectedToPeer) {
                /*requireContext().showToastInCenter(
                    resources
                        .getString(R.string.existing_connection)
                )*/
                log.warn("existing connection present!")
            } else {
                viewModel.initHandshake(p0)?.observe(viewLifecycleOwner) {
                    handshakeSuccess ->
                    initConnectionTimer.cancel()
                    if (!handshakeSuccess) {
                        failedToHandshake()
                    } else {
                        log.info(
                            "Handshake success, " +
                                "peer ip: ${viewModel.peerIP}"
                        )
                        requireContext().showToastInCenter(
                            resources
                                .getString(R.string.connection_successful)
                        )
                        initAfterHandshake()
                    }
                }
            }
        }
    }

    private fun initAfterHandshake() {
        _binding?.run {
            scanButton.visibility = View.GONE
            devicesParent.removeAllViews()
            searchingProgress.visibility = View.GONE
            sendReceiveParent.showFade(200)
            searchingText.visibility = View.VISIBLE
            searchingText.text = resources.getString(R.string.connected_send_receive_hint)
        }

        log.info(
            "Connection established with group owner" +
                " id ${viewModel.groupOwnerIP}"
        )
    }

    override fun onPeersAvailable(peers: WifiP2pDeviceList?) {
        // Handle peers list
        log.info("Found peers: $peers")
        peers?.let {
            _binding?.stopScanButton?.visibility = View.GONE
            _binding?.searchingText?.visibility = View.VISIBLE
            if (it.deviceList.isEmpty()) {
                _binding?.searchingText?.text =
                    getString(R.string.no_devices_found)
            } else {
                _binding?.devicesParent?.removeAllViews()
                monitorDiscoveryTime.cancel()
                peers.deviceList.forEach {
                    device ->
                    log.info("Found peer: $device")
                    _binding?.devicesParent?.addView(getPeerButton(device))
                }
                _binding?.searchingText?.text = resources.getString(R.string.devices_present_select)
            }
        }

        _binding?.searchingProgress?.visibility = View.GONE
        _binding?.searchingText?.visibility = View.GONE
    }

    fun updateThisDevice(device: WifiP2pDevice?) {
        device?.also {
            this.device = device
            _binding?.run {
                deviceName.visibility = View.VISIBLE
                deviceStatus.visibility = View.VISIBLE
                deviceName.text = resources.getString(R.string.my_device)
                    .format(device.deviceName)
                deviceStatus.text = resources.getString(R.string.device_status)
                    .format(getDeviceStatus(device.status))
            }
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
