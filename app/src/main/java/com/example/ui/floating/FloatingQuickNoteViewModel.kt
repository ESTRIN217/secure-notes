package com.example.ui.floating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TextSegment
import com.example.util.RichTextConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class FloatingQuickNoteViewModel : ViewModel() {
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _segments =
        MutableStateFlow(listOf(TextSegment(text = "")))
    val segments: StateFlow<List<TextSegment>> = _segments.asStateFlow()

    private val _selection = MutableStateFlow(0..0)
    val selection: StateFlow<IntRange> = _selection.asStateFlow()

    private val _pendingTypingStyle = MutableStateFlow<TextSegment?>(null)
    val pendingTypingStyle: StateFlow<TextSegment?> =
        _pendingTypingStyle.asStateFlow()

    val activeTextStyles: StateFlow<Set<String>> =
        combine(_segments, _selection, _pendingTypingStyle) { segs, sel, pending ->
            deriveActiveStyles(segs, sel, pending)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun onTitleChange(value: String) {
        _title.value = value
    }

    fun onSegmentsChange(value: List<TextSegment>) {
        if (value.isEmpty()) {
            _segments.value = listOf(TextSegment(text = ""))
            return
        }
        _segments.value = value
    }

    fun onSelectionChange(range: IntRange) {
        _selection.value = range
    }

    fun toggleTag(tag: String) {
        if (tag !in setOf("b", "i", "u", "s")) return
        val segs = _segments.value
        val plainLen = RichTextConverter.segmentsToPlainText(segs).length
        val start = _selection.value.first.coerceIn(0, plainLen)
        val end = _selection.value.last.coerceIn(0, plainLen).coerceAtLeast(start)
        if (start == end) toggleCollapsed(tag, segs, start, plainLen)
        else toggleRange(tag, segs, start, end)
    }

    fun plainText(): String =
        RichTextConverter.segmentsToPlainText(_segments.value)

    fun snapshot(): Pair<String, List<TextSegment>> =
        _title.value to _segments.value

    fun clear() {
        _title.value = ""
        _segments.value = listOf(TextSegment(text = ""))
        _selection.value = 0..0
        _pendingTypingStyle.value = null
    }

    private fun toggleCollapsed(tag: String, segs: List<TextSegment>, cursor: Int, plainLen: Int) {
        val charSeg = if (cursor < plainLen) {
            RichTextConverter.rangeSegments(segs, cursor, cursor + 1).firstOrNull()
        } else null
        val base = _pendingTypingStyle.value ?: TextSegment()
        val active = (charSeg != null && isStyleActive(tag, charSeg)) ||
            isStyleActive(tag, base)
        val nextPending = if (active) clearStyle(tag, base) else applyStyle(tag, base)
        _pendingTypingStyle.value = nextPending.takeIf { it.hasTypingStyle }
        if (charSeg == null) return
        _segments.value = RichTextConverter.applySpanStyle(segs, cursor, cursor + 1) {
            if (active) clearStyle(tag, it) else applyStyle(tag, it)
        }
    }

    private fun toggleRange(tag: String, segs: List<TextSegment>, start: Int, end: Int) {
        val inRange = RichTextConverter.rangeSegments(segs, start, end)
        if (inRange.isEmpty()) return
        val allStyled = inRange.all { isStyleActive(tag, it) }
        _segments.value = RichTextConverter.applySpanStyle(segs, start, end) {
            if (allStyled) clearStyle(tag, it) else applyStyle(tag, it)
        }
    }

    private fun isStyleActive(tag: String, seg: TextSegment): Boolean = when (tag) {
        "b" -> seg.bold
        "i" -> seg.italic
        "u" -> seg.underline
        "s" -> seg.strikethrough
        else -> false
    }

    private fun applyStyle(tag: String, seg: TextSegment): TextSegment = when (tag) {
        "b" -> seg.copy(bold = true)
        "i" -> seg.copy(italic = true)
        "u" -> seg.copy(underline = true)
        "s" -> seg.copy(strikethrough = true)
        else -> seg
    }

    private fun clearStyle(tag: String, seg: TextSegment): TextSegment = when (tag) {
        "b" -> seg.copy(bold = false)
        "i" -> seg.copy(italic = false)
        "u" -> seg.copy(underline = false)
        "s" -> seg.copy(strikethrough = false)
        else -> seg
    }

    private fun deriveActiveStyles(
        segs: List<TextSegment>,
        sel: IntRange,
        pending: TextSegment?
    ): Set<String> {
        val out = mutableSetOf<String>()
        segmentAtCursor(segs, sel.first)?.let { addSegmentStyles(out, it) }
        pending?.let { addSegmentStyles(out, it) }
        return out
    }

    private fun segmentAtCursor(segs: List<TextSegment>, cursor: Int): TextSegment? {
        var pos = 0
        for (seg in segs) {
            if (pos <= cursor && cursor <= pos + seg.text.length) return seg
            pos += seg.text.length
        }
        return null
    }

    private fun addSegmentStyles(out: MutableSet<String>, seg: TextSegment) {
        if (seg.bold) out.add("b")
        if (seg.italic) out.add("i")
        if (seg.underline) out.add("u")
        if (seg.strikethrough) out.add("s")
    }
}
