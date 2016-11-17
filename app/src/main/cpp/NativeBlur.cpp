//
// Created by prife on 16-11-17.
//
#include <jni.h>
#include <android/log.h>
#include <string>

#define TAG "NativeBlur"
#define JAVA_CLASS "com/example/prife/tryopengl/NativeBlurActivity"
#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,  TAG, __VA_ARGS__)
#define LOGDT(T, ...) __android_log_print(ANDROID_LOG_DEBUG,  T, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)


static JavaVM *g_vm;
static jclass g_jclass;

jstring helloString(JNIEnv *env, jclass jclazz, jstring str) {
    const char *strFromJava = env->GetStringUTFChars(str, NULL);
    std::string hello = std::string("Hello, from C++: ") + strFromJava;
    return env->NewStringUTF(hello.c_str());
}

static JNINativeMethod gMethods[] = {
    {"nativeHelloString", "(Ljava/lang/String;)Ljava/lang/String;",  (void *)helloString},
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    jclass javaClass = env->FindClass(JAVA_CLASS);
    if (javaClass == NULL) {
        LOGE("Ops: Unable to find hook class.");
        return JNI_ERR;
    }
    if (env->RegisterNatives(javaClass, gMethods, NELEM(gMethods)) < 0) {
        LOGE("Ops: Unable to register the native methods.");
        return JNI_ERR;
    }
    g_vm = vm;
    g_jclass = (jclass) env->NewGlobalRef(javaClass);
    env->DeleteLocalRef(javaClass);
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return;
    }
    env->DeleteGlobalRef((jobject)g_vm);
    env->DeleteGlobalRef((jobject)g_jclass);
}