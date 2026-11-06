package com.calmloop.sounds.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmloop.sounds.BuildConfig
import com.calmloop.sounds.R
import com.calmloop.sounds.ads.AdsManager
import com.calmloop.sounds.ads.BannerAd
import com.calmloop.sounds.audio.Category
import com.calmloop.sounds.audio.SoundCatalog
import com.calmloop.sounds.audio.SoundDef
import com.calmloop.sounds.data.RemoteConfig
import com.calmloop.sounds.data.TrackStore
import com.calmloop.sounds.data.UserTrack
import com.calmloop.sounds.playback.PlaybackController
import com.calmloop.sounds.playback.PlaybackState
import com.calmloop.sounds.ui.theme.ActiveBrush
import com.calmloop.sounds.ui.theme.BackgroundBrush
import com.calmloop.sounds.ui.theme.CardIdle
import com.calmloop.sounds.ui.theme.Lavender
import com.calmloop.sounds.ui.theme.Mint
import com.calmloop.sounds.ui.theme.Moon
import com.calmloop.sounds.ui.theme.Night
import com.calmloop.sounds.ui.theme.Sky
import com.calmloop.sounds.ui.theme.TextSoft
import kotlinx.coroutines.delay

private fun iconFor(id: String): ImageVector = when (id) {
    "rain" -> Icons.Rounded.Umbrella
    "tropical" -> Icons.Rounded.Forest
    "thunder" -> Icons.Rounded.Thunderstorm
    "ocean" -> Icons.Rounded.Waves
    "wind" -> Icons.Rounded.Air
    "stream" -> Icons.Rounded.Water
    "fire" -> Icons.Rounded.LocalFireDepartment
    "white" -> Icons.Rounded.GraphicEq
    "pink" -> Icons.Rounded.Grain
    "brown" -> Icons.Rounded.BlurOn
    "meditation" -> Icons.Rounded.SelfImprovement
    "bowl" -> Icons.Rounded.Spa
    "binaural" -> Icons.Rounded.Headphones
    else -> Icons.Rounded.MusicNote
}

@Composable
fun MainScreen(activity: Activity) {
    val context = LocalContext.current
    val state by PlaybackController.state.collectAsStateWithLifecycle()
    val config by RemoteConfig.config.collectAsStateWithLifecycle()
    val adsReady by AdsManager.ready.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showTimer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var askedNotifications by rememberSaveable { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(state.isActive) {
        if (state.isActive && !askedNotifications && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askedNotifications = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val trackErrorText = stringResource(R.string.track_error)
    LaunchedEffect(state.trackError) {
        if (state.trackError) {
            Toast.makeText(context, trackErrorText, Toast.LENGTH_LONG).show()
            PlaybackController.clearTrackError()
        }
    }

    RemotePopup(config.popup)

    val onStarted: () -> Unit = { AdsManager.onUserAction(activity, config.ads) }

    Box(
        Modifier
            .fillMaxSize()
            .background(BackgroundBrush),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Column {
                    AnimatedVisibility(visible = state.isActive) {
                        NowPlayingBar(state, onTimer = { showTimer = true })
                    }
                    if (adsReady && config.ads.enabled && config.ads.banner) {
                        BannerAd(BuildConfig.ADMOB_BANNER_ID)
                    }
                    NavigationBar(containerColor = Night.copy(alpha = 0.92f)) {
                        val itemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Night,
                            indicatorColor = Sky,
                            unselectedIconColor = TextSoft,
                            unselectedTextColor = TextSoft,
                            selectedTextColor = Moon,
                        )
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Rounded.GraphicEq, null) },
                            label = { Text(stringResource(R.string.tab_sounds)) },
                            colors = itemColors,
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Rounded.LibraryMusic, null) },
                            label = { Text(stringResource(R.string.tab_music)) },
                            colors = itemColors,
                        )
                    }
                }
            },
        ) { padding ->
            val header: @Composable () -> Unit = {
                Header(
                    title = stringResource(if (tab == 0) R.string.header_sounds else R.string.tab_music),
                    subtitle = stringResource(if (tab == 0) R.string.header_sounds_sub else R.string.music_add_hint),
                    onTimer = { showTimer = true },
                    onSettings = { showSettings = true },
                )
            }
            if (tab == 0) {
                SoundsTab(padding, state, header, onStarted)
            } else {
                MusicTab(padding, state, header, onStarted)
            }
        }
    }

    if (showTimer) TimerDialog(state.timerEndsAt != null, onDismiss = { showTimer = false })
    if (showSettings) SettingsDialog(activity, onDismiss = { showSettings = false })
}

