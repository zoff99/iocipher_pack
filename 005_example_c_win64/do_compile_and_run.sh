#! /bin/bash

_HOME2_=$(dirname "$0")
export _HOME2_
_HOME_=$(cd "$_HOME2_" || exit;pwd)
export _HOME_

cd "$_HOME_"


# copy the current deps to this subdir
cp ../002_src_libsqlfs/sqlfs.h ./
cp ../002_src_libsqlfs/sqlfs_internal.h ./
cp ../002_src_libsqlfs/libsqlfs_win64.a ./
cp ../002_src_libsqlfs/sqlite3_win64.a ./
cp -r ../002_src_libsqlfs/openssl_includes ./
cp -r ../002_src_libsqlfs/openssl_win64_libs ./
cp -r ../002_src_libsqlfs/sqlcipher/ ./

# compile
CFLAGS="-g -O3 -fPIC -Wall -Wextra -pedantic -pthread -I./ -I./openssl_includes -I./sqlcipher/"
LIBS="libsqlfs_win64.a sqlite3_win64.a openssl_win64_libs/libcrypto.a openssl_win64_libs/libssl.a -lm -Wl,-Bstatic -lws2_32 -lcrypt32"
CFSQLCIPHER="-DHAVE_STDINT_H -DHAVE_LIBSQLCIPHER -DSQLITE_HAS_CODEC -DSQLCIPHER_CRYPTO_OPENSSL -DSQLITE_TEMP_STORE=2 \
	-DSQLITE_EXTRA_INIT=sqlcipher_extra_init -DSQLITE_EXTRA_SHUTDOWN=sqlcipher_extra_shutdown \
	-DSQLITE_THREADSAFE=1 -DSQLITE_ENABLE_COLUMN_METADATA -DSQLITE_ENABLE_FTS3_PARENTHESIS \
	-DSQLITE_ENABLE_FTS4 -DSQLITE_ENABLE_FTS4_UNICODE61 -DSQLITE_ENABLE_FTS5 \
	-DSQLITE_ENABLE_MEMORY_MANAGEMENT -DSQLITE_ENABLE_UNLOCK_NOTIFY -DSQLITE_ENABLE_RTREE \
	-DSQLITE_SOUNDEX -DHAVE_USLEEP -DSQLITE_ENABLE_LOAD_EXTENSION -DSQLITE_ENABLE_STAT3 \
	-DSQLITE_ENABLE_STAT4 -DSQLITE_ENABLE_JSON1 -DSQLITE_ENABLE_EXPLAIN_COMMENTS \
	-DSQLITE_DEFAULT_WAL_SYNCHRONOUS=1 -DSQLITE_MAX_VARIABLE_NUMBER=99999 \
	-DSQLITE_DEFAULT_JOURNAL_SIZE_LIMIT=1048576 -DSQLITE_ENABLE_SESSION \
	-DSQLITE_ENABLE_PREUPDATE_HOOK -DSQLITE_ENABLE_DBSTAT_VTAB"

x86_64-w64-mingw32-gcc -Wno-unused-variable -Wno-unused-parameter c_example_win64.c -D_GNU_SOURCE=1 $CFLAGS $LIBS $CFSQLCIPHER -o c_example_win64.exe || exit 1

rm -f example.txt
rm -f vfs.db
rm -f vfs.db-*
rm -Rf example_dir/

wine ./c_example_win64.exe

