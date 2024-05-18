#include <jni.h>
#include <string>
#include "include/opus/opus.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_ptt_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    const char *version = opus_get_version_string();

    return env->NewStringUTF(hello.c_str());
}