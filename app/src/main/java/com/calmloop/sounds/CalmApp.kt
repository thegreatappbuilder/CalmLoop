package com.calmloop.sounds

import android.app.Application
import com.calmloop.sounds.data.RemoteConfig
import com.calmloop.sounds.data.TrackStore
import com.calmloop.sounds.playback.PlaybackController

class CalmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PlaybackController.init(this)
        TrackStore.init(this)
        RemoteConfig.init(this)
    }
}
