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
cd ./004_example_jvm/ || exit 1
echo "build jvm filemanager example"
./do_compile_and_run.sh only_compile 2>&1 || exit 1
echo "build jvm mini example example"
./do_miniexample_compile_and_run.sh 2>&1 || exit 1

cd "$_HOME_""/"
cd ./005_example_c/ || exit 1
echo "build libsqlfs for C (linux)"
./do_compile_and_run.sh  >> "$logfile" 2>&1 || exit 1

cd "$_HOME_""/"
cd ./005_example_c_win64/ || exit 1
echo "build libsqlfs for C (windows)"
./do_compile_and_run.sh  >> "$logfile" 2>&1 || exit 1

cd "$_HOME_""/"
cd ./006_example_python/ || exit 1
echo "build iocipher for python"
./do_compile_and_run.sh  >> "$logfile" 2>&1 || exit 1

