/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define _GNU_SOURCE
#define LOG_TAG "JNIHelp"

#include "JNIHelp.h"

#ifdef __ANDROID__
#include <android/log.h>
#endif

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

/**
 * Equivalent to ScopedLocalRef, but for C_JNIEnv instead. (And slightly more powerful.)
 */
struct scoped_local_ref {
    JNIEnv* mEnv;
    void* mLocalRef;
};

static void scoped_local_ref_init(struct scoped_local_ref* ref, JNIEnv* env, void* localRef) {
    ref->mEnv = env;
    ref->mLocalRef = localRef;
}

static void scoped_local_ref_reset(struct scoped_local_ref* ref, void* localRef) {
    if (ref->mLocalRef != NULL) {
        (*ref->mEnv)->DeleteLocalRef(ref->mEnv, ref->mLocalRef);
        ref->mLocalRef = localRef;
    }
}

static void* scoped_local_ref_get(struct scoped_local_ref* ref) {
    return ref->mLocalRef;
}

static void scoped_local_ref_destroy(struct scoped_local_ref* ref) {
    scoped_local_ref_reset(ref, NULL);
}

static jclass findClass(JNIEnv* env, const char* className) {
    JNIEnv* e = env;
    return (*env)->FindClass(e, className);
}

/*
 * Returns a human-readable summary of an exception object.  The buffer will
 * be populated with the "binary" class name and, if present, the
 * exception message.
 */
static char* getExceptionSummary0(JNIEnv* env, jthrowable exception) {
    JNIEnv* e = env;

    /* get the name of the exception's class */
    struct scoped_local_ref exceptionClass;
    scoped_local_ref_init(&exceptionClass, env, (*env)->GetObjectClass(e, exception)); // can't fail
    struct scoped_local_ref classClass;
    scoped_local_ref_init(&classClass, env, (*env)->GetObjectClass(e, scoped_local_ref_get(&exceptionClass))); // java.lang.Class, can't fail
    jmethodID classGetNameMethod =
            (*env)->GetMethodID(e, scoped_local_ref_get(&classClass), "getName", "()Ljava/lang/String;");
    struct scoped_local_ref classNameStr;
    scoped_local_ref_init(&classNameStr, env, (jstring) (*env)->CallObjectMethod(e, scoped_local_ref_get(&exceptionClass), classGetNameMethod));
    if (scoped_local_ref_get(&classNameStr) == NULL) {
        return NULL;
    }

    /* get printable string */
    const char* classNameChars = (*env)->GetStringUTFChars(e, scoped_local_ref_get(&classNameStr), NULL);
    if (classNameChars == NULL) {
        return NULL;
    }

    /* if the exception has a detail message, get that */
    jmethodID getMessage =
            (*env)->GetMethodID(e, scoped_local_ref_get(&exceptionClass), "getMessage", "()Ljava/lang/String;");
    struct scoped_local_ref messageStr;
    scoped_local_ref_init(&messageStr, env, (jstring) (*env)->CallObjectMethod(e, exception, getMessage));
    if (scoped_local_ref_get(&messageStr) == NULL) {
        return strdup(classNameChars);
    }

    char* result = NULL;
    const char* messageChars = (*env)->GetStringUTFChars(e, scoped_local_ref_get(&messageStr), NULL);
    if (messageChars != NULL) {
        int res_ = asprintf(&result, "%s: %s", classNameChars, messageChars);
        (res_);
        (*env)->ReleaseStringUTFChars(e, scoped_local_ref_get(&messageStr), messageChars);
    } else {
        (*env)->ExceptionClear(e); // clear OOM
        int res_ = asprintf(&result, "%s: <error getting message>", classNameChars);
        (res_);
    }

    (*env)->ReleaseStringUTFChars(e, scoped_local_ref_get(&classNameStr), classNameChars);
    return result;
}

static char* getExceptionSummary(JNIEnv* env, jthrowable exception) {
    JNIEnv* e = env;
    char* result = getExceptionSummary0(env, exception);
    if (result == NULL) {
        (*env)->ExceptionClear(e);
        result = strdup("<error getting class name>");
    }
    return result;
}

/*
 * Returns an exception (with stack trace) as a string.
 */
