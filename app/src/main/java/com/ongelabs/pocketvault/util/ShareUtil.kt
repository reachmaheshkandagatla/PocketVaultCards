package com.ongelabs.pocketvault.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ongelabs.pocketvault.data.CardEntity

object ShareUtil {
    fun shareCard(context: Context, card: CardEntity, shareBack: Boolean = false) {
        val path = if (shareBack) card.backImagePath else card.frontImagePath
        shareImage(context, path, card.name)
    }

    fun shareText(context: Context, subject: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share groceries"))
    }

    fun shareImage(context: Context, path: String, subject: String) {
        val shareCopy = ImageStore.createShareCopy(context, path)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", shareCopy)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via Bluetooth or nearby app"))
    }
}
