/*
 * Copyright (C) 2021-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClient.ProductType.INAPP
import com.android.billingclient.api.BillingClient.ProductType.SUBS
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsParams.Product
import com.android.billingclient.api.QueryPurchasesParams
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
        uniqueId,
    ) {
        this.activity = activity
        this.uniqueId = uniqueId
    }

    private var log: Logger = LoggerFactory.getLogger(Billing::class.java)

    private val inAppSubscriptionList: MutableList<String>
    private val inAppProductList: MutableList<String>
    private var productDetails: ArrayList<ProductDetails> = ArrayList()
    private var fetchedSubs = false
    private var fetchedInApp = false
    private var isPurchaseInApp = false
    private lateinit var activity: AppCompatActivity
    private var purchaseDialog: AlertDialog? = null

    // create new donations client
    private val billingClient: BillingClient

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
        inAppSubscriptionList = ArrayList()
        inAppSubscriptionList.add("subscription_1")
        inAppSubscriptionList.add("subscription_2")
        inAppProductList = ArrayList()
        inAppProductList.add("lifetime_1")
        inAppProductList.add("lifetime_2")
        billingClient =
            BillingClient.newBuilder(context).setListener(this)
                .enablePendingPurchases().build()
    }

    /** True if billing service is connected now.  */
    private var isServiceConnected = false
    private var latestValidPurchase: Purchase? = null

    override fun onPurchasesUpdated(response: BillingResult, purchases: List<Purchase>?) {
        if (response.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            val latestPurchase = handlePurchases(purchases)
            latestValidPurchase = latestPurchase
            latestPurchase?.let {
                if (it.quantity == 1) {
                    // Always returns 1 for BillingClient.SkuType.SUBS
                    log.info("acknowledging subscription")
                    if (latestPurchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (!latestPurchase.isAcknowledged) {
                            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(latestPurchase.purchaseToken)
                            billingClient.acknowledgePurchase(
                                acknowledgePurchaseParams
                                    .build(),
                                acknowledgePurchaseResponseListener,
                            )
                        }
                    }
                }

                // could be greater than 1 for BillingClient.SkuType.INAPP items.
                log.info("consuming in app purchase")
                val consumeParams =
                    ConsumeParams.newBuilder().setPurchaseToken(
                        latestPurchase
                            .purchaseToken,
                    ).build()
                billingClient.consumeAsync(consumeParams, purchaseConsumerListener)
            }
        } else {
            log.warn(
                "failed to acknowledge purchase with response code {} purchases {}",
                response.responseCode,
                purchases?.size,
            )
            activity.getString(R.string.operation_failed).let { activity.showToastInCenter(it) }
        }
    }

    private val acknowledgePurchaseResponseListener = AcknowledgePurchaseResponseListener {
        acknowledgePurchase(it, it.responseCode, latestValidPurchase?.purchaseToken)
    }

    private val purchaseConsumerListener =
        ConsumeResponseListener { responseCode1: BillingResult?,
            purchaseToken: String?, ->
            acknowledgePurchase(
                responseCode1,
                latestValidPurchase?.purchaseState
                    ?: Purchase.PurchaseState.PURCHASED,
                purchaseToken,
            )
        }

    /**
     * subscriptionStatus =
     * @see Purchase.PurchaseState.PURCHASED
     * for lifetime and
     * @see BillingClient.BillingResponseCode.OK for subscriptions for ease of distinction
     */
    private fun acknowledgePurchase(
        responseCode1: BillingResult?,
        subscriptionStatus: Int,
        purchaseToken: String?,
    ) {
        // we consume the purchase, so that user can perform purchase again
        responseCode1?.responseCode?.let {
            responseCode ->
            // mark our cloud function to update subscription status
            if (responseCode == BillingClient.BillingResponseCode.OK) {
                val retrofit = Retrofit.Builder()
                    .baseUrl(TrialValidationApi.CLOUD_FUNCTION_BASE)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(Utils.getOkHttpClient())
                    .build()
                val service = retrofit.create(TrialValidationApi::class.java)
                try {
                    service.postValidation(
                        TrialValidationApi.TrialRequest(
                            TrialValidationApi.AUTH_TOKEN,
                            uniqueId,
                            context.packageName + "_" + BuildConfig.API_REQ_TRIAL_APP_HASH,
                            subscriptionStatus,
                            "$purchaseToken@gplay",
                            isPurchaseInApp,
                        ),
                    )?.execute()?.let { response ->
                        if (response.isSuccessful && response.body() != null) {
                            log.info(
                                "updated subscription state with " +
                                    "response ${response.body()}",
                            )
                            response.body()
                        }
                    }
                } catch (e: Exception) {
                    log.warn("failed to update subscription state for trial validation", e)
                }
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.runOnUiThread {
                        purchaseDialog?.dismiss()
                        Utils.buildSubscriptionPurchasedDialog(activity).show()
                    }
                } else {
                    // do nothing
                }
            } else {
                log.warn(
                    "failed to acknowledge purchase with response {} token {}",
                    responseCode1.responseCode,
                    purchaseToken,
                )
                activity.getString(R.string.operation_failed).let {
                    activity.showToastInCenter(it)
                }
            }
        }
    }

    fun getSubscriptions(resultCallback: () -> Unit) {
        val runnable = Runnable {
            log.info("querying for subscriptions")
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(SUBS).build(),
            ) { _, subscriptions ->
                log.info("found subscriptions {}", subscriptions)
                billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(INAPP).build(),
                ) { _, inApp ->
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

    private fun createProductListFrom(skus: List<String>, @ProductType productType: String):
        List<Product> {
        return skus.map {
            Product.newBuilder().setProductId(it).setProductType(productType).build()
        }
    }

    /** Start a purchase flow  */
    fun initiatePurchaseFlow() {
        val purchaseFlowRequest = Runnable {

            val params = QueryProductDetailsParams.newBuilder()
            params.setProductList(createProductListFrom(inAppSubscriptionList, SUBS))
            billingClient.queryProductDetailsAsync(
                params.build(),
            ) { responseCode: BillingResult, skuDetailsList: List<ProductDetails> ->
                if (skuDetailsList.isNotEmpty()) {
                    // Successfully fetched product details
                    productDetails.addAll(skuDetailsList)
                    fetchedSubs = true
                    popProductsList(responseCode)
                } else {
                    context.showToastInCenter(
                        context.getString(
                            R.string
                                .error_fetching_google_play_product_list,
                        ),
                    )
                    if (BuildConfig.DEBUG) {
                        log.warn(
                            "Error fetching product list - " +
                                "looks like you are running a DEBUG build.",
                        )
                    }
                }
            }
            val paramsInApp = QueryProductDetailsParams.newBuilder()
            paramsInApp.setProductList(createProductListFrom(inAppProductList, INAPP))
            billingClient.queryProductDetailsAsync(
                paramsInApp.build(),
            ) { responseCode: BillingResult, skuDetailsList: List<ProductDetails> ->
                if (skuDetailsList.isNotEmpty()) {
                    // Successfully fetched product details
                    productDetails.addAll(skuDetailsList)
                    fetchedInApp = true
                    popProductsList(responseCode)
                } else {
                    context.showToastInCenter(
                        context.getString(
                            R.string
                                .error_fetching_google_play_product_list,
                        ),
                    )
                    if (BuildConfig.DEBUG) {
                        log.warn(
                            "Error fetching product list - " +
                                "looks like you are running a DEBUG build.",
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
            purchase.products.forEach {
                containsInApp = containsInApp || inAppProductList.contains(it)
            }
            if (latestPurchase == null || containsInApp
            ) {
                latestPurchase = purchase
            }
        }
        if (latestPurchase != null) {
            log.info("found latest purchase {}", latestPurchase)
            val existingTrial = dao.findByDeviceId(uniqueId)
            latestPurchase.products.forEach {
                isPurchaseInApp = isPurchaseInApp || inAppProductList.contains(it)
            }
            val trialStatus = if (isPurchaseInApp) {
                TrialValidationApi.TrialResponse.TRIAL_EXCLUSIVE
            } else {
                TrialValidationApi.TrialResponse.TRIAL_ACTIVE
            }
            if (existingTrial != null) {
                existingTrial.subscriptionStatus = latestPurchase.purchaseState
                existingTrial.purchaseToken = latestPurchase.purchaseToken + "@gplay"
                existingTrial.trialStatus = trialStatus
                dao.insert(existingTrial)
            } else {
                val trial = Trial(
                    uniqueId,
                    trialStatus,
                    Trial.TRIAL_DEFAULT_DAYS,
                    Date(),
                    latestPurchase.purchaseState,
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
     */
    private fun popProductsList(response: BillingResult) {
        if (response.responseCode == BillingClient.BillingResponseCode.OK &&
            productDetails.isNotEmpty() && fetchedInApp && fetchedSubs
        ) {
            showPaymentsDialog()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val rootView: View = AdapterDonationBinding.inflate(
            LayoutInflater.from(
                activity,
            ),
            parent,
            false,
        ).root
        return DonationViewHolder(rootView)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DonationViewHolder && productDetails.size > 0) {
            log.info("display sku details {}", productDetails[position])
            val titleRaw = productDetails[position].title
            holder.TITLE.text = titleRaw.substring(0, titleRaw.indexOf("("))
            if (productDetails[position].productType == INAPP) {
                holder.RENEWAL_CYCLE.text = context.getString(R.string.lifetime_membership)
                holder.PRICE.text =
                    productDetails[position].oneTimePurchaseOfferDetails?.formattedPrice
            } else {
                var cycle = context.getString(R.string.one_year)
                // Crude assumption warning
                if (!productDetails[position].subscriptionOfferDetails?.first()?.pricingPhases
                    ?.pricingPhaseList?.first()?.billingPeriod.equals("P1Y", true)
                ) {
                    cycle = productDetails[position].subscriptionOfferDetails?.first()
                        ?.pricingPhases?.pricingPhaseList?.first()?.billingPeriod.toString()
                }
                holder.RENEWAL_CYCLE.text = context.getString(R.string.renewal_cycle).format(cycle)
                holder.PRICE.text = productDetails[position].subscriptionOfferDetails?.first()
                    ?.pricingPhases?.pricingPhaseList?.first()?.formattedPrice
            }
            holder.SUMMARY.text = productDetails[position].description

            holder.ROOT_VIEW.setOnClickListener { _ ->
                purchaseProduct.purchaseItem(
                    productDetails[position],
                )
                purchaseDialog?.dismiss()
            }
        }
    }

    override fun getItemCount(): Int {
        return productDetails.size
    }

    private interface PurchaseProduct {
        fun purchaseItem(productDetails: ProductDetails)
        fun purchaseCancel()
    }

    private val purchaseProduct: PurchaseProduct = object : PurchaseProduct {
        override fun purchaseItem(productDetails: ProductDetails) {
            // When subscription type, include the offer token too.
            // Crude assumption: there is only one offer available in the given subscription
            val productDetailsParams = if (productDetails.productType == SUBS &&
                !productDetails.subscriptionOfferDetails.isNullOrEmpty() &&
                false == productDetails.subscriptionOfferDetails?.first()?.offerToken?.isEmpty()
            ) {
                ProductDetailsParams
                    .newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(productDetails.subscriptionOfferDetails!!.first().offerToken)
                    .build()
            } else {
                ProductDetailsParams
                    .newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            }
            val billingFlowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(
                listOf(productDetailsParams)
            ).build()
            activity.let {
                billingClient.launchBillingFlow(it, billingFlowParams)
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
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResponse: BillingResult) {
                    log.debug(
                        "Setup finished. Response code: " +
                            billingResponse.responseCode,
                    )
                    if (billingResponse.responseCode == BillingClient.BillingResponseCode.OK) {
                        isServiceConnected = true
                        executeOnSuccess?.run()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isServiceConnected = false
                }
            }
        )
    }

    fun destroyBillingInstance() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    private fun showPaymentsDialog() {
        /*
     * As of Billing library 4.0, all callbacks are running on background thread.
     * Need to use AppConfig.runInApplicationThread() for UI interactions
     *
     *
     */
        if (!activity.isFinishing && !activity.isDestroyed) {
            activity.runOnUiThread {
                val dialogBuilder = AlertDialog.Builder(activity, R.style.Custom_Dialog_Dark)
                    .setTitle(R.string.subscribe)
                    .setNegativeButton(R.string.close) { dialog, _ ->
                        dialog.dismiss()
                    }
                val inflater = activity.layoutInflater
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
