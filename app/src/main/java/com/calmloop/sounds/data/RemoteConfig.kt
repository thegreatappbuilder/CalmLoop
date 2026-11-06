package com.calmloop.sounds.data

import android.content.Context
import com.calmloop.sounds.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class PopupConfig(
    val enabled: Boolean = false,
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val buttonText: String = "OK",
    val linkUrl: String = "",
    val linkText: String = "",
    val showOnce: Boolean = true,
    val delaySeconds: Int = 2,
)

data class AdsConfig(
    val enabled: Boolean = true,
    val banner: Boolean = true,
    val interstitial: Boolean = true,
    /** Reclama pe tot ecranul apare la fiecare N porniri de sunet. */
    val interstitialEveryN: Int = 5,
    /** Pauza minimă (secunde) între două reclame pe tot ecranul. */
    val interstitialMinIntervalSec: Int = 180,
)

data class AppConfig(val ads: AdsConfig = AdsConfig(), val popup: PopupConfig = PopupConfig())

/**
 * Citește fișierul JSON de pe internet (vezi config/app-config.json) la fiecare pornire.
 * Așa poți porni/opri reclamele și popup-urile fără să publici o versiune nouă.
 */
object RemoteConfig {
    private const val PREFS = "remote_config"
    private val _config = MutableStateFlow(AppConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString("cached", null)?.let { cached -> parse(cached)?.let { _config.value = it } }
        val url = BuildConfig.REMOTE_CONFIG_URL
        if (url.isBlank()) return
        scope.launch {
            try {
                val sep = if (url.contains("?")) "&" else "?"
                val conn = URL(url + sep + "t=" + System.currentTimeMillis() / 60000).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.useCaches = false
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val parsed = parse(text)
                if (parsed != null) {
                    _config.value = parsed
                    prefs.edit().putString("cached", text).apply()
                }
            } catch (_: Exception) {
                // fără internet: rămâne configurația salvată anterior
            }
        }
    }

    private fun parse(text: String): AppConfig? = try {
        val root = JSONObject(text)
        val a = root.optJSONObject("ads") ?: JSONObject()
        val p = root.optJSONObject("popup") ?: JSONObject()
        val d = AdsConfig()
        AppConfig(
            ads = AdsConfig(
                enabled = a.optBoolean("enabled", d.enabled),
                banner = a.optBoolean("banner", d.banner),
                interstitial = a.optBoolean("interstitial", d.interstitial),
                interstitialEveryN = a.optInt("interstitial_every_n_plays", d.interstitialEveryN).coerceAtLeast(1),
                interstitialMinIntervalSec = a.optInt("interstitial_min_interval_seconds", d.interstitialMinIntervalSec),
            ),
            popup = PopupConfig(
                enabled = p.optBoolean("enabled", false),
                id = p.optString("id", ""),
                title = p.optString("title", ""),
                message = p.optString("message", ""),
                buttonText = p.optString("button_text", "OK"),
                linkUrl = p.optString("link_url", ""),
                linkText = p.optString("link_text", ""),
                showOnce = p.optBoolean("show_once", true),
                delaySeconds = p.optInt("delay_seconds", 2),
            ),
        )
    } catch (_: Exception) {
        null
    }
}