static char* getStackTrace(JNIEnv* env, jthrowable exception) {
    JNIEnv* e = env;

    struct scoped_local_ref stringWriterClass;
    scoped_local_ref_init(&stringWriterClass, env, findClass(env, "java/io/StringWriter"));
    if (scoped_local_ref_get(&stringWriterClass) == NULL) {
        return NULL;
    }

    jmethodID stringWriterCtor = (*env)->GetMethodID(e, scoped_local_ref_get(&stringWriterClass), "<init>", "()V");
    jmethodID stringWriterToStringMethod =
            (*env)->GetMethodID(e, scoped_local_ref_get(&stringWriterClass), "toString", "()Ljava/lang/String;");

    struct scoped_local_ref printWriterClass;
    scoped_local_ref_init(&printWriterClass, env, findClass(env, "java/io/PrintWriter"));
    if (scoped_local_ref_get(&printWriterClass) == NULL) {
        return NULL;
    }

    jmethodID printWriterCtor =
            (*env)->GetMethodID(e, scoped_local_ref_get(&printWriterClass), "<init>", "(Ljava/io/Writer;)V");

    struct scoped_local_ref stringWriter;
    scoped_local_ref_init(&stringWriter, env, (*env)->NewObject(e, scoped_local_ref_get(&stringWriterClass), stringWriterCtor));
    if (scoped_local_ref_get(&stringWriter) == NULL) {
        return NULL;
    }

    jobject printWriter =
            (*env)->NewObject(e, scoped_local_ref_get(&printWriterClass), printWriterCtor, scoped_local_ref_get(&stringWriter));
    if (printWriter == NULL) {
        return NULL;
    }

    struct scoped_local_ref exceptionClass;
    scoped_local_ref_init(&exceptionClass, env, (*env)->GetObjectClass(e, exception)); // can't fail
    jmethodID printStackTraceMethod =
            (*env)->GetMethodID(e, scoped_local_ref_get(&exceptionClass), "printStackTrace", "(Ljava/io/PrintWriter;)V");
    (*env)->CallVoidMethod(e, exception, printStackTraceMethod, printWriter);

    if ((*env)->ExceptionCheck(e)) {
        return NULL;
    }

    struct scoped_local_ref messageStr;
    scoped_local_ref_init(&messageStr, env, (jstring) (*env)->CallObjectMethod(e, scoped_local_ref_get(&stringWriter), stringWriterToStringMethod));
    if (scoped_local_ref_get(&messageStr) == NULL) {
        return NULL;
    }

    const char* utfChars = (*env)->GetStringUTFChars(e, scoped_local_ref_get(&messageStr), NULL);
    if (utfChars == NULL) {
        return NULL;
    }

    char* result = strdup(utfChars);
    (*env)->ReleaseStringUTFChars(e, scoped_local_ref_get(&messageStr), utfChars);
    return result;
}

int jniThrowException(JNIEnv* env, const char* className, const char* msg) {
    JNIEnv* e = env;

    if ((*env)->ExceptionCheck(e)) {
        /* TODO: consider creating the new exception with this as "cause" */
        struct scoped_local_ref exception;
        scoped_local_ref_init(&exception, env, (*env)->ExceptionOccurred(e));
        (*env)->ExceptionClear(e);

        if (scoped_local_ref_get(&exception) != NULL) {
            char* text = getExceptionSummary(env, scoped_local_ref_get(&exception));
            LOGW("Discarding pending exception (%s) to throw %s", text, className);
            free(text);
        }
    }

    struct scoped_local_ref exceptionClass;
    scoped_local_ref_init(&exceptionClass, env, findClass(env, className));
    if (scoped_local_ref_get(&exceptionClass) == NULL) {
        LOGE("Unable to find exception class %s", className);
        /* ClassNotFoundException now pending */
        return -1;
    }

    if ((*env)->ThrowNew(e, scoped_local_ref_get(&exceptionClass), msg) != JNI_OK) {
        LOGE("Failed throwing '%s' '%s'", className, msg);
        /* an exception, most likely OOM, will now be pending */
        return -1;
    }

    return 0;
}

int jniThrowExceptionFmt(JNIEnv* env, const char* className, const char* fmt, va_list args) {
    char msgBuf[512];
    vsnprintf(msgBuf, sizeof(msgBuf), fmt, args);
    return jniThrowException(env, className, msgBuf);
}

int jniThrowNullPointerException(JNIEnv* env, const char* msg) {
    return jniThrowException(env, "java/lang/NullPointerException", msg);
}

