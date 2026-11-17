package com.clove.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clove.browser.TabState
import com.clove.ui.components.GlassSurface
import com.clove.ui.components.clickableCompat
import com.clove.ui.components.consumeClicks
import com.clove.ui.theme.LocalSafariPalette
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.X

@Composable
fun TabsOverview(
    tabs: List<TabState>,
    currentTabId: String?,
    isPrivate: Boolean,
    tabGroups: List<String> = listOf("Personal", "Work", "Research"),
    selectedTabGroup: String = "Personal",
    onSelectTabGroup: (String) -> Unit = {},
    onAddTabGroup: (String) -> Unit = {},
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onNewTab: () -> Unit,
    onTogglePrivate: (Boolean) -> Unit,
    onDone: () -> Unit,
) {
    val palette = LocalSafariPalette.current
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    if (showNewGroupDialog) {
        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false },
            title = { Text("New Tab Group", color = palette.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newGroupName.trim()
                        if (name.isNotEmpty()) {
                            onAddTabGroup(name)
                        }
                        newGroupName = ""
                        showNewGroupDialog = false
                    }
                ) {
                    Text("Create", color = palette.accent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
            .consumeClicks(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Safari Header Title
            Text(
                text = if (isPrivate) "Private Browsing" else "$selectedTabGroup — ${tabs.size} Tab${if (tabs.size == 1) "" else "s"}",
                color = palette.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            // Safari Tab Groups horizontal pills
            if (!isPrivate) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(tabGroups) { group ->
                        val selected = group == selectedTabGroup
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) palette.accent else palette.glassElevated)
                                .clickableCompat(onClick = { onSelectTabGroup(group) })
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = group,
                                color = if (selected) Color.White else palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.glassElevated)
                                .clickableCompat(onClick = { showNewGroupDialog = true })
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Lucide.Plus, contentDescription = "New Tab Group", tint = palette.accent, modifier = Modifier.size(14.dp))
                                Text(
                                    text = " Group",
                                    color = palette.accent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(tabs, key = { it.id }) { tab ->
                    TabCard(
                        tab = tab,
                        selected = tab.id == currentTabId,
                        onSelect = { onSelect(tab.id) },
                        onClose = { onClose(tab.id) },
                    )
                }
            }
        }

        // Bottom toolbar: private toggle | + | Done
        GlassSurface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isPrivate) "Private" else "Standard",
                    color = palette.accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .then(clickableCompat(onClick = { onTogglePrivate(!isPrivate) }))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .then(clickableCompat(onClick = onNewTab)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Lucide.Plus,
                            contentDescription = "New Tab",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                Text(
                    text = "Done",
                    color = palette.accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .then(clickableCompat(onClick = onDone))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun TabCard(
    tab: TabState,
    selected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalSafariPalette.current
    val shape = RoundedCornerShape(18.dp)
    Column {
        // Header: favicon + title + close
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Lucide.Globe,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = tab.displayTitle,
                color = palette.textPrimary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .then(clickableCompat(onClick = onClose)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Lucide.X,
                    contentDescription = "Close tab",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(15.dp),
                )
            }
        }

        // Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(shape)
                .background(palette.glassElevated)
                .then(
                    if (selected) Modifier.border(2.5.dp, palette.accent, shape) else Modifier
                )
                .then(clickableCompat(onClick = onSelect)),
            contentAlignment = Alignment.Center,
        ) {
            val thumb = tab.thumbnail
            if (thumb != null) {
                Image(
                    bitmap = thumb.asImageBitmap(),
                    contentDescription = tab.displayTitle,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Lucide.Globe,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        text = if (tab.host.isBlank()) "Start Page" else tab.host,
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
