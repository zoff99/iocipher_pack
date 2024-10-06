#define _GNU_SOURCE

#include <alloca.h>
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

#include "sqlfs.h"

#include "JNIHelp.h"

#define PATH_MAX 4096
#define MAX_MSG_LEN 255
#define IOCIPHER_VERSION "1.0.0"

char dbFileName[PATH_MAX] = { 0 };
static sqlfs_t *sqlfs = NULL;
static char msg[256];

static int VirtualFileSystem_isMounted(JNIEnv *env, jobject object);

bool throwContainerReadWriteError() {
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
    if (error) {
        fprintf(stderr, "%s\n", msg);
    }
    return error;
}

void handleCreateError() {
    if (!throwContainerReadWriteError()) {
        snprintf(msg, MAX_MSG_LEN, "Unknown error creating %s", dbFileName);
        fprintf(stderr, "%s\n", msg);
    }
}

void handleMountError() {
    if (!throwContainerReadWriteError()) {
        snprintf(msg, MAX_MSG_LEN,
                 "Could not mount filesystem in %s, bad password given?", dbFileName);
        fprintf(stderr, "%s\n", msg);
    }
}

bool throwKeyLengthException(int keyLen) {
    if (keyLen != REQUIRED_KEY_LENGTH) {
        snprintf(msg, MAX_MSG_LEN, "Key length is not %i bytes (%i bytes)!",
                 REQUIRED_KEY_LENGTH, keyLen);
        fprintf(stderr, "%s\n", msg);
        return true;
    } else {
        return false;
    }
}

bool throwMountedException() {
    if (VirtualFileSystem_isMounted(NULL, NULL)) {
        snprintf(msg, MAX_MSG_LEN, "Filesystem in '%s' already mounted!", dbFileName);
        fprintf(stderr, "%s\n", msg);
        return true;
    } else {
        return false;
    }
}

static jstring VirtualFileSystem_getContainerPath(JNIEnv *env, jobject object) {
    return (*env)->NewStringUTF(env, dbFileName);
}

static void VirtualFileSystem_setContainerPath(JNIEnv *env, jobject object, jstring name_j) {
    memset(dbFileName, 0, PATH_MAX);

    const char *name_j_c = (*env)->GetStringUTFChars(env, name_j, NULL);
    char *name = alloca(PATH_MAX);
    memset(name, 0, PATH_MAX);
    memcpy(name, name_j_c, strlen(name_j_c));
    printf("VirtualFileSystem_setContainerPath:file=%s\n", name);
    (*env)->ReleaseStringUTFChars(env, name_j, name_j_c);


    if (name == NULL || strlen(name) < 1) {
        fprintf(stderr, "blank file name not allowed!\n");
        return;
    }

    printf("VirtualFileSystem_setContainerPath2:file=%s\n", name);
    int validFileName = 1;
    struct stat sb;
    char *name2 = strdup(name);
    const char *dir = dirname((char *)name2);
    printf("VirtualFileSystem_setContainerPath3:dir=%s file=%s\n", dir, name);

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

    free(name2);

    printf("VirtualFileSystem_setContainerPath:file=%s\n", name);

    if (validFileName) {
        printf("validFileName\n");
        strncpy(dbFileName, name, PATH_MAX-2);
        dbFileName[PATH_MAX-1] = '\0';
        printf("validFileName=%s\n", dbFileName);
    } else {
        fprintf(stderr, "%s\n", msg);
    }
}

static int VirtualFileSystem_isMounted(JNIEnv *env, jobject object) {
    return sqlfs != NULL || sqlfs_instance_count() > 0;
}

static void VirtualFileSystem_createNewContainer(JNIEnv *env, jobject object, jstring password_j) {
    if (throwMountedException())
        return;

    const char *password_j_c = (*env)->GetStringUTFChars(env, password_j, NULL);
    char *password = alloca(PATH_MAX);
    memset(password, 0, PATH_MAX);
    memcpy(password, password_j_c, strlen(password_j_c));
    printf("VirtualFileSystem_createNewContainer:password=%s\n", password);
    (*env)->ReleaseStringUTFChars(env, password_j, password_j_c);

    printf("VirtualFileSystem_createNewContainer:dbFileName=%s\n", dbFileName);
    printf("VirtualFileSystem_createNewContainer:password=%s\n", password);

    if (sqlfs_open_password(dbFileName, password, &sqlfs)) {
        printf("sqlfs_open_password:ok\n");
        sqlfs_close(sqlfs);
        sqlfs = NULL;
    } else {
        printf("sqlfs_open_password:ERROR\n");
        handleCreateError();
    }
}

static void VirtualFileSystem_createNewContainer_byte(JNIEnv *env, jobject object, uint8_t *key, int keyLen) {
    if (throwMountedException())
        return;

    if (throwKeyLengthException(keyLen))
        return;

    if (sqlfs_open_key(dbFileName, key, keyLen, &sqlfs)) {
        sqlfs_close(sqlfs);
        sqlfs = NULL;
    } else {
        handleMountError();
    }
}

static void VirtualFileSystem_mount(JNIEnv *env, jobject object, jstring password_j) {
    if (throwMountedException())
        return;

    if (throwContainerReadWriteError())
        return;

    const char *password_j_c = (*env)->GetStringUTFChars(env, password_j, NULL);
    char *password = alloca(PATH_MAX);
    memset(password, 0, PATH_MAX);
    memcpy(password, password_j_c, strlen(password_j_c));
    printf("VirtualFileSystem_mount:password=%s\n", password);
    (*env)->ReleaseStringUTFChars(env, password_j, password_j_c);

    if (!sqlfs_open_password(dbFileName, password, &sqlfs)) {
        handleMountError();
    }
}

static void VirtualFileSystem_mount_byte(JNIEnv *env, jobject object, uint8_t *key, int keyLen) {
    if (throwMountedException())
        return;

    if (throwContainerReadWriteError())
        return;

    if (throwKeyLengthException(keyLen))
        return;

    if (!sqlfs_open_key(dbFileName, key, keyLen, &sqlfs)) {
        handleMountError();
    }
}

static void VirtualFileSystem_unmount(JNIEnv *env, jobject object) {
    if (!VirtualFileSystem_isMounted(NULL, NULL)) {
        snprintf(msg, MAX_MSG_LEN, "Filesystem in '%s' not mounted!", dbFileName);
        fprintf(stderr, "%s\n", msg);
        return;
    }
    if (sqlfs_instance_count() > 1) {
        snprintf(msg, 255,
                 "WARNING: unmounting when threads are still active! (%i threads)",
                 sqlfs_instance_count() - 1);
    }
    sqlfs_close(sqlfs);
    sqlfs = NULL;
}

static void VirtualFileSystem_detachThread() {
    sqlfs_detach_thread();
}

static void VirtualFileSystem_beginTransaction() {
    sqlfs_begin_transaction(0);
}

static void VirtualFileSystem_completeTransaction() {
    sqlfs_complete_transaction(0, 1);
}

static jstring VirtualFileSystem_sqlfsVersion(JNIEnv *env, jobject object) {
    return (*env)->NewStringUTF(env, SQLFS_VERSION);
}

static jstring VirtualFileSystem_iocipherVersion(JNIEnv *env, jobject object) {
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

