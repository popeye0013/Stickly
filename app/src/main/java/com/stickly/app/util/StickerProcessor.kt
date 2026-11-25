package com.stickly.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.core.graphics.scale
import com.aureusapps.android.webpandroid.encoder.WebPAnimEncoder
import com.aureusapps.android.webpandroid.encoder.WebPAnimEncoderOptions
import com.aureusapps.android.webpandroid.encoder.WebPConfig
import com.aureusapps.android.webpandroid.encoder.WebPMuxAnimParams
import com.aureusapps.android.webpandroid.encoder.WebPPreset
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream

/**
 * Turns Telegram stickers into WhatsApp-compatible WebP stickers.
 *
 * WhatsApp requires 512x512 WebP files. Static packs use a single frame under
 * 100 KB; animated packs use animated WebP under 500 KB with every sticker
 * animated. Animated TGS/video stickers keep their motion instead of being
 * flattened to the first frame.
 */
object StickerProcessor {
    enum class SourceFormat { STATIC, TGS, VIDEO }

    const val STICKER_SIZE = 512
    private const val TRAY_SIZE = 96
    private const val MAX_STICKER_BYTES = 100 * 1024
    private const val MAX_ANIMATED_BYTES = 500 * 1024
    private const val MAX_TRAY_BYTES = 50 * 1024
    // WhatsApp requires every frame to last more than 8 ms; 100 ms stays well clear.
    private const val FRAME_MS = 100L

    /**
     * Detects the downloaded sticker's real format instead of trusting Telegram's
     * metadata. TGS is gzipped Lottie JSON, WebM starts with an EBML header, and
     * PNG/WebP are decoded as static images and re-encoded as WebP.
     */
    fun detectSourceFormat(bytes: ByteArray): SourceFormat = when {
        bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte() ->
            SourceFormat.TGS

        bytes.size >= 4 &&
            bytes[0] == 0x1a.toByte() && bytes[1] == 0x45.toByte() &&
            bytes[2] == 0xdf.toByte() && bytes[3] == 0xa3.toByte() -> SourceFormat.VIDEO

        else -> SourceFormat.STATIC
    }

