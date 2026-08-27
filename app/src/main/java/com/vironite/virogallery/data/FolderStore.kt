package com.vironite.virogallery.data

import android.content.Context
import android.net.Uri

class FolderStore(context: Context) {
    private val prefs = context.getSharedPreferences("folders", Context.MODE_PRIVATE)

    fun getTreeUris(): Set<Uri> =
        prefs.getStringSet(KEY_TREES, emptySet())
            .orEmpty()
            .map(Uri::parse)
            .toSet()

    fun addTree(uri: Uri) {
        val values = getTreeUris().map(Uri::toString).toMutableSet()
        values += uri.toString()
        prefs.edit().putStringSet(KEY_TREES, values).apply()
    }

    fun removeTree(uri: Uri) {
        val values = getTreeUris().map(Uri::toString).toMutableSet()
        values -= uri.toString()
        prefs.edit().putStringSet(KEY_TREES, values).apply()
    }

    companion object {
        private const val KEY_TREES = "tree_uris"
    }
}
