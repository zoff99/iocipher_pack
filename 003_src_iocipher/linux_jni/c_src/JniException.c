#include "JniException.h"
#include "JNIHelp.h"
#include <stdbool.h>
#include <string.h>

bool maybeThrowIcuException(JNIEnv* env, UErrorCode error) {
    if (U_SUCCESS(error)) {
        return false;
    }
    const char* message = u_errorName(error);
    switch (error) {
    case U_ILLEGAL_ARGUMENT_ERROR:
        return jniThrowException(env, "java/lang/IllegalArgumentException", message);
    case U_INDEX_OUTOFBOUNDS_ERROR:
    case U_BUFFER_OVERFLOW_ERROR:
        return jniThrowException(env, "java/lang/ArrayIndexOutOfBoundsException", message);
    case U_UNSUPPORTED_ERROR:
        return jniThrowException(env, "java/lang/UnsupportedOperationException", message);
    default:
        return jniThrowRuntimeException(env, message);
    }
}

void jniThrowExceptionWithErrno(JNIEnv* env, const char* exceptionClassName, int error) {
    char buf[BUFSIZ];
    jniThrowException(env, exceptionClassName, jniStrError(error, buf, sizeof(buf)));
}

void jniThrowOutOfMemoryError(JNIEnv* env, const char* message) {
    jniThrowException(env, "java/lang/OutOfMemoryError", message);
}

void jniThrowSocketException(JNIEnv* env, int error) {
    jniThrowExceptionWithErrno(env, "java/net/SocketException", error);
}

