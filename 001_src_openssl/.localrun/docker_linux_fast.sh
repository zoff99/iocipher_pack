#! /bin/bash

_HOME2_=$(dirname "$0")
export _HOME2_
_HOME_=$(cd "$_HOME2_" || exit;pwd)
export _HOME_

echo "$_HOME_"
cd "$_HOME_" || exit

if [ "$1""x" == "buildx" ]; then
    cp ../../000_deps/openssl-3.3.2.tar.gz . && docker build -f Dockerfile_deb12 -t openssl_android_deb12_001 .
    rm -f openssl-3.3.2.tar.gz
    exit 0
fi

build_for='debian:12
'

for system_to_build_for in $build_for ; do

    system_to_build_for_orig="$system_to_build_for"
    system_to_build_for=$(echo "$system_to_build_for_orig" 2>/dev/null|tr ':' '_' 2>/dev/null)"_linux"

    cd "$_HOME_"/ || exit
    mkdir -p "$_HOME_"/"$system_to_build_for"/

    # rm -Rf $_HOME_/"$system_to_build_for"/script 2>/dev/null
    # rm -Rf $_HOME_/"$system_to_build_for"/workspace 2>/dev/null

    mkdir -p "$_HOME_"/"$system_to_build_for"/artefacts
    mkdir -p "$_HOME_"/"$system_to_build_for"/script
    mkdir -p "$_HOME_"/"$system_to_build_for"/workspace

    ls -al "$_HOME_"/"$system_to_build_for"/

    rsync -a ../ --exclude=.localrun "$_HOME_"/"$system_to_build_for"/workspace/data
    chmod a+rwx -R "$_HOME_"/"$system_to_build_for"/workspace/data

    echo '#! /bin/bash


#------------------------

pwd
ls -al
id -a

OLDPATH=$PATH

# ------- Android -------
export ANDROID_NDK_ROOT=/opt/android-sdk/ndk/25.1.8937393
PATH=$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin:$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/:$PATH
archs="android-arm64 android-x86_64 android-x86 android-arm"

for i in $archs; do
    rm -Rf openssl-3.3.2/
    tar -xf /openssl-3.3.2.tar.gz
    cd openssl-3.3.2/
    # patch openSSL see: https://github.com/sfackler/rust-openssl/issues/2154 and https://github.com/android/ndk/issues/1992
    sed -E -i '"'"''"'"' -e '"'"'/[.]hidden.*OPENSSL_armcap_P/d'"'"' -e '"'"'/[.]extern.*OPENSSL_armcap_P/ {p; s/extern/hidden/; }'"'"' crypto/*arm*pl crypto/*/asm/*arm*pl
    ./Configure no-apps no-docs no-dso no-dgram "$i" -D__ANDROID_API__=21
    make -j $(nproc) || exit 1
    ls -al libcrypto.a libssl.a || exit 1
    file libcrypto.a libssl.a
    mkdir -p /artefacts/"$i"/
    cp -av libcrypto.a libssl.a /artefacts/"$i"/ || exit 1
    chmod -R a+rw /artefacts/*
    cd ..
done
# ------- Android -------

# ------- Linux -------
i=linux_debian12_x86_64
export ANDROID_NDK_ROOT=
PATH=$OLDPATH
rm -Rf openssl-3.3.2/
tar -xf /openssl-3.3.2.tar.gz
cd openssl-3.3.2/
./Configure no-apps no-docs no-dso no-dgram --prefix=/opt/openssl --openssldir=/usr/local/ssl
make -j $(nproc) || exit 1
##### make test
make install
ls -al libcrypto.a libssl.a || exit 1
file libcrypto.a libssl.a
mkdir -p /artefacts/"$i"/
cp -av libcrypto.a libssl.a /artefacts/"$i"/ || exit 1
mkdir -p /artefacts/"$i"/include/
cp -av /opt/openssl/include/openssl /artefacts/"$i"/include/
chmod -R a+rw /artefacts/*
cd ..
# ------- Linux -------



' > "$_HOME_"/"$system_to_build_for"/script/run.sh

    docker run --rm \
      -v "$_HOME_"/"$system_to_build_for"/artefacts:/artefacts \
      -v "$_HOME_"/"$system_to_build_for"/script:/script \
      -v "$_HOME_"/"$system_to_build_for"/workspace:/workspace \
      --net=host \
     "openssl_android_deb12_001" \
     /bin/sh -c "apk add bash >/dev/null 2>/dev/null; /bin/bash /script/run.sh"
     if [ $? -ne 0 ]; then
        echo "** ERROR **:$system_to_build_for_orig"
        exit 1
     else
        echo "--SUCCESS--:$system_to_build_for_orig"
     fi
done

