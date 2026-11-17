package com.clove.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clove.ui.theme.LocalSafariPalette

/**
 * A true crystal "liquid glass" surface inspired by Kyant0/AndroidLiquidGlass & iOS 26 Safari:
 * - Rich optical frosted viscosity (milky frosted in light, obsidian smoky in dark)
 * - Inner refractive trough (2.4dp stroke) producing physical lens curvature & internal shadow
 * - 3D convex dome light glaze reflection (overhead light reflection on lens curvature)
 * - Fresnel caustic ambient glow on lower rim
 * - Continuous 45° incident specular rim highlight (dual incident specular light)
 * - 0.75dp inner crisp refraction bead
 * - Soft floating ambient drop shadow
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    elevation: Dp = 12.dp,
    chromaticAberration: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = LocalSafariPalette.current
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(if (palette.isDark) 0.35f else 0.10f),
                spotColor = Color.Black.copy(if (palette.isDark) 0.45f else 0.16f),
            )
            .clip(shape)
            .drawBehind {
                try {
                    val w = size.width
                    val h = size.height
                    if (w <= 0f || h <= 0f) return@drawBehind

                    val maxR = size.minDimension / 2f
                    val cornerRadius = when (shape) {
                        CircleShape -> maxR
                        is CornerBasedShape -> shape.topStart.toPx(size, density).coerceAtMost(maxR)
                        else -> maxR
                    }
                    val cRadius = CornerRadius(cornerRadius, cornerRadius)

                    // 1. LIQUID GLASS FROSTED BODY (Rich optical viscosity, never flat clear)
                    val bodyColor = if (palette.isDark) {
                        Color(0xFF1E1E26).copy(alpha = 0.78f)
                    } else {
                        Color(0xFFF6F6FA).copy(alpha = 0.76f)
                    }
                    drawRoundRect(color = bodyColor, cornerRadius = cRadius)

                    // 2. INNER REFRACTIVE TROUGH (Physical double-rim lens curvature)
                    drawRoundRect(
                        color = if (palette.isDark) Color.Black.copy(alpha = 0.35f) else Color(0xFF404050).copy(alpha = 0.12f),
                        style = Stroke(width = with(density) { 2.4.dp.toPx() }),
                        cornerRadius = cRadius,
                    )

                    // 3. 3D CONVEX GLASS DOME GLAZE (Upper specular curve)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (palette.isDark) 0.35f else 0.65f),
                                Color.White.copy(alpha = if (palette.isDark) 0.08f else 0.18f),
                                Color.Transparent,
                            ),
                            startY = 0f,
                            endY = h * 0.52f,
                        ),
                        cornerRadius = cRadius,
                    )

                    // 4. FRESNEL CAUSTIC GLOW (Lower liquid rim refraction)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (palette.isDark) 0.04f else 0.10f),
                                Color.White.copy(alpha = if (palette.isDark) 0.18f else 0.35f),
                            ),
                            startY = h * 0.65f,
                            endY = h,
                        ),
                        cornerRadius = cRadius,
                    )

                    // 5. 45° PHYSICAL SPECULAR RIM HIGHLIGHT (Kyant incident light)
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (palette.isDark) 0.85f else 0.95f),
                                Color.White.copy(alpha = if (palette.isDark) 0.25f else 0.40f),
                                Color.Transparent,
                                Color.White.copy(alpha = if (palette.isDark) 0.25f else 0.35f),
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w, h),
                        ),
                        style = Stroke(width = with(density) { 1.35.dp.toPx() }),
                        cornerRadius = cRadius,
                    )

                    // 6. INNER CRISP REFRACTION BEVEL (0.75dp edge bead)
                    drawRoundRect(
                        color = Color.White.copy(alpha = if (palette.isDark) 0.22f else 0.35f),
                        style = Stroke(width = with(density) { 0.75.dp.toPx() }),
                        cornerRadius = cRadius,
                    )
                } catch (_: Throwable) {}
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides palette.textPrimary) {
            content()
        }
    }
}

/**
 * A circular liquid glass button with tactile spring-bouncy feedback,
 * used for toolbar actions (back, forward, share, bookmarks, tabs).
 */
@Composable
fun GlassCircleButton(
    onClick: () -> Unit,
    size: Dp = 46.dp,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    // Juicy tactile water-drop spring animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "liquid-bounce",
    )

    GlassSurface(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                clickableCompat(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    enabled = enabled,
                    interactionSource = interaction,
                )
            ),
        shape = CircleShape,
        elevation = if (isPressed) 3.dp else 10.dp,
        chromaticAberration = false,
    ) {
        content()
    }
}
