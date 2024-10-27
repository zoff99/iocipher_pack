#! /bin/bash

_HOME2_=$(dirname "$0")
export _HOME2_
_HOME_=$(cd "$_HOME2_" || exit;pwd)
export _HOME_

logfile="$_HOME_""/compile.log"

build_openssl=1
build_sqlfs=1
if [ "$1""x" == "aarx" ]; then
    echo "====== build type: only AAR ======"
    build_openssl=0
    build_sqlfs=0
fi
if [ "$1""x" == "sqlfsx" ]; then
    echo "====== build type: sqlfs and AAR ======"
    build_openssl=0
    build_sqlfs=1
fi
if [ "$build_openssl""x" == "1x" ] && [ "$build_sqlfs""x" == "1x" ]; then
    echo "====== build type: full ======"
fi

cd "$_HOME_""/"
rm -f "$logfile"

if [ "$build_openssl""x" == "1x" ]; then
    cd ./001_src_openssl/.localrun/ || exit 1
    echo "build/upate docker container for openssl"
    ./docker_linux_fast.sh build >> "$logfile" 2>&1 || exit 1
    echo "build openssl for android and linux"
    ./docker_linux_fast.sh >> "$logfile" 2>&1 || exit 1
    echo "build openssl for windows x64"
    ./docker_win_fast.sh >> "$logfile" 2>&1 || exit 1
fi

