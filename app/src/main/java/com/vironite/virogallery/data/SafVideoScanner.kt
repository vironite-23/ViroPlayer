package com.vironite.virogallery.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.util.ArrayDeque
import java.util.Locale

/**
 * Scans a user-granted SAF tree without filtering dot-prefixed names.
 *
 * This is the important path for hidden folders/videos. MediaStore is an
 * optimized index, but SAF lets the user explicitly grant access to a tree
 * outside that index.
 */
class SafVideoScanner(private val context: Context) {

    private val videoExtensions = setOf(
        "3gp", "3g2", "avi", "divx", "flv", "m2ts", "m4v", "mkv",
        "mov", "mp4", "mpeg", "mpg", "mts", "ogm", "ogv", "ts",
        "vob", "webm", "wmv"
    )

    fun scan(treeUri: Uri): List<VideoItem> {
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootChildrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocumentId)

        val queue = ArrayDeque<PendingDirectory>()
        queue.add(PendingDirectory(rootChildrenUri, ""))

        val result = ArrayList<VideoItem>()

        while (queue.isNotEmpty()) {
            val pending = queue.removeFirst()

            queryChildren(pending.childrenUri) { documentId, name, mimeType, size, modified ->
                val childUri =
                    DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                val relativePath = if (pending.relativePath.isBlank()) {
                    name
                } else {
                    "${pending.relativePath}/$name"
                }

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    // Deliberately do NOT skip hidden directories.
                    val childrenUri =
                        DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
                    queue.add(PendingDirectory(childrenUri, relativePath))
                    return@queryChildren
                }

                if (isVideo(name, mimeType)) {
                    result += VideoItem(
                        uri = childUri,
                        displayName = name,
                        sizeBytes = size,
                        dateModifiedMs = modified,
                        source = VideoItem.Source.SAF_TREE,
                        relativePath = relativePath
                    )
                }
            }
        }

        return result
    }

    private fun queryChildren(
        childrenUri: Uri,
        onChild: (id: String, name: String, mimeType: String, size: Long, modified: Long) -> Unit
    ) {
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val name = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mime = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val size = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            val modified =
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                onChild(
                    cursor.getString(id),
                    cursor.getString(name) ?: "",
                    cursor.getString(mime) ?: "",
                    if (cursor.isNull(size)) 0L else cursor.getLong(size),
                    if (cursor.isNull(modified)) 0L else cursor.getLong(modified)
                )
            }
        }
    }

    private fun isVideo(name: String, mimeType: String): Boolean {
        if (mimeType.startsWith("video/", ignoreCase = true)) return true
        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return extension in videoExtensions
    }

    private data class PendingDirectory(
        val childrenUri: Uri,
        val relativePath: String
    )
}
