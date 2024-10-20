/*
 * Copyright (C) 2011 The Android Open Source Project
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

#define LOG_TAG "Posix"

#include <errno.h>
#include <fcntl.h>
#ifndef __MINGW32__
# include <poll.h>
# include <pwd.h>
# include <linux/limits.h>
#endif
#include <signal.h>
#include <stdlib.h>
#ifndef __MINGW32__
# include <sys/ioctl.h>
# include <sys/mman.h>
#endif
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/types.h>
#ifndef __MINGW32__
# include <sys/uio.h>
# include <sys/utsname.h>
# include <sys/vfs.h> // Bionic doesn't have <sys/statvfs.h>
# include <sys/wait.h>
#endif
#include <unistd.h>

#include <jni.h>
#include "sqlfs.h"

#include "JNIHelp.h"
#include "JniConstants.h"

#ifdef __MINGW32__
struct passwd {
    char    *pw_name;  // User's login name.
    uid_t    pw_uid;   // Numerical user ID.
    gid_t    pw_gid;   // Numerical group ID.
    char    *pw_dir;   // Initial working directory.
    char    *pw_shell; // Program to use as shell.
};
#endif

#ifdef __MINGW32__
typedef off64_t sqlfs_off_t;
typedef struct _stat64 sqlfs_stat;
#else
typedef off_t sqlfs_off_t;
typedef struct stat sqlfs_stat;
#endif

/* right now, we use a single global virtual file system so we don't
 * have to map the structs sqlfs_t and sqlite3 to Java code */
extern char dbFileName[PATH_MAX];

#define TO_JAVA_STRING(NAME, EXP) \
        jstring NAME = (*env)->NewStringUTF(env, EXP); \
        if (NAME == NULL) return NULL;

static void throwException(JNIEnv* env, jclass exceptionClass, jmethodID ctor3, jmethodID ctor2,
                           const char* functionName, int error) {
    jthrowable cause = NULL;
    if ((*env)->ExceptionCheck(env)) {
        cause = (*env)->ExceptionOccurred(env);
        (*env)->ExceptionClear(env);
    }

    jstring detailMessage = (*env)->NewStringUTF(env, functionName);
    if (detailMessage == NULL) {
        // Not really much we can do here. We're probably dead in the water,
        // but let's try to stumble on...
        (*env)->ExceptionClear(env);
    }

    jobject exception;
    if (cause != NULL) {
        exception = (*env)->NewObject(env, exceptionClass, ctor3, detailMessage, error, cause);
    } else {
        exception = (*env)->NewObject(env, exceptionClass, ctor2, detailMessage, error);
    }
    (*env)->Throw(env, exception);
}

static void throwErrnoException(JNIEnv* env, const char* functionName, int error) {
    jclass clse = (*env)->FindClass(env, "info/guardianproject/libcore/io/ErrnoException");
    jmethodID ctor3 = (*env)->GetMethodID(env, clse, "<init>", "(Ljava/lang/String;ILjava/lang/Throwable;)V");
    jmethodID ctor2 = (*env)->GetMethodID(env, clse, "<init>", "(Ljava/lang/String;I)V");
    /* FUSE/sqlfs returns negative errno values, but throwException wants the positive ones */
    throwException(env, clse, ctor3, ctor2, functionName, -error);
}

// sqlfs/FUSE returns errno-style errors as negative values
static int throwIfNegative(JNIEnv* env, const char* name, int rc) {
    if (rc < 0) {
        throwErrnoException(env, name, rc);
    }
    return rc;
}

static jobject makeStructPasswd(JNIEnv* env, const struct passwd* pw) {
    TO_JAVA_STRING(pw_name, pw->pw_name);
    TO_JAVA_STRING(pw_dir, pw->pw_dir);
    TO_JAVA_STRING(pw_shell, pw->pw_shell);
    jclass cls = (*env)->FindClass(env, "info/guardianproject/libcore/io/StructPasswd");
    jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>",
                            "(Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;)V");
    return (*env)->NewObject(env, cls, ctor,
                          pw_name, (jint)pw->pw_uid, (jint)pw->pw_gid, pw_dir, pw_shell);
}

