#! /bin/bash

_HOME2_=$(dirname "$0")
export _HOME2_
_HOME_=$(cd "$_HOME2_" || exit;pwd)
export _HOME_

cd "$_HOME_"


# copy the current JNI linux jar to this subdir
if [ -e ../003_src_iocipher/linux_jni/c_src/iocipher_linux-1.*.jar ]; then
  rm -f ./iocipher_linux-1.*.jar
  cp -v ../003_src_iocipher/linux_jni/c_src/iocipher_linux-1.*.jar ./
fi

jar=$(ls -1 iocipher_linux-1.*.jar 2>/dev/null)

rm -f min_example.db*

# regular run
javac -classpath ".:$jar" MinialExample.java && \
java -classpath ".:$jar" MinialExample "$@"
