/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#define _GNU_SOURCE

#define LOG_TAG "File"

#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/vfs.h>
#include <time.h>
#include <unistd.h>
#include <utime.h>

#include "JNIHelp.h"
#include "JniConstants.h"
#include "ScopedPrimitiveArray.h"
#include "ScopedUtfChars.h"
#include "readlink.h"
#include "toStringArray.h"
#include "sqlfs.h"

/* right now, we use a single global virtual file system so we don't
 * have to map the structs sqlfs_t and sqlite3 to Java code */
extern sqlfs_t *sqlfs;

// from fuse.h
typedef int(* fuse_fill_dir_t )(void *buf, const char *name, const struct stat *stbuf, off_t off);

static jstring File_readlink(JNIEnv* env, jclass cls, jstring javaPath) {
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    if (path == NULL) {
        return NULL;
    }

    char result[PATH_MAX];
    ssize_t len = readlink(path, result, sizeof(result) - 1);
    (*env)->ReleaseStringUTFChars(env, javaPath, path);

    if (len == -1) {
        jniThrowIOException(env, errno);
        return NULL;
    }

    result[len] = '\0';
    return (*env)->NewStringUTF(env, result);
}

static jstring File_realpath(JNIEnv* env, jclass cls, jstring javaPath) {
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    if (path == NULL) {
        return NULL;
    }

    char resolved[PATH_MAX];
    char* result = realpath(path, resolved);
    (*env)->ReleaseStringUTFChars(env, javaPath, path);

    if (result == NULL) {
        jniThrowIOException(env, errno);
        return NULL;
    }

    return (*env)->NewStringUTF(env, result);
}

static jlong File_lastModifiedImpl(JNIEnv* env, jclass cls, jstring javaPath) {
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    if (path == NULL) {
        return JNI_FALSE;
    }
    struct stat sb;
    sqlfs_proc_getattr(0, path, &sb);
    (*env)->ReleaseStringUTFChars(env, javaPath, path);

    return (jlong)((long)sb.st_mtime * 1000L);
}

static jboolean File_setLastModifiedImpl(JNIEnv* env, jclass cls, jstring javaPath, jlong ms) {
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    if (path == NULL) {
        return JNI_FALSE;
    }

    // We want to preserve the access time.
    struct stat sb;
    sqlfs_proc_getattr(0, path, &sb);
    key_attr atime;
    atime.atime = (long)sb.st_atime;

    // TODO: we could get microsecond resolution with utimes(3), "legacy" though it is.
    key_attr mtime;
    mtime.mtime = (time_t)(ms / 1000);

    // TODO: this is not correct. fix me!!
    int result = JNI_FALSE;
    /*
    int result = sqlfs_set_attr(0, "mtime", &mtime);
    if (result) {
        result = sqlfs_set_attr(0, "atime", &atime);
    }
    */

    (*env)->ReleaseStringUTFChars(env, javaPath, path);

    return result ? JNI_TRUE : JNI_FALSE;
}

typedef struct {
    char** entries;
    int count;
    int capacity;
} DirEntries;

/* FUSE filler() function for use with the FUSE style readdir() that
 * libsqlfs provides.  Note: this function only ever expects statp to
 * be NULL and off to be 0.  buf is DirEntries */
static int fill_dir(void *buf, const char *name, const struct stat *statp, off_t off) {
    DirEntries *entries = (DirEntries*) buf;
    if(statp != NULL)
        LOGE("File.listImpl() fill_dir always expects statp to be NULL");
    if(off != 0)
        LOGE("File.listImpl() fill_dir always expects off to be 0");

    if (entries->count >= entries->capacity) {
        int new_capacity = entries->capacity * 2;
        char** new_entries = realloc(entries->entries, new_capacity * sizeof(char*));
        if (new_entries == NULL) {
            return 1;  // Error
        }
        entries->entries = new_entries;
        entries->capacity = new_capacity;
    }

    entries->entries[entries->count] = strdup(name);
    if (entries->entries[entries->count] == NULL) {
        return 0;  // Error (HINT: in the original it always return 0, even on error. so we keep this behaviour)
    }
    entries->count++;

    return 0;
}

static jboolean File_isDirectoryImpl(JNIEnv* env, jclass cls, jstring javaPath) {
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    if (path == NULL) {
        return JNI_FALSE;
    }

    jboolean result = sqlfs_is_dir(0, path);

    (*env)->ReleaseStringUTFChars(env, javaPath, path);

    return result;
}

static jobjectArray File_listImpl(JNIEnv* env, jclass cls, jstring javaPath) {
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    if (path == NULL) {
        return NULL;
    }

    DirEntries entries = {NULL, 0, 16};
    // using FUSE readdir in old getdir() style which gives us the whole thing at once
    entries.entries = malloc(entries.capacity * sizeof(char*));
    if (entries.entries == NULL) {
        (*env)->ReleaseStringUTFChars(env, javaPath, path);
        return NULL;
    }

    sqlfs_proc_readdir(0, path, (void *)&entries, (fuse_fill_dir_t)fill_dir, 0, NULL);

    (*env)->ReleaseStringUTFChars(env, javaPath, path);

    // filter "." and ".." from list of entries
    // Translate the intermediate form into a Java String[].
    int filtered_count = 0;
    for (int i = 0; i < entries.count; i++) {
        if (strcmp(entries.entries[i], ".") != 0 && strcmp(entries.entries[i], "..") != 0) {
            entries.entries[filtered_count++] = entries.entries[i];
        } else {
            free(entries.entries[i]);
        }
    }
    entries.count = filtered_count;

    // Convert to Java String array
    jobjectArray result = (*env)->NewObjectArray(env, entries.count, 
        (*env)->FindClass(env, "java/lang/String"), NULL);

    for (int i = 0; i < entries.count; i++) {
        jstring str = (*env)->NewStringUTF(env, entries.entries[i]);
        (*env)->SetObjectArrayElement(env, result, i, str);
        (*env)->DeleteLocalRef(env, str);
        free(entries.entries[i]);
    }

    free(entries.entries);

    return result;
}

static JNINativeMethod sMethods[] = {
    {"isDirectoryImpl", "(Ljava/lang/String;)Z", (void *)File_isDirectoryImpl},
    {"listImpl", "(Ljava/lang/String;)[Ljava/lang/String;", (void *)File_listImpl},
    {"readlink", "(Ljava/lang/String;)Ljava/lang/String;", (void *)File_readlink},
    {"realpath", "(Ljava/lang/String;)Ljava/lang/String;", (void *)File_realpath},
    {"lastModifiedImpl", "(Ljava/lang/String;)J", (void *)File_lastModifiedImpl},
    {"setLastModifiedImpl", "(Ljava/lang/String;J)Z", (void *)File_setLastModifiedImpl},
};

int register_info_guardianproject_iocipher_File(JNIEnv* env) {
    jclass cls = (*env)->FindClass(env, "info/guardianproject/iocipher/File");
    if (cls == NULL) {
        LOGE("Can't find info/guardianproject/iocipher/File\n");
        return -1;
    }
    return (*env)->RegisterNatives(env, cls, sMethods, sizeof(sMethods)/sizeof(sMethods[0]));
}