static jobject makeStructStat(JNIEnv* env, const sqlfs_stat* sb) {
    jclass cls = (*env)->FindClass(env, "info/guardianproject/libcore/io/StructStat");
    jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>",
                            "(JJIJIIJJJJJJJ)V");
    return (*env)->NewObject(env, cls, ctor,
                          (jlong)sb->st_dev, (jlong)sb->st_ino,
                          (jint)sb->st_mode, (jlong)sb->st_nlink,
                          (jint)sb->st_uid, (jint)sb->st_gid,
                          (jlong)sb->st_rdev, (jlong)sb->st_size,
                          (jlong)sb->st_atime, (jlong)sb->st_mtime,
#ifdef __MINGW32__
                          (jlong)sb->st_ctime, (jlong)512,
                          (jlong)0);
#else
                          (jlong)sb->st_ctime, (jlong)sb->st_blksize,
                          (jlong)sb->st_blocks);
#endif
}

#ifndef __MINGW32__
static jobject makeStructStatFs(JNIEnv* env, const struct statfs* sb) {
    jclass cls = (*env)->FindClass(env, "info/guardianproject/libcore/io/StructStatFs");
    jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>",
                            "(JJJJJJJJ)V");
    return (*env)->NewObject(env, cls, ctor, (jlong)sb->f_bsize,
                          (jlong)sb->f_blocks, (jlong)sb->f_bfree,
                          (jlong)sb->f_bavail, (jlong)sb->f_files,
                          (jlong)sb->f_ffree, (jlong)sb->f_namelen,
                          (jlong)sb->f_frsize);
}
#endif

static jobject makeStructTimeval(JNIEnv* env, const struct timeval* tv) {
    jclass cls = (*env)->FindClass(env, "info/guardianproject/libcore/io/StructTimeval");
    jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>", "(JJ)V");
    return (*env)->NewObject(env, cls, ctor,
                          (jlong)tv->tv_sec, (jlong)tv->tv_usec);
}

#ifndef __MINGW32__
static jobject makeStructUtsname(JNIEnv* env, const struct utsname* buf) {
    TO_JAVA_STRING(sysname, buf->sysname);
    TO_JAVA_STRING(nodename, buf->nodename);
    TO_JAVA_STRING(release, buf->release);
    TO_JAVA_STRING(version, buf->version);
    TO_JAVA_STRING(machine, buf->machine);
    jclass cls = (*env)->FindClass(env, "info/guardianproject/libcore/io/StructUtsname");
    jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    return (*env)->NewObject(env, cls, ctor,
                          sysname, nodename, release, version, machine);
};
#endif

static jobject doStat(JNIEnv* env, jstring javaPath, jboolean isLstat) {
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    if (path == NULL) {
        return NULL;
    }
    sqlfs_stat sb;
    // TODO implement lstat() once symlink support is added
    if (isLstat)
        jniThrowRuntimeException(env, "lstat() is not implemented");
    int rc = TEMP_FAILURE_RETRY(sqlfs_proc_getattr(0, path, &sb));
    (*env)->ReleaseStringUTFChars(env, javaPath, path);
    if (rc < 0) {
        throwErrnoException(env, isLstat ? "lstat" : "stat", rc);
        return NULL;
    }
    return makeStructStat(env, &sb);
}

static jboolean Posix_access(JNIEnv* env, jobject, jstring javaPath, jint mode) {
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    if (path == NULL) {
        return JNI_FALSE;
    }
    int rc = TEMP_FAILURE_RETRY(sqlfs_proc_access(0, path, mode));
    (*env)->ReleaseStringUTFChars(env, javaPath, path);
    if (rc == -1) {
        throwErrnoException(env, "access", rc);
    }
    return (rc == 0);
}

static void Posix_chmod(JNIEnv* env, jobject, jstring javaPath, jint mode) {
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    if (path == NULL) {
        return;
    }
    throwIfNegative(env, "chmod", TEMP_FAILURE_RETRY(sqlfs_proc_chmod(0, path, mode)));
    (*env)->ReleaseStringUTFChars(env, javaPath, path);
}

static void Posix_close(JNIEnv* env, jobject, jobject javaFd) {
    // Get the FileDescriptor's 'fd' field and clear it.
    // sqlfs doesn't have a close() since files don't really need to be open()ed
    jstring path = jniGetPathFromFileDescriptor(env, javaFd);
    jniSetFileDescriptorInvalid(env, javaFd);
}

