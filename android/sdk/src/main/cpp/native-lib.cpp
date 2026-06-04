#include <jni.h>
#include <string>
#include <vector>
#include <thread>
#include <cmath>
#include <limits>
#include <sstream>
#include <android/log.h>
#include "llama.h"
#include "common.h"
#include "sampling.h"

#define LOG_TAG "LokAI-Native"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

constexpr int   BATCH_SIZE           = 512;
constexpr int   OVERFLOW_HEADROOM    = 4;

static inline int android_log_prio_from_ggml(enum ggml_log_level level) {
    switch (level) {
        case GGML_LOG_LEVEL_ERROR: return ANDROID_LOG_ERROR;
        case GGML_LOG_LEVEL_WARN:  return ANDROID_LOG_WARN;
        case GGML_LOG_LEVEL_INFO:  return ANDROID_LOG_INFO;
        case GGML_LOG_LEVEL_DEBUG: return ANDROID_LOG_DEBUG;
        default:                   return ANDROID_LOG_DEFAULT;
    }
}

static inline void lokai_android_log_callback(enum ggml_log_level level,
                                              const char* text,
                                              void* /*user*/) {
    const int prio = android_log_prio_from_ggml(level);
    __android_log_write(prio, LOG_TAG, text);
}

static bool is_valid_utf8(const char* string) {
    if (!string) { return true; }

    const auto* bytes = (const unsigned char*) string;
    int num;

    while (*bytes != 0x00) {
        if ((*bytes & 0x80) == 0x00) {
            num = 1;
        } else if ((*bytes & 0xE0) == 0xC0) {
            num = 2;
        } else if ((*bytes & 0xF0) == 0xE0) {
            num = 3;
        } else if ((*bytes & 0xF8) == 0xF0) {
            num = 4;
        } else {
            return false;
        }

        bytes += 1;
        for (int i = 1; i < num; ++i) {
            if ((*bytes & 0xC0) != 0x80) {
                return false;
            }
            bytes += 1;
        }
    }
    return true;
}

static bool s_backends_loaded = false;

struct InferenceContext {
    llama_model* model;
    llama_context* context;
    llama_batch batch;
    common_sampler* sampler;
    int32_t n_ctx;
    int32_t n_threads;
    int32_t n_past;
    int32_t n_remaining;
    std::string cached_token_chars;
    bool stop_requested;
    jobject callback_obj;
    jmethodID callback_method;

    InferenceContext() : model(nullptr), context(nullptr), sampler(nullptr),
                         n_ctx(2048), n_threads(4),
                         n_past(0), n_remaining(512),
                         stop_requested(false), callback_obj(nullptr), callback_method(nullptr) {}
};

static int32_t getAvailableThreads() {
    return std::max(1, static_cast<int32_t>(std::thread::hardware_concurrency() - 1));
}

