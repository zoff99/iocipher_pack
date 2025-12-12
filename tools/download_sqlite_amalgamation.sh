#! /bin/bash

_SQLITE_VERSION_="3510100"

amalgamation_url="https://sqlite.org/2025/sqlite-amalgamation-""$_SQLITE_VERSION_"".zip"


_HOME2_=$(dirname $0)
export _HOME2_
_HOME_=$(cd $_HOME2_;pwd)
export _HOME_

basedir="$_HOME_""/../"

cd "$basedir""/002_src_libsqlfs/"
rm -f amalgamation.zip

wget "$amalgamation_url" -O amalgamation.zip
mkdir "$basedir""/002_src_libsqlfs/sqlite/"
cd "$basedir""/002_src_libsqlfs/sqlite/"
unzip -j -o "$basedir""/002_src_libsqlfs/"amalgamation.zip
cd "$basedir"

cd "$basedir""/002_src_libsqlfs/"
rm -f amalgamation.zip


