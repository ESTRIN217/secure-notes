package com.example.util

import androidx.compose.ui.text.AnnotatedString

class OffsetMapper {

    fun createMapping(sourceLength: Int): MappingArrays {
        val sourceToTransformed = IntArray(sourceLength + 1) { -1 }
        val transformedToSource = IntArray(sourceLength + 1)
        return MappingArrays(sourceToTransformed, transformedToSource, 0)
    }

    data class MappingArrays(
        val sourceToTransformed: IntArray,
        val transformedToSource: IntArray,
        var transformedCount: Int
    )

    data class FinalMapping(
        val sourceToTransformed: IntArray,
        val transformedToSource: IntArray
    )

    fun addChar(mapping: MappingArrays, sourceIndex: Int, builderLength: Int) {
        val idx = mapping.transformedCount
        mapping.transformedToSource[idx] = sourceIndex
        mapping.sourceToTransformed[sourceIndex] = builderLength
        mapping.transformedCount = idx + 1
    }

    fun skipChar(mapping: MappingArrays, sourceIndex: Int) {
    }

    fun finalize(mapping: MappingArrays, builderLength: Int, sourceLength: Int): FinalMapping {
        val (stt, tts, count) = mapping
        stt[sourceLength] = builderLength

        val totalTransformed = count + 1
        tts[count] = sourceLength

        var lastT = 0
        for (idx in 0..sourceLength) {
            if (stt[idx] == -1) {
                stt[idx] = lastT
            } else {
                lastT = stt[idx]
            }
        }

        val M = builderLength
        val filledTts = IntArray(M + 1)
        for (idx in 0..M) {
            filledTts[idx] = if (idx < totalTransformed) {
                tts[idx].coerceIn(0, sourceLength)
            } else {
                sourceLength
            }
        }

        return FinalMapping(stt, filledTts)
    }

    companion object {
        private val default = OffsetMapper()
        fun createMapping(sourceLength: Int) = default.createMapping(sourceLength)
        fun addChar(mapping: MappingArrays, sourceIndex: Int, builderLength: Int) = default.addChar(mapping, sourceIndex, builderLength)
        fun skipChar(mapping: MappingArrays, sourceIndex: Int) = default.skipChar(mapping, sourceIndex)
        fun finalize(mapping: MappingArrays, builderLength: Int, sourceLength: Int) = default.finalize(mapping, builderLength, sourceLength)
    }
}
