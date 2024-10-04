#! /bin/bash

_HOME_="$(pwd)"
export _HOME_

cd "$_HOME_"

srcdir="../libiocipher2-c/src/main/cpp/"
incdir="$srcdir""/libsqlfs/"

echo "*** compile ***"

echo "JAVADIR1------------------"
find /usr -name "jni.h"
echo "JAVADIR1------------------"

echo "JAVADIR2------------------"
find /usr -name "jni_md.h"
echo "JAVADIR2------------------"


dirname $(find /usr -name "jni.h" 2>/dev/null|grep -v "libavcodec"|head -1) > /tmp/xx1
dirname $(find /usr -name "jni_md.h" 2>/dev/null|head -1) > /tmp/xx2
export JAVADIR1=$(cat /tmp/xx1)
export JAVADIR2=$(cat /tmp/xx2)
echo "JAVADIR1:""$JAVADIR1"
echo "JAVADIR2:""$JAVADIR2"

export CFLAGS=" -DHAVE_LIBSQLCIPHER -fPIC -O3 -g -std=gnu99 -fstack-protector-all -D_FORTIFY_SOURCE=2 "
export CXXFLAGS=" -DHAVE_LIBSQLCIPHER -fPIC -O3 -g -fstack-protector-all -D_FORTIFY_SOURCE=2 "

# -fsanitize=address -fno-omit-frame-pointer -fsanitize-recover=address \

g++ -c $CXXFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I"$incdir"/ "$srcdir"/readlink.cpp -o readlink.o || exit 1
g++ -c $CXXFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I"$incdir"/ "$srcdir"/realpath.cpp -o realpath.o || exit 1
g++ -c $CXXFLAGS -DJAVA_LINUX -D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 -I"$JAVADIR1"/ -I"$JAVADIR2"/ -I./ ./JNIHelp.cpp -o JNIHelp.o || exit 1

exit 1

"$srcdir"/JniConstants.cpp \
"$srcdir"/JNIHelp.cpp \
"$srcdir"/JNI_OnLoad.cpp \
"$srcdir"/readlink.cpp \
"$srcdir"/realpath.cpp \
"$srcdir"/toStringArray.cpp \
"$srcdir"/info_guardianproject_iocipher_File.cpp \
"$srcdir"/info_guardianproject_libcore_io_Memory.cpp \
"$srcdir"/info_guardianproject_libcore_io_OsConstants.cpp \
"$srcdir"/info_guardianproject_libcore_io_Posix.cpp \
"$srcdir"/info_guardianproject_iocipher_VirtualFileSystem.cpp \


ls -al libjni_notifications.so || exit 1
pwd
file libjni_notifications.so

