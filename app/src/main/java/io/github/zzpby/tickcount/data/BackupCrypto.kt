package io.github.zzpby.tickcount.data

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password encryption for the backup file.
 *
 * A password is the only honest option here. A key compiled into the app is a key anyone
 * who has the app has, and there is no server to hold one: the app has no network at all
 * and intends to keep it that way.
 *
 * PBKDF2-HMAC-SHA256 stretches the password, because a password is not a key and most
 * are far too short to be used as one. AES-GCM then encrypts *and* authenticates, which
 * is what makes a wrong password detectable rather than producing plausible rubbish — the
 * tag fails to verify and [decrypt] says so.
 *
 * Deliberately written against plain `java.*` rather than `android.util.Base64` and the
 * like, so that the whole of it runs in a JVM unit test. Encryption that is only checked
 * by reading it is not checked.
 */
object BackupCrypto {

    /** The first line of an encrypted file, and how one is recognised on the way back in. */
    private const val HEADER = "TICKCOUNT-BACKUP-1-ENCRYPTED"

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_FACTORY = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128

    /** True when [text] looks like a file this object wrote. */
    fun isEncrypted(text: String): Boolean = text.trimStart().startsWith(HEADER)

    /**
     * Encrypts [plaintext] under [password], returning the whole file as text.
     *
     * The salt is fresh per call, so the same backup saved twice gives two different
     * files — which is what stops anyone deducing that two files hold the same data.
     */
    fun encrypt(plaintext: String, password: CharArray): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val nonce = ByteArray(NONCE_BYTES).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, keyFrom(password, salt), GCMParameterSpec(TAG_BITS, nonce))
        val sealed = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val blob = Base64.getEncoder().encodeToString(salt + nonce + sealed)
        return "$HEADER\n$blob\n"
    }

    /**
     * Decrypts a file written by [encrypt].
     *
     * Throws [javax.crypto.AEADBadTagException] when the password is wrong, and
     * [IllegalArgumentException] when the file is not one of ours.
     */
    fun decrypt(payload: String, password: CharArray): String {
        val blob = payload.trim().lineSequence().drop(1).firstOrNull { it.isNotBlank() }
            ?: throw IllegalArgumentException("no payload")

        val bytes = Base64.getDecoder().decode(blob)
        require(bytes.size > SALT_BYTES + NONCE_BYTES) { "payload is too short to be whole" }

        val salt = bytes.copyOfRange(0, SALT_BYTES)
        val nonce = bytes.copyOfRange(SALT_BYTES, SALT_BYTES + NONCE_BYTES)
        val sealed = bytes.copyOfRange(SALT_BYTES + NONCE_BYTES, bytes.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keyFrom(password, salt), GCMParameterSpec(TAG_BITS, nonce))
        return String(cipher.doFinal(sealed), Charsets.UTF_8)
    }

    private fun keyFrom(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeySpec(
                SecretKeyFactory.getInstance(KEY_FACTORY).generateSecret(spec).encoded,
                "AES",
            )
        } finally {
            // The spec keeps its own copy; clearing ours is the part we control.
            spec.clearPassword()
        }
    }
}
