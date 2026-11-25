package com.stickly.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stickly.app.data.PackRepository
import com.stickly.app.data.Preferences
import com.stickly.app.data.StickerEntry
import com.stickly.app.data.ThemeMode
import com.stickly.app.data.StickerPack
import com.stickly.app.telegram.TelegramClient
import com.stickly.app.util.StickerProcessor
import com.stickly.app.whatsapp.StickerContentProvider
import com.stickly.app.whatsapp.WhatsAppExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

private const val MAX_STICKERS_PER_PACK = 30
private const val MAX_SELECTION = 25
private const val MAX_NAME_LENGTH = 128
private const val DEFAULT_PUBLISHER = "Stickly"

sealed interface Screen {
    data object Welcome : Screen
    data object Home : Screen
    data object Settings : Screen
    data object Profile : Screen
    data class Detail(val packId: String) : Screen
}

sealed interface ImportState {
    data object Idle : ImportState
    data class Running(val stage: String, val current: Int, val total: Int) : ImportState
    data class Success(val message: String) : ImportState
    data class Error(val message: String) : ImportState
}

data class UiState(
    val packs: List<StickerPack> = emptyList(),
    val screen: Screen = Screen.Home,
    val urlInput: String = "",
    val packNameInput: String = "",
    val userName: String = "",
    val importState: ImportState = ImportState.Idle,
    val tokenInput: String = "",
    val tokenSaved: Boolean = false,
    val dynamicColor: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isWhatsAppInstalled: Boolean = false,
    val hint: String? = null,
)

