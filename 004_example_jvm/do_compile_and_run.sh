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

# ASAN run
# javac -classpath ".:$jar" FileManager.java FileDrop.java && \
# (export ASAN_OPTIONS="halt_on_error=true:detect_leaks=0:handle_segv=0" ; \
# LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libasan.so.6.0.0 java -classpath ".:$jar" FileManager "$@" )

if [ "$1""x" == "only_compile""x" ]; then
  # compile only
  javac -classpath ".:$jar" FileManager.java FileDrop.java
else
  # regular run
  javac -classpath ".:$jar" FileManager.java FileDrop.java && \
  java -classpath ".:$jar" FileManager "$@"
fi
