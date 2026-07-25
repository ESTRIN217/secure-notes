package com.example.data.ai

data class OnDeviceModel(
    val id: String,
    val displayName: String,
    val minRamMb: Long,
    val recommendedRamMb: Long,
    val fileSizeMb: Int,
    val huggingFaceRepo: String,
    val ggufFileName: String,
    val minApiLevel: Int = 24,
    val requiresAbi: String = "arm64-v8a"
)

val MODEL_CATALOG = listOf(
    OnDeviceModel(
        id = "smollm2-360m",
        displayName = "SmolLM2 360M",
        minRamMb = 512,
        recommendedRamMb = 1024,
        fileSizeMb = 258,
        huggingFaceRepo = "hugging-quants/SmolLM2-360M-Instruct-Q4_K_M-GGUF",
        ggufFileName = "smollm2-360m-instruct-q4_k_m.gguf"
    ),
    OnDeviceModel(
        id = "qwen2.5-0.5b",
        displayName = "Qwen 2.5 0.5B",
        minRamMb = 768,
        recommendedRamMb = 1536,
        fileSizeMb = 469,
        huggingFaceRepo = "Qwen/Qwen2.5-0.5B-Instruct-GGUF",
        ggufFileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
    ),
    OnDeviceModel(
        id = "smollm2-1.7b",
        displayName = "SmolLM2 1.7B",
        minRamMb = 1536,
        recommendedRamMb = 3072,
        fileSizeMb = 1007,
        huggingFaceRepo = "hugging-quants/SmolLM2-1.7B-Instruct-Q4_K_M-GGUF",
        ggufFileName = "smollm2-1.7b-instruct-q4_k_m.gguf"
    ),
    OnDeviceModel(
        id = "llama3.2-1b",
        displayName = "Llama 3.2 1B",
        minRamMb = 1536,
        recommendedRamMb = 3072,
        fileSizeMb = 770,
        huggingFaceRepo = "hugging-quants/Llama-3.2-1B-Instruct-Q4_K_M-GGUF",
        ggufFileName = "llama-3.2-1b-instruct-q4_k_m.gguf"
    ),
    OnDeviceModel(
        id = "phi3-mini",
        displayName = "Phi-3 Mini",
        minRamMb = 2048,
        recommendedRamMb = 4096,
        fileSizeMb = 2283,
        huggingFaceRepo = "microsoft/Phi-3-mini-4k-instruct-gguf",
        ggufFileName = "phi-3-mini-4k-instruct-q4.gguf"
    ),
    OnDeviceModel(
        id = "phi3.5-mini",
        displayName = "Phi-3.5 Mini",
        minRamMb = 2560,
        recommendedRamMb = 4096,
        fileSizeMb = 2283,
        huggingFaceRepo = "microsoft/Phi-3.5-mini-instruct-gguf",
        ggufFileName = "phi-3.5-mini-instruct-q4.gguf"
    ),
    OnDeviceModel(
        id = "gemma2-2b",
        displayName = "Gemma 2 2B",
        minRamMb = 2560,
        recommendedRamMb = 4096,
        fileSizeMb = 1630,
        huggingFaceRepo = "bartowski/gemma-2-2b-it-GGUF",
        ggufFileName = "gemma-2-2b-it-Q4_K_M.gguf",
        minApiLevel = 26
    ),
    OnDeviceModel(
        id = "llama3.2-3b",
        displayName = "Llama 3.2 3B",
        minRamMb = 3072,
        recommendedRamMb = 6144,
        fileSizeMb = 1926,
        huggingFaceRepo = "hugging-quants/Llama-3.2-3B-Instruct-Q4_K_M-GGUF",
        ggufFileName = "llama-3.2-3b-instruct-q4_k_m.gguf",
        minApiLevel = 26
    ),
    // Qwen 3
    OnDeviceModel(
        id = "qwen3-0.6b",
        displayName = "Qwen3 0.6B",
        minRamMb = 768,
        recommendedRamMb = 1536,
        fileSizeMb = 492,
        huggingFaceRepo = "bartowski/Qwen_Qwen3-0.6B-GGUF",
        ggufFileName = "Qwen_Qwen3-0.6B-Q4_K_M.gguf"
    ),
    OnDeviceModel(
        id = "qwen3-1.7b",
        displayName = "Qwen3 1.7B",
        minRamMb = 1536,
        recommendedRamMb = 3072,
        fileSizeMb = 1280,
        huggingFaceRepo = "bartowski/Qwen_Qwen3-1.7B-GGUF",
        ggufFileName = "Qwen_Qwen3-1.7B-Q4_K_M.gguf"
    ),
    // Qwen 2.5 1.5B
    OnDeviceModel(
        id = "qwen2.5-1.5b",
        displayName = "Qwen 2.5 1.5B",
        minRamMb = 1536,
        recommendedRamMb = 3072,
        fileSizeMb = 1040,
        huggingFaceRepo = "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
        ggufFileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf"
    ),
    // Gemma 3
    OnDeviceModel(
        id = "gemma3-1b",
        displayName = "Gemma 3 1B",
        minRamMb = 1024,
        recommendedRamMb = 2048,
        fileSizeMb = 769,
        huggingFaceRepo = "unsloth/gemma-3-1b-it-GGUF",
        ggufFileName = "gemma-3-1b-it-Q4_K_M.gguf"
    ),
    OnDeviceModel(
        id = "gemma3-2b",
        displayName = "Gemma 3 2B",
        minRamMb = 2048,
        recommendedRamMb = 4096,
        fileSizeMb = 1500,
        huggingFaceRepo = "unsloth/gemma-3-2b-it-GGUF",
        ggufFileName = "gemma-3-2b-it-Q4_K_M.gguf"
    ),
    // DeepSeek
    OnDeviceModel(
        id = "deepseek-r1-distill-qwen-1.5b",
        displayName = "DeepSeek-R1-Distill-Qwen-1.5B",
        minRamMb = 1536,
        recommendedRamMb = 3072,
        fileSizeMb = 1040,
        huggingFaceRepo = "unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF",
        ggufFileName = "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf"
    ),
    // Gemma 4
    OnDeviceModel(
        id = "gemma4-e2b",
        displayName = "Gemma 4 E2B",
        minRamMb = 2048,
        recommendedRamMb = 4096,
        fileSizeMb = 1600,
        huggingFaceRepo = "unsloth/gemma-4-E2B-it-GGUF",
        ggufFileName = "gemma-4-E2B-it-Q4_K_M.gguf"
    )
)

fun List<OnDeviceModel>.filterForDevice(info: DeviceInfo): List<OnDeviceModel> {
    return filter { model ->
        model.minRamMb <= info.totalRamMb &&
            model.minApiLevel <= info.apiLevel &&
            (model.requiresAbi in info.supportedAbis || model.requiresAbi == "")
    }
}

fun List<OnDeviceModel>.bestForDevice(info: DeviceInfo): OnDeviceModel? {
    val compatible = filterForDevice(info)
    val best = compatible.maxByOrNull { it.fileSizeMb } ?: return null
    if (best.minRamMb < info.availableRamMb) return best
    return compatible
        .sortedByDescending { it.fileSizeMb }
        .firstOrNull { it.minRamMb < info.availableRamMb }
}
