#define _GNU_SOURCE

#define LOG_TAG "VirtualFileSystem.cpp"

#ifndef __MINGW32__
# include <alloca.h>
#endif
#include <jni.h>
#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <libgen.h>
#include <sys/stat.h>
#ifndef __MINGW32__
# include <linux/limits.h>
#endif

#include "sqlfs.h"

#include "JNIHelp.h"

#define IOCIPHER_VERSION "1.0.0"

char dbFileName[PATH_MAX] = { 0 };

#ifdef __MINGW32__
typedef off64_t sqlfs_off_t;
typedef struct _stat64 sqlfs_stat;
#else
typedef off_t sqlfs_off_t;
typedef struct stat sqlfs_stat;
#endif

// store first sqlfs instance as marker for mounted state
static sqlfs_t *sqlfs = NULL;
// memory blob for error messages
static char msg[256];
#define MAX_MSG_LEN 255

static jboolean VirtualFileSystem_isMounted(JNIEnv *env, jobject obj);

bool throwContainerReadWriteError(JNIEnv *env) {
    bool error = false;
    if (access(dbFileName, R_OK) != 0) {
        error = true;
        snprintf(msg, MAX_MSG_LEN,
                 "Could not mount %s does not exist or is not readable (%d)!",
                 dbFileName, errno);
    } else if (access(dbFileName, W_OK) != 0) {
        error = true;
        snprintf(msg, MAX_MSG_LEN,
                 "Could not mount %s is not writable (%d)!",
                 dbFileName, errno);
    }
    if (error)
        jniThrowException(env, "java/lang/IllegalArgumentException", msg);
    return error;
}

void handleCreateError(JNIEnv *env) {
    if (!throwContainerReadWriteError(env)) {
        snprintf(msg, MAX_MSG_LEN, "Unknown error creating %s", dbFileName);
        jniThrowException(env, "java/lang/IllegalStateException", msg);
    }
}

void handleMountError(JNIEnv *env) {
    if (!throwContainerReadWriteError(env)) {
        snprintf(msg, MAX_MSG_LEN,
                 "Could not mount filesystem in %s, bad password given?", dbFileName);
        jniThrowException(env, "java/lang/IllegalArgumentException", msg);
    }
}

bool throwKeyLengthException(JNIEnv *env, jsize keyLen) {
    if (keyLen != REQUIRED_KEY_LENGTH) {
        snprintf(msg, MAX_MSG_LEN, "Key length is not %i bytes (%i bytes)!",
                 REQUIRED_KEY_LENGTH, keyLen);
        jniThrowException(env, "java/lang/IllegalArgumentException", msg);
        return true;
    } else {
        return false;
    }
}

bool throwMountedException(JNIEnv *env, jobject obj) {
    if (VirtualFileSystem_isMounted(env, obj)) {
        snprintf(msg, MAX_MSG_LEN, "Filesystem in '%s' already mounted!", dbFileName);
        jniThrowException(env, "java/lang/IllegalStateException", msg);
        return true;
    } else {
        return false;
    }
}

static jstring VirtualFileSystem_getContainerPath(JNIEnv *env, jobject obj) {
    return (*env)->NewStringUTF(env, dbFileName);
}

