#! /bin/bash

_HOME2_=$(dirname "$0")
export _HOME2_
_HOME_=$(cd "$_HOME2_" || exit;pwd)
export _HOME_

cd "$_HOME_"


# copy the current deps to this subdir
cp ../002_src_libsqlfs/sqlfs.h ./
cp ../002_src_libsqlfs/sqlfs_internal.h ./
cp ../002_src_libsqlfs/libsqlfs.a ./
cp ../002_src_libsqlfs/sqlite3.a ./
cp -r ../002_src_libsqlfs/openssl_includes ./
cp -r ../002_src_libsqlfs/openssl_libs ./
cp -r ../002_src_libsqlfs/sqlcipher/ ./

# temp files
rm -Rf ./tmp/
mkdir -p ./tmp/
cd ./tmp/
ar x ../libsqlfs.a
ar x ../sqlite3.a
# ar x ../openssl_libs/libcrypto.a
# ar x ../openssl_libs/libssl.a
cd ../

# compile
gcc -g -O3 -fPIC -fstack-protector-all -D_FORTIFY_SOURCE=2 \
-D_FILE_OFFSET_BITS=64 -D__USE_GNU=1 \
./tmp/*.o \
./openssl_libs/libcrypto.a \
./openssl_libs/libssl.a \
-lm \
-shared \
-Wl,-soname,libiocipher2_python.so -o libiocipher2_python.so || exit 1

rm -Rf ./tmp/

python3 iocipher_example.py
