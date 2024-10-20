/*
 * Copyright (c) 2003 Constantin S. Svintsoff <kostik@iclub.nsu.ru>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The names of the authors may not be used to endorse or promote
 *    products derived from this software without specific prior written
 *    permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#include "readlink.h"

#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <sys/param.h>
#include <sys/stat.h>
#include <unistd.h>

/**
 * This differs from realpath(3) mainly in its behavior when a path element does not exist or can
 * not be searched. realpath(3) treats that as an error and gives up, but we have Java-compatible
 * behavior where we just assume the path element was not a symbolic link. This leads to a textual
 * treatment of ".." from that point in the path, which may actually lead us back to a path we
 * can resolve (as in "/tmp/does-not-exist/../blah.txt" which would be an error for realpath(3)
 * but "/tmp/blah.txt" under the traditional Java interpretation).
 *
 * This implementation also removes all the fixed-length buffers of the C original.
 */
bool realpath_wrapper(const char* path, char** resolved) {
    // 'path' must be an absolute path.
    if (path[0] != '/') {
        errno = EINVAL;
        return false;
    }

    *resolved = strdup("/");
    if (path[1] == '\0') {
        return true;
    }

    // Iterate over path components in 'left'.
    int symlinkCount = 0;
    char* left = strdup(path + 1);
    while (left[0] != '\0') {
        // Extract the next path component.
        char* nextSlash = strchr(left, '/');
        char* nextPathComponent;
        if (nextSlash) {
            *nextSlash = '\0';
            nextPathComponent = left;
            left = nextSlash + 1;
        } else {
            nextPathComponent = left;
            left = left + strlen(left);
        }

        if (nextPathComponent[0] == '\0') {
            continue;
        } else if (strcmp(nextPathComponent, ".") == 0) {
            continue;
        } else if (strcmp(nextPathComponent, "..") == 0) {
            // Strip the last path component except when we have single "/".
            if (strlen(*resolved) > 1) {
                char* lastSlash = strrchr(*resolved, '/');
                if (lastSlash != *resolved) {
                    *lastSlash = '\0';
                }
            }
            continue;
        }

        // Append the next path component.
        size_t resolvedLen = strlen(*resolved);
        if ((*resolved)[resolvedLen - 1] != '/') {
            *resolved = realloc(*resolved, resolvedLen + 2 + strlen(nextPathComponent));
            strcat(*resolved, "/");
        } else {
            *resolved = realloc(*resolved, resolvedLen + 1 + strlen(nextPathComponent));
        }
        strcat(*resolved, nextPathComponent);

        // See if we've got a symbolic link, and resolve it if so.
#ifndef __MINGW32__
        struct stat sb;
        if (lstat(*resolved, &sb) == 0 && S_ISLNK(sb.st_mode)) {
            if (symlinkCount++ > MAXSYMLINKS) {
                errno = ELOOP;
                free(left);
                return false;
            }

            char* symlink = NULL;
            if (!readlink_wrapper(*resolved, &symlink)) {
                free(left);
                return false;
            }
            if (symlink[0] == '/') {
                // The symbolic link is absolute, so we need to start from scratch.
                free(*resolved);
                *resolved = strdup("/");
            } else if (strlen(*resolved) > 1) {
                // The symbolic link is relative, so we just lose the last path component (which
                // was the link).
                char* lastSlash = strrchr(*resolved, '/');
                if (lastSlash != *resolved) {
                    *lastSlash = '\0';
                }
            }

            if (left[0] != '\0') {
                const char* maybeSlash = (symlink[strlen(symlink) - 1] != '/') ? "/" : "";
                char* newLeft = malloc(strlen(symlink) + strlen(maybeSlash) + strlen(left) + 1);
                sprintf(newLeft, "%s%s%s", symlink, maybeSlash, left);
                free(left);
                left = newLeft;
            } else {
                free(left);
                left = strdup(symlink);
            }
            free(symlink);
        }
#endif
    }

    // Remove trailing slash except when the resolved pathname is a single "/".
    size_t resolvedLen = strlen(*resolved);
    if (resolvedLen > 1 && (*resolved)[resolvedLen - 1] == '/') {
        (*resolved)[resolvedLen - 1] = '\0';
    }
    free(left);
    return true;
}

