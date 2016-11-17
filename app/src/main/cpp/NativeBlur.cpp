//
// Created by prife on 16-11-17.
//
#define LOG_TAG "NativeBlur"

#include <jni.h>
#include <string>
#include "helper.h"
#include "log.h"
#include "Blur.h"

static JavaVM *g_vm;
static jclass g_jclass;
static LayerBlur* g_blur;

jstring helloString(JNIEnv *env, jclass jclazz, jstring str) {
    const char *strFromJava = env->GetStringUTFChars(str, NULL);
    std::string hello = std::string("Hello, from C++: ") + strFromJava;
    return env->NewStringUTF(hello.c_str());
}

int blurTexture(JNIEnv *env, jclass jclazz,
        jint level, jint inId, jint inWidth, jint inheight, jint outId) {
    size_t outWidth = 0;
    size_t outHeight = 0;
    if (g_blur == NULL) {
        g_blur = new LayerBlur(inWidth, inheight);
    }
    g_blur->blurTexture(level, inId, inWidth, inheight, outId, &outWidth, &outHeight);
    return 0;
}

static JNINativeMethod gMethods[] = {
    {"nativeHelloString", "(Ljava/lang/String;)Ljava/lang/String;",  (void *)helloString},
    {"nativeBlurTexture", "(IIIII)I",  (void *)blurTexture},
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
