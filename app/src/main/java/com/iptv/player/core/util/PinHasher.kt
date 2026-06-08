package com.iptv.player.core.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-way hashing for the parental PIN. A 4-digit PIN is only a soft gate, so a salted SHA-256
 * (stored as "salt:hash", both Base64) is sufficient and keeps the PIN out of plaintext.
 */
@Singleton
class PinHasher @Inject constructor() {

    fun hash(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val digest = digest(pin, salt)
        return "${salt.encode()}:${digest.encode()}"
    }

    fun verify(pin: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = runCatching { Base64.decode(parts[0], Base64.NO_WRAP) }.getOrNull() ?: return false
        val expected = runCatching { Base64.decode(parts[1], Base64.NO_WRAP) }.getOrNull() ?: return false
        return MessageDigest.isEqual(digest(pin, salt), expected)
    }

    private fun digest(pin: String, salt: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        return md.digest(pin.toByteArray(Charsets.UTF_8))
    }

    private fun ByteArray.encode(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private companion object {
        const val SALT_BYTES = 16
    }
}