static jobject Posix_fstat(JNIEnv* env, jobject, jobject javaFd) {
    jstring javaPath = jniGetPathFromFileDescriptor(env, javaFd);
    return doStat(env, javaPath, JNI_FALSE);
}

// TODO if sqlfs_proc_fsync changes to need isfdatasync and *fi, then fix here
static void Posix_fsync(JNIEnv* env, jobject, jobject javaFd) {
    jstring javaPath = jniGetPathFromFileDescriptor(env, javaFd);
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    throwIfNegative(env, "fsync", TEMP_FAILURE_RETRY(sqlfs_proc_fsync(0, path, 0, NULL)));
    (*env)->ReleaseStringUTFChars(env, javaPath, path);
}

/* in sqlfs, truncate() and ftruncate() do the same thing since there
 * isn't a difference between and open and a closed file */
static void Posix_ftruncate(JNIEnv* env, jobject, jobject javaFd, jlong length) {
    jstring javaPath = jniGetPathFromFileDescriptor(env, javaFd);
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    throwIfNegative(env, "ftruncate", TEMP_FAILURE_RETRY(sqlfs_proc_truncate(0, path, length)));
    (*env)->ReleaseStringUTFChars(env, javaPath, path);
}

static void Posix_link(JNIEnv* env, jobject, jstring javaFrom, jstring javaTo) {
    const char* from = (*env)->GetStringUTFChars(env, javaFrom, NULL);
    const char* to = (*env)->GetStringUTFChars(env, javaTo, NULL);
    if (from == NULL || to == NULL) {
        if (from != NULL) {
            (*env)->ReleaseStringUTFChars(env, javaFrom, from);
        }
        if (to != NULL) {
            (*env)->ReleaseStringUTFChars(env, javaTo, to);
        }
        return;
    }
    throwIfNegative(env, "link", TEMP_FAILURE_RETRY(sqlfs_proc_link(0, from, to)));
    (*env)->ReleaseStringUTFChars(env, javaFrom, from);
    (*env)->ReleaseStringUTFChars(env, javaTo, to);
}

static void Posix_mkdir(JNIEnv* env, jobject, jstring javaPath, jint mode) {
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    if (path == NULL) {
        return;
    }
    // TODO throw exception warning that VirtualFileSystem is not open
    throwIfNegative(env, "mkdir", TEMP_FAILURE_RETRY(sqlfs_proc_mkdir(0, path, mode)));
    (*env)->ReleaseStringUTFChars(env, javaPath, path);
}

static jobject Posix_open(JNIEnv* env, jobject, jstring javaPath, jint flags, jint mode) {
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    if (path == NULL) {
        return NULL;
    }
    struct fuse_file_info ffi;
    ffi.flags = flags;
    ffi.direct_io = 0; // don't use direct_io so this open() call will create a file

    int do_create = 0;
    // libsqfs' open() doesn't create.
    if( (flags & O_CREAT) && (flags & O_EXCL) ) {
        // we must attempt a create
        do_create = 1;
    } else if ( (flags & O_CREAT) ) {
        int rc = TEMP_FAILURE_RETRY(sqlfs_proc_access(0, path, F_OK));
        if (rc != 0) {
            // file does not exist
            do_create = 1;
        }
    }

    int result = 0;
    if( do_create ) {
        char buf = 0;
        result = sqlfs_proc_create(0, path, mode, &ffi);
    } else {
        result = sqlfs_proc_open(0, path, &ffi);
    }
    if (result < 0) {
        throwErrnoException(env, "open", result);
        (*env)->ReleaseStringUTFChars(env, javaPath, path);
        return NULL;
    } else {
        sqlfs_proc_chmod(0, path, mode);
        jobject ret_obj = jniCreateFileDescriptor(env, javaPath);
        (*env)->ReleaseStringUTFChars(env, javaPath, path);
        return ret_obj;
    }
}

static jint Posix_preadBytes(JNIEnv* env, jobject, jobject javaFd, jobject javaBytes, jint byteOffset, jint byteCount, jlong offset) {
    jbyte* bytes = (*env)->GetByteArrayElements(env, javaBytes, NULL);
    if (bytes == NULL) {
        return -1;
    }
    jstring javaPath = jniGetPathFromFileDescriptor(env, javaFd);
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    if (path == NULL) {
        (*env)->ReleaseByteArrayElements(env, javaBytes, bytes, 0);
        return -1;
    }
    int result = sqlfs_proc_read(0, path, (char*)(bytes + byteOffset), byteCount, (sqlfs_off_t)offset, NULL);
    (*env)->ReleaseStringUTFChars(env, javaPath, path);
    (*env)->ReleaseByteArrayElements(env, javaBytes, bytes, 0);
    if (result < 0) {
        if (result != -EIO) { // sqlfs_proc_open returns EIO on end-of-file
            throwErrnoException(env, "pread", result);
        }
        return -1;
    } else {
        return result;
    }
}

