#! /bin/bash

#javac -classpath ".:iocipher_linux-1.0.3.jar" FileList.java FileRenderer.java TextFileFilter.java || exit 1
#export ASAN_OPTIONS="halt_on_error=true:detect_leaks=0:handle_segv=0"
#LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libasan.so.6.0.0 \
#java -classpath ".:iocipher_linux-1.0.3.jar" -Dsun.java2d.uiScale=2.5 FileList


javac -classpath ".:iocipher_linux-1.0.3.jar" FileManager.java FileDrop.java && \
(export ASAN_OPTIONS="halt_on_error=true:detect_leaks=0:handle_segv=0" ; \
LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libasan.so.6.0.0 java -classpath ".:iocipher_linux-1.0.3.jar" FileManager "$@" )
