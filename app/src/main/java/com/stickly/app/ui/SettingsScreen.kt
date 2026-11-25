package com.stickly.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.stickly.app.BuildConfig
import com.stickly.app.R
import com.stickly.app.data.DeveloperInfo
import com.stickly.app.data.ThemeMode
import com.stickly.app.util.copyText
import com.stickly.app.util.openUpiPayment
import com.stickly.app.util.openUrl

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: UiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    var token by remember { mutableStateOf(state.tokenInput) }
    var showTutorial by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = viewModel::openHome) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            state.hint?.let { text ->
                InAppHint(text = text, onDismiss = viewModel::consumeHint)
            }

            SectionLabel("Telegram")
            TelegramCard(
                token = token,
                onTokenChange = { token = it },
                saved = state.tokenSaved && token == state.tokenInput,
                onSave = { viewModel.setToken(token) },
                onTutorial = { showTutorial = true },
            )

            SectionLabel("Appearance")
            AppearanceCard(
                themeMode = state.themeMode,
                onThemeModeChange = viewModel::setThemeMode,
                dynamicColor = state.dynamicColor,
                onDynamicColorChange = viewModel::setDynamicColor,
            )

            SectionLabel("Support")
            SupportCard(
                onDonate = {
                    val started = context.openUpiPayment(
                        upiId = DeveloperInfo.UPI_ID,
                        payeeName = DeveloperInfo.PAYEE_NAME,
                        note = DeveloperInfo.UPI_NOTE,
                    )
                    if (!started) {
                        context.copyText("UPI ID", DeveloperInfo.UPI_ID)
                        viewModel.showHint("No UPI app found — UPI ID copied")
                    }
                },
                onCopy = {
                    if (context.copyText("UPI ID", DeveloperInfo.UPI_ID)) {
                        viewModel.showHint("UPI ID copied")
                    }
                },
            )

            SectionLabel("Developer")
            DeveloperCard { url ->
                if (!context.openUrl(url)) viewModel.showHint("No browser found to open that link")
            }

            SectionLabel("About")
            AboutCard(
                onOpen = { url ->
                    if (!context.openUrl(url)) viewModel.showHint("No browser found to open that link")
                },
            )
        }
    }

    if (showTutorial) {
        BotTutorialDialog(
            onDismiss = { showTutorial = false },
            onOpenBotFather = {
                showTutorial = false
                if (!context.openUrl("https://t.me/BotFather")) {
                    viewModel.showHint("No browser found to open @BotFather")
                }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TelegramCard(
    token: String,
    onTokenChange: (String) -> Unit,
    saved: Boolean,
    onSave: () -> Unit,
    onTutorial: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            SettingRow(
                icon = Icons.Filled.Check,
                title = "Telegram bot token",
                subtitle = "Needed to read sticker sets from the official Bot API.",
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Bot token") },
                placeholder = { Text("123456:ABC-def...") },
                singleLine = true,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onSave,
                    shapes = ButtonShapes(
                        MaterialShapes.Clover8Leaf.toShape(),
                        MaterialShapes.Pill.toShape(),
                    ),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save token")
                }
                if (saved) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Saved ✓",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            FilledTonalButton(
                onClick = onTutorial,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialShapes.Pill.toShape(),
            ) {
                Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Guide")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BotTutorialDialog(
    onDismiss: () -> Unit,
    onOpenBotFather: () -> Unit,
) {
    val steps = listOf(
        "Open @BotFather in Telegram and send /newbot.",
        "Choose a display name and a username ending in “bot”.",
        "Copy the access token BotFather gives you.",
        "Paste it into the bot token field above and tap Save token.",
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Guide",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Guide to create a bot token.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                steps.forEachIndexed { index, step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(26.dp),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (index < steps.lastIndex) Spacer(Modifier.height(10.dp))
                }

                Spacer(Modifier.height(22.dp))
                FilledTonalButton(
                    onClick = onOpenBotFather,
                    modifier = Modifier.fillMaxWidth(),
                    shapes = ButtonShapes(
                        MaterialShapes.Clover8Leaf.toShape(),
                        MaterialShapes.Pill.toShape(),
                    ),
                ) {
                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open @BotFather")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun AppearanceCard(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                    ) {
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.DARK -> "Dark"
                                ThemeMode.LIGHT -> "Light"
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Material You colors",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Use colors from your wallpaper",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = dynamicColor, onCheckedChange = onDynamicColorChange)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SupportCard(onDonate: () -> Unit, onCopy: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            SettingRow(
                icon = Icons.Filled.Favorite,
                title = "Support Stickly",
                subtitle = "Free, no ads, no tracking.",
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "A donation helps cover the time spent building features and fixing bugs. Any amount is appreciated.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onDonate,
                    modifier = Modifier.weight(1f),
                    shapes = ButtonShapes(
                        MaterialShapes.Clover8Leaf.toShape(),
                        MaterialShapes.Pill.toShape(),
                    ),
                ) { Text("Donate with UPI") }
                FilledTonalButton(
                    onClick = onCopy,
                    shape = MaterialShapes.Pill.toShape(),
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy")
                }
            }
        }
    }
}

@Composable
private fun DeveloperCard(onOpen: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.stickly_developer),
                    contentDescription = "Developer photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = DeveloperInfo.NAME,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "@${DeveloperInfo.HANDLE}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "Developer",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            DeveloperLinkRow(
                label = "GitHub",
                value = "github.com/${DeveloperInfo.HANDLE}",
                iconRes = R.drawable.ic_github,
                brandColor = Color(0xFF24292F),
                onClick = { onOpen(DeveloperInfo.GITHUB_URL) },
            )
            DeveloperLinkRow(
                label = "Telegram",
                value = "t.me/${DeveloperInfo.HANDLE}",
                iconRes = R.drawable.ic_telegram,
                brandColor = Color(0xFF229ED9),
                onClick = { onOpen(DeveloperInfo.TELEGRAM_URL) },
            )
        }
    }
}

@Composable
private fun AboutCard(onOpen: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Stickly",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Telegram stickers, new home. Import packs, split them for WhatsApp, and keep everything on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            SettingsActionRow(
                icon = Icons.Filled.Refresh,
                title = "Check for updates",
                subtitle = "View releases on GitHub",
                onClick = { onOpen(DeveloperInfo.RELEASES_URL) },
            )
            SettingsActionRow(
                icon = Icons.Filled.Code,
                title = "Source code",
                subtitle = "github.com/${DeveloperInfo.HANDLE}/Stickly",
                onClick = { onOpen(DeveloperInfo.REPO_URL) },
            )
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DeveloperLinkRow(
    label: String,
    value: String,
    iconRes: Int,
    brandColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(brandColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