@Composable
private fun Header(title: String, subtitle: String, onTimer: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.padding(top = 8.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = Moon,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onTimer) { Icon(Icons.Rounded.Bedtime, stringResource(R.string.timer), tint = Moon) }
            IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = Moon) }
        }
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSoft)
    }
}

// ----------------------------------------------------------------------- Sunete

@Composable
private fun SoundsTab(
    padding: PaddingValues,
    state: PlaybackState,
    header: @Composable () -> Unit,
    onStarted: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 20.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { header() }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                SectionTitle(stringResource(R.string.preset_title))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(SoundCatalog.presets) { preset ->
                        val active = state.sounds.keys == preset.sounds.map { it.first }.toSet()
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) ActiveBrush else SolidColor(CardIdle))
                                .border(1.dp, if (active) Sky else Color(0x1FFFFFFF), RoundedCornerShape(50))
                                .clickable {
                                    PlaybackController.applyPreset(preset.sounds)
                                    onStarted()
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                        ) {
                            Text(stringResource(preset.name), color = Moon, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        for (category in Category.entries) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "cat_" + category.name) {
                SectionTitle(stringResource(category.title))
            }
            items(SoundCatalog.all.filter { it.category == category }, key = { it.id }) { def ->
                SoundCard(
                    def = def,
                    volume = state.sounds[def.id],
                    onToggle = {
                        val starting = !state.sounds.containsKey(def.id)
                        PlaybackController.toggleSound(def.id)
                        if (starting) onStarted()
                    },
                    onVolume = { PlaybackController.setSoundVolume(def.id, it) },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = Moon,
        modifier = Modifier.padding(top = 12.dp, bottom = 10.dp),
    )
}

@Composable
private fun SoundCard(def: SoundDef, volume: Float?, onToggle: () -> Unit, onVolume: (Float) -> Unit) {
    val active = volume != null
    val shape = RoundedCornerShape(24.dp)
    val iconBg by animateColorAsState(if (active) Moon else Color(0x1FFFFFFF), label = "iconBg")
    val iconTint by animateColorAsState(if (active) Night else Moon, label = "iconTint")
    Column(
        Modifier
            .height(148.dp)
            .clip(shape)
            .background(if (active) ActiveBrush else SolidColor(CardIdle))
            .border(1.dp, if (active) Sky.copy(alpha = 0.7f) else Color(0x14FFFFFF), shape)
            .clickable(onClick = onToggle)
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 4.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(iconFor(def.id), null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(def.name),
            style = MaterialTheme.typography.titleSmall,
            color = Moon,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (volume != null) {
            Slider(
                value = volume,
                onValueChange = onVolume,
                valueRange = 0.05f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Moon,
                    activeTrackColor = Moon,
                    inactiveTrackColor = Color(0x33FFFFFF),
                ),
            )
        } else {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.tap_to_play), style = MaterialTheme.typography.bodySmall, color = TextSoft)
        }
    }
}

// ----------------------------------------------------------------------- Muzica mea

