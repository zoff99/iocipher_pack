#! /bin/bash

# copy the current JNI linux jar to this subdir
if [ -e ../003_src_iocipher/linux_jni/c_src/iocipher_linux-1.0.3.jar ]; then
  cp -v ../003_src_iocipher/linux_jni/c_src/iocipher_linux-1.0.3.jar ./iocipher_linux-1.0.3.jar
fi

# ASAN run
# javac -classpath ".:iocipher_linux-1.0.3.jar" FileManager.java FileDrop.java && \
# (export ASAN_OPTIONS="halt_on_error=true:detect_leaks=0:handle_segv=0" ; \
# LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libasan.so.6.0.0 java -classpath ".:iocipher_linux-1.0.3.jar" FileManager "$@" )

# regular run
javac -classpath ".:iocipher_linux-1.0.3.jar" FileManager.java FileDrop.java && \
java -classpath ".:iocipher_linux-1.0.3.jar" FileManager "$@"