static void VirtualFileSystem_setContainerPath(JNIEnv *env, jobject obj, jstring javaFileName) {
    char const *name = (*env)->GetStringUTFChars(env, javaFileName, NULL);
    jsize nameLen = (*env)->GetStringUTFLength(env, javaFileName);
    memset(dbFileName, 0, PATH_MAX);
    if (name == NULL || nameLen < 1) {
        jniThrowException(env, "java/lang/IllegalArgumentException",
                          "blank file name not allowed!");
        (*env)->ReleaseStringUTFChars(env, javaFileName, name);
        return;
    }

    int validFileName = 1;
    sqlfs_stat sb;
    char *name2 = alloca(nameLen + 1);
    memset(name2, 0, nameLen + 1);
    memcpy(name2, name, nameLen);

    // printf("name=X%sX plen=%d\n", name, (int)nameLen);

    const char *dir = dirname(name2);

    // printf("dir=X%sX plen=%d\n", dir, (int)strlen(dir));

    if (access(dir, R_OK) != 0) {
        validFileName = 0;
        snprintf(msg, MAX_MSG_LEN,
                 "Base directory %s, does not exist or is not readable (%d)!",
                 dir, errno);
    } else if (access(dir, W_OK) != 0) {
        validFileName = 0;
        snprintf(msg, MAX_MSG_LEN, "Could not write to base directory %s (%d)!",
                 dir, errno);
    } else if (stat(dir, &sb) == -1) {
        validFileName = 0;
        snprintf(msg, MAX_MSG_LEN, "Cannot stat %s (%d)!", dir, errno);
    } else if (!(sb.st_mode & S_IFDIR)) {
        validFileName = 0;
        snprintf(msg, MAX_MSG_LEN, "Base path %s is not a directory!", dir);
    }

    if (validFileName) {
        strncpy(dbFileName, name, PATH_MAX-2);
        dbFileName[PATH_MAX-1] = '\0';
    } else {
        jniThrowException(env, "java/lang/IllegalArgumentException", msg);
    }
    (*env)->ReleaseStringUTFChars(env, javaFileName, name);
}

static jboolean VirtualFileSystem_isMounted(JNIEnv *env, jobject obj) {
    return sqlfs != NULL || sqlfs_instance_count() > 0;
}

static void VirtualFileSystem_createNewContainer(JNIEnv *env, jobject obj, jstring javaPassword) {
    if (throwMountedException(env, obj))
        return;

    char const *password = (*env)->GetStringUTFChars(env, javaPassword, NULL);
    jsize passwordLen = (*env)->GetStringUTFLength(env, javaPassword);

    // printf("pass=X%sX plen=%d\n", password, (int)passwordLen);

    /* Attempt to open the database with the password, then immediately close
     * it. If it fails, then the password is likely wrong. */
    if (sqlfs_open_password(dbFileName, password, &sqlfs)) {
        sqlfs_close(sqlfs);
        sqlfs = NULL;
    } else {
        handleCreateError(env);
    }
    (*env)->ReleaseStringUTFChars(env, javaPassword, password);
}

static void VirtualFileSystem_createNewContainer_byte(JNIEnv *env, jobject obj, jbyteArray javaKey) {
    if (throwMountedException(env, obj))
        return;

    jsize keyLen = (*env)->GetArrayLength(env, javaKey);
    if (throwKeyLengthException(env, keyLen))
        return;

    jbyte *key = (*env)->GetByteArrayElements(env, javaKey, NULL); //direct mem ref

    /* attempt to open the database with the key if it fails, most likely the
     * db file does not exist or the key is wrong */
    if (sqlfs_open_key(dbFileName, (uint8_t*)key, keyLen, &sqlfs)) {
        sqlfs_close(sqlfs);
        sqlfs = NULL;
    } else {
        handleMountError(env);
    }

    (*env)->ReleaseByteArrayElements(env, javaKey, key, 0);
}

static void VirtualFileSystem_mount(JNIEnv *env, jobject obj, jstring javaPassword) {
    if (throwMountedException(env, obj))
        return;

    if (throwContainerReadWriteError(env))
        return;

    char const *password = (*env)->GetStringUTFChars(env, javaPassword, NULL);
    jsize passwordLen = (*env)->GetStringUTFLength(env, javaPassword);

    /* Attempt to open the database with the password, then immediately close
     * it. If it fails, then the password is likely wrong. */
    if (!sqlfs_open_password(dbFileName, password, &sqlfs)) {
        handleMountError(env);
    }
    (*env)->ReleaseStringUTFChars(env, javaPassword, password);
}

static void VirtualFileSystem_mount_byte(JNIEnv *env, jobject obj, jbyteArray javaKey) {
    if (throwMountedException(env, obj))
        return;

    if (throwContainerReadWriteError(env))
        return;

    jsize keyLen = (*env)->GetArrayLength(env, javaKey);
    if (throwKeyLengthException(env, keyLen))
        return;

    jbyte *key = (*env)->GetByteArrayElements(env, javaKey, NULL); //direct mem ref

    /* attempt to open the database with the key if it fails, most likely the
     * db file does not exist or the key is wrong */
    if (!sqlfs_open_key(dbFileName, (uint8_t*)key, keyLen, &sqlfs)) {
        handleMountError(env);
    }

    (*env)->ReleaseByteArrayElements(env, javaKey, key, 0);
}

