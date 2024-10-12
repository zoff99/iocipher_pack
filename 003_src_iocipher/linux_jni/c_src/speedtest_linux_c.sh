#! /bin/bash

_HOME_="$(pwd)"
export _HOME_
cd "$_HOME_"

logfile="$_HOME_""/speedtest.log"
rm -f "$logfile"

javacomp=javac
"$javacomp" -cp "iocipher_linux-1.0.1.jar:." com/example/iocipherspeedtest.java || exit 1
echo "***  test   ***"
java -classpath "iocipher_linux-1.0.1.jar:." com.example.iocipherspeedtest || exit 1
echo "***   OK    ***"