if [ "$build_sqlfs""x" == "1x" ]; then
    cd "$_HOME_""/"
    ## maybe clean, in case a headerfile gets removed in th future? ## rm -Rf ./002_src_libsqlfs/openssl_includes/ >> "$logfile" 2>&1
    mkdir -p ./002_src_libsqlfs/openssl_includes/ >> "$logfile" 2>&1
    echo "install openssl includes"
    cp -av ./001_src_openssl/.localrun/debian_12_linux/artefacts/linux_debian12_x86_64/include/* ./002_src_libsqlfs/openssl_includes/ >> "$logfile" 2>&1 || exit 1
    echo "install openssl linux libs"
    cp -av ./001_src_openssl/.localrun/debian_12_linux/artefacts/linux_debian12_x86_64/lib*.a ./002_src_libsqlfs/openssl_libs/ >> "$logfile" 2>&1 || exit 1
    echo "install openssl windows libs"
    mkdir -p ./002_src_libsqlfs/openssl_win64_libs/ >> "$logfile" 2>&1
    cp -av ./001_src_openssl/.localrun/debian_12_win64/artefacts/win_x86_64/lib*.a ./002_src_libsqlfs/openssl_win64_libs/ >> "$logfile" 2>&1 || exit 1
    echo "install openssl android libs"
    cp -av ./001_src_openssl/.localrun/debian_12_linux/artefacts/android-arm/lib*.a ./003_src_iocipher/libiocipher2-c/src/main/jniLibs/armeabi-v7a/ >> "$logfile" 2>&1 || exit 1
    cp -av ./001_src_openssl/.localrun/debian_12_linux/artefacts/android-arm64/lib*.a ./003_src_iocipher/libiocipher2-c/src/main/jniLibs/arm64-v8a/ >> "$logfile" 2>&1 || exit 1
    cp -av ./001_src_openssl/.localrun/debian_12_linux/artefacts/android-x86/lib*.a ./003_src_iocipher/libiocipher2-c/src/main/jniLibs/x86/ >> "$logfile" 2>&1 || exit 1
    cp -av ./001_src_openssl/.localrun/debian_12_linux/artefacts/android-x86_64/lib*.a ./003_src_iocipher/libiocipher2-c/src/main/jniLibs/x86_64/ >> "$logfile" 2>&1 || exit 1

    cd "$_HOME_""/"
    cd ./002_src_libsqlfs/ || exit 1
    echo "build sqlfs and sqlcipher for linux"
    make -j2 >> "$logfile" 2>&1 || exit 1

    if [ "$RUNTESTS""x" == "1x" ]; then
        cd "$_HOME_""/"
        cd ./002_src_libsqlfs/ || exit 1
        echo "run sqlfs ASAN tests for linux"
        make -j2 testclean >> "$logfile" 2>&1 || exit 1
        make -j2 test >> "$logfile" 2>&1 || exit 1
        make -j2 runtest >> "$logfile" 2>&1 || exit 1
        echo "run sqlfs TSAN tests for linux"
        make -j2 testclean >> "$logfile" 2>&1 || exit 1
        make -j2 test_tsan >> "$logfile" 2>&1 || exit 1
        make -j2 runtest_tsan >> "$logfile" 2>&1 || exit 1
    fi

    cd "$_HOME_""/"
    cd ./002_src_libsqlfs/.localrun/ || exit 1
    echo "build/upate docker container for libsqlfs"
    ./docker_linux_fast.sh build >> "$logfile" 2>&1 || exit 1
    echo "build sqlfs and sqlcipher for android"
    ./docker_linux_fast.sh >> "$logfile" 2>&1 || exit 1
    echo "build/upate windows docker container for libsqlfs"
    ./docker_win_fast.sh build >> "$logfile" 2>&1 || exit 1
    echo "build sqlfs and sqlcipher for windows x64"
    ./docker_win_fast.sh >> "$logfile" 2>&1 || exit 1

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

    echo "install sqlfs and sqlcipher windows libs"
    cd "$_HOME_""/"
    cp -av ./002_src_libsqlfs/.localrun/debian_12_win64/artefacts/win_x86_64/libsqlfs.a ./002_src_libsqlfs/libsqlfs_win64.a >> "$logfile" 2>&1 || exit 1
    cp -av ./002_src_libsqlfs/.localrun/debian_12_win64/artefacts/win_x86_64/sqlite3.a ./002_src_libsqlfs/sqlite3_win64.a >> "$logfile" 2>&1 || exit 1

fi

echo "deleting all *.class files"
find . -name '*.class'|xargs rm -v  >> "$logfile" 2>&1

cd "$_HOME_""/"
cd ./003_src_iocipher/ || exit 1
echo "build iocipher for android"
./gradlew build >> "$logfile" 2>&1 || exit 1
./gradlew :libiocipher2-c:assemble >> "$logfile" 2>&1 || exit 1
cd "$_HOME_""/"
echo "AAR artefacts"
ls -1 ./003_src_iocipher/libiocipher2-c/build/outputs/aar/libiocipher2-c-debug.aar || echo "NO ERR" 2>/dev/null
ls -1 ./003_src_iocipher/libiocipher2-c/build/outputs/aar/libiocipher2-c-release.aar

echo "====== build: OK ======"

echo "generate maven repository"
cd "$_HOME_""/"
cd ./003_src_iocipher/ || exit 1

f1="libiocipher2-c/build.gradle"

cur_num_version=$(cat "$f1" | grep 'versionCode ' | head -1 | \
	sed -e 's#^.*versionCode ##' ) >> "$logfile" 2>&1
cur_str_version=$(cat "$f1" | grep 'versionName "' | head -1 | \
	sed -e 's#^.*versionName "##' | \
	sed -e 's#".*$##') >> "$logfile" 2>&1

echo "$cur_num_version" >> "$logfile" 2>&1
echo "$cur_str_version" >> "$logfile" 2>&1

rm -Rf ./stub_work/ >> "$logfile" 2>&1
cp -a ./stub/ ./stub_work/ >> "$logfile" 2>&1 || exit 1
cd ./stub_work/root/.m2/repository/info/guardianproject/iocipher/IOCipher2/ >> "$logfile" 2>&1 || exit 1
sed -i -e 's#0.4.2.104#'"$cur_str_version"'#' maven-metadata-local.xml >> "$logfile" 2>&1 || exit 1
mv -v 0.4.2.104 "$cur_str_version" >> "$logfile" 2>&1 || exit 1
cd ./"$cur_str_version"/ >> "$logfile" 2>&1 || exit 1
mv -v IOCipher2-0.4.2.104.pom IOCipher2-"$cur_str_version".pom >> "$logfile" 2>&1 || exit 1
sed -i -e 's#0.4.2.104#'"$cur_str_version"'#' IOCipher2-"$cur_str_version".pom >> "$logfile" 2>&1 || exit 1

echo "copy aar file into maven repository"
cp -av "$_HOME_"/003_src_iocipher/libiocipher2-c/build/outputs/aar/libiocipher2-c-release.aar ./IOCipher2-"$cur_str_version".aar >> "$logfile" 2>&1 || exit 1

cd "$_HOME_""/"
cd ./003_src_iocipher/ >> "$logfile" 2>&1 || exit 1
cd ./stub_work/root/ >> "$logfile" 2>&1 || exit 1
zip -r ../local_maven.zip ./.m2 >> "$logfile" 2>&1 || exit 1
zip -r ../local_maven_iocipher_"$cur_str_version".zip ./.m2 >> "$logfile" 2>&1 || exit 1

cd "$_HOME_""/"
ls -1 ./003_src_iocipher/stub_work/local_maven_iocipher_"$cur_str_version".zip

echo "====== maven repository: OK ======"

