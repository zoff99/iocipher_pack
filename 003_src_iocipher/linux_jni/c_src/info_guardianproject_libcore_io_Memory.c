
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>

#ifdef __MINGW32__


#ifdef HAVE_SYS_PARAM_H
#include <sys/param.h>
#endif

#ifndef __bswap_constant_16

/* Swap bytes in 16 bit value.  */
#define __bswap_constant_16(x) \
     ((((x) >> 8) & 0xffu) | (((x) & 0xffu) << 8))

#ifdef __GNUC__
# define __bswap_16(x) \
    (__extension__							      \
     ({ unsigned short int __bsx = (x); __bswap_constant_16 (__bsx); }))
#else
static __inline unsigned short int
__bswap_16 (unsigned short int __bsx)
{
  return __bswap_constant_16 (__bsx);
}
#endif

/* Swap bytes in 32 bit value.  */
#define __bswap_constant_32(x) \
     ((((x) & 0xff000000u) >> 24) | (((x) & 0x00ff0000u) >>  8) |	      \
      (((x) & 0x0000ff00u) <<  8) | (((x) & 0x000000ffu) << 24))

#ifdef __GNUC__
# define __bswap_32(x) \
  (__extension__							      \
   ({ register unsigned int __bsx = (x); __bswap_constant_32 (__bsx); }))
#else
static __inline unsigned int
__bswap_32 (unsigned int __bsx)
{
  return __bswap_constant_32 (__bsx);
}
#endif

#if defined __GNUC__ && __GNUC__ >= 2
/* Swap bytes in 64 bit value.  */
# define __bswap_constant_64(x) \
     ((((x) & 0xff00000000000000ull) >> 56)				      \
      | (((x) & 0x00ff000000000000ull) >> 40)				      \
      | (((x) & 0x0000ff0000000000ull) >> 24)				      \
      | (((x) & 0x000000ff00000000ull) >> 8)				      \
      | (((x) & 0x00000000ff000000ull) << 8)				      \
      | (((x) & 0x0000000000ff0000ull) << 24)				      \
      | (((x) & 0x000000000000ff00ull) << 40)				      \
      | (((x) & 0x00000000000000ffull) << 56))

# define __bswap_64(x) \
     (__extension__							      \
      ({ union { __extension__ unsigned long long int __ll;		      \
		 unsigned int __l[2]; } __w, __r;			      \
         if (__builtin_constant_p (x))					      \
	   __r.__ll = __bswap_constant_64 (x);				      \
	 else								      \
	   {								      \
	     __w.__ll = (x);						      \
	     __r.__l[0] = __bswap_32 (__w.__l[1]);			      \
	     __r.__l[1] = __bswap_32 (__w.__l[0]);			      \
	   }								      \
	 __r.__ll; }))
#endif

#ifndef HAVE_NTOHL
#undef	htonl
#undef	ntohl
static inline uint32_t
htonl (x)
     uint32_t x;
{
  return __bswap_32 (x);
}
#define ntohl htonl
#endif


#ifndef HAVE_NTOHS
#undef	htons
#undef	ntohs
static inline uint16_t
htons (x)
     uint16_t x;
{
  return __bswap_16 (x);
}
#define ntohs htons
#endif

#endif

#define bswap_16 __bswap_16
#define bswap_32 __bswap_32

#else
# include <byteswap.h>
#endif

#include <jni.h>
#include "JNIHelp.h"

#define LOG_TAG "Memory"

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wincompatible-pointer-types"
#pragma GCC diagnostic ignored "-Wint-conversion"

static inline jshort get_unaligned_jshort(const jshort* address) {
    struct unaligned { jshort v; } __attribute__ ((packed));
    const struct unaligned* p = (const struct unaligned*)(address);
    return p->v;
}

static inline void put_unaligned_jshort(jshort* address, jshort v) {
    struct unaligned { jshort v; } __attribute__ ((packed));
    struct unaligned* p = (struct unaligned*)(address);
    p->v = v;
}

static inline jint get_unaligned_jint(const jint* address) {
    struct unaligned { jint v; } __attribute__ ((packed));
    const struct unaligned* p = (const struct unaligned*)(address);
    return p->v;
}

