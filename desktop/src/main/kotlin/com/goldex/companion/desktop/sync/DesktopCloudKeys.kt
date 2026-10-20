package com.goldex.companion.desktop.sync

import java.nio.file.Files
import java.nio.file.Path
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

class DesktopCloudKeys(private val directory: Path) {
    private val keysFile = directory.resolve("cloud_keys.json")
    private val sessionFile = directory.resolve("cloud_session.enc")
    private var keyPair: KeyPair? = null
    private var aesKey: SecretKey? = null

    init {
        loadOrGenerateKeys()
    }

    private fun loadOrGenerateKeys() {
        if (Files.exists(keysFile)) {
            try {
                val json = JSONObject(Files.readString(keysFile))
                val privBytes = Base64.getDecoder().decode(json.getString("privateKey"))
                val pubBytes = Base64.getDecoder().decode(json.getString("publicKey"))
                val aesBytes = Base64.getDecoder().decode(json.getString("aesKey"))
                val kf = KeyFactory.getInstance("EC")
                val priv = kf.generatePrivate(PKCS8EncodedKeySpec(privBytes))
                val pub = kf.generatePublic(X509EncodedKeySpec(pubBytes))
                keyPair = KeyPair(pub, priv)
                aesKey = SecretKeySpec(aesBytes, "AES")
                return
            } catch (_: Exception) {}
        }

        val kpg = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }
        val kp = kpg.generateKeyPair()
        val kg = KeyGenerator.getInstance("AES").apply { init(128) }
        val ak = kg.generateKey()

        keyPair = kp
        aesKey = ak

        try {
            Files.createDirectories(directory)
            val json = JSONObject().apply {
                put("privateKey", Base64.getEncoder().encodeToString(kp.private.encoded))
                put("publicKey", Base64.getEncoder().encodeToString(kp.public.encoded))
                put("aesKey", Base64.getEncoder().encodeToString(ak.encoded))
            }
            Files.writeString(keysFile, json.toString())
        } catch (_: Exception) {}
    }

    fun publicKey(): String {
        return Base64.getEncoder().encodeToString(keyPair!!.public.encoded)
    }

    fun sign(message: String): String {
        val sig = Signature.getInstance("SHA256withECDSA").apply {
            initSign(keyPair!!.private)
            update(message.toByteArray(Charsets.UTF_8))
        }
        return Base64.getEncoder().encodeToString(sig.sign())
    }

    fun read(): JSONObject? {
        if (!Files.exists(sessionFile)) return null
        return try {
            val bytes = Files.readAllBytes(sessionFile)
            if (bytes.size <= 12) return null
            val iv = bytes.copyOfRange(0, 12)
            val cipherBytes = bytes.copyOfRange(12, bytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
            }
            val decrypted = cipher.doFinal(cipherBytes)
            JSONObject(String(decrypted, Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }

    fun write(value: JSONObject) {
        try {
            Files.createDirectories(directory)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE, aesKey)
            }
            val encrypted = cipher.doFinal(value.toString().toByteArray(Charsets.UTF_8))
            val combined = cipher.iv + encrypted
            Files.write(sessionFile, combined)
        } catch (_: Exception) {}
    }

    fun clear() {
        try { Files.deleteIfExists(sessionFile) } catch (_: Exception) {}
    }
}
