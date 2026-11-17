package com.clove.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RuntimeShader
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clove.ui.theme.LocalSafariPalette
import org.intellij.lang.annotations.Language
import kotlin.math.PI

// ============================================================================
// AGSL Shaders from Kyant0/AndroidLiquidGlass
// ============================================================================

@Language("AGSL")
private const val RoundedRectSDF = """
float radiusAt(float2 centeredCoord, float4 radii) {
    if (centeredCoord.x >= 0.0) {
        if (centeredCoord.y <= 0.0) return radii.y;
        else return radii.z;
    } else {
        if (centeredCoord.y <= 0.0) return radii.x;
        else return radii.w;
    }
}

float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    float outside = length(max(cornerCoord, 0.0)) - radius;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        return sign(coord) * normalize(max(cornerCoord, 0.0));
    } else {
        float gradX = step(cornerCoord.y, cornerCoord.x);
        return sign(coord) * float2(gradX, 1.0 - gradX);
    }
}
"""

/**
 * Kyant's physical 45° incident light highlight shader from AndroidLiquidGlass.
 * Calculates exact SDF normals and shines dynamic specular light highlights
 * on the curved rim of capsules and rounded rectangles.
 */
@Language("AGSL")
private const val KyantHighlightShaderString = """
uniform float2 size;
uniform float4 cornerRadii;
layout(color) uniform half4 color;
uniform float angle;
uniform float falloff;

$RoundedRectSDF

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    float radius = radiusAt(centeredCoord, cornerRadii);
    
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);
    float2 normal = float2(cos(angle), sin(angle));
    float d = dot(grad, normal);
    float intensity = pow(abs(d), falloff);
    return color * intensity;
}
"""

/**
 * A true crystal "liquid glass" surface inspired by Kyant0/AndroidLiquidGlass:
 * - High optical transparency: genuine see-through translucent liquid body (never opaque)
 * - Continuous SDF rounded-rect physical specular rim highlight (Kyant's 45° incident light)
 * - 3D convex glass glaze highlight
 * - Bottom Fresnel ambient glow
 * - 0.65dp inner glass bevel refraction rim
 * - Ambient drop shadow
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

    val highlightShader = remember {
        try {
            RuntimeShader(KyantHighlightShaderString)
        } catch (_: Throwable) {
            null
        }
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(if (palette.isDark) 0.28f else 0.08f),
                spotColor = Color.Black.copy(if (palette.isDark) 0.38f else 0.14f),
            )
            .clip(shape)
            .drawBehind {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@drawBehind

                val maxR = size.minDimension / 2f
                val cornerRadius = when (shape) {
                    CircleShape -> maxR
                    is CornerBasedShape -> shape.topStart.toPx(size, density).coerceAtMost(maxR)
                    else -> maxR
                }
                val radii = floatArrayOf(cornerRadius, cornerRadius, cornerRadius, cornerRadius)
                val cRadius = CornerRadius(cornerRadius, cornerRadius)

                // 1. GENUINE TRANSLUCENT LIQUID GLASS BODY (Crystal optical transparency)
                val bodyColor = if (palette.isDark) {
                    Color(0xFF141418).copy(alpha = 0.48f)
                } else {
                    Color(0xFFFAFAFA).copy(alpha = 0.42f)
                }
                drawRoundRect(color = bodyColor, cornerRadius = cRadius)

                // 2. 3D CONVEX GLASS GLAZE (Top light reflection on lens curvature)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (palette.isDark) 0.18f else 0.42f),
                            Color.White.copy(alpha = if (palette.isDark) 0.04f else 0.10f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = h * 0.55f,
                    ),
                    cornerRadius = cRadius,
                )

                // 3. FRESNEL AMBIENT RIM GLOW (Subtle bottom-up ambient reflection)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (palette.isDark) 0.03f else 0.06f),
                            Color.White.copy(alpha = if (palette.isDark) 0.10f else 0.18f),
                        ),
                        startY = h * 0.70f,
                        endY = h,
                    ),
                    cornerRadius = cRadius,
                )

                // 4. KYANT0 SDF 45° SPECULAR RIM HIGHLIGHT
                if (highlightShader != null) {
                    drawIntoCanvas { canvas ->
                        val strokeWidthPx = with(density) { 1.25.dp.toPx() }
                        highlightShader.setFloatUniform("size", w, h)
                        highlightShader.setFloatUniform("cornerRadii", radii)
                        highlightShader.setColorUniform(
                            "color",
                            AndroidColor.valueOf(1f, 1f, 1f, if (palette.isDark) 0.75f else 0.90f)
                        )
                        highlightShader.setFloatUniform("angle", (45f * (PI / 180f)).toFloat())
                        highlightShader.setFloatUniform("falloff", 1.0f)

                        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeWidth = strokeWidthPx
                            shader = highlightShader
                        }
                        canvas.nativeCanvas.drawRoundRect(0f, 0f, w, h, cornerRadius, cornerRadius, strokePaint)
                    }
                } else {
                    // Fallback: 45° dual specular gradient stroke
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (palette.isDark) 0.75f else 0.90f),
                                Color.White.copy(alpha = if (palette.isDark) 0.20f else 0.30f),
                                Color.Transparent,
                                Color.White.copy(alpha = if (palette.isDark) 0.22f else 0.30f),
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w, h),
                        ),
                        style = Stroke(width = with(density) { 1.dp.toPx() }),
                        cornerRadius = cRadius,
                    )
                }

                // 5. INNER GLASS BEVEL RIM (0.65dp crisp refraction border)
                drawRoundRect(
                    color = Color.White.copy(alpha = if (palette.isDark) 0.15f else 0.25f),
                    style = Stroke(width = with(density) { 0.65.dp.toPx() }),
                    cornerRadius = cRadius,
                )
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