@Composable
private fun MusicTab(
    padding: PaddingValues,
    state: PlaybackState,
    header: @Composable () -> Unit,
    onStarted: () -> Unit,
) {
    val tracks by TrackStore.tracks.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        TrackStore.add(uris)
    }
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { header() }
        item {
            Button(
                onClick = { picker.launch(arrayOf("audio/*")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Sky, contentColor = Night),
            ) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.music_add), style = MaterialTheme.typography.titleSmall)
            }
        }
        if (state.track != null) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardIdle)
                        .padding(16.dp),
                ) {
                    Text(stringResource(R.string.music_volume), color = Moon, style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = state.trackVolume,
                        onValueChange = { PlaybackController.setTrackVolume(it) },
                        valueRange = 0.05f..1f,
                        colors = SliderDefaults.colors(thumbColor = Moon, activeTrackColor = Lavender),
                    )
                }
            }
        }
        if (tracks.isEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.MusicNote, null, tint = TextSoft, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.music_empty), color = TextSoft)
                }
            }
        }
        items(tracks, key = { it.uri }) { track ->
            TrackRow(
                track = track,
                playing = state.track?.uri == track.uri,
                onPlay = {
                    if (state.track?.uri == track.uri) {
                        PlaybackController.stopTrack()
                    } else {
                        PlaybackController.playTrack(track)
                        onStarted()
                    }
                },
                onDelete = {
                    if (state.track?.uri == track.uri) PlaybackController.stopTrack()
                    TrackStore.remove(track)
                },
            )
        }
    }
}

@Composable
private fun TrackRow(track: UserTrack, playing: Boolean, onPlay: () -> Unit, onDelete: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (playing) ActiveBrush else SolidColor(CardIdle))
            .border(1.dp, if (playing) Sky.copy(alpha = 0.7f) else Color(0x14FFFFFF), shape)
            .clickable(onClick = onPlay)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (playing) Moon else Color(0x1FFFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                null,
                tint = if (playing) Night else Moon,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.name, color = Moon, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.music_loop), color = TextSoft, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = TextSoft)
        }
    }
}

// ----------------------------------------------------------------------- Bara de redare

@Composable
private fun rememberRemaining(endsAt: Long?): String? {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAt) {
        while (endsAt != null) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    if (endsAt == null) return null
    val sec = ((endsAt - now) / 1000).coerceAtLeast(0)
    return "%d:%02d".format(sec / 60, sec % 60)
}

@Composable
private fun NowPlayingBar(state: PlaybackState, onTimer: () -> Unit) {
    val remaining = rememberRemaining(state.timerEndsAt)
    val count = state.sounds.size + if (state.track != null) 1 else 0
    Surface(
        color = Color(0xF21B1D47),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, Color(0x1FFFFFFF)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (state.paused) stringResource(R.string.notif_paused) else stringResource(R.string.now_playing, count),
                    color = Moon,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    if (remaining != null) stringResource(R.string.timer_left, remaining) else stringResource(R.string.timer_hint),
                    color = if (remaining != null) Mint else TextSoft,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onTimer) { Icon(Icons.Rounded.Bedtime, stringResource(R.string.timer), tint = Moon) }
            FilledIconButton(
                onClick = { PlaybackController.togglePause() },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Moon, contentColor = Night),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    if (state.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    stringResource(if (state.paused) R.string.action_resume else R.string.action_pause),
                )
            }
            IconButton(onClick = { PlaybackController.stopAll() }) {
                Icon(Icons.Rounded.Stop, stringResource(R.string.stop_all), tint = Moon)
            }
        }
    }
}

// ----------------------------------------------------------------------- Dialoguri

@Composable
private fun TimerDialog(hasTimer: Boolean, onDismiss: () -> Unit) {
    val options = listOf(15, 30, 45, 60, 90, 120)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.timer)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.timer_desc), color = TextSoft, style = MaterialTheme.typography.bodyMedium)
                options.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { min ->
                            OutlinedButton(
                                onClick = {
                                    PlaybackController.setTimer(min)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(stringResource(R.string.timer_minutes, min), color = Moon)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (hasTimer) {
                TextButton(onClick = {
                    PlaybackController.setTimer(null)
                    onDismiss()
                }) { Text(stringResource(R.string.timer_off)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun SettingsDialog(activity: Activity, onDismiss: () -> Unit) {
    val privacyRequired by AdsManager.privacyOptionsRequired.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (privacyRequired) {
                    OutlinedButton(onClick = { AdsManager.showPrivacyOptions(activity) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.privacy_options), color = Moon)
                    }
                }
                if (BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            try {
                                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL)))
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.privacy_policy), color = Moon)
                    }
                }
                Text(
                    stringResource(R.string.version, BuildConfig.VERSION_NAME),
                    color = TextSoft,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
