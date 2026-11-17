package com.clove.ui.components

import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.clove.ui.theme.LocalSafariPalette
import org.intellij.lang.annotations.Language
import kotlin.math.min
import kotlin.math.roundToInt

// ============================================================================
// AGSL Shaders inspired by Kyant0/AndroidLiquidGlass
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

@Language("AGSL")
private const val RefractionWithDispersionShaderString = """
uniform shader content;
uniform float2 size;
uniform float2 offset;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;
uniform float chromaticAberration;

$RoundedRectSDF

float circleMap(float x) {
    return 1.0 - sqrt(max(0.0, 1.0 - x * x));
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = (coord + offset) - halfSize;
    float radius = radiusAt(centeredCoord, cornerRadii);
    
    float sd = sdRoundedRect(centeredCoord, halfSize, radius);
    if (-sd >= refractionHeight) {
        return content.eval(coord);
    }
    sd = min(sd, 0.0);
    
    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius) + depthEffect * normalize(centeredCoord + 0.0001));
    
    float2 refractedCoord = coord + d * grad;
    
    if (chromaticAberration <= 0.0) {
        return content.eval(refractedCoord);
    }
    
    // High-fidelity chromatic RGB dispersion across the lens rim
    float dispersionDist = d * chromaticAberration * 0.75;
    float2 disp = grad * dispersionDist;
    
    half4 red = content.eval(refractedCoord + disp);
    half4 green = content.eval(refractedCoord);
    half4 blue = content.eval(refractedCoord - disp);
    
    return half4(red.r, green.g, blue.b, (red.a + green.a + blue.a) / 3.0);
}
"""