static jint Posix_pwriteBytes(JNIEnv* env, jobject, jobject javaFd, jobject javaBytes, jint byteOffset, jint byteCount, jlong offset, jint modeFlags) {
    jbyte* bytes = (*env)->GetByteArrayElements(env, javaBytes, NULL);
    if (bytes == NULL) {
        return -1;
    }
    jstring javaPath = jniGetPathFromFileDescriptor(env, javaFd);
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    if (path == NULL) {
        (*env)->ReleaseByteArrayElements(env, javaBytes, bytes, 0);
        return -1;
    }
    int modeFlagsNow = modeFlags;
    int result = sqlfs_proc_write(0,
                                  path,
                                  (const char*)(bytes + byteOffset),
                                  byteCount,
                                  offset,
                                  modeFlagsNow);
    (*env)->ReleaseStringUTFChars(env, javaPath, path);
    (*env)->ReleaseByteArrayElements(env, javaBytes, bytes, 0);
    if (result < 0) {
        throwErrnoException(env, "pwrite", result);
        return -1;
    } else {
        // TODO make this stick the values into javaBytes
        return result;
    }
}

static void Posix_remove(JNIEnv* env, jobject, jstring javaPath) {
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    if (path == NULL) {
        return;
    }
    if(sqlfs_is_dir(0, path))
        throwIfNegative(env, "remove", TEMP_FAILURE_RETRY(sqlfs_proc_rmdir(0, path)));
    else
        throwIfNegative(env, "remove", TEMP_FAILURE_RETRY(sqlfs_proc_unlink(0, path)));
    (*env)->ReleaseStringUTFChars(env, javaPath, path);
}

static void Posix_rename(JNIEnv* env, jobject, jstring javaOldPath, jstring javaNewPath) {
    const char* oldPath = (*env)->GetStringUTFChars(env, javaOldPath, NULL);
    if (oldPath == NULL) {
        return;
    }
    const char* newPath = (*env)->GetStringUTFChars(env, javaNewPath, NULL);
    if (newPath == NULL) {
        (*env)->ReleaseStringUTFChars(env, javaOldPath, oldPath);
        return;
    }
    throwIfNegative(env, "rename", TEMP_FAILURE_RETRY(sqlfs_proc_rename(0, oldPath, newPath)));
    (*env)->ReleaseStringUTFChars(env, javaOldPath, oldPath);
    (*env)->ReleaseStringUTFChars(env, javaNewPath, newPath);
}

static void Posix_rmdir(JNIEnv* env, jobject, jstring javaPath) {
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    if (path == NULL) {
        return;
    }
    throwIfNegative(env, "rmdir", TEMP_FAILURE_RETRY(sqlfs_proc_rmdir(0, path)));
    (*env)->ReleaseStringUTFChars(env, javaPath, path);
}

static jobject Posix_stat(JNIEnv* env, jobject, jstring javaPath) {
    return doStat(env, javaPath, JNI_FALSE);
}

/* we are faking this somewhat by using the data from the underlying
 partition that the database file is stored on.  That means we ignore
 the javaPath passed in and just use the dbFilename. */
static jobject Posix_statfs(JNIEnv* env, jobject, jstring javaPath) {
#ifdef __MINGW32__
    return NULL;
#else
    struct statfs sb;
    int rc = TEMP_FAILURE_RETRY(statfs(dbFileName, &sb));
    if (rc == -1) {
        throwErrnoException(env, "statfs", rc);
        return NULL;
    }
    /* some guesses at how things should be represented */
    sb.f_bsize = 4096; // libsqlfs uses 4k page sizes in sqlite (I think) // Zoff: isn't it using 8k blocks in sqlcipher? hmmm. not sure what's the correct value here.

    sqlfs_stat st;
    stat(dbFileName, &st);
    sb.f_blocks = st.st_blocks;
    return makeStructStatFs(env, &sb);
#endif
}

