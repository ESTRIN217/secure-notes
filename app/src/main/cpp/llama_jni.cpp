#include <jni.h>
#include <android/log.h>
#include <cstring>
#include <cstdlib>
#include <vector>
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

    llama_model* model = llama_load_model_from_file(path, mparams);
    env->ReleaseStringUTFChars(model_path, path);

    if (!model) {
        llama_backend_free();
        return 0;
    }

    auto cparams = llama_context_default_params();
    cparams.n_ctx = n_ctx;
    cparams.n_batch = n_ctx;
    cparams.n_threads = 4;
    cparams.n_threads_batch = 4;

    llama_context* ctx = llama_new_context_with_model(model, cparams);
    if (!ctx) {
        llama_free_model(model);
        llama_backend_free();
        return 0;
    }

    LlamaHandle* handle = new LlamaHandle{model, ctx};
    return reinterpret_cast<jlong>(handle);
}

JNIEXPORT jstring JNICALL
Java_com_example_data_ai_NativeLlamaModel_nativeGenerate(
    JNIEnv* env, jobject thiz, jlong handle, jstring jprompt, jint max_tokens) {
    LlamaHandle* h = reinterpret_cast<LlamaHandle*>(handle);
    if (!h || !h->ctx || !h->model) {
        return env->NewStringUTF("");
    }

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
        vocab, prompt, n_prompt, tokens.data(), tokens.size(), true, false);
    env->ReleaseStringUTFChars(jprompt, prompt);

    if (n_tokens < 0) {
        return env->NewStringUTF("");
    }

    tokens.resize(n_tokens);

    std::string result;

    for (int32_t i = 0; i < max_tokens; i++) {
        auto batch = llama_batch_get_one(tokens.data(), tokens.size());
        if (llama_decode(h->ctx, batch) != 0) break;

        int32_t n_vocab = llama_n_vocab(vocab);
        float* logits = llama_get_logits_ith(h->ctx, batch.n_tokens - 1);

        llama_token token_id = 0;
        float max_logit = logits[0];
        for (int32_t j = 1; j < n_vocab; j++) {
            if (logits[j] > max_logit) {
                max_logit = logits[j];
                token_id = j;
            }
        }

        char buf[16] = {0};
        int32_t len = llama_token_to_piece(vocab, token_id, buf, sizeof(buf), 0, false);
        if (len > 0) {
            result.append(buf, len);
        }

        if (token_id == llama_token_eos(vocab)) break;

        tokens.clear();
        tokens.push_back(token_id);
    }

    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_example_data_ai_NativeLlamaModel_nativeDestroy(
    JNIEnv* env, jobject thiz, jlong handle) {
    LlamaHandle* h = reinterpret_cast<LlamaHandle*>(handle);
    if (!h) return;

    if (h->ctx) llama_free(h->ctx);
    if (h->model) llama_free_model(h->model);
    llama_backend_free();
    delete h;
}

} // extern "C"
