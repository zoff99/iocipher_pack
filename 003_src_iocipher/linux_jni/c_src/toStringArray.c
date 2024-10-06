#include "JniConstants.h"
#include "toStringArray.h"

#include <string.h>
#include <stdlib.h>

struct VectorCounter {
    const char** strings;
    size_t count;
};

void VectorCounter_init(struct VectorCounter* counter, const char** strings) {
    counter->strings = strings;
    counter->count = 0;
    while (strings[counter->count] != NULL) {
        ++counter->count;
    }
}

size_t VectorCounter_count(struct VectorCounter* counter) {
    return counter->count;
}

struct VectorGetter {
    const char** strings;
};

void VectorGetter_init(struct VectorGetter* getter, const char** strings) {
    getter->strings = strings;
}

const char* VectorGetter_get(struct VectorGetter* getter, size_t i) {
    return getter->strings[i];
}

jobjectArray toStringArray(JNIEnv* env, const char* const* strings) {
    struct VectorCounter counter;
    VectorCounter_init(&counter, strings);
    struct VectorGetter getter;
    VectorGetter_init(&getter, strings);
    return toStringArray(env, &counter, &getter);
}

