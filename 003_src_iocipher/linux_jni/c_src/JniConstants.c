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

#include <jni.h>
#include <stdlib.h>
#include <stdio.h>

#define LOG_TAG "JniConstants"

// Define a structure to hold all the class references
typedef struct {
    jclass bigDecimalClass;
    jclass booleanClass;
    jclass byteArrayClass;
    jclass byteClass;
    jclass constructorClass;
    jclass deflaterClass;
    jclass doubleClass;
    jclass errnoExceptionClass;
    jclass fieldClass;
    jclass fileDescriptorClass;
    jclass inflaterClass;
    jclass integerClass;
    jclass longClass;
    jclass methodClass;
    jclass parsePositionClass;
    jclass patternSyntaxExceptionClass;
    jclass stringArrayClass;
    jclass stringClass;
    jclass structAddrinfoClass;
    jclass structFlockClass;
    jclass structGroupReqClass;
    jclass structLingerClass;
    jclass structPasswdClass;
    jclass structPollfdClass;
    jclass structStatClass;
    jclass structStatFsClass;
    jclass structTimevalClass;
    jclass structUtsnameClass;
} JniConstants;

// Global instance of JniConstants
JniConstants jniConstants;

// Function to find a class and create a global reference
static jclass findClass(JNIEnv* env, const char* name) {
    jclass localClass = (*env)->FindClass(env, name);
    jclass result = (*env)->NewGlobalRef(env, localClass);
    (*env)->DeleteLocalRef(env, localClass);
    if (result == NULL) {
        fprintf(stderr, "failed to find class '%s'\n", name);
        exit(1);
    }
    return result;
}

// Function to initialize all the class references
void JniConstants_init(JNIEnv* env) {
    jniConstants.bigDecimalClass = findClass(env, "java/math/BigDecimal");
    jniConstants.booleanClass = findClass(env, "java/lang/Boolean");
    jniConstants.byteClass = findClass(env, "java/lang/Byte");
    jniConstants.byteArrayClass = findClass(env, "[B");
    jniConstants.constructorClass = findClass(env, "java/lang/reflect/Constructor");
    jniConstants.deflaterClass = findClass(env, "java/util/zip/Deflater");
    jniConstants.doubleClass = findClass(env, "java/lang/Double");
    jniConstants.errnoExceptionClass = findClass(env, "info/guardianproject/libcore/io/ErrnoException");
    jniConstants.fieldClass = findClass(env, "java/lang/reflect/Field");
    jniConstants.fileDescriptorClass = findClass(env, "info/guardianproject/iocipher/FileDescriptor");
    jniConstants.inflaterClass = findClass(env, "java/util/zip/Inflater");
    jniConstants.integerClass = findClass(env, "java/lang/Integer");
    jniConstants.longClass = findClass(env, "java/lang/Long");
    jniConstants.methodClass = findClass(env, "java/lang/reflect/Method");
    jniConstants.parsePositionClass = findClass(env, "java/text/ParsePosition");
    jniConstants.patternSyntaxExceptionClass = findClass(env, "java/util/regex/PatternSyntaxException");
    jniConstants.stringArrayClass = findClass(env, "[Ljava/lang/String;");
    jniConstants.stringClass = findClass(env, "java/lang/String");
    jniConstants.structAddrinfoClass = findClass(env, "info/guardianproject/libcore/io/StructAddrinfo");
    jniConstants.structFlockClass = findClass(env, "info/guardianproject/libcore/io/StructFlock");
    jniConstants.structGroupReqClass = findClass(env, "info/guardianproject/libcore/io/StructGroupReq");
    jniConstants.structLingerClass = findClass(env, "info/guardianproject/libcore/io/StructLinger");
    jniConstants.structPasswdClass = findClass(env, "info/guardianproject/libcore/io/StructPasswd");
    jniConstants.structPollfdClass = findClass(env, "info/guardianproject/libcore/io/StructPollfd");
    jniConstants.structStatClass = findClass(env, "info/guardianproject/libcore/io/StructStat");
    jniConstants.structStatFsClass = findClass(env, "info/guardianproject/libcore/io/StructStatFs");
    jniConstants.structTimevalClass = findClass(env, "info/guardianproject/libcore/io/StructTimeval");
    jniConstants.structUtsnameClass = findClass(env, "info/guardianproject/libcore/io/StructUtsname");
}

