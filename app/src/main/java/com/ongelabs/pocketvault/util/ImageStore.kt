package com.ongelabs.pocketvault.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStore {
    private val migrationLock = Any()

    fun saveImage(context: Context, source: Uri): String {
        val plaintext = requireNotNull(context.contentResolver.openInputStream(source)) {
            "Unable to open the selected image"
        }.use { it.readBytes() }
        val dir = File(context.filesDir, "card_images").apply { mkdirs() }
        val outFile = File(dir, "${UUID.randomUUID()}.pvault")
        writeAtomically(outFile, VaultCrypto.encryptFileBytes(plaintext))
        plaintext.fill(0)
        return outFile.absolutePath
    }

    fun migrateLegacyImages(context: Context) {
        val dir = File(context.filesDir, "card_images")
        if (!dir.exists()) return
        synchronized(migrationLock) {
            dir.listFiles()?.filter { it.isFile && !it.name.startsWith(".") }?.forEach { file ->
                val contents = file.readBytes()
                if (!VaultCrypto.isEncryptedFile(contents)) {
                    writeAtomically(file, VaultCrypto.encryptFileBytes(contents))
                    contents.fill(0)
                }
            }
        }
    }

    fun readImage(context: Context, path: String): ByteArray {
        if (path.isBlank()) return ByteArray(0)
        val file = File(path)
        val contents = file.readBytes()
        if (VaultCrypto.isEncryptedFile(contents)) {
            return VaultCrypto.decryptFileBytes(contents)
        }

        synchronized(migrationLock) {
            val latest = file.readBytes()
            if (VaultCrypto.isEncryptedFile(latest)) {
                return VaultCrypto.decryptFileBytes(latest)
            }
            writeAtomically(file, VaultCrypto.encryptFileBytes(latest))
            return latest
        }
    }

    fun createShareCopy(context: Context, path: String): File {
        val shareDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        shareDir.listFiles()?.forEach { candidate ->
            if (System.currentTimeMillis() - candidate.lastModified() > SHARE_COPY_TTL_MS) {
                candidate.delete()
            }
        }
        val copy = File(shareDir, "${UUID.randomUUID()}.jpg")
        writeAtomically(copy, readImage(context, path))
        return copy
    }

    private fun writeAtomically(destination: File, contents: ByteArray) {
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, ".${destination.name}.${UUID.randomUUID()}.tmp")
        try {
            FileOutputStream(temporary).use { output ->
                output.write(contents)
                output.fd.sync()
            }
            check(temporary.renameTo(destination)) { "Unable to save encrypted vault file" }
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private const val SHARE_COPY_TTL_MS = 10 * 60 * 1000L
}
