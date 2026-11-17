package com.clove.ui.components

import android.graphics.BitmapShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.clove.ui.theme.LocalSafariPalette
import org.intellij.lang.annotations.Language
import kotlin.math.PI
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
    
    float dispersionIntensity = chromaticAberration * ((centeredCoord.x * centeredCoord.y) / (halfSize.x * halfSize.y + 0.001));
    float2 dispersedCoord = d * grad * dispersionIntensity;
    
    half4 color = half4(0.0);
    half4 red = content.eval(refractedCoord + dispersedCoord);
    color.r += red.r / 3.5;
    color.a += red.a / 7.0;
    
    half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0 / 3.0));
    color.r += orange.r / 3.5;
    color.g += orange.g / 7.0;
    color.a += orange.a / 7.0;
    
    half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0 / 3.0));
    color.r += yellow.r / 3.5;
    color.g += yellow.g / 3.5;
    color.a += yellow.a / 7.0;
    
    half4 green = content.eval(refractedCoord);
    color.g += green.g / 3.5;
    color.a += green.a / 7.0;
    
    half4 cyan = content.eval(refractedCoord - dispersedCoord * (1.0 / 3.0));
    color.g += cyan.g / 3.5;
    color.b += cyan.b / 3.0;
    color.a += cyan.a / 7.0;
    
    half4 blue = content.eval(refractedCoord - dispersedCoord * (2.0 / 3.0));
    color.b += blue.b / 3.0;
    color.a += blue.a / 7.0;
    
    half4 purple = content.eval(refractedCoord - dispersedCoord);
    color.r += purple.r / 7.0;
    color.b += purple.b / 3.0;
    color.a += purple.a / 7.0;
    
    return color;
}
"""

@Language("AGSL")
private const val HighlightShaderString = """
uniform float2 size;
uniform float4 cornerRadii;
uniform float4 color;
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
    return half4(color) * intensity;
}
"""

/**
 * A true "liquid glass" surface powered by Kyant0/AndroidLiquidGlass AGSL shaders.
 * Evaluates real SDF lens refraction, chromatic dispersion around edges,
 * directional specular highlights, and live backdrop sampling.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    elevation: Dp = 18.dp,
    chromaticAberration: Boolean = true,
    isInteractive: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = LocalSafariPalette.current
    val backdropState = LocalGlassBackdrop.current
    val density = LocalDensity.current

    val highlightColor = if (palette.isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.65f)
    val tint = if (palette.isDark) Color(0xFF1C1C24).copy(alpha = 0.50f) else Color.White.copy(alpha = 0.45f)

    val refractionHeightPx = with(density) { 14.dp.toPx() }
    val refractionAmountPx = with(density) { 20.dp.toPx() }

    val refractionShader = remember {
        try {
            RuntimeShader(RefractionWithDispersionShaderString)
        } catch (_: Throwable) {
            null
        }
    }
    val highlightShader = remember {
        try {
            RuntimeShader(HighlightShaderString)
        } catch (_: Throwable) {
            null
        }
    }

    val posInRoot = remember { floatArrayOf(0f, 0f) }

    Box(
        modifier = modifier
            .shadow(elevation, shape, clip = false, ambientColor = Color.Black.copy(0.25f), spotColor = Color.Black.copy(0.30f))
            .onGloballyPositioned {
                val p = it.positionInRoot()
                posInRoot[0] = p.x
                posInRoot[1] = p.y
            }
            .clip(shape)
            .drawBehind {
                val backdrop = backdropState?.value
                val w = size.width
                val h = size.height

                if (w > 0f && h > 0f) {
                    val cornerRadius = when (shape) {
                        is CircleShape -> min(w, h) / 2f
                        is CornerBasedShape -> shape.topStart.toPx(size, this).coerceAtMost(min(w, h) / 2f)
                        else -> min(w, h) / 2f
                    }
                    val radii = floatArrayOf(cornerRadius, cornerRadius, cornerRadius, cornerRadius)

                    if (refractionShader != null) {
                        drawIntoCanvas { canvas ->
                            val nativeCanvas = canvas.nativeCanvas
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

                            val contentShader: Shader = if (backdrop != null && backdrop.image != null && backdrop.rootWidth > 0 && backdrop.rootHeight > 0) {
                                val bmp = backdrop.image.asAndroidBitmap()
                                val sx = bmp.width.toFloat() / backdrop.rootWidth
                                val sy = bmp.height.toFloat() / backdrop.rootHeight
                                val matrix = Matrix()
                                matrix.setScale(sx, sy)
                                matrix.postTranslate(-posInRoot[0] * sx, -posInRoot[1] * sy)
                                val bs = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                                bs.setLocalMatrix(matrix)
                                bs
                            } else {
                                LinearGradient(
                                    0f, 0f, w, h,
                                    tint.toArgb(),
                                    tint.copy(alpha = tint.alpha * 0.7f).toArgb(),
                                    Shader.TileMode.CLAMP
                                )
                            }

                            // 1. Physical Liquid Glass Refraction & Chromatic Aberration
                            refractionShader.setInputShader("content", contentShader)
                            refractionShader.setFloatUniform("size", w, h)
                            refractionShader.setFloatUniform("offset", 0f, 0f)
                            refractionShader.setFloatUniform("cornerRadii", radii)
                            refractionShader.setFloatUniform("refractionHeight", refractionHeightPx)
                            refractionShader.setFloatUniform("refractionAmount", -refractionAmountPx)
                            refractionShader.setFloatUniform("depthEffect", 0.05f)
                            refractionShader.setFloatUniform("chromaticAberration", if (chromaticAberration) 1.2f else 0f)

                            paint.shader = refractionShader
                            nativeCanvas.drawRoundRect(0f, 0f, w, h, cornerRadius, cornerRadius, paint)

                            // 2. Specular Rim Highlight (Angle: 45° = PI/4)
                            if (highlightShader != null) {
                                highlightShader.setFloatUniform("size", w, h)
                                highlightShader.setFloatUniform("cornerRadii", radii)
                                highlightShader.setFloatUniform(
                                    "color",
                                    highlightColor.red,
                                    highlightColor.green,
                                    highlightColor.blue,
                                    highlightColor.alpha
                                )
                                highlightShader.setFloatUniform("angle", (PI.toFloat() / 4f))
                                highlightShader.setFloatUniform("falloff", 1.6f)

                                paint.shader = highlightShader
                                nativeCanvas.drawRoundRect(0f, 0f, w, h, cornerRadius, cornerRadius, paint)
                            }
                        }
                    } else {
                        // Fallback when hardware AGSL is not present
                        if (backdrop != null) {
                            drawBackdropCrop(backdrop, Offset(posInRoot[0], posInRoot[1]), size)
                        }
                    }
                }
            }
            .background(tint)
            .background(
                Brush.verticalGradient(
                    0f to highlightColor.copy(alpha = highlightColor.alpha * 0.4f),
                    0.45f to Color.Transparent,
                )
            )
            .border(BorderStroke(0.75.dp, palette.glassBorder), shape),
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
 * A circular liquid glass button with spring-bouncy tactile feedback,
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

    // Tactile bouncy spring animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "liquid_glass_button_scale",
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                clickableCompat(onClick, onLongClick, enabled, interaction)
            ),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            elevation = 12.dp,
            chromaticAberration = true,
            isInteractive = isPressed && enabled,
        ) {
            Box(
                modifier = Modifier.graphicsLayer { alpha = if (enabled) 1f else 0.35f },
                contentAlignment = Alignment.Center,
            ) { content() }
        }
    }
}
