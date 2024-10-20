#! /bin/bash

_HOME2_=$(dirname "$0")
export _HOME2_
_HOME_=$(cd "$_HOME2_" || exit;pwd)
export _HOME_

cd "$_HOME_"

logfile="$_HOME_""/compile.win.log"
rm -f "$logfile"

srcdir="./"
incdir="../../libiocipher2-c/src/main/cpp/libsqlfs/"
libsdir1="../../../002_src_libsqlfs/openssl_win64_libs/"
libsdir2="../../../002_src_libsqlfs/"

if [ "$1""x" != "testx" ]; then

    echo "*** compile ***"

    #echo "JAVADIR1------------------"
    #find /usr -name "jni.h"
    #echo "JAVADIR1------------------"

    #echo "JAVADIR2------------------"
    #find /usr -name "jni_md.h"
    #echo "JAVADIR2------------------"


    dirname $(find /usr -name "jni.h" 2>/dev/null|grep -v "android"|grep -v "libavcodec"|head -1) > /tmp/xx1
    dirname $(find /usr -name "jni_md.h" 2>/dev/null|grep -v "android"|head -1) > /tmp/xx2
    export JAVADIR1=$(cat /tmp/xx1)
    export JAVADIR2=$(cat /tmp/xx2)
    echo "JAVADIR1:""$JAVADIR1"
    echo "JAVADIR2:""$JAVADIR2"

    export CFLAGS=" -DHAVE_LIBSQLCIPHER -fPIC -O2 -ggdb3 -std=gnu99 -fstack-protector-all "
    export CXXFLAGS=" -DHAVE_LIBSQLCIPHER -fPIC -O2 -ggdb3 -fstack-protector-all "

    export ASAN_FLAGS=""

    x86_64-w64-mingw32-gcc-win32 -c $ASAN_FLAGS $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/readlink.c -o readlink.o >> "$logfile" 2>&1 || exit 1
    x86_64-w64-mingw32-gcc-win32 -c $ASAN_FLAGS $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/realpath.c -o realpath.o >> "$logfile" 2>&1 || exit 1
    x86_64-w64-mingw32-gcc-win32 -c $ASAN_FLAGS $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/JNIHelp.c -o JNIHelp.o >> "$logfile" 2>&1 || exit 1
    x86_64-w64-mingw32-gcc-win32 -c $ASAN_FLAGS $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/JNI_OnLoad.c -o JNI_OnLoad.o >> "$logfile" 2>&1 || exit 1

    x86_64-w64-mingw32-gcc-win32 -c $ASAN_FLAGS $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/info_guardianproject_libcore_io_OsConstants.c -o info_guardianproject_libcore_io_OsConstants.o >> "$logfile" 2>&1 || exit 1
    x86_64-w64-mingw32-gcc-win32 -c $ASAN_FLAGS $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/info_guardianproject_iocipher_VirtualFileSystem.c -o info_guardianproject_iocipher_VirtualFileSystem.o >> "$logfile" 2>&1 || exit 1
    x86_64-w64-mingw32-gcc-win32 -c $ASAN_FLAGS $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/info_guardianproject_iocipher_File.c -o info_guardianproject_iocipher_File.o >> "$logfile" 2>&1 || exit 1
    x86_64-w64-mingw32-gcc-win32 -c $ASAN_FLAGS $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/info_guardianproject_libcore_io_Posix.c -o info_guardianproject_libcore_io_Posix.o >> "$logfile" 2>&1 || exit 1
    x86_64-w64-mingw32-gcc-win32 -c $ASAN_FLAGS $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/info_guardianproject_libcore_io_Memory.c -o info_guardianproject_libcore_io_Memory.o >> "$logfile" 2>&1 || exit 1

    # rm -fv iocipher2.dll

    x86_64-w64-mingw32-gcc-win32 $CFLAGS \
    -Wall -D_JNI_IMPLEMENTATION_ -Wl,-kill-at \
    $CFLAGS \
    -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 \
    $ASAN_FLAGS \
    -I$JAVADIR1/ \
    -I$JAVADIR2/ \
    readlink.o \
    realpath.o \
    JNIHelp.o \
    JNI_OnLoad.o \
    info_guardianproject_libcore_io_OsConstants.o \
    info_guardianproject_iocipher_VirtualFileSystem.o \
    info_guardianproject_iocipher_File.o \
    info_guardianproject_libcore_io_Posix.o \
    info_guardianproject_libcore_io_Memory.o \
    "$libsdir2"/libsqlfs_win64.a \
    "$libsdir2"/sqlite3_win64.a \
    "$libsdir1"/libcrypto.a \
    "$libsdir1"/libssl.a \
    -l:libiphlpapi.a \
    -Wl,-Bstatic -lcrypt32 \
    -Wl,-Bstatic -lws2_32 \
    -lm \
    -shared \
    -lpthread \
    -o iocipher2.dll >> "$logfile" 2>&1 || exit 1


    ls -al iocipher2.dll >> "$logfile" 2>&1 || exit 1
    pwd
    file iocipher2.dll

fi

echo "*** version ***"
cur_str_version=$(cat ./info/guardianproject/iocipher/VirtualFileSystem.java|grep 'static String IOCIPHER_JNI_VERSION'|sed -e 's#^.*static String IOCIPHER_JNI_VERSION = "##'|sed -e 's#".*$##')

echo "*** insert windows dll into existing jar ***"

tmpdir="tmp_extr/"

rm -Rf "$tmpdir" || exit 1
mkdir -p "$tmpdir"
mv ./iocipher_linux-"$cur_str_version".jar "$tmpdir" || exit 1
cd "$tmpdir" || exit 1
jar -xf iocipher_linux-"$cur_str_version".jar || exit 1
rm -f iocipher_linux-"$cur_str_version".jar || exit 1
mkdir -p jnilibs/win_x64/
cp -v ../iocipher2.dll jnilibs/win_x64/ || exit 1
jar -cf iocipher_linux-"$cur_str_version".jar jnilibs com info || exit 1
mv iocipher_linux-"$cur_str_version".jar ../ || exit 1
cd ../ || exit 1
rm -Rf "$tmpdir" || exit 1