static inline void put_unaligned_jint(jint* address, jint v) {
    struct unaligned { jint v; } __attribute__ ((packed));
    struct unaligned* p = (struct unaligned*)(address);
    p->v = v;
}

static inline int get_unaligned_int(const int* address) {
    struct unaligned { int v; } __attribute__ ((packed));
    const struct unaligned* p = (const struct unaligned*)(address);
    return p->v;
}

static inline void put_unaligned_int(int* address, int v) {
    struct unaligned { int v; } __attribute__ ((packed));
    struct unaligned* p = (struct unaligned*)(address);
    p->v = v;
}

static inline jlong get_unaligned_jlong(const jlong* address) {
    struct unaligned { jlong v; } __attribute__ ((packed));
    const struct unaligned* p = (const struct unaligned*)(address);
    return p->v;
}

static inline void put_unaligned_jlong(jlong* address, jlong v) {
    struct unaligned { jlong v; } __attribute__ ((packed));
    struct unaligned* p = (struct unaligned*)(address);
    p->v = v;
}

// Byte-swap 2 jshort values packed in a jint.
static inline jint bswap_2x16(jint v) {
    // v is initially ABCD
    v = bswap_32(v);                              // v=DCBA
#if defined(__mips__) && defined(__mips_isa_rev) && (__mips_isa_rev >= 2)
    __asm__ volatile ("wsbh %0, %0" : "+r" (v));  // v=BADC
#else
    v = (v << 16) | ((v >> 16) & 0xffff);         // v=BADC
#endif
    return v;
}

static inline void swapShorts(jshort* dstShorts, const jshort* srcShorts, size_t count) {
    jint* dst = (jint*)dstShorts;
    const jint* src = (const jint*)srcShorts;
    for (size_t i = 0; i < count / 2; ++i) {
        jint v = get_unaligned_jint(src++);
        put_unaligned_jint(dst++, bswap_2x16(v));
    }
    if ((count % 2) != 0) {
        jshort v = get_unaligned_jshort((const jshort*)src);
        put_unaligned_jshort((jshort*)dst, bswap_16(v));
    }
}

static inline void swapInts(jint* dstInts, const jint* srcInts, size_t count) {
    for (size_t i = 0; i < count; ++i) {
        jint v = get_unaligned_int(srcInts++);
        put_unaligned_jint(dstInts++, bswap_32(v));
    }
}

static inline void swapLongs(jlong* dstLongs, const jlong* srcLongs, size_t count) {
    jint* dst = (jint*)dstLongs;
    const jint* src = (const jint*)srcLongs;
    for (size_t i = 0; i < count; ++i) {
        jint v1 = get_unaligned_jint(src++);
        jint v2 = get_unaligned_jint(src++);
        put_unaligned_jint(dst++, bswap_32(v2));
        put_unaligned_jint(dst++, bswap_32(v1));
    }
}

static void Memory_memmove(jobject dstObject, jint dstOffset, jobject srcObject, jint srcOffset, jlong length) {
    // Implementation of memory move
}

static jbyte Memory_peekByte(jlong srcAddress) {
    return *(jbyte*)srcAddress;
}

static void Memory_peekByteArray(jlong srcAddress, jbyte* dst, jint dstOffset, jint byteCount) {
    memcpy(dst + dstOffset, (const jbyte*)srcAddress, byteCount);
}

#define PEEKER(SCALAR_TYPE, SWAP_TYPE, SWAP_FN) { \
    if (swap) { \
        SWAP_FN((SWAP_TYPE*)(dst + dstOffset), (const SWAP_TYPE*)srcAddress, count); \
    } else { \
        memcpy(dst + dstOffset, (const SCALAR_TYPE*)srcAddress, count * sizeof(SCALAR_TYPE)); \
    } \
}

static void Memory_peekCharArray(jlong srcAddress, jshort* dst, jint dstOffset, jint count, jboolean swap) {
    PEEKER(jshort, jshort, swapShorts);
}

static void Memory_peekDoubleArray(jlong srcAddress, double* dst, jint dstOffset, jint count, jboolean swap) {
    PEEKER(double, jlong, swapLongs);
}

