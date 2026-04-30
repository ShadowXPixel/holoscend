#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_holoscend_chat_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {

    std::string msg = "JNI WORKS 😈";
    return env->NewStringUTF(msg.c_str());
}
