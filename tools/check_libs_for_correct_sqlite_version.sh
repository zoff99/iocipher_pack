#! /bin/bash

_HOME2_=$(dirname $0)
export _HOME2_
_HOME_=$(cd $_HOME2_;pwd)
export _HOME_

basedir="$_HOME_""/../"

cd "$basedir"

echo "*.o"
find . -name '*.o'|xargs -L1 strings | grep '3\.53\.'
echo "*.a"
find . -name '*.a'|xargs -L1 strings | grep '3\.53\.'
echo "*.dll"
find . -name '*.dll'|xargs -L1 strings | grep '3\.53\.'
echo "*.so"
find . -name '*.so'|xargs -L1 strings | grep '3\.53\.'

