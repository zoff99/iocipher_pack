#! /bin/bash

_HOME2_=$(dirname "$0")
export _HOME2_
_HOME_=$(cd "$_HOME2_" || exit;pwd)
export _HOME_

cd "$_HOME_"


# copy the current deps to this subdir
cp ../002_src_libsqlfs/sqlfs.h ./ || exit 1
cp ../002_src_libsqlfs/sqlfs_internal.h ./ || exit 1
cp ../002_src_libsqlfs/libsqlfs.a ./
cp ../002_src_libsqlfs/sqlite3.a ./
cp -r ../002_src_libsqlfs/openssl_includes ./ || exit 1
cp -r ../002_src_libsqlfs/openssl_libs ./ || exit 1
cp -r ../002_src_libsqlfs/sqlcipher/ ./ || exit 1

# temp files
rm -Rf ./tmp/
mkdir -p ./tmp/
cd ./tmp/
ar x ../libsqlfs.a
ar x ../sqlite3.a
# ar x ../openssl_libs/libcrypto.a || exit 1
ar x ../openssl_libs/libssl.a || exit 1
cd ../


# clean
rm -v libiocipher2_python.so

# compile
gcc -g -O3 -fPIC -fstack-protector-all -D_FORTIFY_SOURCE=2 \
-D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 \
./tmp/*.o \
./openssl_libs/libcrypto.a \
./openssl_libs/libssl.a \
sqlfs_python_helper.c \
-lm \
-shared \
-Wl,-soname,libiocipher2_python.so -o libiocipher2_python.so || exit 1

rm -Rf ./tmp/

python3 iocipher_example.py
