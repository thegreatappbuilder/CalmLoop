package com.calmloop.sounds.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.calmloop.sounds.data.PopupConfig
import kotlinx.coroutines.delay

private object PopupSession {
    val shownThisSession = mutableSetOf<String>()

    fun seen(context: Context, id: String): Boolean =
        context.getSharedPreferences("popups", Context.MODE_PRIVATE).getBoolean(id, false)

    fun markSeen(context: Context, id: String) {
        context.getSharedPreferences("popups", Context.MODE_PRIVATE).edit().putBoolean(id, true).apply()
    }
}

/** Afișează popup-ul definit în config/app-config.json (vezi README). */
@Composable
fun RemotePopup(popup: PopupConfig) {
    val context = LocalContext.current
    var visible by remember(popup.id) { mutableStateOf(false) }

    LaunchedEffect(popup.id, popup.enabled) {
        val eligible = popup.enabled && popup.id.isNotBlank() &&
            popup.id !in PopupSession.shownThisSession &&
            (!popup.showOnce || !PopupSession.seen(context, popup.id))
        if (eligible) {
            delay(popup.delaySeconds.coerceAtLeast(0) * 1000L)
            PopupSession.shownThisSession.add(popup.id)
            PopupSession.markSeen(context, popup.id)
            visible = true
        }
    }

    if (!visible) return
    val titleSlot: (@Composable () -> Unit)? =
        if (popup.title.isNotBlank()) {
            { Text(popup.title) }
        } else {
            null
        }
    val linkSlot: (@Composable () -> Unit)? =
        if (popup.linkUrl.isNotBlank()) {
            {
                TextButton(onClick = {
                    visible = false
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(popup.linkUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    } catch (_: Exception) {
                    }
                }) { Text(popup.linkText.ifBlank { "Open" }) }
            }
        } else {
            null
        }
    AlertDialog(
        onDismissRequest = { visible = false },
        containerColor = MaterialTheme.colorScheme.surface,
        title = titleSlot,
        text = { Text(popup.message) },
        confirmButton = {
            TextButton(onClick = { visible = false }) { Text(popup.buttonText.ifBlank { "OK" }) }
        },
        dismissButton = linkSlot,
    )
}