static int decode_tokens_in_batches(
        InferenceContext* ctx,
        const std::vector<llama_token>& tokens,
        const llama_pos start_pos,
        const bool compute_last_logit = false) {
    LOGD("decode_tokens_in_batches: Decode %d tokens starting at position %d", (int) tokens.size(), start_pos);
    for (int i = 0; i < (int) tokens.size(); i += BATCH_SIZE) {
        const int cur_batch_size = std::min((int) tokens.size() - i, BATCH_SIZE);
        common_batch_clear(ctx->batch);
        LOGD("decode_tokens_in_batches: Preparing a batch size of %d starting at: %d", cur_batch_size, i);

        for (int j = 0; j < cur_batch_size; j++) {
            const llama_token token_id = tokens[i + j];
            const llama_pos position = start_pos + i + j;
            const bool want_logit = compute_last_logit && (i + j == tokens.size() - 1);
            common_batch_add(ctx->batch, token_id, position, {0}, want_logit);
        }

        const int decode_result = llama_decode(ctx->context, ctx->batch);
        if (decode_result) {
            LOGE("decode_tokens_in_batches: llama_decode failed w/ %d", decode_result);
            return 1;
        }
    }
    return 0;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_lokai_sdk_NativeLib_createContext(
        JNIEnv* env,
        jobject thiz,
        jstring modelPath,
        jint n_ctx,
        jint n_threads,
        jfloat temperature,
        jint top_k,
        jfloat top_p,
        jfloat repeat_penalty,
        jfloat frequency_penalty,
        jfloat presence_penalty,
        jint penalty_last_n) {

    LOGI("createContext called");

    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    if (path == nullptr) {
        LOGE("Failed to get model path string");
        return 0;
    }

    LOGI("Model path: %s", path);
    LOGI("Params: n_ctx=%d, n_threads=%d, temp=%.2f, top_k=%d, top_p=%.2f",
         n_ctx, n_threads, temperature, top_k, top_p);
    LOGI("Penalties: repeat=%.2f, freq=%.2f, pres=%.2f, last_n=%d",
         repeat_penalty, frequency_penalty, presence_penalty, penalty_last_n);

    FILE* file = fopen(path, "rb");
    if (file == nullptr) {
        LOGE("Model file not found or cannot open: %s", path);
        env->ReleaseStringUTFChars(modelPath, path);
        return 0;
    }
    fclose(file);
    LOGI("Model file exists and accessible");

    InferenceContext* ctx = new InferenceContext();
    ctx->n_ctx = n_ctx > 0 ? n_ctx : 2048;
    ctx->n_threads = n_threads > 0 ? n_threads : getAvailableThreads();
    ctx->n_remaining = ctx->n_ctx;

    LOGI("Creating model with n_ctx=%d, n_threads=%d", ctx->n_ctx, ctx->n_threads);

    llama_log_set(lokai_android_log_callback, nullptr);

    if (!s_backends_loaded) {
        LOGI("Initializing llama backend...");
        llama_backend_init();
        s_backends_loaded = true;
        LOGI("Llama backend initialized successfully");
    } else {
        LOGI("Llama backend already initialized");
    }

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;
    model_params.use_mmap = true;
    model_params.use_mlock = false;

    LOGI("Calling llama_model_load_from_file...");
    ctx->model = llama_model_load_from_file(path, model_params);
    if (ctx->model == nullptr) {
        LOGE("Failed to load model from file");
        delete ctx;
        env->ReleaseStringUTFChars(modelPath, path);
        return 0;
    }
    LOGI("Model loaded successfully");

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = ctx->n_ctx;
    ctx_params.n_batch = BATCH_SIZE;
    ctx_params.n_ubatch = BATCH_SIZE;
    ctx_params.n_threads = ctx->n_threads;
    ctx_params.n_threads_batch = ctx->n_threads;

    LOGI("Creating context...");
    ctx->context = llama_init_from_model(ctx->model, ctx_params);
    if (ctx->context == nullptr) {
        LOGE("Failed to create context");
        llama_model_free(ctx->model);
        delete ctx;
        env->ReleaseStringUTFChars(modelPath, path);
        return 0;
    }
    LOGI("Context created successfully");

    LOGI("Initializing batch...");
    ctx->batch = llama_batch_init(BATCH_SIZE, 0, 1);

    common_params_sampling sparams;
    sparams.temp = temperature > 0 ? temperature : 0.8f;
    sparams.top_k = top_k > 0 ? top_k : 40;
    sparams.top_p = top_p > 0 ? top_p : 0.95f;
    sparams.penalty_repeat = repeat_penalty > 0 ? repeat_penalty : 1.0f;
    sparams.penalty_freq = frequency_penalty;
    sparams.penalty_present = presence_penalty;
    sparams.penalty_last_n = penalty_last_n >= 0 ? penalty_last_n : 64;

    LOGI("Initializing common sampler with params...");
    ctx->sampler = common_sampler_init(ctx->model, sparams);
    if (ctx->sampler == nullptr) {
        LOGE("Failed to initialize common sampler");
        llama_batch_free(ctx->batch);
        llama_free(ctx->context);
        llama_model_free(ctx->model);
        delete ctx;
        env->ReleaseStringUTFChars(modelPath, path);
        return 0;
    }
    LOGI("Common sampler initialized successfully, context handle: %lld", reinterpret_cast<long long>(ctx));

    env->ReleaseStringUTFChars(modelPath, path);
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_lokai_sdk_NativeLib_complete(
        JNIEnv* env,
        jobject thiz,
        jlong contextHandle,
        jstring prompt,
        jint max_tokens) {

    LOGI("[DEBUG] complete called with max_tokens=%d", max_tokens);

    if (contextHandle == 0) {
        LOGE("[DEBUG] contextHandle is 0");
        return env->NewStringUTF("");
    }

    InferenceContext* ctx = reinterpret_cast<InferenceContext*>(contextHandle);
    if (ctx->model == nullptr || ctx->context == nullptr || ctx->sampler == nullptr) {
        LOGE("[DEBUG] model=%p, context=%p, sampler=%p - one is null", 
             ctx->model, ctx->context, ctx->sampler);
        return env->NewStringUTF("");
    }
    LOGI("[DEBUG] Context initialized: model=%p, context=%p, sampler=%p", 
         ctx->model, ctx->context, ctx->sampler);

    const char* prompt_str = env->GetStringUTFChars(prompt, nullptr);
    if (prompt_str == nullptr) {
        LOGE("[DEBUG] prompt_str is null");
        return env->NewStringUTF("");
    }

    size_t prompt_len = strlen(prompt_str);
    std::string prompt_text(prompt_str, prompt_len);
    LOGI("[DEBUG] Prompt received (len=%zu): %s", prompt_len, prompt_str);

    ctx->n_remaining = max_tokens > 0 ? max_tokens : 128;
    ctx->n_past = 0;
    ctx->cached_token_chars.clear();
    llama_memory_clear(llama_get_memory(ctx->context), false);
    LOGI("[DEBUG] Memory cleared, n_ctx=%d", ctx->n_ctx);
    common_sampler_reset(ctx->sampler);
    LOGI("[DEBUG] Sampler reset");

    LOGI("[DEBUG] Starting tokenization...");
    
    std::string formatted_prompt = "<system>你是一个友好的AI助手，请用自然语言回答用户的问题。</system>\n<user>" + prompt_text + "</user>\n<assistant>";
    LOGI("[DEBUG] Formatted prompt: %s", formatted_prompt.c_str());
    
    std::vector<llama_token> prompt_tokens = common_tokenize(ctx->context, formatted_prompt, true, true);
    LOGI("[DEBUG] Tokenization complete: %d tokens generated", (int) prompt_tokens.size());

    if (prompt_tokens.size() > 0 && prompt_tokens.size() <= 20) {
        std::string token_debug;
        for (size_t i = 0; i < prompt_tokens.size(); i++) {
            if (i > 0) token_debug += ",";
            token_debug += std::to_string(prompt_tokens[i]);
        }
        LOGI("[DEBUG] First %d tokens: [%s]", (int) prompt_tokens.size(), token_debug.c_str());
    } else if (prompt_tokens.size() > 20) {
        std::string token_debug;
        for (size_t i = 0; i < 10; i++) {
            if (i > 0) token_debug += ",";
            token_debug += std::to_string(prompt_tokens[i]);
        }
        LOGI("[DEBUG] First 10 tokens: [%s], total=%d", token_debug.c_str(), (int) prompt_tokens.size());
    }

    const int max_batch_size = ctx->n_ctx - OVERFLOW_HEADROOM;
    if ((int) prompt_tokens.size() > max_batch_size) {
        const int skipped_tokens = (int) prompt_tokens.size() - max_batch_size;
        prompt_tokens.resize(max_batch_size);
        LOGW("[DEBUG] Prompt too long! Skipped %d tokens!", skipped_tokens);
    }

    LOGI("[DEBUG] Starting prompt decoding...");
    if (decode_tokens_in_batches(ctx, prompt_tokens, ctx->n_past, true)) {
        LOGE("[DEBUG] Failed to decode prompt tokens!");
        env->ReleaseStringUTFChars(prompt, prompt_str);
        return env->NewStringUTF("[prompt decode failed]");
    }
    ctx->n_past += (int) prompt_tokens.size();
    LOGI("[DEBUG] Prompt decoded successfully, n_past=%d", ctx->n_past);

    std::ostringstream result_ss;

    const int max_generate = ctx->n_remaining > 0 ? ctx->n_remaining : 128;
    LOGI("[DEBUG] Starting generation loop, max_generate=%d", max_generate);
    int generated_tokens = 0;
    int log_interval = std::max(1, max_generate / 5);
    ctx->stop_requested = false;
    while (generated_tokens < max_generate) {
        if (ctx->stop_requested) {
            LOGI("[DEBUG] Generation stopped by user request");
            break;
        }
        if (ctx->n_past >= ctx->n_ctx - OVERFLOW_HEADROOM) {
            LOGW("[DEBUG] Context window full at n_past=%d, n_ctx=%d", ctx->n_past, ctx->n_ctx);
            break;
        }

        LOGD("[DEBUG] Sampling token %d/%d", generated_tokens + 1, max_generate);
        llama_token new_token = common_sampler_sample(ctx->sampler, ctx->context, -1);
        LOGD("[DEBUG] Sampled token: %d", new_token);
        common_sampler_accept(ctx->sampler, new_token, true);

        const llama_vocab* vocab = llama_model_get_vocab(ctx->model);
        if (llama_vocab_is_eog(vocab, new_token)) {
            LOGI("[DEBUG] End of generation reached after %d tokens (EOG token: %d)", generated_tokens, new_token);
            break;
        }

        std::string token_text = common_token_to_piece(ctx->context, new_token);
        LOGD("[DEBUG] Token %d -> text: '%s'", new_token, token_text.c_str());
        ctx->cached_token_chars += token_text;

        if (is_valid_utf8(ctx->cached_token_chars.c_str())) {
            LOGD("[DEBUG] Appending to result: '%s'", ctx->cached_token_chars.c_str());
            result_ss << ctx->cached_token_chars;
            ctx->cached_token_chars.clear();
        }

        common_batch_clear(ctx->batch);
        common_batch_add(ctx->batch, new_token, ctx->n_past, {0}, true);
        LOGD("[DEBUG] Decoding token %d at position %d", new_token, ctx->n_past);
        if (llama_decode(ctx->context, ctx->batch) != 0) {
            LOGE("[DEBUG] Failed to decode sampled token!");
            break;
        }
        ctx->n_past += 1;

        generated_tokens++;
        ctx->n_remaining--;

        if (generated_tokens % log_interval == 0) {
            std::string partial = result_ss.str();
            LOGI("[DEBUG] Generated %d tokens, current output: %.100s", generated_tokens, partial.c_str());
        }
    }

    if (!ctx->cached_token_chars.empty() && is_valid_utf8(ctx->cached_token_chars.c_str())) {
        LOGD("[DEBUG] Flushing remaining cached chars: '%s'", ctx->cached_token_chars.c_str());
        result_ss << ctx->cached_token_chars;
    }

    std::string result = result_ss.str();
    LOGI("Generated %d tokens, result length: %zu", generated_tokens, result.length());

    env->ReleaseStringUTFChars(prompt, prompt_str);
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_lokai_sdk_NativeLib_getContextSize(
        JNIEnv* env,
        jobject thiz,
        jlong contextHandle) {

    if (contextHandle == 0) {
        return 0;
    }

    InferenceContext* ctx = reinterpret_cast<InferenceContext*>(contextHandle);
    return ctx->n_ctx;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_lokai_sdk_NativeLib_getMemoryUsage(
        JNIEnv* env,
        jobject thiz,
        jlong contextHandle) {

    if (contextHandle == 0) {
        return 0;
    }

    InferenceContext* ctx = reinterpret_cast<InferenceContext*>(contextHandle);
    if (ctx->model == nullptr) {
        return 0;
    }

    return llama_model_size(ctx->model) + llama_state_get_size(ctx->context);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_lokai_sdk_NativeLib_getVocabSize(
        JNIEnv* env,
        jobject thiz,
        jlong contextHandle) {

    if (contextHandle == 0) {
        return 0;
    }

    InferenceContext* ctx = reinterpret_cast<InferenceContext*>(contextHandle);
    if (ctx->model == nullptr) {
        return 0;
    }

    const llama_vocab* vocab = llama_model_get_vocab(ctx->model);
    return llama_vocab_n_tokens(vocab);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_lokai_sdk_NativeLib_getModelName(
        JNIEnv* env,
        jobject thiz,
        jlong contextHandle) {

    if (contextHandle == 0) {
        return env->NewStringUTF("unknown");
    }

    InferenceContext* ctx = reinterpret_cast<InferenceContext*>(contextHandle);
    if (ctx->model == nullptr) {
        return env->NewStringUTF("unknown");
    }

    char model_desc[128];
    llama_model_desc(ctx->model, model_desc, sizeof(model_desc));
    return env->NewStringUTF(model_desc);
}

extern "C" JNIEXPORT void JNICALL
Java_com_lokai_sdk_NativeLib_freeContext(
        JNIEnv* env,
        jobject thiz,
        jlong contextHandle) {

    if (contextHandle == 0) {
        return;
    }

    InferenceContext* ctx = reinterpret_cast<InferenceContext*>(contextHandle);

    if (ctx->sampler != nullptr) {
        common_sampler_free(ctx->sampler);
        ctx->sampler = nullptr;
    }

    llama_batch_free(ctx->batch);

    if (ctx->context != nullptr) {
        llama_free(ctx->context);
        ctx->context = nullptr;
    }

    if (ctx->model != nullptr) {
        llama_model_free(ctx->model);
        ctx->model = nullptr;
    }

    delete ctx;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_lokai_sdk_NativeLib_getModelSize(
        JNIEnv* env,
        jobject thiz,
        jlong contextHandle) {

    if (contextHandle == 0) {
        return 0;
    }

    InferenceContext* ctx = reinterpret_cast<InferenceContext*>(contextHandle);
    if (ctx->model == nullptr) {
        return 0;
    }

    return llama_model_size(ctx->model);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_lokai_sdk_NativeLib_tokenize(
        JNIEnv* env,
        jobject thiz,
        jlong contextHandle,
        jstring text) {

    LOGI("tokenize called");

    if (contextHandle == 0) {
        LOGE("contextHandle is 0");
        return env->NewStringUTF("");
    }

    InferenceContext* ctx = reinterpret_cast<InferenceContext*>(contextHandle);
    if (ctx->model == nullptr) {
        LOGE("model is null");
        return env->NewStringUTF("");
    }

    const char* text_str = env->GetStringUTFChars(text, nullptr);
    if (text_str == nullptr) {
        LOGE("text_str is null");
        return env->NewStringUTF("");
    }

    size_t text_len = strlen(text_str);
    std::string input_text(text_str, text_len);
    LOGI("Text (len=%zu): %.20s", text_len, text_str);

    std::vector<llama_token> tokens = common_tokenize(ctx->context, input_text, true, true);
    LOGI("Successfully tokenized to %d tokens", (int)tokens.size());

    std::string result;
    result.reserve(tokens.size() * 6);

    for (int i = 0; i < (int)tokens.size(); i++) {
        if (i > 0) result += ",";
        result += std::to_string(tokens[i]);
    }

    env->ReleaseStringUTFChars(text, text_str);
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_lokai_sdk_NativeLib_detokenize(
        JNIEnv* env,
        jobject thiz,
        jlong contextHandle,
        jstring tokens_str) {

    if (contextHandle == 0) {
        return env->NewStringUTF("");
    }

    InferenceContext* ctx = reinterpret_cast<InferenceContext*>(contextHandle);
    if (ctx->model == nullptr) {
        return env->NewStringUTF("");
    }

    const char* tokens_cstr = env->GetStringUTFChars(tokens_str, nullptr);
    if (tokens_cstr == nullptr) {
        return env->NewStringUTF("");
    }

    std::vector<llama_token> tokens;
    std::string token_str(tokens_cstr);
    size_t pos = 0;
    while (pos < token_str.length()) {
        size_t comma_pos = token_str.find(',', pos);
        std::string token_num;
        if (comma_pos == std::string::npos) {
            token_num = token_str.substr(pos);
            pos = token_str.length();
        } else {
            token_num = token_str.substr(pos, comma_pos - pos);
            pos = comma_pos + 1;
        }

        try {
            tokens.push_back(std::stoi(token_num));
        } catch (...) {
            break;
        }
    }

    std::ostringstream result_ss;
    ctx->cached_token_chars.clear();

    for (llama_token token : tokens) {
        std::string token_text = common_token_to_piece(ctx->context, token);
        ctx->cached_token_chars += token_text;
        if (is_valid_utf8(ctx->cached_token_chars.c_str())) {
            result_ss << ctx->cached_token_chars;
            ctx->cached_token_chars.clear();
        }
    }

    if (!ctx->cached_token_chars.empty() && is_valid_utf8(ctx->cached_token_chars.c_str())) {
        result_ss << ctx->cached_token_chars;
    }

    env->ReleaseStringUTFChars(tokens_str, tokens_cstr);
    return env->NewStringUTF(result_ss.str().c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_lokai_sdk_NativeLib_stopGeneration(
        JNIEnv* env,
        jobject thiz,
        jlong contextHandle) {

    LOGI("[DEBUG] stopGeneration called");

    if (contextHandle == 0) {
        return;
    }

    InferenceContext* ctx = reinterpret_cast<InferenceContext*>(contextHandle);
    ctx->stop_requested = true;
    LOGI("[DEBUG] Stop flag set to true");
}

extern "C" JNIEXPORT void JNICALL
Java_com_lokai_sdk_NativeLib_resetContext(
        JNIEnv* env,
        jobject thiz,
        jlong contextHandle) {

    LOGI("[DEBUG] resetContext called");

    if (contextHandle == 0) {
        return;
    }

    InferenceContext* ctx = reinterpret_cast<InferenceContext*>(contextHandle);
    if (ctx->context == nullptr || ctx->sampler == nullptr) {
        LOGE("[DEBUG] context or sampler is null");
        return;
    }

    ctx->n_past = 0;
    ctx->n_remaining = ctx->n_ctx;
    ctx->stop_requested = false;
    ctx->cached_token_chars.clear();
    llama_memory_clear(llama_get_memory(ctx->context), false);
    common_sampler_reset(ctx->sampler);
    LOGI("[DEBUG] Context reset successfully");
}

extern "C" JNIEXPORT void JNICALL
Java_com_lokai_sdk_NativeLib_completeStream(
        JNIEnv* env,
        jobject thiz,
        jlong contextHandle,
        jstring prompt,
        jint max_tokens,
        jobject callback) {

    LOGI("[DEBUG] completeStream called with max_tokens=%d", max_tokens);

    if (contextHandle == 0) {
        LOGE("[DEBUG] contextHandle is 0");
        return;
    }

    InferenceContext* ctx = reinterpret_cast<InferenceContext*>(contextHandle);
    if (ctx->model == nullptr || ctx->context == nullptr || ctx->sampler == nullptr) {
        LOGE("[DEBUG] model=%p, context=%p, sampler=%p - one is null",
             ctx->model, ctx->context, ctx->sampler);
        return;
    }

    if (callback != nullptr) {
        ctx->callback_obj = env->NewGlobalRef(callback);
        jclass callback_class = env->GetObjectClass(callback);
        ctx->callback_method = env->GetMethodID(callback_class, "onToken", "(Ljava/lang/String;)V");
        env->DeleteLocalRef(callback_class);
    }

    const char* prompt_str = env->GetStringUTFChars(prompt, nullptr);
    if (prompt_str == nullptr) {
        LOGE("[DEBUG] prompt_str is null");
        return;
    }

    size_t prompt_len = strlen(prompt_str);
    std::string prompt_text(prompt_str, prompt_len);
    LOGI("[DEBUG] Prompt received (len=%zu): %s", prompt_len, prompt_str);

    ctx->n_remaining = max_tokens > 0 ? max_tokens : 128;
    ctx->n_past = 0;
    ctx->stop_requested = false;
    ctx->cached_token_chars.clear();
    llama_memory_clear(llama_get_memory(ctx->context), false);
    common_sampler_reset(ctx->sampler);

    std::string formatted_prompt = "<system>你是一个友好的AI助手，请用自然语言回答用户的问题。</system>\n<user>" + prompt_text + "</user>\n<assistant>";
    LOGI("[DEBUG] Formatted prompt: %s", formatted_prompt.c_str());

    std::vector<llama_token> prompt_tokens = common_tokenize(ctx->context, formatted_prompt, true, true);
    LOGI("[DEBUG] Tokenization complete: %d tokens generated", (int) prompt_tokens.size());

    const int max_batch_size = ctx->n_ctx - OVERFLOW_HEADROOM;
    if ((int) prompt_tokens.size() > max_batch_size) {
        const int skipped_tokens = (int) prompt_tokens.size() - max_batch_size;
        prompt_tokens.resize(max_batch_size);
        LOGW("[DEBUG] Prompt too long! Skipped %d tokens!", skipped_tokens);
    }

    if (decode_tokens_in_batches(ctx, prompt_tokens, ctx->n_past, true)) {
        LOGE("[DEBUG] Failed to decode prompt tokens!");
        env->ReleaseStringUTFChars(prompt, prompt_str);
        return;
    }
    ctx->n_past += (int) prompt_tokens.size();
    LOGI("[DEBUG] Prompt decoded successfully, n_past=%d", ctx->n_past);

    const int max_generate = ctx->n_remaining > 0 ? ctx->n_remaining : 128;
    LOGI("[DEBUG] Starting streaming generation loop, max_generate=%d", max_generate);
    int generated_tokens = 0;

    while (generated_tokens < max_generate) {
        if (ctx->stop_requested) {
            LOGI("[DEBUG] Streaming generation stopped by user request");
            break;
        }
        if (ctx->n_past >= ctx->n_ctx - OVERFLOW_HEADROOM) {
            LOGW("[DEBUG] Context window full at n_past=%d, n_ctx=%d", ctx->n_past, ctx->n_ctx);
            break;
        }

        llama_token new_token = common_sampler_sample(ctx->sampler, ctx->context, -1);
        common_sampler_accept(ctx->sampler, new_token, true);

        const llama_vocab* vocab = llama_model_get_vocab(ctx->model);
        if (llama_vocab_is_eog(vocab, new_token)) {
            LOGI("[DEBUG] End of streaming generation reached after %d tokens", generated_tokens);
            break;
        }

        std::string token_text = common_token_to_piece(ctx->context, new_token);
        ctx->cached_token_chars += token_text;

        if (is_valid_utf8(ctx->cached_token_chars.c_str())) {
            if (ctx->callback_obj != nullptr && ctx->callback_method != nullptr) {
                jstring jtoken = env->NewStringUTF(ctx->cached_token_chars.c_str());
                env->CallVoidMethod(ctx->callback_obj, ctx->callback_method, jtoken);
                env->DeleteLocalRef(jtoken);
            }
            ctx->cached_token_chars.clear();
        }

        common_batch_clear(ctx->batch);
        common_batch_add(ctx->batch, new_token, ctx->n_past, {0}, true);
        if (llama_decode(ctx->context, ctx->batch) != 0) {
            LOGE("[DEBUG] Failed to decode sampled token!");
            break;
        }
        ctx->n_past += 1;

        generated_tokens++;
        ctx->n_remaining--;
    }

    if (!ctx->cached_token_chars.empty() && is_valid_utf8(ctx->cached_token_chars.c_str())) {
        if (ctx->callback_obj != nullptr && ctx->callback_method != nullptr) {
            jstring jtoken = env->NewStringUTF(ctx->cached_token_chars.c_str());
            env->CallVoidMethod(ctx->callback_obj, ctx->callback_method, jtoken);
            env->DeleteLocalRef(jtoken);
        }
    }

    if (ctx->callback_obj != nullptr) {
        env->DeleteGlobalRef(ctx->callback_obj);
        ctx->callback_obj = nullptr;
        ctx->callback_method = nullptr;
    }

    LOGI("[DEBUG] Streaming generation completed, generated %d tokens", generated_tokens);

    env->ReleaseStringUTFChars(prompt, prompt_str);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_lokai_sdk_NativeLib_getBosToken(
        JNIEnv* env,
        jobject thiz,
        jlong contextHandle) {

    if (contextHandle == 0) {
        return 1;
    }

    InferenceContext* ctx = reinterpret_cast<InferenceContext*>(contextHandle);
    if (ctx->model == nullptr) {
        return 1;
    }

    const llama_vocab* vocab = llama_model_get_vocab(ctx->model);
    return llama_vocab_bos(vocab);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_lokai_sdk_NativeLib_getEosToken(
        JNIEnv* env,
        jobject thiz,
        jlong contextHandle) {

    if (contextHandle == 0) {
        return 2;
    }

    InferenceContext* ctx = reinterpret_cast<InferenceContext*>(contextHandle);
    if (ctx->model == nullptr) {
        return 2;
    }

    const llama_vocab* vocab = llama_model_get_vocab(ctx->model);
    return llama_vocab_eos(vocab);
}
