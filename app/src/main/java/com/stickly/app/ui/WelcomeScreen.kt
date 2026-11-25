package com.stickly.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stickly.app.R

/** Animated first-launch screen. No tutorial steps — just the brand and a CTA. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    val float by rememberInfiniteTransition(label = "welcome").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logoFloat",
    )

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .offset(x = (-84).dp, y = (-200).dp)
                    .clip(MaterialShapes.Clover8Leaf.toShape())
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            )
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .offset(x = 100.dp, y = 240.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)),
            )

            AnimatedVisibility(
                visible = shown,
                enter = fadeIn(tween(500)) + scaleIn(initialScale = 0.9f, animationSpec = tween(600)),
                exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.95f, animationSpec = tween(200)),
            ) {
                Surface(
                    shape = RoundedCornerShape(36.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 8.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AppIcon(
                            modifier = Modifier.size(112.dp + (float * 8).dp),
                        )

                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = "Stickly",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Move your stickers from Telegram to WhatsApp",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth(),
                            shapes = ButtonShapes(
                                MaterialShapes.Clover8Leaf.toShape(),
                                MaterialShapes.Pill.toShape(),
                            ),
                        ) {
                            Text("Get started")
                        }
                    }
                }
            }
        }
    }
}
