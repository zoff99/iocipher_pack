#! /bin/bash

_HOME2_=$(dirname $0)
export _HOME2_
_HOME_=$(cd $_HOME2_;pwd)
export _HOME_

basedir="$_HOME_""/../"

cd "$basedir"

if [[ $(git status --porcelain --untracked-files=no) ]]; then
	echo "WARNING: git repo has changes."
	echo "please commit or cleanup the git repo."
else
	echo "git repo clean."
fi

f3="003_src_iocipher/libiocipher2-c/build.gradle"
#        versionCode 10002
#        versionName "1.0.2"

cur_p_version=$(cat "$f3" | grep 'versionCode ' | head -1 | \
	sed -e 's#^.*versionCode ##' )
cur_m_version=$(cat "$f3" | grep 'versionName "' | head -1 | \
	sed -e 's#^.*versionName "##' | \
	sed -e 's#".*$##')

echo $cur_p_version
echo $cur_m_version


commit_message="new version ""$cur_m_version"
tag_name="v""$cur_m_version"

echo "$commit_message"
echo "$tag_name"

faar="./003_src_iocipher/stub_work/root/.m2/repository/info/guardianproject/iocipher/IOCipher2/""$cur_m_version""/IOCipher2-""$cur_m_version"".aar"

if [ ! -e "$faar" ]; then
    echo "================================="
    echo "$faar"
    echo ""
    echo "aar missing or wrong version. did you forget to compile all again?"
    echo "================================="
    exit 1
fi

fjar="./003_src_iocipher/linux_jni/c_src/iocipher_linux-""$cur_m_version"".jar"

if [ ! -e "$fjar" ]; then
    echo "================================="
    echo "$fjar"
    echo ""
    echo "jar missing or wrong version. did you forget to compile all again?"
    echo "================================="
    exit 1
fi

file ./003_src_iocipher/linux_jni/c_src/info/guardianproject/iocipher/VirtualFileSystem.class 2>/dev/null|grep 'version 52.0' >/dev/null 2>/dev/null
jdk_ver_used=$?

if [ "$jdk_ver_used""x" != "0x" ]; then
    echo "================================="
    file ./003_src_iocipher/linux_jni/c_src/info/guardianproject/iocipher/VirtualFileSystem.class
    echo ""
    echo "java classes seem to be compiled by higher jdk version than 1.8, please compile with 1.8"
    echo "================================="
    exit 1
fi

git add "$fjar"

git commit -a -m "$commit_message"
git tag -a "$tag_name" -m "$tag_name"
