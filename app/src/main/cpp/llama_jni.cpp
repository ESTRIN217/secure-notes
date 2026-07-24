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

static void sample_top_k(std::vector<std::pair<float, llama_token>>& candidates, int32_t top_k) {
    if ((int32_t)candidates.size() > top_k) {
        std::nth_element(candidates.begin(), candidates.begin() + top_k, candidates.end(),
            [](const std::pair<float, llama_token>& a, const std::pair<float, llama_token>& b) {
                return a.first > b.first;
            });
        candidates.resize(top_k);
    }
}

static int32_t tokenize_prompt(const struct llama_vocab* vocab, const char* prompt, int32_t prompt_len,
                                std::vector<llama_token>& tokens) {
    tokens.resize(prompt_len + 1024);
    int32_t n_tokens = llama_tokenize(vocab, prompt, prompt_len, tokens.data(), (int32_t)tokens.size(), false, true);
    if (n_tokens < 0) return n_tokens;
    tokens.resize(n_tokens);
    return n_tokens;
}

static std::string token_to_string(const struct llama_vocab* vocab, llama_token token_id) {
    char buf[64] = {0};
    int32_t len = llama_token_to_piece(vocab, token_id, buf, sizeof(buf) - 1, 0, false);
    if (len > 0) {
        buf[len] = '\0';
        return std::string(buf, len);
    }
    return "";
}

