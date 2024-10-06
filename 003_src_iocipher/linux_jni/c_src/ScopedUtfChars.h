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

#ifndef SCOPED_UTF_CHARS_H_included
#define SCOPED_UTF_CHARS_H_included

#include "JNIHelp.h"
#include <string.h>
#include <stdlib.h>

// A smart pointer that provides read-only access to a Java string's UTF chars.
// Unlike GetStringUTFChars, we throw NullPointerException rather than abort if
// passed a null jstring, and c_str will return NULL.
// This makes the correct idiom very simple:
//
//   ScopedUtfChars name(env, javaName);
//   if (name.c_str() == NULL) {
//       return NULL;
//   }
typedef struct {
    JNIEnv* mEnv;
    jstring mString;
    const char* mUtfChars;
} ScopedUtfChars;

void ScopedUtfChars_init(ScopedUtfChars* self, JNIEnv* env, jstring s) {
    self->mEnv = env;
    self->mString = s;
    if (s == NULL) {
        self->mUtfChars = NULL;
        jniThrowRuntimeException(env, "null pointer");
    } else {
        self->mUtfChars = (*env)->GetStringUTFChars(env, s, NULL);
    }
}

void ScopedUtfChars_destroy(ScopedUtfChars* self) {
    if (self->mUtfChars) {
        (*(self->mEnv))->ReleaseStringUTFChars(self->mEnv, self->mString, self->mUtfChars);
    }
}

const char* ScopedUtfChars_c_str(const ScopedUtfChars* self) {
    return self->mUtfChars;
}

size_t ScopedUtfChars_size(const ScopedUtfChars* self) {
    return strlen(self->mUtfChars);
}

// Element access.
const char ScopedUtfChars_operator_index(const ScopedUtfChars* self, size_t n) {
    return self->mUtfChars[n];
}


#endif  // SCOPED_UTF_CHARS_H_included
