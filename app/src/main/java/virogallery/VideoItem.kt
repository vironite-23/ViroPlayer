package virogallery

import android.net.Uri
import java.io.Serializable

data class VideoItem(
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val dateAdded: Long,
    val path: String? = null
) : Serializable
