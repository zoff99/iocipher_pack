#! /bin/bash

_HOME2_=$(dirname "$0")
export _HOME2_
_HOME_=$(cd "$_HOME2_" || exit;pwd)
export _HOME_

echo "$_HOME_"
cd "$_HOME_" || exit

if [ "$1""x" == "buildx" ]; then
    cp ../../000_deps/openssl.tar.gz . && docker build -f Dockerfile_deb12 -t openssl_android_deb12_001 .
    rm -f openssl.tar.gz
    exit 0
fi

build_for='debian:12
'

for system_to_build_for in $build_for ; do

    system_to_build_for_orig="$system_to_build_for"
    system_to_build_for=$(echo "$system_to_build_for_orig" 2>/dev/null|tr ':' '_' 2>/dev/null)"_win64"

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

apt-get update && \
          DEBIAN_FRONTEND=noninteractive \
          apt-get install -y --no-install-recommends \
          wine libwine-dev libwine wine64 wine64-tools \
          make wget git coreutils autoconf \
          libtool pkg-config gcc-mingw-w64-x86-64 \
          gcc-mingw-w64-x86-64 g++-mingw-w64-x86-64 binutils-mingw-w64-x86-64 \
          mingw-w64-tools pkg-config-mingw-w64-x86-64 \
          mingw-w64-x86-64-dev

dpkg -L gcc-mingw-w64-x86-64


# ------- Win64 -------
i=win_x86_64
export ANDROID_NDK_ROOT=
PATH=$OLDPATH
rm -Rf openssl-*/
tar -xf /openssl.tar.gz
cd openssl-*/
./Configure mingw64 --cross-compile-prefix=x86_64-w64-mingw32- no-apps no-docs no-dso no-dgram --prefix=/opt/openssl --openssldir=/usr/local/ssl
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
# ------- Win64 -------



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

