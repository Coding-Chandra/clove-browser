package com.clove.ui.components

import android.os.Build
import android.text.Html
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.clove.ui.theme.LocalSafariPalette
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X

@Composable
fun ReaderView(
    title: String,
    contentHtml: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalSafariPalette.current
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 68.dp),
        ) {
            // Article Title
            Text(
                text = title.ifBlank { "Article" },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = palette.textPrimary,
                lineHeight = 36.sp,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            // Article Body
            AndroidView(
                factory = { context ->
                    TextView(context).apply {
                        setTextColor(palette.textPrimary.toArgb())
                        textSize = 18f
                        setLineSpacing(8f, 1.35f)
                    }
                },
                update = { textView ->
                    textView.setTextColor(palette.textPrimary.toArgb())
                    val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Html.fromHtml(contentHtml, Html.FROM_HTML_MODE_COMPACT)
                    } else {
                        @Suppress("DEPRECATION")
                        Html.fromHtml(contentHtml)
                    }
                    textView.text = spanned
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(100.dp))
        }

        // Top Floating Liquid Glass Reader Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassCircleButton(
                onClick = onClose,
                size = 42.dp,
            ) {
                Icon(
                    Lucide.X,
                    contentDescription = "Close Reader",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Lucide.BookOpen,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "  Reader",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accent,
                )
            }
        }
    }
}
