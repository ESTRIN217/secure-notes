package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BlockType
import com.example.data.model.TextSegment
import com.example.util.RichTextConverter

@Composable
fun ReadOnlyTextBlock(
    segments: List<TextSegment>,
    blockType: BlockType = BlockType.TEXT,
    numberIndex: Int? = null,
    onActivate: (originalOffset: Int) -> Unit,
    onUrlClicked: (String, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val annotated = remember(segments) { RichTextConverter.segmentsToAnnotatedString(segments) }

    val currentUrlClick by rememberUpdatedState(onUrlClicked)
    val currentActivate by rememberUpdatedState(onActivate)
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val annotatedWithLinks = remember(annotated) {
        val builder = AnnotatedString.Builder(annotated)
        for (range in annotated.getStringAnnotations(RichTextConverter.URL_ANNOTATION, 0, annotated.length)) {
            builder.addLink(
                LinkAnnotation.Url(
                    url = range.item,
                    linkInteractionListener = LinkInteractionListener {
                        currentUrlClick(range.item, range.start)
                    }
                ),
                start = range.start,
                end = range.end
            )
        }
        builder.toAnnotatedString()
    }

    val prefix = when (blockType) {
        BlockType.BULLET_LIST -> "• "
        BlockType.NUMBERED_LIST -> "${numberIndex ?: 1}. "
        BlockType.QUOTE -> "▎ "
        BlockType.CODE_BLOCK -> "  "
        else -> ""
    }

    val textStyle = when (blockType) {
        BlockType.HEADING1 -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        BlockType.HEADING2 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        BlockType.HEADING3 -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        BlockType.HEADING4 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        BlockType.QUOTE -> MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal
        )
        BlockType.CODE_BLOCK -> MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
        BlockType.BULLET_LIST, BlockType.NUMBERED_LIST -> MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        )
        else -> MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    }

    val rowModifier = if (blockType == BlockType.CALLOUT) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    } else {
        modifier.fillMaxWidth()
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prefix.isNotEmpty()) {
            Text(
                text = prefix,
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
        }

        if (blockType == BlockType.CALLOUT) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
        }

        val contentModifier = if (blockType == BlockType.QUOTE) {
            Modifier.weight(1f).padding(start = 8.dp)
        } else {
            Modifier.weight(1f)
        }

        BasicText(
            text = annotatedWithLinks,
            style = textStyle,
            onTextLayout = { layoutResult = it },
            modifier = contentModifier
                .heightIn(min = 24.dp)
                .pointerInput(annotated) {
                    detectTapGestures { position ->
                        val layout = layoutResult ?: return@detectTapGestures
                        val offset = layout.getOffsetForPosition(position)
                        currentActivate(offset.coerceIn(0, annotated.length))
                    }
                }
        )
    }
}