private data class PreparedSticker(
    val emoji: String,
    val bytes: ByteArray,
    /** True when [bytes] is an animated WebP. */
    val animatedOutput: Boolean,
    val animatedSource: Boolean,
    val sourceBytes: ByteArray? = null,
    val sourceExt: String? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = PackRepository(app)
    private val preferences = Preferences(app)
    private var importJob: Job? = null

    private val _state = MutableStateFlow(
        UiState(
            urlInput = preferences.lastUrl,
            packNameInput = preferences.lastPackName,
            userName = preferences.userName,
            tokenInput = preferences.botToken,
            dynamicColor = preferences.dynamicColor,
            themeMode = preferences.themeMode,
        ),
    )
    val state: StateFlow<UiState> = _state

    init {
        _state.update {
            it.copy(screen = if (preferences.hasSeenWelcome) Screen.Home else Screen.Welcome)
        }
        refresh()
    }

    private fun refresh() {
        _state.update {
            it.copy(
                packs = repository.list(),
                isWhatsAppInstalled = WhatsAppExporter.installedPackages(getApplication()).isNotEmpty(),
            )
        }
    }

    fun setUrl(value: String) = _state.update { it.copy(urlInput = value) }

    fun setToken(value: String) {
        preferences.botToken = value
        _state.update {
            it.copy(
                tokenInput = preferences.botToken,
                tokenSaved = true,
                hint = "Bot token saved on this device",
            )
        }
    }

    fun setDynamicColor(value: Boolean) {
        preferences.dynamicColor = value
        _state.update { it.copy(dynamicColor = value) }
    }

    fun setThemeMode(value: ThemeMode) {
        preferences.themeMode = value
        _state.update { it.copy(themeMode = value) }
    }

    fun setUserName(value: String) {
        val name = value.replace(Regex("[\\r\\n]+"), " ").trim().take(40)
        preferences.userName = name
        _state.update { it.copy(userName = name, hint = "Profile saved") }
    }

    fun finishWelcome() {
        preferences.hasSeenWelcome = true
        _state.update { it.copy(screen = Screen.Home) }
    }

    fun openHome() = _state.update { it.copy(screen = Screen.Home) }
    fun openSettings() = _state.update { it.copy(screen = Screen.Settings) }
    fun openProfile() = _state.update { it.copy(screen = Screen.Profile) }
    fun openPack(id: String) = _state.update { it.copy(screen = Screen.Detail(id)) }

    fun consumeHint() = _state.update { it.copy(hint = null) }

    fun showHint(text: String) = _state.update { it.copy(hint = text) }

    fun importPack(nameInput: String) {
        if (importJob?.isActive == true) return

        val packName = nameInput.replace(Regex("[\\r\\n]+"), " ").trim().take(MAX_NAME_LENGTH)
        if (packName.isEmpty()) {
            _state.update { it.copy(hint = "Enter a pack name.") }
            return
        }

        val input = _state.value.urlInput
        _state.update { it.copy(packNameInput = packName) }

        val createdIds = mutableListOf<String>()
        importJob = viewModelScope.launch {
            try {
                val name = TelegramClient.parsePackName(input)
                val token = preferences.botToken
                if (token.isBlank()) {
                    _state.update {
                        it.copy(
                            screen = Screen.Settings,
                            importState = ImportState.Idle,
                            hint = "Add a free @BotFather token in Settings to import packs.",
                        )
                    }
                    return@launch
                }

                progress("Connecting to Telegram", 0, 0)
                val set = TelegramClient.getStickerSet(token, name)
                val allStickers = set.stickers.orEmpty()
                if (allStickers.size < 3) {
                    throw IllegalArgumentException(
                        "This pack has only ${allStickers.size} stickers. WhatsApp needs at least 3.",
                    )
                }

                val app = getApplication<Application>()

                // WhatsApp forbids mixed packs, so decide once for the whole set. Static
                // stickers in an animated set are converted in parallel with the rest.
                val animatedSet = allStickers.any { it.isAnimated || it.isVideo }
                val permits = (Runtime.getRuntime().availableProcessors() * 2).coerceIn(4, 6)
                val limit = Semaphore(permits)
                val completed = AtomicInteger(0)
                val prepared: List<PreparedSticker> = coroutineScope {
                    allStickers.mapIndexed { index, sticker ->
                        async(Dispatchers.IO) {
                            limit.withPermit {
                                currentCoroutineContext().ensureActive()

                                val emoji = sticker.emoji?.takeIf { it.isNotBlank() } ?: "😀"
                                var animatedSource = sticker.isAnimated || sticker.isVideo
                                var sourceBytesForApp: ByteArray? = null
                                var sourceExtForApp: String? = null
                                val bytes = try {
                                    val filePath = TelegramClient.getFile(token, sticker.fileId)
                                        ?: throw IllegalArgumentException("No file path returned")
                                    val sourceBytes = TelegramClient.downloadFile(token, filePath)

                                    // Detect PNG/WebP/TGS/WebM from the bytes themselves.
                                    val format = StickerProcessor.detectSourceFormat(sourceBytes)
                                    val tgsSource = sticker.isAnimated ||
                                        format == StickerProcessor.SourceFormat.TGS
                                    val videoSource = sticker.isVideo ||
                                        format == StickerProcessor.SourceFormat.VIDEO
                                    animatedSource = tgsSource || videoSource

                                    // Keep animated sources so Stickly can play TGS/WebM stickers in-app.
                                    sourceBytesForApp = sourceBytes.takeIf { animatedSource }
                                    sourceExtForApp = when {
                                        tgsSource -> "tgs"
                                        videoSource -> "webm"
                                        else -> null
                                    }

                                    when {
                                        tgsSource -> {
                                            val temp = File.createTempFile(
                                                "stickly_tgs_",
                                                ".webp",
                                                app.cacheDir,
                                            )
                                            try {
                                                StickerProcessor.encodeAnimatedTgs(app, sourceBytes, temp)
                                                    ?: StickerProcessor.animatedFallback(app, emoji)
                                            } finally {
                                                temp.delete()
                                            }
                                        }

                                        videoSource -> {
                                            val temp = File.createTempFile(
                                                "stickly_video_sticker_",
                                                ".webp",
                                                app.cacheDir,
                                            )
                                            try {
                                                StickerProcessor.encodeAnimatedVideo(
                                                    app,
                                                    sourceBytes,
                                                    app.cacheDir,
                                                    temp,
                                                ) ?: StickerProcessor.animatedFallback(app, emoji)
                                            } finally {
                                                temp.delete()
                                            }
                                        }

                                        animatedSet -> {
                                            val temp = File.createTempFile(
                                                "stickly_static_to_anim_",
                                                ".webp",
                                                app.cacheDir,
                                            )
                                            try {
                                                StickerProcessor.encodeAnimatedStatic(
                                                    app,
                                                    StickerProcessor.decodeStatic(sourceBytes),
                                                    temp,
                                                ) ?: StickerProcessor.animatedFallback(app, emoji)
                                            } finally {
                                                temp.delete()
                                            }
                                        }

                                        else -> StickerProcessor.encodeWhatsAppSticker(
                                            StickerProcessor.fitToSticker(
                                                StickerProcessor.decodeStatic(sourceBytes),
                                            ),
                                        ) ?: StickerProcessor.staticFallback(emoji)
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    sourceBytesForApp = null
                                    sourceExtForApp = null
                                    if (animatedSet || animatedSource) StickerProcessor.animatedFallback(app, emoji) else StickerProcessor.staticFallback(emoji)
                                }

                                val result = PreparedSticker(
                                    emoji = emoji,
                                    bytes = bytes,
                                    animatedOutput = animatedSet || animatedSource,
                                    animatedSource = animatedSource,
                                    sourceBytes = sourceBytesForApp,
                                    sourceExt = sourceExtForApp,
                                )
                                progress("Importing stickers", completed.incrementAndGet(), allStickers.size)
                                index to result
                            }
                        }
                    }.awaitAll()
                        .sortedBy { it.first }
                        .map { it.second }
                }

                currentCoroutineContext().ensureActive()
                val sizes = balancedSplit(prepared.size)
                progress("Saving ${sizes.size} WhatsApp pack${if (sizes.size == 1) "" else "s"}", 1, 1)

                var cursor = 0
                sizes.forEachIndexed { partIndex, size ->
                    currentCoroutineContext().ensureActive()
                    val part = prepared.subList(cursor, cursor + size)
                    cursor += size

                    val partCount = sizes.size
                    val id = partId(name, partIndex, partCount, createdIds)
                    val directory = repository.prepare(id)
                    createdIds += id

                    // WhatsApp does not allow mixing static and animated stickers in
                    // one pack. If any sticker animates, every sticker becomes animated.
                    val animatedPack = part.any { it.animatedSource }
                    val entries = part.mapIndexed { stickerIndex, sticker ->
                        val base = "s${String.format("%03d", stickerIndex + 1)}"
                        val fileName = "$base.webp"
                        val outputBytes = if (animatedPack && !sticker.animatedOutput) {
                            val temp = File.createTempFile("stickly_static_", ".webp", app.cacheDir)
                            try {
                                StickerProcessor.encodeAnimatedStatic(
                                    app,
                                    try {
                                        StickerProcessor.decodeStatic(sticker.bytes)
                                    } catch (e: Exception) {
                                        StickerProcessor.fallbackSticker(sticker.emoji)
                                    },
                                    temp,
                                )?.takeIf { it.isNotEmpty() }
                                    ?: StickerProcessor.animatedFallback(app, sticker.emoji)
                            } finally {
                                temp.delete()
                            }
                        } else {
                            sticker.bytes
                        }
                        File(directory, fileName).writeBytes(outputBytes)
                        val sourceFile = sticker.sourceBytes?.let { sourceBytes ->
                            val sourceName = "$base.${sticker.sourceExt ?: "tgs"}"
                            File(directory, sourceName).writeBytes(sourceBytes)
                            sourceName
                        }
                        StickerEntry(fileName, sticker.emoji, sourceFile)
                    }

                    val first = try {
                        StickerProcessor.decodeStatic(part.first().bytes)
                    } catch (e: Exception) {
                        StickerProcessor.fallbackSticker(part.first().emoji)
                    }
                    File(directory, "tray.png").writeBytes(StickerProcessor.makeTrayIcon(first))

                    repository.save(
                        StickerPack(
                            id = id,
                            title = partTitle(packName, partIndex, partCount),
                            publisher = DEFAULT_PUBLISHER,
                            sourceUrl = input.trim(),
                            importedAt = System.currentTimeMillis() + partIndex,
                            trayFile = "tray.png",
                            stickers = entries,
                            animatedConverted = part.count { it.animatedSource },
                            animated = animatedPack,
                            imageDataVersion = System.currentTimeMillis().toString(),
                            partIndex = partIndex + 1,
                            partCount = partCount,
                        ),
                    )
                }

                preferences.lastUrl = ""
                preferences.lastPackName = packName
                StickerContentProvider.notifyPacksChanged(getApplication())
                refresh()

                val summary = if (sizes.size == 1) {
                    "Imported ${prepared.size} stickers"
                } else {
                    "Imported ${prepared.size} stickers into ${sizes.size} packs"
                }
                _state.update {
                    it.copy(
                        urlInput = "",
                        packNameInput = packName,
                        importState = ImportState.Success(summary),
                        screen = if (sizes.size == 1) Screen.Detail(createdIds.first()) else Screen.Home,
                    )
                }
            } catch (e: CancellationException) {
                createdIds.forEach(repository::delete)
                _state.update { it.copy(importState = ImportState.Idle) }
            } catch (e: Exception) {
                createdIds.forEach(repository::delete)
                _state.update {
                    it.copy(
                        importState = ImportState.Error(e.message ?: "Import failed"),
                        hint = e.message ?: "Import failed",
                    )
                }
            }
        }
    }

    fun createPackFromSelection(sourceId: String, selectedFiles: List<String>, titleInput: String) {
        val title = titleInput.replace(Regex("[\\r\\n]+"), " ").trim().take(MAX_NAME_LENGTH)
        if (title.isEmpty()) {
            _state.update { it.copy(hint = "Enter a pack name.") }
            return
        }
        if (selectedFiles.size !in 3..MAX_SELECTION) {
            _state.update { it.copy(hint = "Select between 3 and $MAX_SELECTION stickers.") }
            return
        }

        val source = repository.get(sourceId) ?: return
        val selected = source.stickers.filter { it.file in selectedFiles }
        if (selected.size != selectedFiles.size || selected.any { !repository.file(sourceId, it.file).exists() }) {
            _state.update { it.copy(hint = "Some selected stickers are missing.") }
            return
        }

        viewModelScope.launch {
            val safeSource = sourceId.replace(Regex("[^A-Za-z0-9_-]"), "_").take(110)
            val id = "sel_${safeSource}_${UUID.randomUUID().toString().take(8)}"
            val directory = repository.prepare(id)
            try {
                val entries = selected.mapIndexed { index, sticker ->
                    val base = "s${String.format("%03d", index + 1)}"
                    val fileName = "$base.webp"
                    File(directory, fileName).writeBytes(repository.file(sourceId, sticker.file).readBytes())
                    val sourceFile = sticker.sourceFile?.let { original ->
                        val originalFile = repository.file(sourceId, original)
                        if (originalFile.exists()) {
                            val name = "$base.${original.substringAfterLast('.', "tgs")}"
                            File(directory, name).writeBytes(originalFile.readBytes())
                            name
                        } else {
                            null
                        }
                    }
                    StickerEntry(fileName, sticker.emoji.ifBlank { "😀" }, sourceFile)
                }
                val firstBytes = repository.file(sourceId, selected.first().file).readBytes()
                File(directory, "tray.png").writeBytes(
                    StickerProcessor.makeTrayIcon(StickerProcessor.decodeStatic(firstBytes)),
                )

                repository.save(
                    StickerPack(
                        id = id,
                        title = title,
                        publisher = DEFAULT_PUBLISHER,
                        sourceUrl = source.sourceUrl,
                        importedAt = System.currentTimeMillis(),
                        trayFile = "tray.png",
                        stickers = entries,
                        animated = source.animated,
                        animatedConverted = if (source.animated) entries.size else 0,
                        imageDataVersion = System.currentTimeMillis().toString(),
                    ),
                )
                StickerContentProvider.notifyPacksChanged(getApplication())
                refresh()
                _state.update {
                    it.copy(
                        screen = Screen.Detail(id),
                        hint = "Created a WhatsApp pack from ${entries.size} stickers",
                    )
                }
            } catch (e: Exception) {
                directory.deleteRecursively()
                _state.update { it.copy(hint = e.message ?: "Could not create the pack") }
            }
        }
    }

    fun cancelImport() {
        importJob?.cancel()
        importJob = null
        _state.update { it.copy(importState = ImportState.Idle, hint = "Import cancelled") }
    }

    fun dismissImportResult() = _state.update { it.copy(importState = ImportState.Idle) }

    fun deletePack(id: String) {
        repository.delete(id)
        StickerContentProvider.notifyPacksChanged(getApplication())
        refresh()
        _state.update {
            it.copy(
                screen = Screen.Home,
                hint = "Pack deleted",
            )
        }
    }

    fun markAdded(id: String) {
        repository.markAdded(id, true)
        refresh()
    }

    /** Repairs legacy packs before handing them to WhatsApp. */
    fun prepareForWhatsApp(pack: StickerPack, onReady: (StickerPack) -> Unit) {
        viewModelScope.launch {
            showHint("Preparing pack for WhatsApp…")
            val result = withContext(Dispatchers.IO) {
                WhatsAppExporter.prepare(getApplication(), pack)
            }
            result
                .onSuccess { ready ->
                    refresh()
                    onReady(ready)
                }
                .onFailure { error ->
                    showHint(error.message ?: "This pack can't be added to WhatsApp")
                }
        }
    }

    fun renamePack(id: String, title: String) {
        val name = title.replace(Regex("[\\r\\n]+"), " ").trim().take(MAX_NAME_LENGTH)
        if (name.isEmpty()) {
            _state.update { it.copy(hint = "Pack name can't be empty") }
            return
        }
        repository.rename(id, name)
        StickerContentProvider.notifyPacksChanged(getApplication())
        refresh()
        _state.update { it.copy(hint = "Pack renamed") }
    }

    fun exportZip(packId: String, target: Uri) {
        val pack = repository.get(packId)
        val ok = pack != null && repository.exportZip(pack, target)
        _state.update {
            it.copy(hint = if (ok) "ZIP exported" else "Could not export the ZIP")
        }
    }

    fun currentPack(): StickerPack? = when (val screen = _state.value.screen) {
        is Screen.Detail -> repository.get(screen.packId)
        else -> null
    }

    fun packById(id: String): StickerPack? = repository.get(id)

    private fun progress(stage: String, current: Int, total: Int) {
        _state.update { it.copy(importState = ImportState.Running(stage, current, total)) }
    }

    /** Keep every WhatsApp pack within 3-30 stickers while spreading them evenly. */
    private fun balancedSplit(total: Int): List<Int> {
        val groups = minOf((total + MAX_STICKERS_PER_PACK - 1) / MAX_STICKERS_PER_PACK, total / 3)
            .coerceAtLeast(1)
        val base = total / groups
        val remainder = total % groups
        return List(groups) { index -> base + if (index < remainder) 1 else 0 }
    }

    private fun partTitle(base: String, partIndex: Int, partCount: Int): String {
        if (partCount == 1) return base.take(MAX_NAME_LENGTH)
        val suffix = " (${partIndex + 1}/$partCount)"
        return base.take((MAX_NAME_LENGTH - suffix.length).coerceAtLeast(1)) + suffix
    }

    private fun partId(baseName: String, partIndex: Int, partCount: Int, createdIds: List<String>): String {
        // WhatsApp caps identifiers at 128 characters, including the split suffix.
        val suffix = if (partCount == 1) "" else "_p${partIndex + 1}"
        val random = UUID.randomUUID().toString().take(8)
        val budget = (128 - suffix.length - random.length - 3).coerceAtLeast(1)
        val safeName = baseName.replace(Regex("[^A-Za-z0-9_]"), "_").take(budget)
        val candidate = "tg_${safeName}_$random$suffix"
        return if (candidate in createdIds) {
            candidate + "_${System.currentTimeMillis().toString().take(8)}"
        } else candidate
    }
}