static void send_token_batch(JNIEnv* env, jobject jcallback, jmethodID onToken, std::string& batch_buffer) {
    if (batch_buffer.empty()) return;
    jstring tokenStr = env->NewStringUTF(batch_buffer.c_str());
    env->CallVoidMethod(jcallback, onToken, tokenStr);
    env->DeleteLocalRef(tokenStr);
    batch_buffer.clear();
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_data_ai_NativeLlamaModel_nativeCreate(
    JNIEnv* env, jobject thiz, jstring model_path, jint n_ctx, jint n_gpu_layers, jint n_threads) {
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
    cparams.n_threads = n_threads;
    cparams.n_threads_batch = n_threads;

    llama_context* ctx = llama_init_from_model(model, cparams);
    if (!ctx) {
        llama_model_free(model);
        llama_backend_free();
        return 0;
    }

    LlamaHandle* handle = new LlamaHandle{model, ctx};
    return reinterpret_cast<jlong>(handle);
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

    int32_t prompt_len = strlen(prompt);
    std::vector<llama_token> tokens;
    int32_t n_tokens = tokenize_prompt(vocab, prompt, prompt_len, tokens);
    env->ReleaseStringUTFChars(jprompt, prompt);

    if (n_tokens <= 0) {
        return env->NewStringUTF("");
    }

    std::string result;
    std::mt19937 rng(std::random_device{}());

    for (int32_t i = 0; i < max_tokens; i++) {
        auto batch = llama_batch_get_one(tokens.data(), tokens.size());
        if (llama_decode(h->ctx, batch) != 0) break;

        int32_t n_vocab = llama_vocab_n_tokens(vocab);
        float* raw_logits = llama_get_logits_ith(h->ctx, batch.n_tokens - 1);

        std::vector<float> logits(raw_logits, raw_logits + n_vocab);

        if (repetition_penalty != 1.0f) {
            for (size_t j = 0; j < tokens.size(); j++) {
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
            result += token_to_string(vocab, max_idx);
            if (max_idx == llama_vocab_eos(vocab)) break;
            tokens.clear();
            tokens.push_back(max_idx);
            n_tokens = 1;
            continue;
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

        result += token_to_string(vocab, token_id);

        if (token_id == llama_vocab_eos(vocab)) break;

        tokens.clear();
        tokens.push_back(token_id);
        n_tokens = 1;
    }

    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_example_data_ai_NativeLlamaModel_nativeGenerateStreaming(
    JNIEnv* env, jobject thiz, jlong handle, jstring jprompt, jint max_tokens,
    jfloat temperature, jfloat repetition_penalty, jint top_k, jobject jcallback) {

    LlamaHandle* h = reinterpret_cast<LlamaHandle*>(handle);
    if (!h || !h->ctx || !h->model) return;

    jclass callbackClass = env->GetObjectClass(jcallback);
    if (!callbackClass) return;
    jmethodID onToken = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
    jmethodID onComplete = env->GetMethodID(callbackClass, "onComplete", "()V");
    jmethodID onError = env->GetMethodID(callbackClass, "onError", "(Ljava/lang/String;)V");
    if (!onToken || !onComplete || !onError) return;

    llama_memory_clear(llama_get_memory(h->ctx), true);

    const char* prompt = env->GetStringUTFChars(jprompt, nullptr);
    if (!prompt) return;

    const struct llama_vocab* vocab = llama_model_get_vocab(h->model);
    if (!vocab) {
        env->ReleaseStringUTFChars(jprompt, prompt);
        jstring errorMsg = env->NewStringUTF("Failed to get vocabulary");
        env->CallVoidMethod(jcallback, onError, errorMsg);
        env->DeleteLocalRef(errorMsg);
        return;
    }

    int32_t prompt_len = strlen(prompt);
    std::vector<llama_token> tokens;
    int32_t n_tokens = tokenize_prompt(vocab, prompt, prompt_len, tokens);
    env->ReleaseStringUTFChars(jprompt, prompt);

    if (n_tokens <= 0) {
        jstring errorMsg = env->NewStringUTF("Tokenization failed");
        env->CallVoidMethod(jcallback, onError, errorMsg);
        env->DeleteLocalRef(errorMsg);
        return;
    }

    std::mt19937 rng(std::random_device{}());
    std::string batch_buffer;
    const int BATCH_CHARS = 50;

    for (int32_t i = 0; i < max_tokens; i++) {
        auto batch = llama_batch_get_one(tokens.data(), tokens.size());
        if (llama_decode(h->ctx, batch) != 0) break;

        int32_t n_vocab = llama_vocab_n_tokens(vocab);
        float* raw_logits = llama_get_logits_ith(h->ctx, batch.n_tokens - 1);

        std::vector<float> logits(raw_logits, raw_logits + n_vocab);

        if (repetition_penalty != 1.0f) {
            for (size_t j = 0; j < tokens.size(); j++) {
                llama_token pt = tokens[j];
                if (pt >= 0 && pt < n_vocab) {
                    logits[pt] = logits[pt] < 0.0f
                        ? logits[pt] * repetition_penalty
                        : logits[pt] / repetition_penalty;
                }
            }
        }

        llama_token token_id;
        if (temperature > 0.0f) {
            for (int32_t j = 0; j < n_vocab; j++) {
                logits[j] /= temperature;
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
            token_id = candidates[0].second;
            float cum = 0.0f;
            for (size_t j = 0; j < candidates.size(); j++) {
                cum += candidates[j].first;
                if (r <= cum) {
                    token_id = candidates[j].second;
                    break;
                }
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
            token_id = max_idx;
        }

        std::string token_str = token_to_string(vocab, token_id);
        if (!token_str.empty()) {
            batch_buffer += token_str;
        }

        if (token_id == llama_vocab_eos(vocab)) {
            send_token_batch(env, jcallback, onToken, batch_buffer);
            env->CallVoidMethod(jcallback, onComplete);
            return;
        }

        if ((int32_t)batch_buffer.size() >= BATCH_CHARS) {
            send_token_batch(env, jcallback, onToken, batch_buffer);
        }

        tokens.clear();
        tokens.push_back(token_id);
        n_tokens = 1;
    }

    send_token_batch(env, jcallback, onToken, batch_buffer);
    env->CallVoidMethod(jcallback, onComplete);
}

JNIEXPORT jstring JNICALL
Java_com_example_data_ai_NativeLlamaModel_nativeGenerateChat(
    JNIEnv* env, jobject thiz, jlong handle,
    jobjectArray jroles, jobjectArray jcontents,
    jint max_tokens, jfloat temperature, jfloat repetition_penalty, jint top_k) {

    LlamaHandle* h = reinterpret_cast<LlamaHandle*>(handle);
    if (!h || !h->ctx || !h->model) {
        return env->NewStringUTF("");
    }

    llama_memory_clear(llama_get_memory(h->ctx), true);

    jsize n_msg = env->GetArrayLength(jroles);
    std::vector<const char*> role_ptrs(n_msg);
    std::vector<const char*> content_ptrs(n_msg);
    std::vector<std::string> role_storage(n_msg);
    std::vector<std::string> content_storage(n_msg);

    for (jsize i = 0; i < n_msg; i++) {
        jstring jrole = (jstring)env->GetObjectArrayElement(jroles, i);
        jstring jcontent = (jstring)env->GetObjectArrayElement(jcontents, i);
        const char* role_c = env->GetStringUTFChars(jrole, nullptr);
        const char* content_c = env->GetStringUTFChars(jcontent, nullptr);
        role_storage[i] = role_c ? role_c : "";
        content_storage[i] = content_c ? content_c : "";
        role_ptrs[i] = role_storage[i].c_str();
        content_ptrs[i] = content_storage[i].c_str();
        if (role_c) env->ReleaseStringUTFChars(jrole, role_c);
        if (content_c) env->ReleaseStringUTFChars(jcontent, content_c);
        env->DeleteLocalRef(jrole);
        env->DeleteLocalRef(jcontent);
    }

    std::vector<llama_chat_message> chat(n_msg);
    for (jsize i = 0; i < n_msg; i++) {
        chat[i].role = role_ptrs[i];
        chat[i].content = content_ptrs[i];
    }

    const char* tmpl = llama_model_chat_template(h->model, nullptr);
    if (!tmpl) tmpl = "";

    int32_t fmt_len = llama_chat_apply_template(tmpl, chat.data(), n_msg, true, nullptr, 0);
    if (fmt_len < 0) {
        return env->NewStringUTF("");
    }

    std::string formatted(fmt_len, '\0');
    fmt_len = llama_chat_apply_template(tmpl, chat.data(), n_msg, true, &formatted[0], fmt_len);
    if (fmt_len < 0) {
        return env->NewStringUTF("");
    }
    formatted.resize(fmt_len);

    const struct llama_vocab* vocab = llama_model_get_vocab(h->model);
    if (!vocab) {
        return env->NewStringUTF("");
    }

    std::vector<llama_token> tokens;
    int32_t n_tokens = tokenize_prompt(vocab, formatted.c_str(), (int32_t)formatted.size(), tokens);
    if (n_tokens <= 0) {
        return env->NewStringUTF("");
    }

    std::string result;
    std::mt19937 rng(std::random_device{}());

    for (int32_t i = 0; i < max_tokens; i++) {
        auto batch = llama_batch_get_one(tokens.data(), tokens.size());
        if (llama_decode(h->ctx, batch) != 0) break;

        int32_t n_vocab = llama_vocab_n_tokens(vocab);
        float* raw_logits = llama_get_logits_ith(h->ctx, batch.n_tokens - 1);

        std::vector<float> logits(raw_logits, raw_logits + n_vocab);

        if (repetition_penalty != 1.0f) {
            for (size_t j = 0; j < tokens.size(); j++) {
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
            result += token_to_string(vocab, max_idx);
            if (max_idx == llama_vocab_eos(vocab)) break;
            tokens.clear();
            tokens.push_back(max_idx);
            n_tokens = 1;
            continue;
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

        result += token_to_string(vocab, token_id);

        if (token_id == llama_vocab_eos(vocab)) break;

        tokens.clear();
        tokens.push_back(token_id);
        n_tokens = 1;
    }

    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_example_data_ai_NativeLlamaModel_nativeGenerateChatStreaming(
    JNIEnv* env, jobject thiz, jlong handle,
    jobjectArray jroles, jobjectArray jcontents,
    jint max_tokens, jfloat temperature, jfloat repetition_penalty, jint top_k,
    jobject jcallback) {

    LlamaHandle* h = reinterpret_cast<LlamaHandle*>(handle);
    if (!h || !h->ctx || !h->model) return;

    jclass callbackClass = env->GetObjectClass(jcallback);
    if (!callbackClass) return;
    jmethodID onToken = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
    jmethodID onComplete = env->GetMethodID(callbackClass, "onComplete", "()V");
    jmethodID onError = env->GetMethodID(callbackClass, "onError", "(Ljava/lang/String;)V");
    if (!onToken || !onComplete || !onError) return;

    llama_memory_clear(llama_get_memory(h->ctx), true);

    jsize n_msg = env->GetArrayLength(jroles);
    std::vector<const char*> role_ptrs(n_msg);
    std::vector<const char*> content_ptrs(n_msg);
    std::vector<std::string> role_storage(n_msg);
    std::vector<std::string> content_storage(n_msg);

    for (jsize i = 0; i < n_msg; i++) {
        jstring jrole = (jstring)env->GetObjectArrayElement(jroles, i);
        jstring jcontent = (jstring)env->GetObjectArrayElement(jcontents, i);
        const char* role_c = env->GetStringUTFChars(jrole, nullptr);
        const char* content_c = env->GetStringUTFChars(jcontent, nullptr);
        role_storage[i] = role_c ? role_c : "";
        content_storage[i] = content_c ? content_c : "";
        role_ptrs[i] = role_storage[i].c_str();
        content_ptrs[i] = content_storage[i].c_str();
        if (role_c) env->ReleaseStringUTFChars(jrole, role_c);
        if (content_c) env->ReleaseStringUTFChars(jcontent, content_c);
        env->DeleteLocalRef(jrole);
        env->DeleteLocalRef(jcontent);
    }

    std::vector<llama_chat_message> chat(n_msg);
    for (jsize i = 0; i < n_msg; i++) {
        chat[i].role = role_ptrs[i];
        chat[i].content = content_ptrs[i];
    }

    const char* tmpl = llama_model_chat_template(h->model, nullptr);
    if (!tmpl) tmpl = "";

    int32_t fmt_len = llama_chat_apply_template(tmpl, chat.data(), n_msg, true, nullptr, 0);
    if (fmt_len < 0) {
        jstring errorMsg = env->NewStringUTF("Chat template formatting failed");
        env->CallVoidMethod(jcallback, onError, errorMsg);
        env->DeleteLocalRef(errorMsg);
        return;
    }

    std::string formatted(fmt_len, '\0');
    fmt_len = llama_chat_apply_template(tmpl, chat.data(), n_msg, true, &formatted[0], fmt_len);
    if (fmt_len < 0) {
        jstring errorMsg = env->NewStringUTF("Chat template formatting failed");
        env->CallVoidMethod(jcallback, onError, errorMsg);
        env->DeleteLocalRef(errorMsg);
        return;
    }
    formatted.resize(fmt_len);

    const struct llama_vocab* vocab = llama_model_get_vocab(h->model);
    if (!vocab) {
        jstring errorMsg = env->NewStringUTF("Failed to get vocabulary");
        env->CallVoidMethod(jcallback, onError, errorMsg);
        env->DeleteLocalRef(errorMsg);
        return;
    }

    std::vector<llama_token> tokens;
    int32_t n_tokens = tokenize_prompt(vocab, formatted.c_str(), (int32_t)formatted.size(), tokens);
    if (n_tokens <= 0) {
        jstring errorMsg = env->NewStringUTF("Tokenization failed");
        env->CallVoidMethod(jcallback, onError, errorMsg);
        env->DeleteLocalRef(errorMsg);
        return;
    }

    std::mt19937 rng(std::random_device{}());
    std::string batch_buffer;
    const int BATCH_CHARS = 50;

    for (int32_t i = 0; i < max_tokens; i++) {
        auto batch = llama_batch_get_one(tokens.data(), tokens.size());
        if (llama_decode(h->ctx, batch) != 0) break;

        int32_t n_vocab = llama_vocab_n_tokens(vocab);
        float* raw_logits = llama_get_logits_ith(h->ctx, batch.n_tokens - 1);

        std::vector<float> logits(raw_logits, raw_logits + n_vocab);

        if (repetition_penalty != 1.0f) {
            for (size_t j = 0; j < tokens.size(); j++) {
                llama_token pt = tokens[j];
                if (pt >= 0 && pt < n_vocab) {
                    logits[pt] = logits[pt] < 0.0f
                        ? logits[pt] * repetition_penalty
                        : logits[pt] / repetition_penalty;
                }
            }
        }

        llama_token token_id;
        if (temperature > 0.0f) {
            for (int32_t j = 0; j < n_vocab; j++) {
                logits[j] /= temperature;
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
            token_id = candidates[0].second;
            float cum = 0.0f;
            for (size_t j = 0; j < candidates.size(); j++) {
                cum += candidates[j].first;
                if (r <= cum) {
                    token_id = candidates[j].second;
                    break;
                }
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
            token_id = max_idx;
        }

        std::string token_str = token_to_string(vocab, token_id);
        if (!token_str.empty()) {
            batch_buffer += token_str;
        }

        if (token_id == llama_vocab_eos(vocab)) {
            send_token_batch(env, jcallback, onToken, batch_buffer);
            env->CallVoidMethod(jcallback, onComplete);
            return;
        }

        if ((int32_t)batch_buffer.size() >= BATCH_CHARS) {
            send_token_batch(env, jcallback, onToken, batch_buffer);
        }

        tokens.clear();
        tokens.push_back(token_id);
        n_tokens = 1;
    }

    send_token_batch(env, jcallback, onToken, batch_buffer);
    env->CallVoidMethod(jcallback, onComplete);
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
