package com.vironite.virogallery.data

import android.net.Uri

data class VideoItem(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val dateModifiedMs: Long = 0L,
    val source: Source,
    val relativePath: String? = null
) {
    enum class Source {
        MEDIA_STORE,
        SAF_TREE
    }

    val extension: String
        get() = displayName.substringAfterLast('.', "").lowercase()

    val isHidden: Boolean
        get() = displayName.startsWith(".") ||
            relativePath?.split('/').orEmpty().any { it.startsWith(".") }
}
