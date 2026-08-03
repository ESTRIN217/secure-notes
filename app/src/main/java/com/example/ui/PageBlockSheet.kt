package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.R
import com.example.data.model.DataBlock
import java.io.File
import java.io.FileOutputStream

val PAGE_BLOCK_EMOJIS: List<String> = listOf(
    "📝", "📄", "📚", "📌", "🔖", "💡", "⭐", "🔥", "❤️", "✅",
    "🎯", "🚀", "🔒", "🔑", "💼", "🏠", "🛒", "📅", "⏰", "📞",
    "📧", "📍", "🏫", "👤", "👥", "🎓", "☕", "🍽️", "✈️", "🌐",
    "⚡", "🔔", "💾", "🗂️", "📁", "🎉", "🙏", "👍", "💪", "🧠"
)

val PAGE_BLOCK_ICONS: List<Pair<String, ImageVector>> = listOf(
    "star" to Icons.Default.Star,
    "favorite" to Icons.Default.Favorite,
    "lock" to Icons.Default.Lock,
    "home" to Icons.Default.Home,
    "work" to Icons.Default.Work,
    "school" to Icons.Default.School,
    "cart" to Icons.Default.ShoppingCart,
    "notifications" to Icons.Default.Notifications,
    "email" to Icons.Default.Email,
    "phone" to Icons.Default.Phone,
    "location" to Icons.Default.LocationOn,
    "calendar" to Icons.Default.CalendarToday,
    "time" to Icons.Default.AccessTime,
    "check" to Icons.Default.CheckCircle,
    "warning" to Icons.Default.Warning,
    "info" to Icons.Default.Info,
    "face" to Icons.Default.Face,
    "tag" to Icons.Default.Tag,
    "bookmark" to Icons.Default.Bookmark,
    "folder" to Icons.Default.Folder,
    "settings" to Icons.Default.Settings,
    "person" to Icons.Default.Person,
    "place" to Icons.Default.Place,
    "cloud" to Icons.Default.Cloud,
    "security" to Icons.Default.Security,
    "edit" to Icons.Default.Edit,
    "share" to Icons.Default.Share,
    "thumb" to Icons.Default.ThumbUp,
    "search" to Icons.Default.Search,
    "send" to Icons.AutoMirrored.Filled.Send,
    "refresh" to Icons.Default.Refresh,
    "build" to Icons.Default.Build,
    "account" to Icons.Default.AccountCircle,
    "alarm" to Icons.Default.Alarm,
    "cafe" to Icons.Default.LocalCafe,
    "flight" to Icons.Default.Flight,
    "restaurant" to Icons.Default.Restaurant,
    "star_border" to Icons.Default.StarBorder,
    "arrow" to Icons.AutoMirrored.Filled.ArrowForward,
    "plus" to Icons.Default.Add
)

fun pageBlockIconVector(name: String): ImageVector? =
    PAGE_BLOCK_ICONS.firstOrNull { it.first == name }?.second

private enum class PageBlockSheetTab(val labelRes: Int) {
    EMOJI(R.string.page_block_emoji),
    ICONS(R.string.page_block_icons),
    UPLOAD(R.string.page_block_upload)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PageBlockOptionsSheet(
    block: DataBlock,
    onIconSelected: (type: String, value: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(PageBlockSheetTab.EMOJI) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var linkInput by remember { mutableStateOf("") }
    val iconType = block.meta["iconType"]
    val iconValue = block.meta["iconValue"].orEmpty()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        pendingImageUri = uri
    }

    val savePendingImage: () -> Unit = {
        val uri = pendingImageUri
        if (uri != null) {
            try {
                val resolver = context.contentResolver
                var ext = "jpg"
                resolver.getType(uri)?.let { mime ->
                    ext = when (mime) {
                        "image/png" -> "png"
                        "image/gif" -> "gif"
                        "image/webp" -> "webp"
                        else -> "jpg"
                    }
                }
                val localFile = File(context.filesDir, "page_icon_${System.currentTimeMillis()}.$ext")
                resolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(localFile).use { output -> input.copyTo(output) }
                }
                onIconSelected("image", localFile.absolutePath)
            } catch (e: Exception) {
                android.util.Log.e("PageBlockSheet", "save page icon failed", e)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.btn_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                PageBlockIconPreview(
                    iconType = iconType,
                    iconValue = iconValue,
                    pendingUri = pendingImageUri,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                PageBlockSheetTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(stringResource(tab.labelRes), fontSize = 13.sp) }
                    )
                }
            }

