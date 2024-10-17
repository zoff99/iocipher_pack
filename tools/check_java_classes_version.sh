#! /bin/bash

_HOME2_=$(dirname $0)
export _HOME2_
_HOME_=$(cd $_HOME2_;pwd)
export _HOME_

basedir="$_HOME_""/../"

cd "$basedir"

find ./ -name '*.class' | while read c ; do
    file "$c" 2>/dev/null|grep 'version 52.0' >/dev/null 2>/dev/null
    jdk_ver_used=$?

    if [ "$jdk_ver_used""x" != "0x" ]; then
        echo "================================="
        echo "File: $c"
        file "$c"
        echo ""
        echo "java classes seem to be compiled by higher jdk version than 1.8, please compile with 1.8"
        echo "================================="
        exit 1
    fi
done

