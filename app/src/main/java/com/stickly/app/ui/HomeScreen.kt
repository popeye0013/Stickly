package com.stickly.app.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.stickly.app.R
import com.stickly.app.data.PackRepository
import com.stickly.app.data.StickerPack

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState,
    viewModel: MainViewModel,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val repository = PackRepository(LocalContext.current.applicationContext)
    var showSetupDialog by remember { mutableStateOf(false) }
    var setupName by remember { mutableStateOf(state.packNameInput) }
    var packToDelete by remember { mutableStateOf<StickerPack?>(null) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Stickly",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::openSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 164.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.hint?.let { text ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    InAppHint(
                        text = text,
                        onDismiss = viewModel::consumeHint,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                ImportHero(
                    state = state,
                    viewModel = viewModel,
                    onImportClick = {
                        setupName = state.packNameInput
                        showSetupDialog = true
                    },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Your packs",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${state.packs.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.packs.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState()
                }
            }

            items(state.packs) { pack ->
                PackCard(
                    pack = pack,
                    repository = repository,
                    onClick = { viewModel.openPack(pack.id) },
                    onLongClick = { packToDelete = pack },
                )
            }
        }
    }

    if (showSetupDialog) {
        ImportSetupDialog(
            initialName = setupName,
            onDismiss = { showSetupDialog = false },
            onConfirm = { name ->
                showSetupDialog = false
                viewModel.importPack(name)
            },
        )
    }

    packToDelete?.let { pack ->
        DeletePackDialog(
            title = pack.title,
            onDismiss = { packToDelete = null },
            onConfirm = {
                viewModel.deletePack(pack.id)
                packToDelete = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DeletePackDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(24.dp)) {
                Surface(
                    shape = MaterialShapes.Clover8Leaf.toShape(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Delete pack?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "“$title” will be removed from Stickly. This can’t be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    shapes = ButtonShapes(
                        MaterialShapes.Clover8Leaf.toShape(),
                        MaterialShapes.Pill.toShape(),
                    ),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportHero(
    state: UiState,
    viewModel: MainViewModel,
    onImportClick: () -> Unit,
) {
    val context = LocalContext.current

    // Plain rounded card: no decorative shapes behind or over the text.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Telegram stickers,\nnew home.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Paste a t.me/addstickers link. Every sticker is imported; large packs are split into WhatsApp-sized sets of 30.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
            )
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = state.urlInput,
                onValueChange = viewModel::setUrl,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Sticker pack URL") },
                placeholder = { Text("t.me/addstickers/PackName") },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clip.primaryClip?.getItemAt(0)?.text?.toString()?.let(viewModel::setUrl)
                        },
                    ) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = "Paste link")
                    }
                },
            )

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onImportClick,
                    enabled = state.importState !is ImportState.Running,
                    shapes = ButtonShapes(
                        MaterialShapes.Cookie9Sided.toShape(),
                        MaterialShapes.Pill.toShape(),
                    ),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Import pack")
                }
                Spacer(Modifier.width(12.dp))
                if (state.tokenInput.isBlank()) {
                    Text(
                        text = "Bot token needed in Settings",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PackCard(
    pack: StickerPack,
    repository: PackRepository,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    PlayfulCard(onClick = onClick, onLongClick = onLongClick) {
        Column {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                StickerImage(
                    file = repository.file(pack.id, pack.trayFile),
                    contentDescription = pack.title,
                    modifier = Modifier.size(76.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = pack.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${pack.stickers.size} stickers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (pack.partCount > 1) {
                Text(
                    text = "Part ${pack.partIndex} of ${pack.partCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (pack.addedToWhatsApp) {
                Surface(
                    shape = MaterialShapes.Pill.toShape(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Added to WhatsApp",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            } else {
                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(
                            text = "Add to WhatsApp",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandMark(
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Nothing here yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Your imported Telegram packs will show up here, ready to hop over to WhatsApp.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
