package com.stickly.app.data

/**
 * A single sticker inside a pack. [file] is relative to the pack directory.
 */
data class StickerEntry(
    val file: String,
    val emoji: String = "",
    /** Original animated source (TGS/GIF) kept so Stickly can play it in-app. */
    val sourceFile: String? = null,
)

/**
 * Everything Stickly needs to display a pack and to hand it to WhatsApp.
 */
data class StickerPack(
    val id: String,
    val title: String,
    val publisher: String,
    val sourceUrl: String,
    val importedAt: Long,
    val trayFile: String,
    val stickers: List<StickerEntry>,
    val animatedConverted: Int = 0,
    val animatedSkipped: Int = 0,
    val videoSkipped: Int = 0,
    /** WhatsApp packs must be entirely static or entirely animated. */
    val animated: Boolean = false,
    val addedToWhatsApp: Boolean = false,
    /** Bumped when stickers or the name change so WhatsApp refreshes its cached copy. */
    val imageDataVersion: String? = null,
    /** Set when one Telegram pack is split into several WhatsApp packs. */
    val partIndex: Int = 1,
    val partCount: Int = 1,
)
