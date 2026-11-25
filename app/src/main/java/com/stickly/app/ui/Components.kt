package com.stickly.app.ui

import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.stickly.app.R
import kotlinx.coroutines.delay
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.GZIPInputStream

@Composable
fun BrandMark(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onPrimary) {
    Image(
        painter = painterResource(R.drawable.brand_mark),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier,
    )
}

/** The actual launcher icon, for places that should show the app itself. */
@Composable
fun AppIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = "Stickly",
) {
    Image(
        painter = painterResource(R.drawable.app_icon),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}

@Composable
fun StickerImage(
    file: File?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    animatedFile: File? = null,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val animated = animatedFile?.takeIf { it.exists() }
        when (animated?.extension?.lowercase()) {
            "tgs" -> TgsSticker(
                file = animated,
                fallbackFile = file,
                contentDescription = contentDescription,
                contentScale = contentScale,
            )

            "gif", "webp" -> AsyncImage(
                model = animated,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )

            // Video stickers are exported as animated WebP. Play that file instead
            // of the original WebM, which Android's VideoView cannot decode here.
            "webm", "mp4" -> if (file != null && file.exists()) {
                AsyncImage(
                    model = file,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            else -> if (file != null && file.exists()) {
                AsyncImage(
                    model = file,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun TgsSticker(
    file: File,
    fallbackFile: File?,
    contentDescription: String?,
    contentScale: ContentScale,
) {
    var composition by remember(file) { mutableStateOf<LottieComposition?>(null) }
    LaunchedEffect(file) {
        composition = runCatching {
            val json = GZIPInputStream(file.inputStream()).use { it.readBytes() }
            LottieCompositionFactory
                .fromJsonInputStreamSync(ByteArrayInputStream(json), file.path)
                .value
        }.getOrNull()
    }

    val current = composition
    val description = contentDescription
    if (current != null) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                LottieAnimationView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    scaleType = when (contentScale) {
                        ContentScale.Crop -> ImageView.ScaleType.CENTER_CROP
                        else -> ImageView.ScaleType.FIT_CENTER
                    }
                    setComposition(current)
                    repeatCount = LottieDrawable.INFINITE
                    playAnimation()
                    this.contentDescription = description
                }
            },
            update = { view ->
                view.setComposition(current)
                view.repeatCount = LottieDrawable.INFINITE
                view.playAnimation()
            },
        )
    } else if (fallbackFile != null && fallbackFile.exists()) {
        AsyncImage(
            model = fallbackFile,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayfulCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Box(Modifier.padding(14.dp)) {
            content()
        }
    }
}

/** Persistent in-app feedback. Replaces transient toast/snackbar messages. */
@Composable
fun InAppHint(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(text) {
        delay(3_000)
        onDismiss()
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss hint",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}
