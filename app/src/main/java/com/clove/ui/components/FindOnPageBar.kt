package com.clove.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clove.ui.theme.LocalSafariPalette
import com.composables.icons.lucide.ArrowDown
import com.composables.icons.lucide.ArrowUp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.X

@Composable
fun FindOnPageBar(
    query: String,
    currentMatch: Int,
    totalMatches: Int,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalSafariPalette.current

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        elevation = 16.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Lucide.Search,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(18.dp),
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        "Find on page",
                        color = palette.textSecondary,
                        fontSize = 15.sp,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                    ),
                    cursorBrush = SolidColor(palette.accent),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onNext() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (query.isNotEmpty()) {
                val matchText = if (totalMatches > 0) "$currentMatch of $totalMatches" else "0 matches"
                Text(
                    text = matchText,
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }

            // Up arrow (Previous)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickableCompat(onClick = onPrevious),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Lucide.ArrowUp,
                    contentDescription = "Previous match",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }

            // Down arrow (Next)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickableCompat(onClick = onNext),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Lucide.ArrowDown,
                    contentDescription = "Next match",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Close button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickableCompat(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Lucide.X,
                    contentDescription = "Close find on page",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
