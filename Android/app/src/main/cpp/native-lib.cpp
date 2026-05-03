#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "llama.h"
#include "common.h"
#include "sampling.h"

#define TAG "LLAMA_ANDROID"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static llama_model * model = nullptr;
static llama_context * ctx = nullptr;

extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGI("JNI_OnLoad started");
    llama_backend_init();
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_holoscend_chat_MainActivity_initLlama(JNIEnv *env, jobject thiz, jstring model_path) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);

    LOGI("Loading model from %s", path);

    if (ctx) {
        llama_free(ctx);
        ctx = nullptr;
    }
    if (model) {
        llama_model_free(model);
        model = nullptr;
    }

    auto mparams = llama_model_default_params();
    model = llama_model_load_from_file(path, mparams);

    if (model == nullptr) {
        LOGE("Failed to load model from %s", path);
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_FALSE;
    }

    auto cparams = llama_context_default_params();
    cparams.n_ctx = 2048;
    cparams.n_batch = 512;
    cparams.n_threads = 4; // Use 4 threads for mobile

    ctx = llama_init_from_model(model, cparams);
    if (ctx == nullptr) {
        LOGE("Failed to create context");
        llama_model_free(model);
        model = nullptr;
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_FALSE;
    }

    LOGI("Model loaded successfully");
    env->ReleaseStringUTFChars(model_path, path);
    return JNI_TRUE;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_holoscend_chat_MainActivity_completion(JNIEnv *env, jobject thiz, jstring prompt) {
    if (ctx == nullptr) return env->NewStringUTF("Model not initialized");

    const char *p = env->GetStringUTFChars(prompt, nullptr);
    std::string prompt_str(p);
    env->ReleaseStringUTFChars(prompt, p);

    const auto * vocab = llama_model_get_vocab(model);
    const bool add_bos = llama_vocab_get_add_bos(vocab);
    std::vector<llama_token> tokens = common_tokenize(vocab, prompt_str, add_bos, true);

    const int n_ctx = llama_n_ctx(ctx);
    const int n_kv_req = tokens.size() + 128; // Request 128 tokens for response

    if (n_kv_req > n_ctx) {
        return env->NewStringUTF("Prompt too long");
    }

    // Clear KV cache
    llama_memory_clear(llama_get_memory(ctx), true);

    // Evaluate prompt
    for (size_t i = 0; i < tokens.size(); i += 512) {
        int n_eval = tokens.size() - i;
        if (n_eval > 512) n_eval = 512;
        if (llama_decode(ctx, llama_batch_get_one(&tokens[i], n_eval))) {
            return env->NewStringUTF("Failed to eval");
        }
    }

    std::string response = "";
    llama_token curr_token;

    // Set up sampling
    common_params_sampling sparams;
    auto sampler = common_sampler_init(model, sparams);

    for (int i = 0; i < 128; i++) {
        curr_token = common_sampler_sample(sampler, ctx, -1);
        common_sampler_accept(sampler, curr_token, true);

        if (llama_vocab_is_eog(vocab, curr_token)) break;

        char buf[128];
        int n = llama_token_to_piece(vocab, curr_token, buf, sizeof(buf), 0, true);
        if (n < 0) break;
        response += std::string(buf, n);

        if (llama_decode(ctx, llama_batch_get_one(&curr_token, 1))) {
            break;
        }
    }

    common_sampler_free(sampler);

    return env->NewStringUTF(response.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_holoscend_chat_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {

    std::string msg = "LLAMA JNI READY";
    return env->NewStringUTF(msg.c_str());
}