static void VirtualFileSystem_unmount(JNIEnv *env, jobject obj) {
    if (!VirtualFileSystem_isMounted(env, obj)) {
        snprintf(msg, MAX_MSG_LEN, "Filesystem in '%s' not mounted!", dbFileName);
        jniThrowException(env, "java/lang/IllegalStateException", msg);
        return;
    }
    /* VFS holds a sqlfs instance open purely as a marker of the filesystem
     * being mounted. If there is more than one sqlfs instance, that means
     * that threads are still active. Closing the final sqlfs instances causes
     * libsqlfs to close the database and zero out the key/password. */
    if (sqlfs_instance_count() > 1) {
        snprintf(msg, 255,
                 "WARNING: unmounting when threads are still active! (%i threads)",
                 sqlfs_instance_count() - 1);
        // Zoff: since I am using this in java on the main thread, this is now only a warning
        //       be careful and know what you are using, when using this pachted version!
        //
        // jniThrowException(env, "java/lang/IllegalStateException", msg);
        // return;
        //
        // Zoff: since I am using this in java on the main thread, this is now only a warning
        //       be careful and know what you are using, when using this pachted version!
    }
    sqlfs_close(sqlfs);
    sqlfs = NULL;
}

static void VirtualFileSystem_detachThread(JNIEnv *env, jobject obj) {
    sqlfs_detach_thread();
    return;
}

static void VirtualFileSystem_beginTransaction(JNIEnv *env, jobject obj) {
    sqlfs_begin_transaction(0);
    return;
}

static void VirtualFileSystem_completeTransaction(JNIEnv *env, jobject obj) {
    sqlfs_complete_transaction(0,1);
    return;
}

static jstring VirtualFileSystem_sqlfsVersion(JNIEnv *env, jobject obj) {
    return (*env)->NewStringUTF(env, SQLFS_VERSION);
}

static jstring VirtualFileSystem_iocipherVersion(JNIEnv *env, jobject obj) {
    return (*env)->NewStringUTF(env, IOCIPHER_VERSION);
}

static JNINativeMethod sMethods[] = {
    {"getContainerPath", "()Ljava/lang/String;", (void *)VirtualFileSystem_getContainerPath},
    {"setContainerPath", "(Ljava/lang/String;)V", (void *)VirtualFileSystem_setContainerPath},
    {"createNewContainer", "(Ljava/lang/String;)V", (void *)VirtualFileSystem_createNewContainer},
    {"createNewContainer", "([B)V", (void *)VirtualFileSystem_createNewContainer_byte},
    {"mount", "(Ljava/lang/String;)V", (void *)VirtualFileSystem_mount},
    {"mount", "([B)V", (void *)VirtualFileSystem_mount_byte},
    {"unmount", "()V", (void *)VirtualFileSystem_unmount},
    {"isMounted", "()Z", (void *)VirtualFileSystem_isMounted},
    {"detachThread", "()V", (void *)VirtualFileSystem_detachThread},
    {"beginTransaction", "()V", (void *)VirtualFileSystem_beginTransaction},
    {"completeTransaction", "()V", (void *)VirtualFileSystem_completeTransaction},
    {"sqlfsVersion", "()Ljava/lang/String;", (void *)VirtualFileSystem_sqlfsVersion},
    {"iocipherVersion", "()Ljava/lang/String;", (void *)VirtualFileSystem_iocipherVersion},
};

int register_info_guardianproject_iocipher_VirtualFileSystem(JNIEnv* env) {
    jclass cls = (*env)->FindClass(env, "info/guardianproject/iocipher/VirtualFileSystem");
    if (cls == NULL) {
        LOGE("Can't find info/guardianproject/iocipher/VirtualFileSystem\n");
        return -1;
    }
    return (*env)->RegisterNatives(env, cls, sMethods, sizeof(sMethods)/sizeof(sMethods[0]));
}

