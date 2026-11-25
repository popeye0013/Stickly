package com.stickly.app.whatsapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.stickly.app.data.PackRepository
import com.stickly.app.data.StickerPack
import com.stickly.app.util.StickerProcessor
import java.io.File

object WhatsAppExporter {
    const val CONSUMER = "com.whatsapp"
    const val BUSINESS = "com.whatsapp.w4b"

    private const val ACTION_ADD_PACK = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
    private const val EXTRA_ID = "sticker_pack_id"
    private const val EXTRA_AUTHORITY = "sticker_pack_authority"
    private const val EXTRA_NAME = "sticker_pack_name"
    private const val MAX_TRAY_BYTES = 50 * 1024

    /** WhatsApp's sticker search only accepts emojis from its published tag list. */
    private val ALLOWED_EMOJIS = setOf(
        "❤", "❤️", "♥", "❣", "❣️", "😍", "😘", "💕", "😻", "💑", "👩‍❤‍👩", "👨‍❤‍👨",
        "💏", "👩‍❤‍💋‍👩", "👨‍❤‍💋‍👨", "🧡", "💛", "💚", "💙", "💜", "🖤", "💔", "💞",
        "💓", "💗", "💖", "💘", "💝", "💟", "💌", "💋", "👩‍❤️‍💋‍👩", "👨‍❤️‍💋‍👨",
        "👩‍❤️‍👨", "👩‍❤️‍👩", "👨‍❤️‍👨", "👩‍❤️‍💋‍👨", "👬", "👭", "👫", "🥰", "😚",
        "😙", "👄", "🌹", "😽", "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🙂",
        "😛", "😝", "😜", "🤪", "🤗", "😺", "😸", "😹", "☺", "😌", "😉", "😊", "☹",
        "😣", "😖", "😫", "😩", "😢", "😭", "😞", "😔", "😟", "😕", "😤", "😠", "😥",
        "😰", "😨", "😿", "😾", "😓", "🙍‍♂", "🙍‍♀", "🙁", "🥺", "🤕", "☔️", "⛈",
        "🌩", "🌧", "😯", "😦", "😧", "😮", "😲", "🙀", "😱", "🤯", "😳", "❗", "❕",
        "🤬", "🙄", "👿", "💢", "👺", "🗯️", "😒", "🥵", "👋", "🎊", "🎉", "🎁", "🎈",
        "👯‍♂️", "👯", "👯‍♀️", "💃", "🕺", "🔥", "⭐️", "✨", "💫", "🎇", "🎆", "🍻",
        "🥂", "🍾", "🎂", "🍰",
    )

    fun installedPackages(context: Context): List<String> = listOf(CONSUMER, BUSINESS).filter {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(it, PackageManager.GET_META_DATA)
        }.isSuccess
    }

    /** WhatsApp allows at most 128 characters for a pack name. */
    fun displayName(title: String): String =
        title.replace(Regex("[\\r\\n]+"), " ").trim().take(128).ifEmpty { "Stickly pack" }

    fun emoji(value: String?): String =
        value?.trim()?.takeIf { it in ALLOWED_EMOJIS } ?: "😀"

    fun addIntent(pack: StickerPack, targetPackage: String? = null): Intent =
        Intent(ACTION_ADD_PACK)
            .putExtra(EXTRA_ID, pack.id)
            .putExtra(EXTRA_AUTHORITY, StickerContentProvider.AUTHORITY)
            .putExtra(EXTRA_NAME, displayName(pack.title))
            .apply { targetPackage?.let(::setPackage) }

    /**
     * Makes sure a legacy or repaired pack obeys WhatsApp's contract before the
     * add intent is launched. Mixed static/animated packs are normalized to a
     * fully animated pack, and the metadata version is bumped so WhatsApp
     * refreshes any cached copy.
     */
    fun prepare(context: Context, pack: StickerPack): Result<StickerPack> = try {
        if (installedPackages(context).isEmpty()) {
            throw IllegalArgumentException("WhatsApp isn't installed on this device.")
        }

        val repository = PackRepository(context)
        val files = pack.stickers.map { repository.file(pack.id, it.file) }
        if (files.any { !it.exists() }) {
            throw IllegalArgumentException("Some stickers are missing. Import the pack again.")
        }
        if (files.size !in 3..30) {
            throw IllegalArgumentException(
                "WhatsApp needs 3-30 stickers (this pack has ${files.size}). Re-import it to split it into smaller packs.",
            )
        }

        val tray = repository.file(pack.id, pack.trayFile)
        if (!tray.exists() || tray.length() > MAX_TRAY_BYTES) {
            throw IllegalArgumentException("The pack icon is missing or too large. Re-import the pack.")
        }

        val originalFlags = files.map { StickerProcessor.validAnimatedWebp(it.readBytes()) }
        val animatedPack = pack.animated || originalFlags.any { it }

        if (animatedPack) {
            files.forEachIndexed { index, file ->
                if (!originalFlags[index]) {
                    val temp = File.createTempFile("stickly_repair_", ".webp", context.cacheDir)
                    try {
                        val converted = StickerProcessor.encodeAnimatedStatic(
                            context,
                            StickerProcessor.decodeStatic(file.readBytes()),
                            temp,
                        ) ?: throw IllegalArgumentException("A sticker could not be prepared for WhatsApp.")
                        file.writeBytes(converted)
                    } finally {
                        temp.delete()
                    }
                }
            }
        }

        val finalBytes = files.map { it.readBytes() }
        val finalFlags = finalBytes.map { StickerProcessor.validAnimatedWebp(it) }
        if (animatedPack && finalFlags.any { !it }) {
            throw IllegalArgumentException("This pack still mixes static and animated stickers. Re-import it.")
        }
        if (!animatedPack && finalFlags.any { it }) {
            throw IllegalArgumentException("This pack mixes static and animated stickers. Re-import it.")
        }

        val limit = if (animatedPack) 500 * 1024 else 100 * 1024
        if (finalBytes.any { it.size > limit }) {
            val label = if (animatedPack) "500 KB animated" else "100 KB static"
            throw IllegalArgumentException("A sticker is larger than WhatsApp's $label limit. Re-import the pack.")
        }

        val updated = pack.copy(
            animated = animatedPack,
            animatedConverted = if (animatedPack) pack.stickers.size else pack.animatedConverted,
            imageDataVersion = System.currentTimeMillis().toString(),
        )
        if (updated != pack) {
            repository.save(updated)
            StickerContentProvider.notifyPacksChanged(context)
        }
        Result.success(updated)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Backwards-compatible validation for callers that don't need a repaired pack. */
    fun validationError(context: Context, pack: StickerPack): String? =
        prepare(context, pack).exceptionOrNull()?.message
}