static void Memory_peekFloatArray(jlong srcAddress, float* dst, jint dstOffset, jint count, jboolean swap) {
    PEEKER(float, jint, swapInts);
}

static void Memory_peekIntArray(jlong srcAddress, jint* dst, jint dstOffset, jint count, jboolean swap) {
    PEEKER(jint, jint, swapInts);
}

static void Memory_peekLongArray(jlong srcAddress, jlong* dst, jint dstOffset, jint count, jboolean swap) {
    PEEKER(jlong, jlong, swapLongs);
}

static void Memory_peekShortArray(jlong srcAddress, jshort* dst, jint dstOffset, jint count, jboolean swap) {
    PEEKER(jshort, jshort, swapShorts);
}

static void Memory_pokeByte(jlong dstAddress, jbyte value) {
    *(jbyte*)dstAddress = value;
}

static void Memory_pokeByteArray(jlong dstAddress, jbyte* src, jint offset, jint length) {
    memcpy((jbyte*)dstAddress, src + offset, length);
}

#define POKER(SCALAR_TYPE, SWAP_TYPE, SWAP_FN) { \
    if (swap) { \
        SWAP_FN((SWAP_TYPE*)dstAddress, (const SWAP_TYPE*)(src + srcOffset), count); \
    } else { \
        memcpy(dstAddress, (const SCALAR_TYPE*)(src + srcOffset), count * sizeof(SCALAR_TYPE)); \
    } \
}

static void Memory_pokeCharArray(jlong dstAddress, jshort* src, jint srcOffset, jint count, jboolean swap) {
    POKER(jshort, jshort, swapShorts);
}

static void Memory_pokeDoubleArray(jlong dstAddress, double* src, jint srcOffset, jint count, jboolean swap) {
    POKER(double, jlong, swapLongs);
}

static void Memory_pokeFloatArray(jlong dstAddress, float* src, jint srcOffset, jint count, jboolean swap) {
    POKER(float, jint, swapInts);
}

static void Memory_pokeIntArray(jlong dstAddress, jint* src, jint srcOffset, jint count, jboolean swap) {
    POKER(jint, jint, swapInts);
}

static void Memory_pokeLongArray(jlong dstAddress, jlong* src, jint srcOffset, jint count, jboolean swap) {
    POKER(jlong, jlong, swapLongs);
}

static void Memory_pokeShortArray(jlong dstAddress, jshort* src, jint srcOffset, jint count, jboolean swap) {
    POKER(jshort, jshort, swapShorts);
}

static jshort Memory_peekShortNative(jlong srcAddress) {
    return get_unaligned_jshort((const jshort*)srcAddress);
}

static void Memory_pokeShortNative(jlong dstAddress, jshort value) {
    put_unaligned_jshort((jshort*)dstAddress, value);
}

static jint Memory_peekIntNative(jlong srcAddress) {
    return get_unaligned_jint((const jint*)srcAddress);
}

static void Memory_pokeIntNative(jlong dstAddress, jint value) {
    put_unaligned_jint((jint*)dstAddress, value);
}

static jlong Memory_peekLongNative(jlong srcAddress) {
    return get_unaligned_jlong((const jlong*)srcAddress);
}

static void Memory_pokeLongNative(jlong dstAddress, jlong value) {
    put_unaligned_jlong((jlong*)dstAddress, value);
}

static void unsafeBulkCopy(jbyte* dst, const jbyte* src, jint byteCount, jint sizeofElement, jboolean swap) {
    if (!swap) {
        memcpy(dst, src, byteCount);
        return;
    }

    if (sizeofElement == 2) {
        jshort* dstShorts = (jshort*)dst;
        const jshort* srcShorts = (const jshort*)src;
        swapShorts(dstShorts, srcShorts, byteCount / 2);
    } else if (sizeofElement == 4) {
        jint* dstInts = (jint*)dst;
        const jint* srcInts = (const jint*)src;
        swapInts(dstInts, srcInts, byteCount / 4);
    } else if (sizeofElement == 8) {
        jlong* dstLongs = (jlong*)dst;
        const jlong* srcLongs = (const jlong*)src;
        swapLongs(dstLongs, srcLongs, byteCount / 8);
    }
}

