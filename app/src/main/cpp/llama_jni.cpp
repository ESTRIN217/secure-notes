#include <jni.h>
#include <android/log.h>
#include <cstring>
#include <cstdlib>
#include <vector>
#include <random>
#include <cmath>
#include "llama.h"

static void llama_log_cb(enum ggml_log_level level, const char * text, void * user_data) {
    (void)user_data;
    int android_prio;
    switch (level) {
        case GGML_LOG_LEVEL_DEBUG: android_prio = ANDROID_LOG_DEBUG; break;
        case GGML_LOG_LEVEL_INFO:  android_prio = ANDROID_LOG_INFO;  break;
        case GGML_LOG_LEVEL_WARN:  android_prio = ANDROID_LOG_WARN;  break;
        case GGML_LOG_LEVEL_ERROR: android_prio = ANDROID_LOG_ERROR; break;
        default:                   android_prio = ANDROID_LOG_INFO;  break;
    }
    __android_log_print(android_prio, "LLaMa", "%s", text);
}

struct LlamaHandle {
    llama_model* model;
    llama_context* ctx;
};

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_data_ai_NativeLlamaModel_nativeCreate(
    JNIEnv* env, jobject thiz, jstring model_path, jint n_ctx, jint n_gpu_layers) {
    llama_log_set(llama_log_cb, nullptr);

    const char* path = env->GetStringUTFChars(model_path, nullptr);
    if (!path) return 0;

    llama_backend_init();

    auto mparams = llama_model_default_params();
    mparams.n_gpu_layers = n_gpu_layers;

    llama_model* model = llama_model_load_from_file(path, mparams);
    env->ReleaseStringUTFChars(model_path, path);

    if (!model) {
        llama_backend_free();
        return 0;
    }

    auto cparams = llama_context_default_params();
    cparams.n_ctx = n_ctx;
    cparams.n_batch = n_ctx;
    cparams.n_ubatch = n_ctx;
    cparams.n_threads = 4;
    cparams.n_threads_batch = 4;

    llama_context* ctx = llama_init_from_model(model, cparams);
    if (!ctx) {
        llama_model_free(model);
        llama_backend_free();
        return 0;
    }

    LlamaHandle* handle = new LlamaHandle{model, ctx};
    return reinterpret_cast<jlong>(handle);
}

static void sample_top_k(std::vector<std::pair<float, llama_token>>& candidates, int32_t top_k) {
    if ((int32_t)candidates.size() > top_k) {
        std::nth_element(candidates.begin(), candidates.begin() + top_k, candidates.end(),
            [](const std::pair<float, llama_token>& a, const std::pair<float, llama_token>& b) {
                return a.first > b.first;
            });
        candidates.resize(top_k);
    }
}

JNIEXPORT jstring JNICALL
Java_com_example_data_ai_NativeLlamaModel_nativeGenerate(
    JNIEnv* env, jobject thiz, jlong handle, jstring jprompt, jint max_tokens,
    jfloat temperature, jfloat repetition_penalty, jint top_k) {
    LlamaHandle* h = reinterpret_cast<LlamaHandle*>(handle);
    if (!h || !h->ctx || !h->model) {
        return env->NewStringUTF("");
    }

    llama_memory_clear(llama_get_memory(h->ctx), true);

    const char* prompt = env->GetStringUTFChars(jprompt, nullptr);
    if (!prompt) return env->NewStringUTF("");

    const struct llama_vocab* vocab = llama_model_get_vocab(h->model);
    if (!vocab) {
        env->ReleaseStringUTFChars(jprompt, prompt);
        return env->NewStringUTF("");
    }

    int32_t n_prompt = strlen(prompt);
    std::vector<llama_token> tokens(n_prompt + 1024);
    int32_t n_tokens = llama_tokenize(
        vocab, prompt, n_prompt, tokens.data(), tokens.size(), false, false);
    env->ReleaseStringUTFChars(jprompt, prompt);

    if (n_tokens < 0) {
        return env->NewStringUTF("");
    }

    tokens.resize(n_tokens);

    std::string result;
    std::mt19937 rng(std::random_device{}());

    for (int32_t i = 0; i < max_tokens; i++) {
        auto batch = llama_batch_get_one(tokens.data(), tokens.size());
        if (llama_decode(h->ctx, batch) != 0) break;

        int32_t n_vocab = llama_vocab_n_tokens(vocab);
        float* raw_logits = llama_get_logits_ith(h->ctx, batch.n_tokens - 1);

        std::vector<float> logits(raw_logits, raw_logits + n_vocab);

        if (repetition_penalty != 1.0f) {
            for (size_t j = 0; j < n_tokens; j++) {
                llama_token pt = tokens[j];
                if (pt >= 0 && pt < n_vocab) {
                    logits[pt] = logits[pt] < 0.0f
                        ? logits[pt] * repetition_penalty
                        : logits[pt] / repetition_penalty;
                }
            }
        }

        if (temperature > 0.0f) {
            for (int32_t j = 0; j < n_vocab; j++) {
                logits[j] /= temperature;
            }
        } else {
            float max_logit = -1e38f;
            int32_t max_idx = 0;
            for (int32_t j = 0; j < n_vocab; j++) {
                if (logits[j] > max_logit) {
                    max_logit = logits[j];
                    max_idx = j;
                }
            }
            char buf[16] = {0};
            int32_t len = llama_token_to_piece(vocab, max_idx, buf, sizeof(buf), 0, false);
            if (len > 0) {
                result.append(buf, len);
            }
            break;
        }

        std::vector<std::pair<float, llama_token>> candidates;
        candidates.reserve(n_vocab);
        for (int32_t j = 0; j < n_vocab; j++) {
            candidates.emplace_back(logits[j], j);
        }

        if (top_k > 0) {
            sample_top_k(candidates, top_k);
        }

        float max_logit = candidates[0].first;
        for (size_t j = 0; j < candidates.size(); j++) {
            candidates[j].first = std::exp(candidates[j].first - max_logit);
        }

        float sum = 0.0f;
        for (size_t j = 0; j < candidates.size(); j++) {
            sum += candidates[j].first;
        }
        float r = std::uniform_real_distribution<float>(0.0f, sum)(rng);
        llama_token token_id = candidates[0].second;
        float cum = 0.0f;
        for (size_t j = 0; j < candidates.size(); j++) {
            cum += candidates[j].first;
            if (r <= cum) {
                token_id = candidates[j].second;
                break;
            }
        }

        char buf[16] = {0};
        int32_t len = llama_token_to_piece(vocab, token_id, buf, sizeof(buf), 0, false);
        if (len > 0) {
            result.append(buf, len);
        }

        if (token_id == llama_vocab_eos(vocab)) break;

        tokens.clear();
        tokens.push_back(token_id);
        n_tokens = 1;
    }

    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_example_data_ai_NativeLlamaModel_nativeDestroy(
    JNIEnv* env, jobject thiz, jlong handle) {
    LlamaHandle* h = reinterpret_cast<LlamaHandle*>(handle);
    if (!h) return;

    if (h->ctx) llama_free(h->ctx);
    if (h->model) llama_model_free(h->model);
    llama_backend_free();
    delete h;
}

} // extern "C"
