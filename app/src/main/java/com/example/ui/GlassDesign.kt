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
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.text.font.FontWeight

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
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val solidColor = if (isLight) Color(0xFFF7F7F9) else Color(0xFF282A3A)
    val borderColor = if (isLight) Color(0xFFE0E0E0) else Color(0xFF3F415A)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(solidColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}

// ── FloatingGlassNavBar: the iOS 26-style floating tab bar ─────
@Composable
fun FloatingGlassNavBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(percent = 50))
                .background(Color(0xFFF0F0F5)) // Matte background
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            content()
        }
    }
}

@Composable
fun IosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        if (label.isNotBlank()) {
            Text(
                text = label.uppercase(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
            )
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF3F415A)),
            readOnly = readOnly,
            enabled = enabled,
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = AccentBlue,
                disabledContainerColor = Color.Transparent,
                disabledTextColor = Color.White
            )
        )
    }
}
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