            when (selectedTab) {
                PageBlockSheetTab.EMOJI -> PageBlockEmojiTab(
                    selected = iconValue.takeIf { iconType == "emoji" },
                    onPick = {
                        onIconSelected("emoji", it)
                        onDismiss()
                    }
                )
                PageBlockSheetTab.ICONS -> PageBlockIconsTab(
                    selected = iconValue.takeIf { iconType == "icon" },
                    onPick = {
                        onIconSelected("icon", it)
                        onDismiss()
                    }
                )
                PageBlockSheetTab.UPLOAD -> PageBlockUploadTab(
                    pendingImageUri = pendingImageUri,
                    linkInput = linkInput,
                    onLinkInputChange = { linkInput = it },
                    onPickImage = { imagePickerLauncher.launch("image/*") },
                    onSaveImage = {
                        savePendingImage()
                        onDismiss()
                    },
                    onCancelImage = { pendingImageUri = null },
                    onApplyLink = {
                        if (linkInput.isNotBlank()) {
                            onIconSelected("image", linkInput.trim())
                            onDismiss()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PageBlockIconPreview(
    iconType: String?,
    iconValue: String,
    pendingUri: Uri?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.block_page),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(8.dp))
        PageBlockIconContent(iconType, iconValue, pendingUri, size = 24.dp)
    }
}

@Composable
fun PageBlockIconContent(
    iconType: String?,
    iconValue: String,
    pendingUri: Uri? = null,
    size: androidx.compose.ui.unit.Dp = 20.dp,
    tint: androidx.compose.ui.graphics.Color? = null
) {
    val uri = pendingUri
    when {
        uri != null -> AsyncImage(
            model = uri,
            contentDescription = stringResource(R.string.block_page),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        )
        iconType == "emoji" && iconValue.isNotBlank() -> Text(
            text = iconValue,
            fontSize = (size.value * 0.9f).sp,
            maxLines = 1
        )
        iconType == "icon" -> {
            val vector = pageBlockIconVector(iconValue)
            if (vector != null) {
                Icon(
                    imageVector = vector,
                    contentDescription = stringResource(R.string.block_page),
                    tint = tint ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(size)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = stringResource(R.string.block_page),
                    tint = tint ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(size)
                )
            }
        }
        iconType == "image" && iconValue.isNotBlank() -> AsyncImage(
            model = iconValue,
            contentDescription = stringResource(R.string.block_page),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        )
        else -> Icon(
            imageVector = Icons.Default.Description,
            contentDescription = stringResource(R.string.block_page),
            tint = tint ?: MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(size)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PageBlockEmojiTab(
    selected: String?,
    onPick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PAGE_BLOCK_EMOJIS.forEach { emoji ->
                val isSelected = emoji == selected
                Text(
                    text = emoji,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .clickable { onPick(emoji) }
                        .padding(vertical = 10.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PageBlockIconsTab(
    selected: String?,
    onPick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PAGE_BLOCK_ICONS.forEach { (name, vector) ->
                val isSelected = name == selected
                Icon(
                    imageVector = vector,
                    contentDescription = name,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onPick(name) }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun PageBlockUploadTab(
    pendingImageUri: Uri?,
    linkInput: String,
    onLinkInputChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onSaveImage: () -> Unit,
    onCancelImage: () -> Unit,
    onApplyLink: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (pendingImageUri == null) {
            OutlinedCard(
                onClick = onPickImage,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.page_block_upload_image), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.label_option_gallery), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            OutlinedTextField(
                value = linkInput,
                onValueChange = onLinkInputChange,
                label = { Text(stringResource(R.string.page_block_paste_link)) },
                placeholder = { Text(stringResource(R.string.page_block_link_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onApplyLink,
                enabled = linkInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.page_block_next))
            }
        } else {
            AsyncImage(
                model = pendingImageUri,
                contentDescription = stringResource(R.string.page_block_upload_image),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelImage,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.back))
                }
                Button(
                    onClick = onSaveImage,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.btn_save))
                }
            }
        }
    }
}
