package com.calmloop.sounds.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.calmloop.sounds.BuildConfig
import com.calmloop.sounds.data.AdsConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reclame AdMob + formularul de consimțământ GDPR (obligatoriu pentru utilizatorii din UE).
 */
object AdsManager {
    private val initStarted = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private var interstitial: InterstitialAd? = null
    private var loading = false
    private var actionCount = 0
    private var lastShownAt = 0L

    fun gatherConsent(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    updatePrivacyFlag(info)
                    if (info.canRequestAds()) initialize(activity)
                }
            },
            { _ ->
                if (info.canRequestAds()) initialize(activity)
            },
        )
        // Consimțământ dat într-o sesiune anterioară: putem porni imediat
        updatePrivacyFlag(info)
        if (info.canRequestAds()) initialize(activity)
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> }
    }

    private fun updatePrivacyFlag(info: ConsentInformation) {
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun initialize(activity: Activity) {
        if (initStarted.getAndSet(true)) return
        val ctx = activity.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(ctx) {}
            withContext(Dispatchers.Main) {
                _ready.value = true
                loadInterstitial(ctx)
            }
        }
    }

    private fun loadInterstitial(context: Context) {
        if (loading || interstitial != null) return
        loading = true
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    loading = false
                }
            },
        )
    }

    /** Se apelează când utilizatorul pornește un sunet; arată reclama după regulile din config. */
    fun onUserAction(activity: Activity, config: AdsConfig) {
        if (!_ready.value || !config.enabled || !config.interstitial) return
        actionCount++
        val now = SystemClock.elapsedRealtime()
        val intervalOk = lastShownAt == 0L || now - lastShownAt >= config.interstitialMinIntervalSec * 1000L
        if (actionCount < config.interstitialEveryN || !intervalOk) return
        val ad = interstitial
        if (ad == null) {
            loadInterstitial(activity.applicationContext)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                loadInterstitial(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                loadInterstitial(activity.applicationContext)
            }
        }
        actionCount = 0
        lastShownAt = now
        ad.show(activity)
    }
}
