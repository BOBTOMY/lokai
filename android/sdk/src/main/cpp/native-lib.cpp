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
    params.n_ctx = 2048;
    params.n_threads = 4;
    
    llama_model* model = llama_load_model_from_file(path, params);
    
    env->ReleaseStringUTFChars(modelPath, path);
    
    return (jlong) model;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_lokai_sdk_LokAIEngine_nativeComplete(
        JNIEnv* env,
        jobject thiz,
        jlong modelHandle,
        jstring prompt,
        jobject params) {
    llama_model* model = (llama_model*) modelHandle;
    
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    
    // TODO: Implement inference
    std::string result = std::string(promptStr) + " (generated)";
    
    env->ReleaseStringUTFChars(prompt, promptStr);
    
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_lokai_sdk_LokAIEngine_nativeRelease(
        JNIEnv* env,
        jobject thiz,
        jlong modelHandle) {
    llama_model* model = (llama_model*) modelHandle;
    if (model != nullptr) {
        llama_free_model(model);
    }
}
