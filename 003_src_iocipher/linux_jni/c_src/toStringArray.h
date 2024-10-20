
#include "JniConstants.h"
#include <jni.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>

jobjectArray toStringArray(JNIEnv* env, size_t (*counter)(), const char* (*getter)(size_t)) {
    size_t count = counter();
    jobjectArray result = (*env)->NewObjectArray(env, count, (*env)->FindClass(env, "java/lang/String"), NULL);
    if (result == NULL) {
        return NULL;
    }
    for (size_t i = 0; i < count; ++i) {
        const char* str = getter(i);
        jstring s = (*env)->NewStringUTF(env, str);
        if (s == NULL || (*env)->ExceptionCheck(env)) {
            return NULL;
        }
        (*env)->SetObjectArrayElement(env, result, i, s);
        if ((*env)->ExceptionCheck(env)) {
            return NULL;
        }
    }
    return result;
}

jobjectArray toStringArray16(JNIEnv* env, size_t (*counter)(), const jchar* (*getter)(int32_t*)) {
    size_t count = counter();
    jobjectArray result = (*env)->NewObjectArray(env, count, (*env)->FindClass(env, "java/lang/String"), NULL);
    if (result == NULL) {
        return NULL;
    }
    for (size_t i = 0; i < count; ++i) {
        int32_t charCount;
        const jchar* chars = getter(&charCount);
        jstring s = (*env)->NewString(env, chars, charCount);
        if (s == NULL || (*env)->ExceptionCheck(env)) {
            return NULL;
        }
        (*env)->SetObjectArrayElement(env, result, i, s);
        if ((*env)->ExceptionCheck(env)) {
            return NULL;
        }
    }
    return result;
}

