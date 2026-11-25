package com.stickly.app.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Stores packs in internal storage:
 *   files/packs/<id>/pack.json
 *   files/packs/<id>/tray.png
 *   files/packs/<id>/s01.webp ...
 */
class PackRepository(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, "packs").apply { mkdirs() }
    private val gson = Gson()

    fun list(): List<StickerPack> =
        root.listFiles { it.isDirectory }
            ?.mapNotNull(::readPack)
            ?.sortedByDescending { it.importedAt }
            ?: emptyList()

    fun get(id: String): StickerPack? = readPack(directory(id))

    fun prepare(id: String): File = directory(id).apply { mkdirs() }

    fun save(pack: StickerPack) {
        synchronized(this) {
            File(directory(pack.id), INDEX_FILE).writeText(gson.toJson(pack))
        }
    }

    fun delete(id: String) {
        directory(id).deleteRecursively()
    }

    fun markAdded(id: String, added: Boolean) {
        val pack = get(id) ?: return
        save(pack.copy(addedToWhatsApp = added))
    }

    fun rename(id: String, title: String) {
        val pack = get(id) ?: return
        save(
            pack.copy(
                title = title,
                imageDataVersion = System.currentTimeMillis().toString(),
            ),
        )
    }

    fun file(id: String, name: String): File {
        require(!name.contains('/') && !name.contains('\\') && name !in setOf(".", "..")) {
            "Unsafe file name: $name"
        }
        return File(directory(id), name)
    }

    fun exportZip(pack: StickerPack, target: Uri): Boolean = try {
        val output = appContext.contentResolver.openOutputStream(target) ?: return false
        ZipOutputStream(output).use { zip ->
            directory(pack.id).listFiles()?.filter { it.isFile }?.forEach { file ->
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        true
    } catch (e: Exception) {
        false
    }

    private fun directory(id: String): File = File(root, id.replace(Regex("[^A-Za-z0-9_-]"), "_"))

    private fun readPack(dir: File): StickerPack? = try {
        val index = File(dir, INDEX_FILE)
        if (!index.exists()) null else gson.fromJson(index.readText(), StickerPack::class.java)
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val INDEX_FILE = "pack.json"
    }
}
