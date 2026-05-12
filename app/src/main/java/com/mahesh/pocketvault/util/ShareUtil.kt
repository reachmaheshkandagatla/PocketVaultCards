package com.mahesh.pocketvault.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mahesh.pocketvault.data.CardEntity
import java.io.File

object ShareUtil {
    fun shareCard(context: Context, card: CardEntity, shareBack: Boolean = false) {
        val path = if (shareBack) card.backImagePath else card.frontImagePath
        val file = File(path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, card.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via Bluetooth or nearby app"))
    }
}
