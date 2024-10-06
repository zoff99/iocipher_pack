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

#ifndef SCOPED_LOCAL_REF_H_included
#define SCOPED_LOCAL_REF_H_included

#include "JNIHelp.h"

typedef struct {
    JNIEnv* env;
    jobject localRef;
} ScopedLocalRef;

void ScopedLocalRef_init(ScopedLocalRef* self, JNIEnv* env, jobject localRef) {
    self->env = env;
    self->localRef = localRef;
}

void ScopedLocalRef_reset(ScopedLocalRef* self) {
    if (self->localRef != NULL) {
        (*self->env)->DeleteLocalRef(self->env, self->localRef);
        self->localRef = NULL;
    }
}

jobject ScopedLocalRef_get(ScopedLocalRef* self) {
    return self->localRef;
}

#endif  // SCOPED_LOCAL_REF_H_included

