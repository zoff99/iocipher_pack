#! /bin/bash

_HOME2_=$(dirname "$0")
export _HOME2_
_HOME_=$(cd "$_HOME2_" || exit;pwd)
export _HOME_

echo "$_HOME_"
cd "$_HOME_" || exit

if [ "$1""x" == "buildx" ]; then
    docker build -f Dockerfile_deb12 -t libsqlfs_android_deb12_001 .
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

# ------- win64 -------
i=win_x86_64

cd /workspace/data/
make clean
ls -al
export CC="x86_64-w64-mingw32-gcc"
export AR="x86_64-w64-mingw32-ar"
make -j $(nproc) || exit 1
file sqlite3.o
file sqlfs.o
mkdir -p /artefacts/"$i"/
cp -av sqlite3.a libsqlfs.a /artefacts/"$i"/ || exit 1
chmod -R a+rw /artefacts/*
# ------- win64 -------


' > "$_HOME_"/"$system_to_build_for"/script/run.sh

    docker run --rm \
      -v "$_HOME_"/"$system_to_build_for"/artefacts:/artefacts \
      -v "$_HOME_"/"$system_to_build_for"/script:/script \
      -v "$_HOME_"/"$system_to_build_for"/workspace:/workspace \
      --net=host \
     "libsqlfs_android_deb12_001" \
     /bin/sh -c "apk add bash >/dev/null 2>/dev/null; /bin/bash /script/run.sh"
     if [ $? -ne 0 ]; then
        echo "** ERROR **:$system_to_build_for_orig"
        exit 1
     else
        echo "--SUCCESS--:$system_to_build_for_orig"
     fi
done

