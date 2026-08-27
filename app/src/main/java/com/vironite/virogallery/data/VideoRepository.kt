package com.vironite.virogallery.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoRepository(private val context: Context) {

    suspend fun scanAll(treeUris: Set<Uri>): List<VideoItem> = withContext(Dispatchers.IO) {
        val result = LinkedHashMap<String, VideoItem>()

        if (hasMediaStorePermission()) {
            MediaStoreScanner(context).scan().forEach {
                result[it.uri.toString()] = it
            }
        }

        val safScanner = SafVideoScanner(context)
        for (treeUri in treeUris) {
            runCatching {
                safScanner.scan(treeUri).forEach {
                    result[it.uri.toString()] = it
                }
            }
        }

        result.values.sortedWith(
            compareByDescending<VideoItem> { it.dateModifiedMs }
                .thenBy { it.displayName.lowercase() }
        )
    }

    private fun hasMediaStorePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

private class MediaStoreScanner(private val context: Context) {
    fun scan(): List<VideoItem> {
        val resolver = context.contentResolver
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.RELATIVE_PATH
        )

        val result = ArrayList<VideoItem>()

        resolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val pathIndex = cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val uri = ContentUris.withAppendedId(collection, id)
                result += VideoItem(
                    uri = uri,
                    displayName = cursor.getString(nameIndex) ?: "Unknown",
                    durationMs = cursor.getLong(durationIndex),
                    sizeBytes = cursor.getLong(sizeIndex),
                    dateModifiedMs = cursor.getLong(dateIndex) * 1000L,
                    source = VideoItem.Source.MEDIA_STORE,
                    relativePath = pathIndex.takeIf { it >= 0 }?.let(cursor::getString)
                )
            }
        }

        return result
    }
}
