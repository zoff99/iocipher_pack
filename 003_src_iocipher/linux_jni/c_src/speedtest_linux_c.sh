#! /bin/bash

_HOME_="$(pwd)"
export _HOME_
cd "$_HOME_"

logfile="$_HOME_""/speedtest.log"
rm -f "$logfile"

echo "*** version ***"
cur_str_version=$(cat ./info/guardianproject/iocipher/VirtualFileSystem.java|grep 'static String IOCIPHER_JNI_VERSION'|sed -e 's#^.*static String IOCIPHER_JNI_VERSION = "##'|sed -e 's#".*$##')

javacomp=javac
"$javacomp" -classpath ".:iocipher_linux-""$cur_str_version"".jar" com/example/iocipherspeedtest.java || exit 1
echo "***  test   ***"
java -classpath ".:iocipher_linux-""$cur_str_version"".jar:." com.example.iocipherspeedtest || exit 1
echo "***   OK    ***"

