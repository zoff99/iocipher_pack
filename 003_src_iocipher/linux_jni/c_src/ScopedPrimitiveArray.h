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

#ifndef SCOPED_PRIMITIVE_ARRAY_H_included
#define SCOPED_PRIMITIVE_ARRAY_H_included

#include "JNIHelp.h"

// ScopedBooleanArrayRO, ScopedByteArrayRO, ScopedCharArrayRO, ScopedDoubleArrayRO,
// ScopedFloatArrayRO, ScopedIntArrayRO, ScopedLongArrayRO, and ScopedShortArrayRO provide
// convenient read-only access to Java arrays from JNI code. This is cheaper than read-write
// access and should be used by default.
#include <jni.h>
#include <stddef.h>

#define INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RO(PRIMITIVE_TYPE, NAME) \
    typedef struct { \
        JNIEnv* mEnv; \
        PRIMITIVE_TYPE ## Array mJavaArray; \
        PRIMITIVE_TYPE* mRawArray; \
    } Scoped ## NAME ## ArrayRO; \
    \
    void Scoped ## NAME ## ArrayRO_init(Scoped ## NAME ## ArrayRO* self, JNIEnv* env, PRIMITIVE_TYPE ## Array javaArray) { \
        self->mEnv = env; \
        self->mJavaArray = javaArray; \
        self->mRawArray = NULL; \
        if (self->mJavaArray == NULL) { \
            jniThrowRuntimeException(self->mEnv, "null pointer"); \
        } else { \
            self->mRawArray = (*self->mEnv)->Get ## NAME ## ArrayElements(self->mEnv, self->mJavaArray, NULL); \
        } \
    } \
    \
    void Scoped ## NAME ## ArrayRO_release(Scoped ## NAME ## ArrayRO* self) { \
        if (self->mRawArray) { \
            (*self->mEnv)->Release ## NAME ## ArrayElements(self->mEnv, self->mJavaArray, self->mRawArray, JNI_ABORT); \
        } \
    } \
    \
    const PRIMITIVE_TYPE* Scoped ## NAME ## ArrayRO_get(const Scoped ## NAME ## ArrayRO* self) { \
        return self->mRawArray; \
    } \
    \
    const PRIMITIVE_TYPE Scoped ## NAME ## ArrayRO_operator_index(const Scoped ## NAME ## ArrayRO* self, size_t n) { \
        return self->mRawArray[n]; \
    } \
    \
    size_t Scoped ## NAME ## ArrayRO_size(const Scoped ## NAME ## ArrayRO* self) { \
        return (*self->mEnv)->GetArrayLength(self->mEnv, self->mJavaArray); \
    }

INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RO(jboolean, Boolean);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RO(jbyte, Byte);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RO(jchar, Char);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RO(jdouble, Double);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RO(jfloat, Float);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RO(jint, Int);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RO(jlong, Long);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RO(jshort, Short);

#undef INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RO

// ScopedBooleanArrayRW, ScopedByteArrayRW, ScopedCharArrayRW, ScopedDoubleArrayRW,
// ScopedFloatArrayRW, ScopedIntArrayRW, ScopedLongArrayRW, and ScopedShortArrayRW provide
// convenient read-write access to Java arrays from JNI code. These are more expensive,
// since they entail a copy back onto the Java heap, and should only be used when necessary.
#define INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RW(PRIMITIVE_TYPE, NAME) \
    typedef struct { \
        JNIEnv* mEnv; \
        PRIMITIVE_TYPE ## Array mJavaArray; \
        PRIMITIVE_TYPE* mRawArray; \
    } Scoped ## NAME ## ArrayRW; \
    \
    void Scoped ## NAME ## ArrayRW_init(Scoped ## NAME ## ArrayRW* self, JNIEnv* env, PRIMITIVE_TYPE ## Array javaArray) { \
        self->mEnv = env; \
        self->mJavaArray = javaArray; \
        self->mRawArray = NULL; \
        if (self->mJavaArray == NULL) { \
            jniThrowRuntimeException(env, "null pointer"); \
        } else { \
            self->mRawArray = (*env)->Get ## NAME ## ArrayElements(env, javaArray, NULL); \
        } \
    } \
    \
    void Scoped ## NAME ## ArrayRW_destroy(Scoped ## NAME ## ArrayRW* self) { \
        if (self->mRawArray) { \
            (*self->mEnv)->Release ## NAME ## ArrayElements(self->mEnv, self->mJavaArray, self->mRawArray, 0); \
        } \
    } \
    \
    const PRIMITIVE_TYPE* Scoped ## NAME ## ArrayRW_get_const(const Scoped ## NAME ## ArrayRW* self) { return self->mRawArray; } \
    const PRIMITIVE_TYPE Scoped ## NAME ## ArrayRW_operator_index_const(const Scoped ## NAME ## ArrayRW* self, size_t n) { return self->mRawArray[n]; } \
    PRIMITIVE_TYPE* Scoped ## NAME ## ArrayRW_get(Scoped ## NAME ## ArrayRW* self) { return self->mRawArray; } \
    PRIMITIVE_TYPE Scoped ## NAME ## ArrayRW_operator_index(Scoped ## NAME ## ArrayRW* self, size_t n) { return self->mRawArray[n]; } \
    size_t Scoped ## NAME ## ArrayRW_size(const Scoped ## NAME ## ArrayRW* self) { return (*self->mEnv)->GetArrayLength(self->mEnv, self->mJavaArray); }

INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RW(jboolean, Boolean);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RW(jbyte, Byte);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RW(jchar, Char);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RW(jdouble, Double);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RW(jfloat, Float);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RW(jint, Int);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RW(jlong, Long);
INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RW(jshort, Short);

#undef INSTANTIATE_SCOPED_PRIMITIVE_ARRAY_RW



#endif  // SCOPED_PRIMITIVE_ARRAY_H_included
