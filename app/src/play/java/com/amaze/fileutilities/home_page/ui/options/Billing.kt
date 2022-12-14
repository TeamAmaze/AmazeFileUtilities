/*
 * Copyright (C) 2021-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class Billing(val context: Context, private var uniqueId: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder?>(),
    PurchasesUpdatedListener {
    constructor(activity: AppCompatActivity, uniqueId: String) : this(
        activity.baseContext,
        uniqueId
    ) {
        this.activity = activity
        this.uniqueId = uniqueId
    }

    var log: Logger = LoggerFactory.getLogger(Billing::class.java)

    private val skuList: MutableList<String>
    private val skuListInApp: MutableList<String>
    private var skuDetails: ArrayList<SkuDetails> = ArrayList()
    private var fetchedSubs = false
    private var fetchedInApp = false
    private var isPurchaseInApp = false
    private var activity: AppCompatActivity? = null
    private var purchaseDialog: AlertDialog? = null

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
        skuListInApp = ArrayList()
        skuListInApp.add("lifetime_1")
        skuListInApp.add("lifetime_2")
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
                        // mark our cloud function to update subscription status
                        val retrofit = Retrofit.Builder()
                            .baseUrl(TrialValidationApi.CLOUD_FUNCTION_BASE)
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(Utils.getOkHttpClient())
                            .build()
                        val service = retrofit.create(TrialValidationApi::class.java)
                        try {
                            service.postValidation(
                                TrialValidationApi.TrialRequest(
                                    TrialValidationApi.AUTH_TOKEN, uniqueId,
                                    context.packageName + "_" + BuildConfig.API_REQ_TRIAL_APP_HASH,
                                    latestPurchase?.purchaseState
                                        ?: Trial.SUBSCRIPTION_STATUS_DEFAULT,
                                    latestPurchase?.purchaseToken + "@gplay", isPurchaseInApp
                                )
                            )?.execute()?.let { response ->
                                if (response.isSuccessful && response.body() != null) {
                                    log.info(
                                        "updated subscription state with " +
                                            "response ${response.body()}"
                                    )
                                    response.body()
                                }
                            }
                        } catch (e: Exception) {
                            log.warn("failed to update subscription state for trial validation", e)
                        }
                        if (activity?.isFinishing == false && activity?.isDestroyed == false) {
                            activity?.runOnUiThread {
                                purchaseDialog?.dismiss()
                                Utils.buildSubscriptionPurchasedDialog(activity!!).show()
                            }
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

    fun getSubscriptions(resultCallback: () -> Unit) {
        val runnable = Runnable {
            log.info("querying for subscriptions")
            billingClient?.queryPurchasesAsync(
                BillingClient.SkuType.SUBS
            ) { p0, subscriptions ->
                log.info("found subscriptions {}", subscriptions)
                billingClient?.queryPurchasesAsync(
                    BillingClient.SkuType.INAPP
                ) { p0, inApp ->
                    log.info("found in app purchases {}", inApp)
                    val purchases = ArrayList<Purchase>()
                    purchases.addAll(subscriptions)
                    purchases.addAll(inApp)
                    handlePurchases(purchases)
                    resultCallback.invoke()
                }
            }
        }
        executeGetSubscriptions(runnable)
    }

    private fun executeGetSubscriptions(runnable: Runnable) {
        if (isServiceConnected) {
            runnable.run()
        } else {
            // If billing service was disconnected, we try to reconnect 1 time.
            // (feel free to introduce your retry policy here).
            startServiceConnection(runnable)
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
                    skuDetails.addAll(skuDetailsList)
                    fetchedSubs = true
                    popProductsList(responseCode)
                } else {
                    context.showToastInCenter(
                        context.getString(
                            R.string
                                .error_fetching_google_play_product_list
                        )
                    )
                    if (BuildConfig.DEBUG) {
                        log.warn(
                            "Error fetching product list - " +
                                "looks like you are running a DEBUG build."
                        )
                    }
                }
            }
            val paramsInApp = SkuDetailsParams.newBuilder()
            paramsInApp.setSkusList(skuListInApp).setType(BillingClient.SkuType.INAPP)
            billingClient!!.querySkuDetailsAsync(
                paramsInApp.build()
            ) { responseCode: BillingResult, skuDetailsList: List<SkuDetails>? ->
                if (skuDetailsList != null && skuDetailsList.isNotEmpty()) {
                    // Successfully fetched product details
                    skuDetails.addAll(skuDetailsList)
                    fetchedInApp = true
                    popProductsList(responseCode)
                } else {
                    context.showToastInCenter(
                        context.getString(
                            R.string
                                .error_fetching_google_play_product_list
                        )
                    )
                    if (BuildConfig.DEBUG) {
                        log.warn(
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
            log.info("querying purchase {}", purchase)
            var containsInApp = false
            purchase.skus.forEach {
                containsInApp = containsInApp || skuListInApp.contains(it)
            }
            if (latestPurchase == null || containsInApp
            ) {
                latestPurchase = purchase
            }
        }
        if (latestPurchase != null) {
            log.info("found latest purchase {}", latestPurchase)
            val existingTrial = dao.findByDeviceId(uniqueId)
            latestPurchase.skus.forEach {
                isPurchaseInApp = isPurchaseInApp || skuListInApp.contains(it)
            }
            val trialStatus = if (isPurchaseInApp)
                TrialValidationApi.TrialResponse.TRIAL_EXCLUSIVE
            else TrialValidationApi.TrialResponse.TRIAL_ACTIVE
            if (existingTrial != null) {
                existingTrial.subscriptionStatus = latestPurchase.purchaseState
                existingTrial.purchaseToken = latestPurchase.purchaseToken + "@gplay"
                existingTrial.trialStatus = trialStatus
                dao.insert(existingTrial)
            } else {
                val trial = Trial(
                    uniqueId,
                    trialStatus,
                    Trial.TRIAL_DEFAULT_DAYS, Date(), latestPurchase.purchaseState
                )
                trial.purchaseToken = latestPurchase.purchaseToken + "@gplay"
                dao.insert(trial)
            }
        } else {
            // no subscription found, expire existing
            log.info("no subscription found")
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
    private fun popProductsList(response: BillingResult) {
        if (response.responseCode == BillingClient.BillingResponseCode.OK &&
            skuDetails.isNotEmpty() && fetchedInApp && fetchedSubs
        ) {
            showPaymentsDialog()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val rootView: View = AdapterDonationBinding.inflate(
            LayoutInflater.from(
                activity
            ),
            parent, false
        ).getRoot()
        return DonationViewHolder(rootView)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DonationViewHolder && skuDetails.size > 0) {
            log.info("display sku details {}", skuDetails[position])
            val titleRaw = skuDetails[position].title
            holder.TITLE.text = titleRaw.substring(0, titleRaw.indexOf("("))
            if (skuDetails[position].type == BillingClient.SkuType.INAPP) {
                holder.RENEWAL_CYCLE.text = context.getString(R.string.lifetime_membership)
            } else {
                var cycle = context.getString(R.string.one_year)
                if (!skuDetails[position].subscriptionPeriod.equals("P1Y", true)) {
                    cycle = skuDetails[position].subscriptionPeriod
                }
                holder.RENEWAL_CYCLE.text = context.getString(R.string.renewal_cycle)
                    .format(cycle)
            }
            holder.SUMMARY.text = skuDetails[position].description
            holder.PRICE.text = skuDetails[position].price
            holder.ROOT_VIEW.setOnClickListener { v ->
                purchaseProduct.purchaseItem(
                    skuDetails[position]
                )
                purchaseDialog?.dismiss()
            }
        }
    }

    override fun getItemCount(): Int {
        return skuDetails.size
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
        if (activity?.isFinishing == false && activity?.isDestroyed == false) {
            activity?.runOnUiThread {
                val dialogBuilder = AlertDialog.Builder(activity!!, R.style.Custom_Dialog_Dark)
                    .setTitle(R.string.subscribe)
                    .setNegativeButton(R.string.close) { dialog, _ ->
                        dialog.dismiss()
                    }
                val inflater = activity!!.layoutInflater
                val dialogView: View = inflater
                    .inflate(R.layout.subtitles_search_results_view, null)
                dialogBuilder.setView(dialogView)
                val recyclerView = dialogView.findViewById<RecyclerView>(R.id.search_results_list)
                dialogBuilder.setOnCancelListener {
                    purchaseProduct.purchaseCancel()
                }
                recyclerView.visibility = View.VISIBLE
                recyclerView.layoutManager = LinearLayoutManager(activity)
                recyclerView.adapter = this
                purchaseDialog = dialogBuilder.create()
                purchaseDialog?.show()
            }
        }
    }
}
