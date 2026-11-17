package com.clove.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.PixelCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * A downscaled snapshot of the whole window (WebView included). Coordinates are in
 * window/root-pixel space so they line up with `positionInRoot()`.
 */
data class GlassBackdrop(
    val image: ImageBitmap,
    val rootWidth: Int,
    val rootHeight: Int,
)

/**
 * Holds the latest backdrop as a [State]. Provided as the State object so that
 * [GlassSurface] can read `.value` inside its draw phase without recomposing.
 */
val LocalGlassBackdrop = compositionLocalOf<State<GlassBackdrop?>?> { null }

/**
 * Returns the backdrop state plus a throttled `capture()` trigger. Uses PixelCopy (a GPU
 * surface read — works over a hardware-accelerated WebView, unlike Compose's own blur) and
 * ping-pongs two reusable bitmaps to avoid per-frame allocation.
 */
@Composable
fun rememberGlassBackdrop(): Pair<State<GlassBackdrop?>, () -> Unit> {
    val view = LocalView.current
    val state = remember { mutableStateOf<GlassBackdrop?>(null) }
    val handler = remember { Handler(Looper.getMainLooper()) }
    val buffers = remember { arrayOfNulls<Bitmap>(2) }
    val index = remember { intArrayOf(0) }
    val lastCapture = remember { longArrayOf(0L) }
    val inFlight = remember { booleanArrayOf(false) }

    val capture: () -> Unit = remember {
        capture@{
            val now = SystemClock.uptimeMillis()
            if (inFlight[0] || now - lastCapture[0] < 40L) return@capture
            val w = view.width
            val h = view.height
            if (w <= 0 || h <= 0) return@capture
            val window = view.context.findActivity()?.window ?: return@capture

            val scale = 0.25f
            val dw = (w * scale).toInt().coerceAtLeast(1)
            val dh = (h * scale).toInt().coerceAtLeast(1)

            val i = index[0]
            var bmp = buffers[i]
            if (bmp == null || bmp.width != dw || bmp.height != dh) {
                bmp = Bitmap.createBitmap(dw, dh, Bitmap.Config.ARGB_8888)
                buffers[i] = bmp
            }
            val target = bmp
            try {
                inFlight[0] = true
                lastCapture[0] = now
                PixelCopy.request(
                    window,
                    Rect(0, 0, w, h),
                    target,
                    { result ->
                        inFlight[0] = false
                        if (result == PixelCopy.SUCCESS) {
                            state.value = GlassBackdrop(target.asImageBitmap(), w, h)
                            index[0] = (i + 1) % 2
                        }
                    },
                    handler,
                )
            } catch (e: Exception) {
                inFlight[0] = false
            }
        }
    }

    LaunchedEffect(view) {
        // Fast initial warm-up captures
        delay(60)
        capture()
        delay(180)
        capture()
        delay(400)
        capture()
    }

    return state to capture
}
