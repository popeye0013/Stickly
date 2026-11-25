package com.stickly.app.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.stickly.app.data.PackRepository
import com.stickly.app.data.StickerEntry
import com.stickly.app.data.StickerPack
import com.stickly.app.whatsapp.WhatsAppExporter

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun PackDetailScreen(
    state: UiState,
    pack: StickerPack,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val repository = PackRepository(context.applicationContext)
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(pack.title) }
    var selectionMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showSelectionDialog by remember { mutableStateOf(false) }
    var selectionName by remember { mutableStateOf("") }
    var viewingSticker by remember { mutableStateOf<StickerEntry?>(null) }

    BackHandler(enabled = viewingSticker != null) { viewingSticker = null }

    fun clearSelection() {
        selectionMode = false
        selected = emptySet()
    }

    fun toggleSelection(file: String) {
        if (file in selected) {
            selected = selected - file
        } else if (selected.size >= 25) {
            viewModel.showHint("You can select up to 25 stickers.")
        } else {
            selected = selected + file
        }
    }

    val addLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.markAdded(pack.id)
            viewModel.showHint("Added to WhatsApp")
        }
    }

    val zipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) viewModel.exportZip(pack.id, uri)
    }

    fun launchAddIntent(ready: StickerPack) {
        val installed = WhatsAppExporter.installedPackages(context)
        val intent = if (installed.size > 1) {
            Intent.createChooser(WhatsAppExporter.addIntent(ready), "Add pack with")
        } else {
            WhatsAppExporter.addIntent(ready, installed.firstOrNull())
        }
        try {
            addLauncher.launch(intent)
            viewModel.showHint("Confirm the pack in WhatsApp")
        } catch (e: Exception) {
            viewModel.showHint("Couldn't open WhatsApp")
        }
    }

    fun launchAddToWhatsApp() {
        viewModel.prepareForWhatsApp(pack) { ready -> launchAddIntent(ready) }
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "${selected.size} selected",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Choose 3-25 stickers for WhatsApp",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = ::clearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (selected.size !in 3..25) {
                                viewModel.showHint("Select between 3 and 25 stickers.")
                            } else {
                                selectionName = "${pack.title} picks"
                                showSelectionDialog = true
                            }
                        }) {
                            Icon(Icons.Filled.Check, contentDescription = "Create WhatsApp pack")
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = pack.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = viewModel::openHome) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            newName = pack.title
                            showRenameDialog = true
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Rename pack")
                        }
                        IconButton(onClick = { viewModel.deletePack(pack.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete pack")
                        }
                    },
                )
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 108.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.hint?.let { hint ->
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    InAppHint(
                        text = hint,
                        onDismiss = viewModel::consumeHint,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                PackHero(
                    pack = pack,
                    repository = repository,
                    whatsAppInstalled = WhatsAppExporter.installedPackages(context).isNotEmpty(),
                    onAdd = ::launchAddToWhatsApp,
                    onExport = {
                        zipLauncher.launch("stickly-${pack.title.filter { it.isLetterOrDigit() }.take(24)}.zip")
                    },
                )
            }

            items(pack.stickers) { sticker ->
                val isSelected = sticker.file in selected
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (selectionMode) {
                                    toggleSelection(sticker.file)
                                } else {
                                    viewingSticker = sticker
                                }
                            },
                            onLongClick = {
                                selectionMode = true
                                toggleSelection(sticker.file)
                            },
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .height(110.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        StickerImage(
                            file = repository.file(pack.id, sticker.file),
                            animatedFile = sticker.sourceFile?.let { repository.file(pack.id, it) },
                            contentDescription = sticker.emoji.ifBlank { "Sticker" },
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (isSelected) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(4.dp).size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename pack") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Pack name") },
                    placeholder = { Text("My sticker pack") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renamePack(pack.id, newName)
                    showRenameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showSelectionDialog = false },
            title = { Text("Create WhatsApp pack") },
            text = {
                Column {
                    Text(
                        text = "A new pack will be made from ${selected.size} selected stickers. Stickly will be the author.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = selectionName,
                        onValueChange = { selectionName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Pack name") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createPackFromSelection(pack.id, selected.toList(), selectionName)
                    showSelectionDialog = false
                    clearSelection()
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelectionDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    viewingSticker?.let { sticker ->
        Dialog(onDismissRequest = { viewingSticker = null }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StickerImage(
                        file = repository.file(pack.id, sticker.file),
                        animatedFile = sticker.sourceFile?.let { repository.file(pack.id, it) },
                        contentDescription = sticker.emoji.ifBlank { "Sticker" },
                        modifier = Modifier.size(280.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = sticker.emoji.ifBlank { "Sticker" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tap outside to close",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PackHero(
    pack: StickerPack,
    repository: PackRepository,
    whatsAppInstalled: Boolean,
    onAdd: () -> Unit,
    onExport: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                StickerImage(
                    file = repository.file(pack.id, pack.trayFile),
                    contentDescription = pack.title,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = pack.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "by ${pack.publisher}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
            )
            if (pack.partCount > 1) {
                Text(
                    text = "Part ${pack.partIndex} of ${pack.partCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Long-press stickers to make a custom WhatsApp pack (up to 25).",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${pack.stickers.size} stickers" +
                    if (pack.animatedConverted > 0) "  ·  ${pack.animatedConverted} animated" else "",
                style = MaterialTheme.typography.labelLarge,
            )

            Spacer(Modifier.height(18.dp))
            if (pack.addedToWhatsApp) {
                Surface(
                    shape = MaterialShapes.Pill.toShape(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Added to WhatsApp",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            } else {
                Button(
                    onClick = onAdd,
                    enabled = whatsAppInstalled,
                    modifier = Modifier.fillMaxWidth(),
                    shapes = ButtonShapes(
                        MaterialShapes.Clover8Leaf.toShape(),
                        MaterialShapes.Pill.toShape(),
                    ),
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add to WhatsApp")
                }
            }

            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialShapes.Pill.toShape(),
            ) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export as ZIP")
            }

            if (!whatsAppInstalled) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Install WhatsApp to add packs directly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Link,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.Unspecified,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = pack.sourceUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