static jstring Posix_strerror(JNIEnv* env, jobject, jint errnum) {
    char buffer[BUFSIZ];
    const char* message = jniStrError(errnum, buffer, sizeof(buffer));
    return (*env)->NewStringUTF(env, message);
}

static void Posix_symlink(JNIEnv* env, jobject, jstring javaOldPath, jstring javaNewPath) {
    const char* oldPath = (*env)->GetStringUTFChars(env, javaOldPath, NULL);
    if (oldPath == NULL) {
        return;
    }
    const char* newPath = (*env)->GetStringUTFChars(env, javaNewPath, NULL);
    if (newPath == NULL) {
        (*env)->ReleaseStringUTFChars(env, javaOldPath, oldPath);
        return;
    }
    throwIfNegative(env, "symlink", TEMP_FAILURE_RETRY(sqlfs_proc_symlink(0, oldPath, newPath)));
    (*env)->ReleaseStringUTFChars(env, javaOldPath, oldPath);
    (*env)->ReleaseStringUTFChars(env, javaNewPath, newPath);
}

static void Posix_unlink(JNIEnv* env, jobject, jstring javaPath) {
    const char* path = (*env)->GetStringUTFChars(env, javaPath, NULL);
    if (path == NULL) {
        return;
    }
    throwIfNegative(env, "unlink", TEMP_FAILURE_RETRY(sqlfs_proc_unlink(0, path)));
    (*env)->ReleaseStringUTFChars(env, javaPath, path);
}

static JNINativeMethod sMethods[] = {
    {"access", "(Ljava/lang/String;I)Z", (void *)Posix_access},
    {"chmod", "(Ljava/lang/String;I)V", (void *)Posix_chmod},
    {"close", "(Linfo/guardianproject/iocipher/FileDescriptor;)V", (void *)Posix_close},
    {"fstat", "(Linfo/guardianproject/iocipher/FileDescriptor;)Linfo/guardianproject/libcore/io/StructStat;", (void *)Posix_fstat},
    {"fsync", "(Linfo/guardianproject/iocipher/FileDescriptor;)V", (void *)Posix_fsync},
    {"ftruncate", "(Linfo/guardianproject/iocipher/FileDescriptor;J)V", (void *)Posix_ftruncate},
    {"link", "(Ljava/lang/String;Ljava/lang/String;)V", (void *)Posix_link},
    {"mkdir", "(Ljava/lang/String;I)V", (void *)Posix_mkdir},
    {"open", "(Ljava/lang/String;II)Linfo/guardianproject/iocipher/FileDescriptor;", (void *)Posix_open},
    {"preadBytes", "(Linfo/guardianproject/iocipher/FileDescriptor;Ljava/lang/Object;IIJ)I", (void *)Posix_preadBytes},
    {"pwriteBytes", "(Linfo/guardianproject/iocipher/FileDescriptor;Ljava/lang/Object;IIJI)I", (void *)Posix_pwriteBytes},
    {"remove", "(Ljava/lang/String;)V", (void *)Posix_remove},
    {"rename", "(Ljava/lang/String;Ljava/lang/String;)V", (void *)Posix_rename},
    {"rmdir", "(Ljava/lang/String;)V", (void *)Posix_rmdir},
    {"stat", "(Ljava/lang/String;)Linfo/guardianproject/libcore/io/StructStat;", (void *)Posix_stat},
    {"statfs", "(Ljava/lang/String;)Linfo/guardianproject/libcore/io/StructStatFs;", (void *)Posix_statfs},
    {"strerror", "(I)Ljava/lang/String;", (void *)Posix_strerror},
    {"symlink", "(Ljava/lang/String;Ljava/lang/String;)V", (void *)Posix_symlink},
    {"unlink", "(Ljava/lang/String;)V", (void *)Posix_unlink},
};

int register_info_guardianproject_libcore_io_Posix(JNIEnv* env) {
    jclass cls;

    cls = (*env)->FindClass(env, "info/guardianproject/libcore/io/Posix");
    if (cls == NULL) {
        LOGE("Can't find info/guardianproject/libcore/io/Posix\n");
        return -1;
    }
    return (*env)->RegisterNatives(env, cls, sMethods, sizeof(sMethods) / sizeof(JNINativeMethod));
}

