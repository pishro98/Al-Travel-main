package com.example.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Core glass colors ──────────────────────────────────────────
val GlassWhite    = Color.White.copy(alpha = 0.18f)
val GlassBorder   = Color.White.copy(alpha = 0.35f)
val GlassDark     = Color(0xFF1C1C2E).copy(alpha = 0.55f)
val GlassBlack    = Color.Black.copy(alpha = 0.12f)
val AccentBlue    = Color(0xFF3A7BFF)
val AccentPurple  = Color(0xFF9B59FF)
val AccentTeal    = Color(0xFF00C9B1)
val GradientTravel = Brush.linearGradient(
    colors = listOf(Color(0xFF1A1A3E), Color(0xFF0D2B55), Color(0xFF003344))
)

// ── GlassCard: the core reusable glass surface ─────────────────
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    alpha: Float = 0.18f,
    content: @Composable BoxScope.() -> Unit
) {
    val glassColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5f)
        Color.White.copy(alpha = alpha)
    else
        Color.White.copy(alpha = alpha * 0.6f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        glassColor,
                        glassColor.copy(alpha = alpha * 0.5f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        GlassBorder,
                        GlassBorder.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .applyBlur(12f),
        content = content
    )
}

// ── Blur modifier: real blur on Android 12+, clean fallback ────
fun Modifier.applyBlur(radius: Float): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.graphicsLayer {
            renderEffect = android.graphics.RenderEffect
                .createBlurEffect(radius, radius, android.graphics.Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
        }
    } else {
        // Fallback: slightly more opaque surface for older devices
        this.background(Color.White.copy(alpha = 0.08f))
    }
}

// ── FloatingGlassNavBar: the iOS 26-style floating tab bar ─────
@Composable
fun FloatingGlassNavBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E1E3A).copy(alpha = 0.85f),
                            Color(0xFF12122A).copy(alpha = 0.92f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .applyBlur(20f)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            content()
        }
    }
}

// ── GlassScrim: transparent gradient overlay for hero sections ─
@Composable
fun GlassScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.3f),
                    Color.Black.copy(alpha = 0.7f)
                )
            )
        )
    )
}
