package com.ongelabs.pocketvault.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object ImageStore {
    fun saveImage(context: Context, source: Uri): String {
        val dir = File(context.filesDir, "card_images").apply { mkdirs() }
        val outFile = File(dir, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(source).use { input ->
            outFile.outputStream().use { output -> input?.copyTo(output) }
        }
        return outFile.absolutePath
    }
}
