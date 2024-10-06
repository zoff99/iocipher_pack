/*
 * Copyright (C) 2010 The Android Open Source Project
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

#ifndef SCOPED_BYTES_H_included
#define SCOPED_BYTES_H_included

#include <jni.h>

typedef struct {
    JNIEnv* env;
    jobject object;
    jbyteArray byteArray;
    jbyte* ptr;
} ScopedBytes;

// Function to create a ScopedBytes instance
void ScopedBytes_init(ScopedBytes* scopedBytes, JNIEnv* env, jobject object) {
    scopedBytes->env = env;
    scopedBytes->object = object;
    scopedBytes->byteArray = NULL;
    scopedBytes->ptr = NULL;

    jclass clse = (*env)->FindClass(env, "[B");

    if (scopedBytes->object == NULL) {
        // jniThrowRuntimeException equivalent function needs to be implemented
        jniThrowRuntimeException(env, "null pointer");
    } else if ((*env)->IsInstanceOf(env, scopedBytes->object, clse)) {
        scopedBytes->byteArray = (jbyteArray)scopedBytes->object;
        scopedBytes->ptr = (*env)->GetByteArrayElements(env, scopedBytes->byteArray, NULL);
    } else {
        scopedBytes->ptr = (jbyte*)(*env)->GetDirectBufferAddress(env, scopedBytes->object);
    }
}

// Function to release resources in ScopedBytes
void ScopedBytes_release(ScopedBytes* scopedBytes, int readOnly) {
    if (scopedBytes->byteArray != NULL) {
        (*scopedBytes->env)->ReleaseByteArrayElements(scopedBytes->env, scopedBytes->byteArray, scopedBytes->ptr, readOnly ? JNI_ABORT : 0);
    }
}

// ScopedBytesRO equivalent
typedef struct {
    ScopedBytes base;
} ScopedBytesRO;

void ScopedBytesRO_init(ScopedBytesRO* scopedBytesRO, JNIEnv* env, jobject object) {
    ScopedBytes_init(&scopedBytesRO->base, env, object);
}

const jbyte* ScopedBytesRO_get(const ScopedBytesRO* scopedBytesRO) {
    return scopedBytesRO->base.ptr;
}

// ScopedBytesRW equivalent
typedef struct {
    ScopedBytes base;
} ScopedBytesRW;

void ScopedBytesRW_init(ScopedBytesRW* scopedBytesRW, JNIEnv* env, jobject object) {
    ScopedBytes_init(&scopedBytesRW->base, env, object);
}

jbyte* ScopedBytesRW_get(ScopedBytesRW* scopedBytesRW) {
    return scopedBytesRW->base.ptr;
}

#endif  // SCOPED_BYTES_H_included
