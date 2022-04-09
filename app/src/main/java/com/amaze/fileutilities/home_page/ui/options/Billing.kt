/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.options

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.BuildConfig
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.AdapterDonationBinding
import com.amaze.fileutilities.home_page.database.AppDatabase
import com.amaze.fileutilities.home_page.database.Trial
import com.amaze.fileutilities.home_page.ui.files.TrialValidationApi
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences
import com.amaze.fileutilities.utilis.showToastInCenter
import com.android.billingclient.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class Billing(val context: Context, private var uniqueId: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder?>(),
    PurchasesUpdatedListener {
    constructor(activity: AppCompatActivity, uniqueId: String) : this(
        activity.applicationContext,
        uniqueId
    ) {
        this.activity = activity
        this.uniqueId = uniqueId
    }

    private val skuList: MutableList<String>
    private var skuDetails: List<SkuDetails>? = null
    private var activity: AppCompatActivity? = null

    // create new donations client
    private var billingClient: BillingClient? = null

    companion object {
        private val TAG = Billing::class.java.simpleName

        fun getInstance(activity: AppCompatActivity): Billing? {
            var billing: Billing? = null
            val deviceId = activity.getAppCommonSharedPreferences()
                .getString(PreferencesConstants.KEY_DEVICE_UNIQUE_ID, null)
            deviceId?.let {
                billing = Billing(activity, deviceId)
            }
            return billing
        }

        fun getInstance(context: Context): Billing? {
            var billing: Billing? = null
            val deviceId = context.getAppCommonSharedPreferences()
                .getString(PreferencesConstants.KEY_DEVICE_UNIQUE_ID, null)
            deviceId?.let {
                billing = Billing(context, deviceId)
            }
            return billing
        }
    }

    init {
        skuList = ArrayList()
        skuList.add("subscription_1")
        skuList.add("subscription_2")
        skuList.add("subscription_3")
        skuList.add("subscription_4")
        billingClient =
            BillingClient.newBuilder(context).setListener(this)
                .enablePendingPurchases().build()
    }

    /** True if billing service is connected now.  */
    private var isServiceConnected = false
    override fun onPurchasesUpdated(response: BillingResult, purchases: List<Purchase>?) {
        if (response.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            val latestPurchase = handlePurchases(purchases)
            val listener =
                ConsumeResponseListener { responseCode1: BillingResult?,
                    purchaseToken: String? ->
                    // we consume the purchase, so that user can perform purchase again
                    responseCode1?.responseCode?.let {
                        responseCode ->
                        activity?.runOnUiThread {
                            Utils.buildSubscriptionPurchasedDialog(context)
                            // mark our cloud function to update subscription status
                        }
                    }
                }
            latestPurchase?.let {
                val consumeParams =
                    ConsumeParams.newBuilder().setPurchaseToken(
                        latestPurchase
                            .purchaseToken
                    ).build()
                billingClient!!.consumeAsync(consumeParams, listener)
            }
        }
    }

    fun getSubscriptions(resultCallback: suspend () -> Unit) {
        billingClient?.queryPurchasesAsync(
            BillingClient.SkuType.SUBS
        ) { p0, p1 ->
            handlePurchases(p1)
            GlobalScope.launch(Dispatchers.Default) { resultCallback.invoke() }
        }
    }

    /** Start a purchase flow  */
    fun initiatePurchaseFlow() {
        val purchaseFlowRequest = Runnable {
            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)
            billingClient!!.querySkuDetailsAsync(
                params.build()
            ) { responseCode: BillingResult, skuDetailsList: List<SkuDetails>? ->
                if (skuDetailsList != null && skuDetailsList.isNotEmpty()) {
                    // Successfully fetched product details
                    skuDetails = skuDetailsList
                    popProductsList(responseCode, skuDetailsList)
                } else {
                    context.showToastInCenter(
                        context.getString(
                            R.string
                                .error_fetching_google_play_product_list
                        )
                    )
                    if (BuildConfig.DEBUG) {
                        Log.w(
                            TAG,
                            "Error fetching product list - " +
                                "looks like you are running a DEBUG build."
                        )
                    }
                }
            }
        }
        executeServiceRequest(purchaseFlowRequest)
    }

    private fun handlePurchases(purchases: List<Purchase>): Purchase? {
        val dao = AppDatabase.getInstance(context).trialValidatorDao()
        var latestPurchase: Purchase? = null
        for (purchase in purchases) {
            if (latestPurchase == null ||
                purchase.purchaseTime > latestPurchase.purchaseTime
            ) {
                latestPurchase = purchase
            }
        }
        if (latestPurchase != null) {
            val existingTrial = dao.findByDeviceId(uniqueId)
            if (existingTrial != null) {
                existingTrial.subscriptionStatus = latestPurchase.purchaseState
                existingTrial.purchaseToken = latestPurchase.purchaseToken
                dao.insert(existingTrial)
            } else {
                val trial = Trial(
                    uniqueId,
                    TrialValidationApi.TrialResponse.TRIAL_ACTIVE,
                    Trial.TRIAL_DEFAULT_DAYS, Date(), latestPurchase.purchaseState
                )
                trial.purchaseToken = latestPurchase.purchaseToken
                dao.insert(trial)
            }
        } else {
            // no subscription found, expire existing
            val existingTrial = dao.findByDeviceId(uniqueId)
            if (existingTrial != null) {
                existingTrial.subscriptionStatus = Trial.SUBSCRIPTION_STATUS_DEFAULT
                existingTrial.purchaseToken = null
                dao.insert(existingTrial)
            }
        }
        return latestPurchase
    }

    /**
     * Got products list from play store, pop their details
     *
     * @param response
     * @param skuDetailsList
     */
    private fun popProductsList(response: BillingResult, skuDetailsList: List<SkuDetails>?) {
        if (response.responseCode == BillingClient.BillingResponseCode.OK &&
            skuDetailsList != null
        ) {
            showPaymentsDialog()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val rootView: View = AdapterDonationBinding.inflate(
            LayoutInflater.from(
                context
            ),
            parent, false
        ).getRoot()
        return DonationViewHolder(rootView)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DonationViewHolder && skuDetails!!.size > 0) {
            val titleRaw = skuDetails!![position].title
            (holder as DonationViewHolder).TITLE.setText(
                titleRaw.subSequence(
                    0,
                    titleRaw.lastIndexOf("(")
                )
            )
            (holder as DonationViewHolder).SUMMARY.setText(skuDetails!![position].description)
            (holder as DonationViewHolder).PRICE.setText(skuDetails!![position].price)
            (holder as DonationViewHolder).ROOT_VIEW.setOnClickListener { v ->
                purchaseProduct.purchaseItem(
                    skuDetails!![position]
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return skuList.size
    }

    private interface PurchaseProduct {
        fun purchaseItem(skuDetails: SkuDetails?)
        fun purchaseCancel()
    }

    private val purchaseProduct: PurchaseProduct = object : PurchaseProduct {
        override fun purchaseItem(skuDetails: SkuDetails?) {
            val billingFlowParams = BillingFlowParams.newBuilder().setSkuDetails(
                skuDetails!!
            ).build()
            activity?.let {
                billingClient!!.launchBillingFlow(it, billingFlowParams)
            }
        }

        override fun purchaseCancel() {
            destroyBillingInstance()
        }
    }

    /**
     * We executes a connection request to Google Play
     *
     * @param runnable
     */
    private fun executeServiceRequest(runnable: Runnable) {
        if (isServiceConnected) {
            runnable.run()
        } else {
            // If billing service was disconnected, we try to reconnect 1 time.
            // (feel free to introduce your retry policy here).
            startServiceConnection(runnable)
        }
    }

    /**
     * Starts a connection to Google Play services
     *
     * @param executeOnSuccess
     */
    private fun startServiceConnection(executeOnSuccess: Runnable?) {
        billingClient!!.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResponse: BillingResult) {
                    Log.d(
                        TAG,
                        "Setup finished. Response code: " +
                            billingResponse.responseCode
                    )
                    if (billingResponse.responseCode == BillingClient.BillingResponseCode.OK) {
                        isServiceConnected = true
                        executeOnSuccess?.run()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isServiceConnected = false
                }
            })
    }

    fun destroyBillingInstance() {
        if (billingClient != null && billingClient!!.isReady) {
            billingClient!!.endConnection()
            billingClient = null
        }
    }

    private fun showPaymentsDialog() {
        /*
     * As of Billing library 4.0, all callbacks are running on background thread.
     * Need to use AppConfig.runInApplicationThread() for UI interactions
     *
     *
     */
        activity?.runOnUiThread {
            val dialogBuilder = AlertDialog.Builder(context)
                .setTitle(R.string.subscribe)
                .setNegativeButton(R.string.close) { dialog, _ ->
                    dialog.dismiss()
                }
            val inflater = activity!!.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.subtitles_search_results_view, null)
            dialogBuilder.setView(dialogView)
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.search_results_list)
            dialogBuilder.setOnCancelListener {
                purchaseProduct.purchaseCancel()
            }
            recyclerView.visibility = View.VISIBLE
            recyclerView.layoutManager = LinearLayoutManager(activity)
            recyclerView.adapter = this
            val alertDialog = dialogBuilder.create()
            alertDialog.show()
        }
    }
}
