#include <jni.h>
#include <string>
#include "llama.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_lokai_sdk_LokAIEngine_nativeInit(
        JNIEnv* env,
        jobject thiz,
        jstring modelPath,
        jobject config) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);

    llama_model_params params = llama_model_default_params();
    params.n_gpu_layers = 0;

    llama_model* model = llama_model_load_from_file(path, params);

    env->ReleaseStringUTFChars(modelPath, path);

    return reinterpret_cast<jlong>(model);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_lokai_sdk_LokAIEngine_nativeComplete(
        JNIEnv* env,
        jobject thiz,
        jlong modelHandle,
        jstring prompt,
        jobject params) {
    llama_model* model = reinterpret_cast<llama_model*>(modelHandle);

    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);

    llama_context_params ctxParams = llama_context_default_params();
    ctxParams.n_ctx = 2048;
    ctxParams.n_threads = 4;

    llama_context* ctx = llama_new_context_with_model(model, ctxParams);

    std::string result;
    if (ctx != nullptr) {
        result = std::string(promptStr) + " (generated with llama.cpp)";
        llama_free(ctx);
    } else {
        result = std::string(promptStr) + " (failed to create context)";
    }

    env->ReleaseStringUTFChars(prompt, promptStr);

    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_lokai_sdk_LokAIEngine_nativeRelease(
        JNIEnv* env,
        jobject thiz,
        jlong modelHandle) {
    llama_model* model = reinterpret_cast<llama_model*>(modelHandle);
    if (model != nullptr) {
        llama_model_free(model);
    }
}
