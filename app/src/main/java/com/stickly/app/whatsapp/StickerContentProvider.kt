package com.stickly.app.whatsapp

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.stickly.app.data.PackRepository
import com.stickly.app.data.StickerPack

/**
 * The documented WhatsApp third-party sticker pack interface.
 * WhatsApp queries metadata/stickers and then reads the WebP files through
 * stickers_asset/<identifier>/<file>.
 */
class StickerContentProvider : ContentProvider() {

    private lateinit var repository: PackRepository

    override fun onCreate(): Boolean {
        repository = PackRepository(requireNotNull(context))
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor {
        val segments = uri.pathSegments
        return when {
            segments.size == 1 && segments[0] == METADATA ->
                packCursor(uri, repository.list())

            segments.size == 2 && segments[0] == METADATA ->
                packCursor(uri, listOfNotNull(repository.get(segments[1])))

            segments.size == 2 && segments[0] == STICKERS ->
                stickerCursor(uri, repository.get(segments[1]))

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val segments = uri.pathSegments
        if (segments.size != 3 || segments[0] != STICKERS_ASSET || mode != "r") return null

        val pack = repository.get(segments[1]) ?: return null
        val fileName = segments[2]
        val valid = fileName == pack.trayFile || pack.stickers.any { it.file == fileName }
        if (!valid) return null

        val file = repository.file(pack.id, fileName)
        if (!file.exists()) return null
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, file.length())
    }

    override fun getType(uri: Uri): String = when {
        uri.pathSegments == listOf(METADATA) -> "vnd.android.cursor.dir/vnd.$AUTHORITY.$METADATA"
        uri.pathSegments.size == 2 && uri.pathSegments[0] == METADATA ->
            "vnd.android.cursor.item/vnd.$AUTHORITY.$METADATA"
        uri.pathSegments.size == 2 && uri.pathSegments[0] == STICKERS ->
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$STICKERS"
        uri.pathSegments.size == 3 && uri.pathSegments[0] == STICKERS_ASSET ->
            if (uri.lastPathSegment == repository.get(uri.pathSegments[1])?.trayFile) "image/png" else "image/webp"
        else -> throw IllegalArgumentException("Unknown URI: $uri")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri =
        throw UnsupportedOperationException()

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int =
        throw UnsupportedOperationException()

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int = throw UnsupportedOperationException()

    private fun packCursor(uri: Uri, packs: List<StickerPack>): Cursor {
        val cursor = MatrixCursor(PACK_COLUMNS)
        packs.forEach { pack ->
            cursor.newRow().apply {
                add(pack.id)
                add(WhatsAppExporter.displayName(pack.title))
                add(WhatsAppExporter.displayName("Stickly"))
                add(pack.trayFile)
                add(PLAY_STORE_LINK)
                add("")
                add("")
                add("https://t.me/addstickers/${pack.sourceUrl.substringAfterLast('/')}")
                add("")
                add("")
                add(pack.imageDataVersion?.takeIf { it.isNotBlank() } ?: "1")
                add(0)
                add(if (pack.animated) 1 else 0)
            }
        }
        cursor.setNotificationUri(requireNotNull(context).contentResolver, uri)
        return cursor
    }

    private fun stickerCursor(uri: Uri, pack: StickerPack?): Cursor {
        val cursor = MatrixCursor(STICKER_COLUMNS)
        pack?.stickers?.forEach { sticker ->
            val emoji = WhatsAppExporter.emoji(sticker.emoji)
            cursor.addRow(arrayOf<Any>(sticker.file, emoji, emoji))
        }
        cursor.setNotificationUri(requireNotNull(context).contentResolver, uri)
        return cursor
    }

    companion object {
        const val AUTHORITY = "com.stickly.app.stickerprovider"
        private const val SCHEME = "content"
        private const val METADATA = "metadata"
        private const val STICKERS = "stickers"
        private const val STICKERS_ASSET = "stickers_asset"
        private const val PLAY_STORE_LINK = "https://play.google.com/store/apps/details?id=com.stickly.app"

        // Column names are fixed by WhatsApp's sticker API. Do not rename.
        private val PACK_COLUMNS = arrayOf(
            "sticker_pack_identifier",
            "sticker_pack_name",
            "sticker_pack_publisher",
            "sticker_pack_icon",
            "android_play_store_link",
            "ios_app_download_link",
            "sticker_pack_publisher_email",
            "sticker_pack_publisher_website",
            "sticker_pack_privacy_policy_website",
            "sticker_pack_license_agreement_website",
            "image_data_version",
            "whatsapp_will_not_cache_stickers",
            "animated_sticker_pack",
        )
        private val STICKER_COLUMNS = arrayOf(
            "sticker_file_name",
            "sticker_emoji",
            "sticker_accessibility_text",
        )

        fun notifyPacksChanged(androidContext: android.content.Context) {
            androidContext.contentResolver.notifyChange(
                Uri.parse("$SCHEME://$AUTHORITY/$METADATA"),
                null,
            )
        }
    }
}