    fun decodeStatic(bytes: ByteArray): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(ByteBuffer.wrap(bytes)),
            ) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        @Suppress("DEPRECATION")
        return requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) {
            "Could not decode sticker"
        }
    }

    /** TGS files are gzipped Lottie JSON. */
    private fun loadTgs(bytes: ByteArray) = LottieCompositionFactory
        .fromJsonInputStreamSync(
            ByteArrayInputStream(GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }),
            null,
        ).value
        ?: throw IllegalArgumentException("Invalid animated sticker")

    /** Renders the first visible frame for tray icons and static fallbacks. */
    fun decodeTgs(bytes: ByteArray): Bitmap = decodeTgsFrames(bytes, 1).first()

    fun decodeTgsFrames(bytes: ByteArray, frameCount: Int = 12): List<Bitmap> {
        val composition = loadTgs(bytes)
        val drawable = LottieDrawable().apply {
            setComposition(composition)
            repeatCount = 0
            setBounds(0, 0, STICKER_SIZE, STICKER_SIZE)
        }
        return (0 until frameCount.coerceAtLeast(2)).map { index ->
            // Start slightly into the animation so the first frame is complete.
            drawable.progress = (index + 1).toFloat() / (frameCount + 1)
            val bitmap = Bitmap.createBitmap(STICKER_SIZE, STICKER_SIZE, Bitmap.Config.ARGB_8888)
            drawable.draw(Canvas(bitmap))
            bitmap
        }
    }

    /** Video stickers are WEBM; sample frames across the clip. */
    fun decodeVideoFirstFrame(bytes: ByteArray, cacheDir: File): Bitmap =
        decodeVideoFrames(bytes, cacheDir, 1).first()

    fun decodeVideoFrames(bytes: ByteArray, cacheDir: File, frameCount: Int = 10): List<Bitmap> {
        val temp = File.createTempFile("stickly_video", ".webm", cacheDir)
        return try {
            temp.writeBytes(bytes)
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(temp.absolutePath)
                val durationMs = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION,
                )?.toLongOrNull()?.coerceIn(1_000L, 10_000L) ?: 2_000L
                val count = frameCount.coerceAtLeast(2)
                (0 until count).mapNotNull { index ->
                    // Avoid the first decoded frame, which is often blank in WebM stickers.
                    val timeUs = (durationMs * 1_000L) * (index + 1) / (count + 1)
                    val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                        ?: retriever.frameAtTime
                    when {
                        frame == null -> null
                        frame.config == Bitmap.Config.HARDWARE ->
                            fitToSticker(frame.copy(Bitmap.Config.ARGB_8888, false))

                        else -> fitToSticker(frame)
                    }
                }.ifEmpty { throw IllegalArgumentException("No frame in video sticker") }
            } finally {
                retriever.release()
            }
        } finally {
            temp.delete()
        }
    }

    /** Last-resort sticker so the pack still shows every Telegram sticker. */
    fun fallbackSticker(emoji: String): Bitmap {
        val bitmap = Bitmap.createBitmap(STICKER_SIZE, STICKER_SIZE, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = 320f
        }
        val text = emoji.ifBlank { "⭐" }
        val y = STICKER_SIZE / 2f - (paint.descent() + paint.ascent()) / 2f
        Canvas(bitmap).drawText(text, STICKER_SIZE / 2f, y, paint)
        return bitmap
    }

    /**
     * Last-resort static sticker. The native encoder is best effort; Android's
     * own WebP compressor is the safety net so an import never aborts.
     */
    fun staticFallback(emoji: String): ByteArray {
        val bitmap = fitToSticker(fallbackSticker(emoji))
        return encodeWhatsAppSticker(bitmap) ?: compress(bitmap, lossy = true, quality = 80)
    }

    /** Last-resort animated sticker, with a static WebP safety net. */
    fun animatedFallback(context: Context, emoji: String): ByteArray {
        val temp = File.createTempFile("stickly_anim_fallback_", ".webp", context.cacheDir)
        return try {
            encodeAnimatedStatic(context, fallbackSticker(emoji), temp) ?: staticFallback(emoji)
        } finally {
            temp.delete()
        }
    }

    /** Centers the artwork on a transparent 512x512 canvas. */
    fun fitToSticker(source: Bitmap): Bitmap = fitInto(source, STICKER_SIZE)

    fun encodeWhatsAppSticker(bitmap: Bitmap): ByteArray? {
        for (scale in floatArrayOf(1f, 0.88f, 0.74f, 0.6f)) {
            val canvasBitmap = if (scale == 1f) bitmap else fitInto(
                bitmap.scale(
                    (STICKER_SIZE * scale).toInt().coerceAtLeast(1),
                    (STICKER_SIZE * scale).toInt().coerceAtLeast(1),
                    false,
                ),
                STICKER_SIZE,
            )

            val qualities = if (scale == 1f) {
                intArrayOf(95, 90, 85, 80, 75, 70, 65, 60, 55, 50, 45, 40)
            } else {
                intArrayOf(85, 70, 55)
            }

            for (quality in qualities) {
                val bytes = compress(canvasBitmap, lossy = true, quality = quality)
                if (bytes.size <= MAX_STICKER_BYTES && preservesAlpha(canvasBitmap, bytes)) return bytes
            }

            // Lossless keeps crisp edges for line-art stickers; effort trades size for speed.
            for (effort in intArrayOf(100, 80, 60, 40, 20, 0)) {
                val bytes = compress(canvasBitmap, lossy = false, quality = effort)
                if (bytes.size <= MAX_STICKER_BYTES && preservesAlpha(canvasBitmap, bytes)) return bytes
            }
        }
        return null
    }

    fun encodeAnimatedTgs(context: Context, bytes: ByteArray, output: File): ByteArray? =
        encodeAnimatedWebP(context, decodeTgsFrames(bytes), output)

    fun encodeAnimatedVideo(
        context: Context,
        bytes: ByteArray,
        cacheDir: File,
        output: File,
    ): ByteArray? = encodeAnimatedWebP(context, decodeVideoFrames(bytes, cacheDir), output)

    /**
     * Static stickers in an animated pack get two frames. Shifting the artwork
     * by one pixel gives libwebp a real difference to retain, so it cannot
     * collapse the animation into a single frame that WhatsApp rejects.
     */
    fun encodeAnimatedStatic(context: Context, bitmap: Bitmap, output: File): ByteArray? {
        val first = fitToSticker(bitmap)
        val second = Bitmap.createBitmap(STICKER_SIZE, STICKER_SIZE, Bitmap.Config.ARGB_8888)
        Canvas(second).drawBitmap(first, 1f, 0f, null)
        return encodeAnimatedWebP(context, listOf(first, second), output)
    }

    /**
     * Writes a real animated WebP. WhatsApp rejects packs that mix static and
     * animated stickers, so callers mark the whole pack animated and make sure
     * every sticker goes through this encoder.
     */
    private fun encodeAnimatedWebP(
        context: Context,
        frames: List<Bitmap>,
        output: File,
    ): ByteArray? {
        val fitted = frames.map(::fitToSticker)
        if (fitted.size < 2) return null

        for ((quality, keepEvery) in arrayOf(70f to 1, 55f to 1, 45f to 2, 35f to 3)) {
            val selected = if (keepEvery == 1) {
                fitted
            } else {
                fitted.filterIndexed { index, _ -> index % keepEvery == 0 }
                    .takeIf { it.size >= 2 } ?: fitted.take(2)
            }
            val frameMs = FRAME_MS

            val encoder = try {
                WebPAnimEncoder(
                    context = context,
                    width = STICKER_SIZE,
                    height = STICKER_SIZE,
                    options = WebPAnimEncoderOptions(
                        // Keyframes and minimizeSize slow encoding and inflate files.
                        minimizeSize = false,
                        kmax = 0,
                        allowMixed = false,
                        animParams = WebPMuxAnimParams(backgroundColor = 0, loopCount = 0),
                    ),
                ).configure(
                    config = WebPConfig(
                        lossless = WebPConfig.COMPRESSION_LOSSY,
                        quality = quality,
                        method = 4,
                        alphaQuality = 90,
                        alphaFiltering = WebPConfig.ALPHA_FILTERING_FAST,
                    ),
                    preset = WebPPreset.WEBP_PRESET_DRAWING,
                )
            } catch (e: Exception) {
                return null
            }

            try {
                selected.forEachIndexed { index, frame ->
                    encoder.addFrame((index * frameMs), frame)
                }
                if (output.exists()) output.delete()
                encoder.assemble(selected.size * frameMs, Uri.fromFile(output))
            } catch (e: Exception) {
                continue
            } finally {
                encoder.release()
            }

            val bytes = if (output.exists()) output.readBytes() else ByteArray(0)
            if (bytes.isNotEmpty() &&
                bytes.size <= MAX_ANIMATED_BYTES &&
                hasAnimatedWebpFrames(bytes)
            ) return bytes
        }
        return null
    }

    /** Fast structural check used by the encoder. */
    fun hasAnimatedWebpFrames(bytes: ByteArray): Boolean = animatedWebpInfo(bytes).first >= 2

    /**
     * WhatsApp rejects animated packs whose files are single-frame WebPs or
     * whose frame durations are outside its limits. Check the RIFF chunks
     * before a file is exposed to WhatsApp.
     */
    fun validAnimatedWebp(bytes: ByteArray): Boolean {
        val (frames, minDuration, totalDuration) = animatedWebpInfo(bytes)
        return frames >= 2 && minDuration > 8 && totalDuration in 1..10_000
    }

    private fun animatedWebpInfo(bytes: ByteArray): Triple<Int, Int, Int> {
        if (bytes.size < 12) return Triple(0, 0, 0)
        if (ascii(bytes, 0, 4) != "RIFF" || ascii(bytes, 8, 4) != "WEBP") {
            return Triple(0, 0, 0)
        }

        var frames = 0
        var minDuration = Int.MAX_VALUE
        var totalDuration = 0
        var offset = 12
        while (offset + 8 <= bytes.size) {
            val id = ascii(bytes, offset, 4)
            val size = littleEndian32(bytes, offset + 4)
            if (id == "ANMF" && size >= 16 && offset + 24 <= bytes.size) {
                frames++
                val duration = littleEndian24(bytes, offset + 20)
                minDuration = minOf(minDuration, duration)
                totalDuration += duration
            }
            val next = offset.toLong() + 8 + size + (size and 1L)
            if (next > bytes.size) break
            offset = next.toInt()
        }

        return Triple(frames, if (frames == 0) 0 else minDuration, totalDuration)
    }

    private fun ascii(bytes: ByteArray, offset: Int, length: Int): String =
        String(bytes.copyOfRange(offset, offset + length), Charsets.US_ASCII)

    private fun littleEndian24(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16)

    private fun littleEndian32(bytes: ByteArray, offset: Int): Long =
        (bytes[offset].toLong() and 0xFF) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFF) shl 24)

    /** WhatsApp requires the tray icon to be exactly 96x96 PNG, so never downscale it. */
    @Suppress("DEPRECATION")
    fun makeTrayIcon(bitmap: Bitmap): ByteArray {
        val icon = bitmap.scale(TRAY_SIZE, TRAY_SIZE, false)
        var compact = ByteArray(0)
        for (config in arrayOf(Bitmap.Config.ARGB_8888, Bitmap.Config.ARGB_4444)) {
            val stream = ByteArrayOutputStream()
            val candidate = if (config == icon.config) icon else icon.copy(config, false)
            candidate.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val bytes = stream.toByteArray()
            if (config == Bitmap.Config.ARGB_4444) compact = bytes
            if (bytes.size <= MAX_TRAY_BYTES) return bytes
        }
        // ARGB_4444 at 96x96 is below 36 KB before compression, so this always fits.
        return compact
    }

    private fun fitInto(source: Bitmap, size: Int): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val scale = minOf(size.toFloat() / source.width, size.toFloat() / source.height)
        val width = source.width * scale
        val height = source.height * scale
        val left = (size - width) / 2f
        val top = (size - height) / 2f

        Canvas(output).drawBitmap(
            source,
            null,
            RectF(left, top, left + width, top + height),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
        return output
    }

    @Suppress("DEPRECATION")
    private fun compress(bitmap: Bitmap, lossy: Boolean, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        val format = when {
            !lossy && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Bitmap.CompressFormat.WEBP_LOSSLESS
            lossy && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Bitmap.CompressFormat.WEBP_LOSSY
            !lossy -> Bitmap.CompressFormat.WEBP // quality 100 means lossless below API 30
            else -> Bitmap.CompressFormat.WEBP
        }
        bitmap.compress(format, quality, stream)
        return stream.toByteArray()
    }

    private fun preservesAlpha(source: Bitmap, encoded: ByteArray): Boolean {
        val corners = arrayOf(
            0 to 0,
            source.width - 1 to 0,
            0 to source.height - 1,
            source.width - 1 to source.height - 1,
        )
        val sourceHasTransparentCorner = corners.any { (x, y) ->
            (source.getPixel(x, y) and 0xFF000000.toInt()) == 0
        }
        if (!sourceHasTransparentCorner) return true

        return try {
            val decoded = decodeStatic(encoded)
            corners.any { (x, y) ->
                val px = x.coerceIn(0, decoded.width - 1)
                val py = y.coerceIn(0, decoded.height - 1)
                (decoded.getPixel(px, py) and 0xFF000000.toInt()) == 0
            }
        } catch (e: Exception) {
            false
        }
    }
}
