/* //device/libs/include/android_runtime/sqlite3_exception.h
**
** Copyright 2012, The Guardian Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

#define LOG_TAG "JNI_OnLoad"

#include <jni.h>
#include "JNIHelp.h"
#include "JniConstants.h"

/* each class file includes its own register function */
int registerJniHelp(JNIEnv* env);
void unregisterJniHelp(JNIEnv* env);
int register_info_guardianproject_iocipher_File(JNIEnv *env);
int register_info_guardianproject_iocipher_VirtualFileSystem(JNIEnv *env);
int register_info_guardianproject_libcore_io_Memory(JNIEnv *env);
int register_info_guardianproject_libcore_io_OsConstants(JNIEnv *env);
int register_info_guardianproject_libcore_io_Posix(JNIEnv *env);

extern "C" jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    LOGI("JNI_OnLoad called");
    JNIEnv* env;
    int res;

    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("Failed to get the environment using GetEnv()");
        return JNI_ERR;
    }

    LOGI("JNI_OnLoad init cached classes in JniConstants:");
    JniConstants::init(env);

    LOGI("JNI_OnLoad register methods:");
    res = registerJniHelp(env);
    LOGI("registerJniHelp:res=%d", res);
    if (res != 0) {
        LOGE("registerJniHelp failed");
        return JNI_ERR;
    }

    res = register_info_guardianproject_iocipher_File(env);
    LOGI("register_info_guardianproject_iocipher_File:res=%d", res);
    if (res != 0) {
        LOGE("register_info_guardianproject_iocipher_File failed");
        unregisterJniHelp(env);
        return JNI_ERR;
    }

    res = register_info_guardianproject_iocipher_VirtualFileSystem(env);
    LOGI("register_info_guardianproject_iocipher_VirtualFileSystem:res=%d", res);
    if (res != 0) {
        LOGE("register_info_guardianproject_iocipher_VirtualFileSystem failed");
        unregisterJniHelp(env);
        return JNI_ERR;
    }

    res = register_info_guardianproject_libcore_io_Memory(env);
    LOGI("register_info_guardianproject_libcore_io_Memory:res=%d", res);
    if (res != 0) {
        LOGE("register_info_guardianproject_libcore_io_Memory failed");
        unregisterJniHelp(env);
        return JNI_ERR;
    }

    res = register_info_guardianproject_libcore_io_OsConstants(env);
    LOGI("register_info_guardianproject_libcore_io_OsConstants:res=%d", res);
    if (res != 0) {
        LOGE("register_info_guardianproject_libcore_io_OsConstants failed");
        unregisterJniHelp(env);
        return JNI_ERR;
    }

    res = register_info_guardianproject_libcore_io_Posix(env);
    LOGI("register_info_guardianproject_libcore_io_Posix:res=%d", res);
    if (res != 0) {
        LOGE("register_info_guardianproject_libcore_io_Posix failed");
        unregisterJniHelp(env);
        return JNI_ERR;
    }

    LOGI("JNI_OnLoad done");

    return JNI_VERSION_1_6;
}

