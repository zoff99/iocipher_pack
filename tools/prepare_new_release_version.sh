#! /bin/bash

_HOME2_=$(dirname $0)
export _HOME2_
_HOME_=$(cd $_HOME2_;pwd)
export _HOME_

basedir="$_HOME_""/../"

cd "$basedir"

if [[ $(git status --porcelain --untracked-files=no) ]]; then
	echo "ERROR: git repo has changes."
	echo "please commit or cleanup the git repo."
	exit 1
else
	echo "git repo clean."
fi


f1="003_src_iocipher/libiocipher2-c/src/main/java/info/guardianproject/iocipher/VirtualFileSystem.java"
#    final public static String IOCIPHER_JNI_VERSION = "1.0.2";

f2="003_src_iocipher/linux_jni/c_src/info/guardianproject/iocipher/VirtualFileSystem.java"
#    final public static String IOCIPHER_JNI_VERSION = "1.0.2";

f3="003_src_iocipher/libiocipher2-c/build.gradle"
#        versionCode 10002
#        versionName "1.0.2"

cur_p_version=$(cat "$f3" | grep 'versionCode ' | head -1 | \
	sed -e 's#^.*versionCode ##' )
cur_m_version=$(cat "$f3" | grep 'versionName "' | head -1 | \
	sed -e 's#^.*versionName "##' | \
	sed -e 's#".*$##')

next_p_version=$[ $cur_p_version + 1 ]
# thanks to: https://stackoverflow.com/a/8653732
next_m_version=$(echo "$cur_m_version"|awk -F. -v OFS=. 'NF==1{print ++$NF}; NF>1{if(length($NF+1)>length($NF))$(NF-1)++; $NF=sprintf("%0*d", length($NF), ($NF+1)%(10^length($NF))); print}')

echo $cur_p_version
echo $next_p_version

echo $cur_m_version
echo $next_m_version

sed -i -e 's#versionCode .*#versionCode '"$next_p_version"'#g' "$f3"
sed -i -e 's#versionName ".*#versionName "'"$next_m_version"'"#g' "$f3"

sed -i -e 's#public static String IOCIPHER_JNI_VERSION = ".*$#public static String IOCIPHER_JNI_VERSION = "'"$next_m_version"'";#g' "$f1"
sed -i -e 's#public static String IOCIPHER_JNI_VERSION = ".*$#public static String IOCIPHER_JNI_VERSION = "'"$next_m_version"'";#g' "$f2"


commit_message="new version ""$next_m_version"
tag_name="$next_m_version"

echo "$commit_message"
echo "v""$tag_name"

exit 0

# HINT: !!first build it all again. then commit and tag!!

# git commit -m "$commit_message" "$f1" "$f2" "$f3"
# git tag -a "$next_m_version" -m "$next_m_version"


