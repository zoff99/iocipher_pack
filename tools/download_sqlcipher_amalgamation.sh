#! /bin/bash

base_url="https://github.com/zoff99/gen_sqlcipher_amalgamation/releases/download/nightly/"


_HOME2_=$(dirname $0)
export _HOME2_
_HOME_=$(cd $_HOME2_;pwd)
export _HOME_

basedir="$_HOME_""/../"
cd "$basedir"

mkdir "$basedir""/002_src_libsqlfs/sqlcipher/"
cd "$basedir""/002_src_libsqlfs/sqlcipher/"
wget "$base_url""sqlite3.c" -O sqlite3.c
wget "$base_url""sqlite3.h" -O sqlite3.h
wget "$base_url""sqlite3ext.h" -O sqlite3ext.h
cd "$basedir""/002_src_libsqlfs/"

v_num_str=$(cat ./sqlcipher/sqlite3.c|grep 'define CIPHER_VERSION_NUMBER' 2>/dev/null|sed -e 's#.*define CIPHER_VERSION_NUMBER ###' 2>/dev/null)
v_build_str=$(cat ./sqlcipher/sqlite3.c|grep 'define CIPHER_VERSION_BUILD' 2>/dev/null|sed -e 's#.*define CIPHER_VERSION_BUILD ###' 2>/dev/null)

echo "v_num_str:""$v_num_str"
echo "v_build_str:""$v_build_str"

