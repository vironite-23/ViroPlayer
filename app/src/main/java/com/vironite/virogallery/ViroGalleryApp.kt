package com.vironite.virogallery

import android.app.Application
import com.vironite.virogallery.data.VideoRepository

class ViroGalleryApp : Application() {
    val videoRepository: VideoRepository by lazy {
        VideoRepository(this)
    }
}
