#! /bin/bash

_HOME_="$(pwd)"
export _HOME_

cd "$_HOME_"

srcdir="./"
incdir="../../libiocipher2-c/src/main/cpp/libsqlfs/"
libsdir1="../../../002_src_libsqlfs/openssl_libs/"
libsdir2="../../../002_src_libsqlfs/"

echo "*** compile ***"

#echo "JAVADIR1------------------"
#find /usr -name "jni.h"
#echo "JAVADIR1------------------"

#echo "JAVADIR2------------------"
#find /usr -name "jni_md.h"
#echo "JAVADIR2------------------"


dirname $(find /usr -name "jni.h" 2>/dev/null|grep -v "libavcodec"|head -1) > /tmp/xx1
dirname $(find /usr -name "jni_md.h" 2>/dev/null|head -1) > /tmp/xx2
export JAVADIR1=$(cat /tmp/xx1)
export JAVADIR2=$(cat /tmp/xx2)
echo "JAVADIR1:""$JAVADIR1"
echo "JAVADIR2:""$JAVADIR2"

export CFLAGS=" -DHAVE_LIBSQLCIPHER -fPIC -O3 -g -std=gnu99 -fstack-protector-all -D_FORTIFY_SOURCE=2 "
export CXXFLAGS=" -DHAVE_LIBSQLCIPHER -fPIC -O3 -g -fstack-protector-all -D_FORTIFY_SOURCE=2 "

# -fsanitize=address -fno-omit-frame-pointer -fsanitize-recover=address \

gcc -c $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/readlink.c -o readlink.o || exit 1
gcc -c $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/realpath.c -o realpath.o || exit 1
gcc -c $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/JNIHelp.c -o JNIHelp.o || exit 1
gcc -c $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/JNI_OnLoad.c -o JNI_OnLoad.o || exit 1

gcc -c $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/info_guardianproject_libcore_io_OsConstants.c -o info_guardianproject_libcore_io_OsConstants.o || exit 1
gcc -c $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/info_guardianproject_iocipher_VirtualFileSystem.c -o info_guardianproject_iocipher_VirtualFileSystem.o || exit 1
gcc -c $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/info_guardianproject_iocipher_File.c -o info_guardianproject_iocipher_File.o || exit 1
gcc -c $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/info_guardianproject_libcore_io_Posix.c -o info_guardianproject_libcore_io_Posix.o || exit 1
gcc -c $CFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ -I"$incdir"/ "$srcdir"/info_guardianproject_libcore_io_Memory.c -o info_guardianproject_libcore_io_Memory.o || exit 1

# -fsanitize=address -fno-omit-frame-pointer -fsanitize-recover=address -static-libasan \

gcc $CFLAGS \
-D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 \
-fsanitize=address -fno-omit-frame-pointer -fsanitize-recover=address -static-libasan \
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
"$libsdir2"/libsqlfs.a \
"$libsdir2"/sqlite3.a \
"$libsdir1"/libcrypto.a \
"$libsdir1"/libssl.a \
-shared \
-Wl,-soname,libiocipher2.so -o libiocipher2.so || exit 1

ld libiocipher2.so || echo "!!linker missing some symbols!!"

ls -al libiocipher2.so || exit 1
pwd
file libiocipher2.so

## java part ##

javacomp=javac

"$javacomp" ./info/guardianproject/iocipher/FilenameFilter.java ./info/guardianproject/iocipher/VirtualFileSystem.java ./info/guardianproject/iocipher/File.java ./info/guardianproject/iocipher/RandomAccessFile.java ./info/guardianproject/iocipher/IOCipherFileChannel.java ./info/guardianproject/iocipher/FileReader.java ./info/guardianproject/iocipher/FileInputStream.java ./info/guardianproject/iocipher/FileOutputStream.java ./info/guardianproject/iocipher/FileWriter.java ./info/guardianproject/iocipher/FileFilter.java ./info/guardianproject/iocipher/FileDescriptor.java ./info/guardianproject/libcore/io/StructStat.java ./info/guardianproject/libcore/io/ErrnoException.java ./info/guardianproject/libcore/io/SizeOf.java ./info/guardianproject/libcore/io/StructStatFs.java ./info/guardianproject/libcore/io/Memory.java ./info/guardianproject/libcore/io/StructPasswd.java ./info/guardianproject/libcore/io/Libcore.java ./info/guardianproject/libcore/io/StructUtsname.java ./info/guardianproject/libcore/io/StructAddrinfo.java ./info/guardianproject/libcore/io/StructPollfd.java ./info/guardianproject/libcore/io/StructTimeval.java ./info/guardianproject/libcore/io/OsConstants.java ./info/guardianproject/libcore/io/StructLinger.java ./info/guardianproject/libcore/io/StructGroupReq.java ./info/guardianproject/libcore/io/Posix.java ./info/guardianproject/libcore/io/IoUtils.java ./info/guardianproject/libcore/io/StructFlock.java ./info/guardianproject/libcore/io/IoBridge.java ./info/guardianproject/libcore/io/Os.java || exit 1

## test program ##

"$javacomp" com/example/iociphertest.java com/example/Log.java || exit 1

## run the test program ##

export ASAN_OPTIONS=halt_on_error=false
LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libasan.so.8.0.0 \
java -cp . -Djava.library.path=$(pwd) com.example.iociphertest




