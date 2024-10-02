#! /bin/bash

_HOME2_=$(dirname "$0")
export _HOME2_
_HOME_=$(cd "$_HOME2_" || exit;pwd)
export _HOME_

logfile="$_HOME_""/compile.log"

cd "$_HOME_""/"
cd ./001_src_openssl/.localrun/ || exit 1

rm -f "$logfile"

echo "build/upate docker container"
./docker_linux_fast.sh build >> "$logfile" 2>&1 || exit 1

echo "build openssl for android and linux"
./docker_linux_fast.sh >> "$logfile" 2>&1 || exit 1


cd "$_HOME_""/"
## rm -Rf ./002_src_libsqlfs/openssl_includes/ >> "$logfile" 2>&1
mkdir -p ./002_src_libsqlfs/openssl_includes/ >> "$logfile" 2>&1
echo "install openssl includes"
cp -av ./001_src_openssl/.localrun/debian_12_linux/artefacts/linux_debian12_x86_64/include/* ./002_src_libsqlfs/openssl_includes/ >> "$logfile" 2>&1 || exit 1

echo "install openssl linux libs"
cp -av ./001_src_openssl/.localrun/debian_12_linux/artefacts/linux_debian12_x86_64/lib*.a ./002_src_libsqlfs/openssl_libs/ >> "$logfile" 2>&1 || exit 1

echo "install openssl android libs"
cp -av ./001_src_openssl/.localrun/debian_12_linux/artefacts/android-arm/lib*.a ./003_src_iocipher/libiocipher2-c/src/main/jniLibs/armeabi-v7a/ >> "$logfile" 2>&1 || exit 1
cp -av ./001_src_openssl/.localrun/debian_12_linux/artefacts/android-arm64/lib*.a ./003_src_iocipher/libiocipher2-c/src/main/jniLibs/arm64-v8a/ >> "$logfile" 2>&1 || exit 1
cp -av ./001_src_openssl/.localrun/debian_12_linux/artefacts/android-x86/lib*.a ./003_src_iocipher/libiocipher2-c/src/main/jniLibs/x86/ >> "$logfile" 2>&1 || exit 1
cp -av ./001_src_openssl/.localrun/debian_12_linux/artefacts/android-x86_64/lib*.a ./003_src_iocipher/libiocipher2-c/src/main/jniLibs/x86_64/ >> "$logfile" 2>&1 || exit 1


cd "$_HOME_""/"
cd ./002_src_libsqlfs/ || exit 1

echo "build sqlfs and sqlcipher for linux"
make -j2 >> "$logfile" 2>&1 || exit 1

cd "$_HOME_""/"
cd ./002_src_libsqlfs/.localrun/ || exit 1

echo "build/upate docker container"
./docker_linux_fast.sh build >> "$logfile" 2>&1 || exit 1

echo "build sqlfs and sqlcipher for android"
./docker_linux_fast.sh >> "$logfile" 2>&1 || exit 1


echo "install sqlfs and sqlcipher android libs"
cd "$_HOME_""/"
cp -av ./002_src_libsqlfs/.localrun/debian_12_linux/artefacts/android-arm/*.a ./003_src_iocipher/libiocipher2-c/src/main/jniLibs/armeabi-v7a/ >> "$logfile" 2>&1 || exit 1
cp -av ./002_src_libsqlfs/.localrun/debian_12_linux/artefacts/android-arm64/*.a ./003_src_iocipher/libiocipher2-c/src/main/jniLibs/arm64-v8a/ >> "$logfile" 2>&1 || exit 1
cp -av ./002_src_libsqlfs/.localrun/debian_12_linux/artefacts/android-x86/*.a ./003_src_iocipher/libiocipher2-c/src/main/jniLibs/x86/ >> "$logfile" 2>&1 || exit 1
cp -av ./002_src_libsqlfs/.localrun/debian_12_linux/artefacts/android-x86_64/*.a ./003_src_iocipher/libiocipher2-c/src/main/jniLibs/x86_64/ >> "$logfile" 2>&1 || exit 1

echo "install sqlfs android includes"
cd "$_HOME_""/"
cp -av ./002_src_libsqlfs/sqlfs.h ./003_src_iocipher/libiocipher2-c/src/main/cpp/libsqlfs/ >> "$logfile" 2>&1 || exit 1
cp -av ./002_src_libsqlfs/sqlfs_internal.h ./003_src_iocipher/libiocipher2-c/src/main/cpp/libsqlfs/ >> "$logfile" 2>&1 || exit 1

cd "$_HOME_""/"
cd ./003_src_iocipher/ || exit 1

echo "build iocipher for android"
./gradlew build >> "$logfile" 2>&1 || exit 1
./gradlew :libiocipher2-c:assemble >> "$logfile" 2>&1 || exit 1

echo "====== build: OK ======"