int jniThrowRuntimeException(JNIEnv* env, const char* msg) {
    return jniThrowException(env, "java/lang/RuntimeException", msg);
}

int jniThrowIOException(JNIEnv* env, int errnum) {
    char buffer[80];
    const char* message = jniStrError(errnum, buffer, sizeof(buffer));
    return jniThrowException(env, "java/io/IOException", message);
}

void jniLogException(JNIEnv* env, int priority, const char* tag, jthrowable exception) {
    JNIEnv* e = env;

    struct scoped_local_ref currentException;
    scoped_local_ref_init(&currentException, env, NULL);
    if (exception == NULL) {
        exception = (*env)->ExceptionOccurred(e);
        if (exception == NULL) {
            return;
        }

        (*env)->ExceptionClear(e);
        // TODO: // currentException = exception;
    }

    char* buffer = getStackTrace(env, exception);
    if (buffer == NULL) {
        (*env)->ExceptionClear(e);
        buffer = getExceptionSummary(env, exception);
    }

#ifdef __ANDROID__
    __android_log_write(priority, tag, buffer);
#endif
    free(buffer);

    if (scoped_local_ref_get(&currentException) != NULL) {
        (*env)->Throw(e, exception); // rethrow
    }
}

const char* jniStrError(int errnum, char* buf, size_t buflen) {
#if __GLIBC__
    // Note: glibc has a nonstandard strerror_r that returns char* rather than POSIX's int.
    // char *strerror_r(int errnum, char *buf, size_t n);
    return strerror_r(errnum, buf, buflen);
#else
    int rc = strerror_r(errnum, buf, buflen);
    if (rc != 0) {
        snprintf(buf, buflen, "errno %d", errnum);
    }
    return buf;
#endif
}

struct CachedFields {
    jclass fileDescriptorClass;
    jmethodID fileDescriptorCtor;
    jfieldID pathField;
    jfieldID invalidField;
} gCachedFields;

int registerJniHelp(JNIEnv* env) {
    LOGI("registerJniHelp:enter");
    gCachedFields.fileDescriptorClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "info/guardianproject/iocipher/FileDescriptor"));
    if (gCachedFields.fileDescriptorClass == NULL) {
        return -1;
    }

    gCachedFields.fileDescriptorCtor =
            (*env)->GetMethodID(env, gCachedFields.fileDescriptorClass, "<init>", "()V");
    if (gCachedFields.fileDescriptorCtor == NULL) {
        return -1;
    }

    gCachedFields.pathField =
        (*env)->GetFieldID(env, gCachedFields.fileDescriptorClass,
                        "path", "Ljava/lang/String;");
    if (gCachedFields.pathField == NULL) {
        return -1;
    }

    gCachedFields.invalidField =
        (*env)->GetFieldID(env, gCachedFields.fileDescriptorClass,
                        "invalid", "Ljava/lang/String;");
    if (gCachedFields.invalidField == NULL) {
        return -1;
    }

    return 0;
}

/* in sqlfs, the full path is used as the file descriptor */
jobject jniCreateFileDescriptor(JNIEnv* env, jstring javaPath) {
    JNIEnv* e = env;
    jobject fileDescriptor = (*env)->NewObject(e,
            gCachedFields.fileDescriptorClass, gCachedFields.fileDescriptorCtor);
    jniSetFileDescriptorWithPath(env, fileDescriptor, javaPath);
    return fileDescriptor;
}

jstring jniGetPathFromFileDescriptor(JNIEnv* env, jobject fileDescriptor) {
    JNIEnv* e = env;
    return (jstring)(*env)->GetObjectField(e, fileDescriptor, gCachedFields.pathField);
}

void jniSetFileDescriptorWithPath(JNIEnv* env, jobject fileDescriptor, jstring javaPath) {
    JNIEnv* e = env;
    (*env)->SetObjectField(e, fileDescriptor, gCachedFields.pathField, javaPath);
}

void jniSetFileDescriptorInvalid(JNIEnv* env, jobject fileDescriptor) {
    JNIEnv* e = env;
    jstring javaInvalid = (jstring)(*env)->GetObjectField(e, fileDescriptor, gCachedFields.invalidField);
    (*env)->SetObjectField(e, fileDescriptor, gCachedFields.pathField, javaInvalid);
}

