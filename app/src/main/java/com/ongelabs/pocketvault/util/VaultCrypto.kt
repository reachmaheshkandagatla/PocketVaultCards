package com.ongelabs.pocketvault.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object VaultCrypto {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "pocketvault_master_key_v1"
    private const val PREFS = "vault_crypto"
    private const val DB_PASSPHRASE = "database_passphrase_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private val FILE_MAGIC = byteArrayOf(0x50, 0x56, 0x4C, 0x54, 0x01)
    private const val IV_SIZE = 12

    fun databasePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(DB_PASSPHRASE, null)
        if (stored != null) {
            return decrypt(Base64.decode(stored, Base64.NO_WRAP))
        }

        val passphrase = ByteArray(32).also(SecureRandom()::nextBytes)
        val wrapped = Base64.encodeToString(encrypt(passphrase), Base64.NO_WRAP)
        check(prefs.edit().putString(DB_PASSPHRASE, wrapped).commit()) {
            "Unable to protect the database encryption key"
        }
        return passphrase
    }

    fun encryptFileBytes(plaintext: ByteArray): ByteArray = FILE_MAGIC + encrypt(plaintext)

    fun decryptFileBytes(contents: ByteArray): ByteArray {
        require(isEncryptedFile(contents)) { "Vault file is not encrypted" }
        return decrypt(contents.copyOfRange(FILE_MAGIC.size, contents.size))
    }

    fun isEncryptedFile(contents: ByteArray): Boolean {
        return contents.size > FILE_MAGIC.size + IV_SIZE &&
            contents.copyOfRange(0, FILE_MAGIC.size).contentEquals(FILE_MAGIC)
    }

    private fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return cipher.iv + cipher.doFinal(plaintext)
    }

    private fun decrypt(payload: ByteArray): ByteArray {
        require(payload.size > IV_SIZE) { "Invalid encrypted payload" }
        val iv = payload.copyOfRange(0, IV_SIZE)
        val ciphertext = payload.copyOfRange(IV_SIZE, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
