#! /bin/bash

_HOME2_=$(dirname "$0")
export _HOME2_
_HOME_=$(cd "$_HOME2_" || exit;pwd)
export _HOME_

echo "***************"
echo ""
echo "set java and javac to version 1.8 before running this script!"
echo ""
echo "***************"


logfile="$_HOME_""/compile_other.log"

cd "$_HOME_""/"
rm -f "$logfile"

cd "$_HOME_""/"
cd ./003_src_iocipher/linux_jni/c_src/ || exit 1
echo "build iocipher for linux"
./compile_linux_c.sh >> "$logfile" 2>&1 || exit 1
echo "build iocipher for windows"
./compile_win_c.sh >> "$logfile" 2>&1 || exit 1

cd "$_HOME_""/"
cd ./005_example_c/ || exit 1
echo "build libsqlfs for C"
./do_compile_and_run.sh  >> "$logfile" 2>&1 || exit 1

cd "$_HOME_""/"
cd ./006_example_python/ || exit 1
echo "build iocipher for python"
./do_compile_and_run.sh  >> "$logfile" 2>&1 || exit 1