static void Memory_unsafeBulkGet(jobject dstObject, jint dstOffset, jint byteCount, jbyte* srcArray, jint srcOffset, jint sizeofElement, jboolean swap) {
    jbyte* dstBytes = (jbyte*)dstObject; // Assuming dstObject is a byte array
    jbyte* dst = dstBytes + dstOffset * sizeofElement;
    const jbyte* src = srcArray + srcOffset;
    unsafeBulkCopy(dst, src, byteCount, sizeofElement, swap);
}

static void Memory_unsafeBulkPut(jbyte* dstArray, jint dstOffset, jint byteCount, jobject srcObject, jint srcOffset, jint sizeofElement, jboolean swap) {
    jbyte* srcBytes = (jbyte*)srcObject; // Assuming srcObject is a byte array
    jbyte* dst = dstArray + dstOffset;
    const jbyte* src = srcBytes + srcOffset * sizeofElement;
    unsafeBulkCopy(dst, src, byteCount, sizeofElement, swap);
}

static JNINativeMethod sMethods[] = {
    {"memmove", "(Ljava/lang/Object;ILjava/lang/Object;IJ)V", (void *)Memory_memmove},
    {"peekByte", "(J)B", (void *)Memory_peekByte},
    {"peekByteArray", "(J[BII)V", (void *)Memory_peekByteArray},
    {"peekCharArray", "(J[CIIZ)V", (void *)Memory_peekCharArray},
    {"peekDoubleArray", "(J[DIIZ)V", (void *)Memory_peekDoubleArray},
    {"peekFloatArray", "(J[FIIZ)V", (void *)Memory_peekFloatArray},
    {"peekIntNative", "(J)I", (void *)Memory_peekIntNative},
    {"peekIntArray", "(J[IIIZ)V", (void *)Memory_peekIntArray},
    {"peekLongNative", "(J)J", (void *)Memory_peekLongNative},
    {"peekLongArray", "(J[JIIZ)V", (void *)Memory_peekLongArray},
    {"peekShortNative", "(J)S", (void *)Memory_peekShortNative},
    {"peekShortArray", "(J[SIIZ)V", (void *)Memory_peekShortArray},
    {"pokeByte", "(JB)V", (void *)Memory_pokeByte},
    {"pokeByteArray", "(J[BII)V", (void *)Memory_pokeByteArray},
    {"pokeCharArray", "(J[CIIZ)V", (void *)Memory_pokeCharArray},
    {"pokeDoubleArray", "(J[DIIZ)V", (void *)Memory_pokeDoubleArray},
    {"pokeFloatArray", "(J[FIIZ)V", (void *)Memory_pokeFloatArray},
    {"pokeIntNative", "(JI)V", (void *)Memory_pokeIntNative},
    {"pokeIntArray", "(J[IIIZ)V", (void *)Memory_pokeIntArray},
    {"pokeLongNative", "(JJ)V", (void *)Memory_pokeLongNative},
    {"pokeLongArray", "(J[JIIZ)V", (void *)Memory_pokeLongArray},
    {"pokeShortNative", "(JS)V", (void *)Memory_pokeShortNative},
    {"pokeShortArray", "(J[SIIZ)V", (void *)Memory_pokeShortArray},
    {"unsafeBulkGet", "(Ljava/lang/Object;II[BIIZ)V", (void *)Memory_unsafeBulkGet},
    {"unsafeBulkPut", "([BIILjava/lang/Object;IIZ)V", (void *)Memory_unsafeBulkPut},
};

int register_info_guardianproject_libcore_io_Memory(JNIEnv* env) {
    jclass cls = (*env)->FindClass(env, "info/guardianproject/libcore/io/Memory");
    if (cls == NULL) {
        LOGE("Can't find info/guardianproject/libcore/io/Memory\n");
        return -1;
    }
    return (*env)->RegisterNatives(env, cls, sMethods, sizeof(sMethods)/sizeof(sMethods[0]));
}

#pragma GCC diagnostic pop