/**
 * A true crystal "liquid glass" surface inspired by Kyant0/AndroidLiquidGlass:
 * - High optical transparency (genuine see-through, never opaque)
 * - Continuous SDF rounded-rect physical lens curvature
 * - High-fidelity chromatic RGB dispersion splitting colors along the curved rim
 * - 45° specular rim highlight border with dual-angle illumination
 * - 3D convex glass glaze highlight
 * - Ambient drop shadow
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    elevation: Dp = 14.dp,
    chromaticAberration: Boolean = true,
    content: @Composable () -> Unit,
) {
    val palette = LocalSafariPalette.current
    val backdropState = LocalGlassBackdrop.current
    val density = LocalDensity.current

    val refractionHeightPx = with(density) { 18.dp.toPx() }
    val refractionAmountPx = with(density) { 22.dp.toPx() }

    val refractionShader = remember {
        try {
            RuntimeShader(RefractionWithDispersionShaderString)
        } catch (_: Throwable) {
            null
        }
    }

    val posInRoot = remember { floatArrayOf(0f, 0f) }

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(if (palette.isDark) 0.22f else 0.10f),
                spotColor = Color.Black.copy(if (palette.isDark) 0.28f else 0.14f),
            )
            .onGloballyPositioned {
                val p = it.positionInRoot()
                posInRoot[0] = p.x
                posInRoot[1] = p.y
            }
            .clip(shape)
            .drawBehind {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@drawBehind

                val cornerRadius = when {
                    shape == CircleShape -> min(w, h) / 2f
                    shape is CornerBasedShape -> shape.topStart.toPx(size, density).coerceAtMost(min(w, h) / 2f)
                    else -> min(w, h) / 2f
                }
                val radii = floatArrayOf(cornerRadius, cornerRadius, cornerRadius, cornerRadius)
                val cRadius = CornerRadius(cornerRadius, cornerRadius)

                // 1. LIVE BACKDROP OPTICAL REFRACTION (Kyant0/AndroidLiquidGlass physical lens)
                val backdrop = backdropState?.value
                if (backdrop != null && backdrop.image != null && backdrop.rootWidth > 0 && backdrop.rootHeight > 0) {
                    if (refractionShader != null) {
                        drawIntoCanvas { canvas ->
                            val nativeCanvas = canvas.nativeCanvas
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                            val bmp = backdrop.image.asAndroidBitmap()
                            val sx = bmp.width.toFloat() / backdrop.rootWidth
                            val sy = bmp.height.toFloat() / backdrop.rootHeight
                            val matrix = Matrix()
                            matrix.setScale(1f / sx, 1f / sy)
                            matrix.postTranslate(-posInRoot[0], -posInRoot[1])
                            val bs = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                            bs.setLocalMatrix(matrix)

                            refractionShader.setInputShader("content", bs)
                            refractionShader.setFloatUniform("size", w, h)
                            refractionShader.setFloatUniform("offset", 0f, 0f)
                            refractionShader.setFloatUniform("cornerRadii", radii)
                            refractionShader.setFloatUniform("refractionHeight", refractionHeightPx)
                            refractionShader.setFloatUniform("refractionAmount", -refractionAmountPx)
                            refractionShader.setFloatUniform("depthEffect", 0.08f)
                            refractionShader.setFloatUniform("chromaticAberration", if (chromaticAberration) 1.5f else 0f)

                            paint.shader = refractionShader
                            nativeCanvas.drawRoundRect(0f, 0f, w, h, cornerRadius, cornerRadius, paint)
                        }
                    } else {
                        drawBackdropCrop(backdrop, Offset(posInRoot[0], posInRoot[1]), size)
                    }
                }

                // 2. ULTRA-TRANSPARENT CRYSTAL BODY (Genuinely see-through, never opaque)
                val bodyColor = if (palette.isDark) {
                    Color(0xFF1E1E28).copy(alpha = 0.38f)
                } else {
                    Color(0xFFFFFFFF).copy(alpha = 0.30f)
                }
                drawRoundRect(color = bodyColor, cornerRadius = cRadius)

                // 3. TOP CONVEX GLASS SHEEN (3D Liquid Glass Glaze)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (palette.isDark) 0.32f else 0.50f),
                            Color.White.copy(alpha = if (palette.isDark) 0.06f else 0.12f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = h * 0.60f,
                    ),
                    cornerRadius = cRadius,
                )

                // 4. SPECULAR RIM HIGHLIGHT (The 45° Apple refraction rim)
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (palette.isDark) 0.75f else 0.85f),
                            Color.White.copy(alpha = if (palette.isDark) 0.20f else 0.30f),
                            Color.Transparent,
                            Color.White.copy(alpha = if (palette.isDark) 0.15f else 0.20f),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h),
                    ),
                    style = Stroke(width = with(density) { 0.85.dp.toPx() }),
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

/** Draws the slice of the window snapshot sitting behind this surface (fallback). */
private fun DrawScope.drawBackdropCrop(backdrop: GlassBackdrop, posInRoot: Offset, size: Size) {
    val img = backdrop.image
    if (backdrop.rootWidth <= 0 || backdrop.rootHeight <= 0) return
    val sx = img.width.toFloat() / backdrop.rootWidth
    val sy = img.height.toFloat() / backdrop.rootHeight

    val srcLeft = (posInRoot.x * sx).roundToInt().coerceIn(0, (img.width - 1).coerceAtLeast(0))
    val srcTop = (posInRoot.y * sy).roundToInt().coerceIn(0, (img.height - 1).coerceAtLeast(0))
    val srcW = (size.width * sx).roundToInt().coerceIn(1, img.width - srcLeft)
    val srcH = (size.height * sy).roundToInt().coerceIn(1, img.height - srcTop)

    drawImage(
        image = img,
        srcOffset = IntOffset(srcLeft, srcTop),
        srcSize = IntSize(srcW, srcH),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
        filterQuality = FilterQuality.Low,
    )
}

/**
 * A circular liquid glass button with tactile spring-bouncy feedback,
 * used for toolbar actions (back, forward, share, reading/bookmarks, tabs).
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
        elevation = if (isPressed) 4.dp else 10.dp,
        chromaticAberration = true,
    ) {
        content()
    }
}
