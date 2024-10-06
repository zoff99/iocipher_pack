#include <jni.h>
#include "JNIHelp.h"

#define LOG_TAG "JNI_OnLoad"

int registerJniHelp(JNIEnv* env);
int register_info_guardianproject_iocipher_File(JNIEnv *env);
int register_info_guardianproject_iocipher_VirtualFileSystem(JNIEnv *env);
int register_info_guardianproject_libcore_io_Memory(JNIEnv *env);
int register_info_guardianproject_libcore_io_OsConstants(JNIEnv *env);
int register_info_guardianproject_libcore_io_Posix(JNIEnv *env);

jint JNI_OnLoad(JavaVM* vm, void* reserved) 
{ 
    LOGI("JNI_OnLoad called\n");
    JNIEnv* env;
    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE("Failed to get the environment using GetEnv()");
        return -1;
    }

    LOGI("JNI_OnLoad register methods:\n");
    int res = registerJniHelp(env);
    LOGI("registerJniHelp:res=%d\n", res);
    res = register_info_guardianproject_iocipher_File(env);
    LOGI("register_info_guardianproject_iocipher_File:res=%d\n", res);
    res = register_info_guardianproject_iocipher_VirtualFileSystem(env);
    LOGI("register_info_guardianproject_iocipher_VirtualFileSystem:res=%d\n", res);
    res = register_info_guardianproject_libcore_io_Memory(env);
    LOGI("register_info_guardianproject_libcore_io_Memory:res=%d\n", res);
    res = register_info_guardianproject_libcore_io_Posix(env);
    LOGI("register_info_guardianproject_libcore_io_Posix:res=%d\n", res);
    // res = register_info_guardianproject_libcore_io_OsConstants(env);
    // LOGI("register_info_guardianproject_libcore_io_OsConstants:res=%d\n", res);

    LOGI("JNI_OnLoad done\n");

    return JNI_VERSION_1_6;
}

