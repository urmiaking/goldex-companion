package com.goldex.companion.desktop.license

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONObject

data class DesktopDeviceIdentity(
    val fingerprint: String,
    val deviceModel: String,
    val manufacturer: String,
    val osVersion: String,
    val appVersion: String
) {
    companion object {
        fun get(directory: Path, appVersion: String = "0.56.73"): DesktopDeviceIdentity {
            val file = directory.resolve("device_identity.json")
            var fingerprint = ""
            if (Files.exists(file)) {
                try {
                    val json = JSONObject(Files.readString(file))
                    fingerprint = json.optString("fingerprint", "")
                } catch (_: Exception) {}
            }

            if (fingerprint.isBlank()) {
                fingerprint = generateFingerprint()
                try {
                    Files.createDirectories(directory)
                    Files.writeString(file, JSONObject().put("fingerprint", fingerprint).toString())
                } catch (_: Exception) {}
            }

            val osName = System.getProperty("os.name") ?: "Windows"
            val osVer = System.getProperty("os.version") ?: "10"
            val osArch = System.getProperty("os.arch") ?: "x64"

            return DesktopDeviceIdentity(
                fingerprint = fingerprint,
                deviceModel = "Windows PC ($osArch)",
                manufacturer = "Microsoft Windows",
                osVersion = "$osName $osVer ($osArch)",
                appVersion = appVersion
            )
        }

        private fun generateFingerprint(): String {
            return try {
                val machineGuid = readWindowsMachineGuid()
                val rawData = buildString {
                    append(machineGuid ?: "")
                    append("|")
                    append(System.getenv("COMPUTERNAME") ?: "")
                    append("|")
                    append(System.getProperty("user.name") ?: "")
                    append("|")
                    append(System.getProperty("os.arch") ?: "")
                    append("|")
                    append(Runtime.getRuntime().availableProcessors())
                }
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(rawData.toByteArray(Charsets.UTF_8))
                "WIN-" + digest.joinToString("") { "%02x".format(it) }.take(28).uppercase()
            } catch (_: Exception) {
                "WIN-" + UUID.randomUUID().toString().replace("-", "").take(28).uppercase()
            }
        }

        private fun readWindowsMachineGuid(): String? {
            return try {
                val process = ProcessBuilder("reg", "query", "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid")
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                val match = Regex("""MachineGuid\s+REG_SZ\s+([A-Fa-f0-9\-]+)""").find(output)
                match?.groupValues?.get(1)
            } catch (_: Exception) {
                null
            }
        }
    }
}
