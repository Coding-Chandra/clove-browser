package com.clove.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clove.browser.TabState
import com.clove.ui.theme.LocalSafariPalette
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search

/**
 * Safari iOS Page Format Menu (`aA` popup) with authentic liquid glass aesthetics:
 * - Text Zoom ([ - ] 100% [ + ])
 * - Reader View toggle ("Show Reader" / "Hide Reader")
 * - Distraction Control ("Hide Distracting Items")
 * - Desktop Website toggle ("Request Desktop Website")
 * - Find on Page
 */
@Composable
fun PageFormatMenu(
    tab: TabState,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onToggleReader: () -> Unit,
    onToggleDistractionControl: () -> Unit,
    onToggleDesktopSite: () -> Unit,
    onFindOnPage: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalSafariPalette.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickableCompat(onClick = onDismiss)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 76.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickableCompat(onClick = {}) // intercept clicks
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                elevation = 20.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    // Header: host name & security
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Lucide.Lock,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.host.ifBlank { "Website Settings" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Text Zoom Control: [ - ]  100%  [ + ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.glassElevated)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Smaller 'a' / Minus
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickableCompat(onClick = onZoomOut),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "a",
                                color = palette.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Text(
                            text = "${tab.textZoom}%",
                            color = palette.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )

                        // Larger 'A' / Plus
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickableCompat(onClick = onZoomIn),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "A",
                                color = palette.textPrimary,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Menu Options Group
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.glassElevated),
                    ) {
                        // Reader View
                        PageMenuRow(
                            icon = Lucide.BookOpen,
                            title = if (tab.isReaderMode) "Hide Reader" else "Show Reader",
                            subtitle = if (tab.isReaderMode) "Reader Mode Active" else "Distraction-free article view",
                            accented = tab.isReaderMode,
                            onClick = {
                                onDismiss()
                                onToggleReader()
                            },
                        )

                        MenuSeparator()

                        // Distraction Control
                        PageMenuRow(
                            icon = Lucide.EyeOff,
                            title = if (tab.isDistractionControlActive) "Exit Distraction Control" else "Hide Distracting Items",
                            subtitle = if (tab.isDistractionControlActive) "Item selection active" else "Tap items to hide them",
                            accented = tab.isDistractionControlActive,
                            onClick = {
                                onDismiss()
                                onToggleDistractionControl()
                            },
                        )

                        MenuSeparator()

                        // Request Desktop Website
                        PageMenuRow(
                            icon = Lucide.Globe,
                            title = if (tab.isDesktopSite) "Request Mobile Website" else "Request Desktop Website",
                            subtitle = if (tab.isDesktopSite) "Desktop layout requested" else "Standard mobile view",
                            accented = tab.isDesktopSite,
                            onClick = {
                                onDismiss()
                                onToggleDesktopSite()
                            },
                        )

                        MenuSeparator()

                        // Find on Page
                        PageMenuRow(
                            icon = Lucide.Search,
                            title = "Find on Page",
                            subtitle = "Search words within this page",
                            accented = tab.findActive,
                            onClick = {
                                onDismiss()
                                onFindOnPage()
                            },
                        )
                    }
                }
            }
        }
    }

@Composable
private fun PageMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accented: Boolean = false,
    onClick: () -> Unit,
) {
    val palette = LocalSafariPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableCompat(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (accented) palette.accent else palette.textPrimary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (accented) palette.accent else palette.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                color = palette.textSecondary,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun MenuSeparator() {
    val palette = LocalSafariPalette.current
    Divider(
        color = palette.glassBorder.copy(alpha = 0.35f),
        thickness = 0.6.dp,
        modifier = Modifier.padding(horizontal = 14.dp),
    )
}
