package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.random.Random

@Composable
fun StarryBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val starGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MidnightBase,
                        MidnightSurface,
                        Color(0xFF1B0B33),
                        MidnightBase
                    )
                )
            )
    ) {
        // Star canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Deterministic positions
            val starPositions = listOf(
                Offset(width * 0.12f, height * 0.08f),
                Offset(width * 0.85f, height * 0.14f),
                Offset(width * 0.25f, height * 0.22f),
                Offset(width * 0.72f, height * 0.35f),
                Offset(width * 0.15f, height * 0.48f),
                Offset(width * 0.88f, height * 0.58f),
                Offset(width * 0.35f, height * 0.72f),
                Offset(width * 0.65f, height * 0.82f),
                Offset(width * 0.82f, height * 0.90f),
                Offset(width * 0.18f, height * 0.92f)
            )

            starPositions.forEachIndexed { index, pos ->
                val factor = if (index % 2 == 0) starGlow else (1.2f - starGlow)
                val radius = (1.5f + (index % 3) * 0.8f) * factor
                drawCircle(
                    color = Color.White.copy(alpha = (0.4f + factor * 0.5f).coerceIn(0.2f, 1f)),
                    radius = radius,
                    center = Offset(pos.x, pos.y + floatingOffset * (if (index % 2 == 0) 1 else -1) * 0.5f)
                )
            }
        }

        content()
    }
}

@Composable
fun RomanticCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    borderColor: Color = MidnightCardBorder,
    backgroundColor: Color = MidnightCard,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor, shape = shape),
        shape = shape,
        color = backgroundColor,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun OpeningBookAnimation(
    modifier: Modifier = Modifier,
    isOpen: Boolean = false
) {
    val rotation by animateFloatAsState(
        targetValue = if (isOpen) -60f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "bookRotation"
    )

    Box(
        modifier = modifier
            .size(width = 160.dp, height = 200.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 16 * density
            },
        contentAlignment = Alignment.Center
    ) {
        // Book Cover
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, bottomStart = 4.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF2C1452),
                            Color(0xFF4C248A),
                            Color(0xFF5D2D9E)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = StarGoldTertiary.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, bottomStart = 4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = SoftRoseSecondary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Dairy Sayangg",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = StarGoldTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun RomanticEnvelope(
    title: String,
    content: String,
    author: String,
    template: String,
    modifier: Modifier = Modifier
) {
    var isOpen by remember { mutableStateOf(false) }

    val flapRotation by animateFloatAsState(
        targetValue = if (isOpen) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "flapRotation"
    )

    val letterOffset by animateFloatAsState(
        targetValue = if (isOpen) -60f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 200),
        label = "letterOffset"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isOpen = !isOpen }
            .testTag("envelope_card"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Envelope Body
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationY = letterOffset },
                shape = RoundedCornerShape(16.dp),
                color = if (isOpen) Color(0xFF2C164F) else MidnightCardElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, MidnightCardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = template,
                            color = LavenderPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isOpen) "Sentuh untuk melipat 💌" else "Sentuh untuk membuka 💜",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Dari: $author",
                        color = SoftRoseSecondary,
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    if (isOpen) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MidnightCardBorder)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }
    }
}
