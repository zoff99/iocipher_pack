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

#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define INITIAL_BUF_SIZE 512

bool readlink_wrapper(const char* path, char** result) {
    size_t bufSize = INITIAL_BUF_SIZE;
    char* buf = NULL;
    ssize_t len;

    while (true) {
        buf = realloc(buf, bufSize);
        if (buf == NULL) {
            return false;
        }

        len = readlink(path, buf, bufSize);
        if (len == -1) {
            // An error occurred.
            free(buf);
            return false;
        }
        if ((size_t)len < bufSize) {
            // The buffer was big enough.
            buf[len] = '\0';  // Null-terminate the string
            *result = buf;
            return true;
        }
        // Try again with a bigger buffer.
        bufSize *= 2;
    }
}

