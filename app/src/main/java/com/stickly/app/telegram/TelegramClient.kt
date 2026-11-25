package com.stickly.app.telegram

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class TelegramException(message: String) : Exception(message)

data class TelegramStickerSet(
    @SerializedName("name") val name: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("stickers") val stickers: List<TelegramSticker>? = null,
)

data class TelegramSticker(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("emoji") val emoji: String? = null,
    @SerializedName("is_animated") val isAnimated: Boolean = false,
    @SerializedName("is_video") val isVideo: Boolean = false,
)

data class TelegramFile(
    @SerializedName("file_path") val filePath: String? = null,
)

private data class Envelope<T>(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("result") val result: T? = null,
    @SerializedName("description") val description: String? = null,
)

/**
 * Thin wrapper around the official Telegram Bot API.
 * A bot token from @BotFather is required; Telegram has no public unauthenticated
 * endpoint for reading sticker sets.
 */
object TelegramClient {
    private const val API = "https://api.telegram.org"
    private const val FILE_API = "https://api.telegram.org/file"

    private val gson = Gson()
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    fun parsePackName(input: String): String {
        val raw = input.trim()
        require(raw.isNotEmpty()) { "Paste a Telegram sticker pack link first." }

        val name = if (raw.startsWith("@")) {
            raw.removePrefix("@")
        } else {
            val uri = Uri.parse(if ("://" in raw) raw else "https://$raw")
            val host = uri.host?.lowercase().orEmpty()
            val segments = uri.pathSegments.orEmpty()
            if (host in setOf("t.me", "telegram.me", "telegram.dog")) {
                if (segments.size < 2 || segments[0] != "addstickers") {
                    throw IllegalArgumentException("That doesn't look like a sticker pack link. Use t.me/addstickers/PackName")
                }
                segments[1]
            } else if (host.isEmpty() && segments.size == 2 && segments[0] == "addstickers") {
                segments[1]
            } else {
                raw
            }
        }

        require(name.matches(Regex("^[A-Za-z0-9_]{1,64}$"))) {
            "Invalid pack name \"$name\". Use the link from Telegram, e.g. t.me/addstickers/DonutTheDog"
        }
        return name
    }

    suspend fun getStickerSet(token: String, name: String): TelegramStickerSet = apiCall(
        token,
        "getStickerSet",
        listOf("name" to name),
        TelegramStickerSet::class.java,
    )

    suspend fun getFile(token: String, fileId: String): String? = apiCall(
        token,
        "getFile",
        listOf("file_id" to fileId),
        TelegramFile::class.java,
    ).filePath

    suspend fun downloadFile(token: String, filePath: String): ByteArray = withContext(Dispatchers.IO) {
        val url = "$FILE_API/bot$token/${filePath.trimStart('/')}"
        val response = http.newCall(Request.Builder().url(url).build()).execute()
        response.use { resp ->
            if (!resp.isSuccessful) throw TelegramException("Download failed (HTTP ${resp.code})")
            resp.body?.bytes() ?: throw TelegramException("Download returned no data")
        }
    }

    private suspend fun <T : Any> apiCall(
        token: String,
        method: String,
        params: List<Pair<String, String>>,
        resultClass: Class<T>,
    ): T = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "Add your Telegram bot token in Settings first." }

        val base = "$API/bot$token/$method"
        val url = android.net.Uri.parse(base).buildUpon().apply {
            params.forEach { (key, value) -> appendQueryParameter(key, value) }
        }.build().toString()

        val response = http.newCall(Request.Builder().url(url).build()).execute()
        response.use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw TelegramException("Telegram HTTP ${resp.code}: ${body.take(200)}")
            }

            // Build the Type with a concrete Class instead of a generic TypeToken<T>,
            // which Gson rejects because T is only a captured type variable here.
            val type = TypeToken.getParameterized(Envelope::class.java, resultClass).type
            val envelope: Envelope<T> = try {
                gson.fromJson(body, type)
            } catch (e: Exception) {
                throw TelegramException("Could not read Telegram's response")
            }
            val result = envelope.result
            if (!envelope.ok || result == null) {
                throw TelegramException(envelope.description ?: "Telegram rejected the request")
            }
            result
        }
    }
}
