package com.example.util

import androidx.compose.ui.text.AnnotatedString

class OffsetMapper {

    fun createMapping(sourceLength: Int): MappingArrays {
        val sourceToTransformed = IntArray(sourceLength + 1) { -1 }
        val transformedToSource = mutableListOf<Int>()
        return MappingArrays(sourceToTransformed, transformedToSource)
    }

    data class MappingArrays(
        val sourceToTransformed: IntArray,
        val transformedToSource: MutableList<Int>
    )

    data class FinalMapping(
        val sourceToTransformed: IntArray,
        val transformedToSource: IntArray
    )

    fun addChar(mapping: MappingArrays, sourceIndex: Int, builderLength: Int) {
        mapping.transformedToSource.add(sourceIndex)
        mapping.sourceToTransformed[sourceIndex] = builderLength
    }

    fun skipChar(mapping: MappingArrays, sourceIndex: Int) {
        mapping.sourceToTransformed[sourceIndex] = mapping.sourceToTransformed.getOrElse(sourceIndex) { 0 }
    }

    fun finalize(mapping: MappingArrays, builderLength: Int, sourceLength: Int): FinalMapping {
        val (stt, tts) = mapping
        stt[sourceLength] = builderLength
        tts.add(sourceLength)

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
            filledTts[idx] = if (idx < tts.size) {
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
