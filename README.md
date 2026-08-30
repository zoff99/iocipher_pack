IOCipher: Encrypted Virtual Disk (for Android and Desktop JVM)
--------------------------------------------------------------

IOCipher is a virtual encrypted disk for apps without requiring the device to
be rooted. It uses a clone of the standard java.io API for working with
files. Just password handling & opening the virtual disk are what stand
between developers and fully encrypted file storage. It is based on libsqlfs
and SQLCipher.

If you are using this in your app, we'd love to hear about it!

The 2 Main Components
---------------------

[<b>libsqlfs</b>](https://github.com/zoff99/iocipher_pack/blob/master/002_src_libsqlfs/sqlfs.c) is a single source library that translates filesystem functions into SQL queries and vice versa.
<br><br>
<b>IOCipher</b> is a bunch of [JNI code](https://github.com/zoff99/iocipher_pack/tree/master/003_src_iocipher/libiocipher2-c/src/main/cpp) and [Java classes](https://github.com/zoff99/iocipher_pack/tree/master/003_src_iocipher/libiocipher2-c/src/main/java/info/guardianproject) to almost transparently emulate `java.io.*` classes with
`info.guardianproject.iocipher.*` classes of the same name.

<img src="https://raw.githubusercontent.com/zoff99/iocipher_pack/refs/heads/master/iocipher_coms.png" width="300">


⚠️ Critical Quirks & Divergences from `java.io.File`
------------

Because IOCipher is backed by a SQLite database (`sqlcipher`) rather than a native OS file system,
it behaves slightly differently than the standard `java.io.File` API. 

If you are migrating existing Java code to use IOCipher, please review these known quirks to avoid unexpected bugs:

| API / Method | Standard `java.io.File` Behavior | Actual IOCipher Behavior | Why it happens |
| :--- | :--- | :--- | :--- |
| **`setLastModified(long)`** | Returns `true` and updates the file's timestamp. | Returns `false`. The timestamp remains unchanged. | The underlying SQLite backend does not support manual overrides of file modification timestamps. |
| **`setReadOnly()`**<br>**`canWrite()`** | Locks the file. `canWrite()` returns `false`. | `setReadOnly()` returns `true` (no-op), but `canWrite()` continues to return `true`. | SQLite databases do not have per-file Unix/Windows permission bits. The VFS does not enforce or track these states. |
| **`getCanonicalPath()`** | Resolves `.` and `..` and returns the absolute canonical path. | Throws `java.io.IOException: No such file or directory` **even for files that exist**. | The native C implementation (`realpath`) requires the literal path string to exist in the DB and fails to normalize relative dot-paths textually before querying the VFS. |
| **`list(FilenameFilter)`**<br>**`listFiles(FileFilter)`** | Filters files using `java.io.FilenameFilter`. | **Silent Failure:** If you use `java.io.*` filters, it queries the **host OS** instead of the VFS. | Due to class inheritance, the compiler routes standard `java.io` interfaces to the host OS. You **must** import and use `info.guardianproject.iocipher.FilenameFilter` and `info.guardianproject.iocipher.FileFilter`. |
| **`getTotalSpace()`**<br>**`getFreeSpace()`** | Returns exact partition capacity. Free space is always `<` Total space. | `getFreeSpace()` can mathematically exceed `getTotalSpace()`. Non-existent paths return `> 0`. | IOCipher maps these to the host partition's stats. The SQLite container grows dynamically, so "free" host space often exceeds the current "total" container size. |
| **`deleteOnExit()`** | Schedules the file for deletion when the JVM terminates. | Throws `UnsupportedOperationException`. | Mobile/Android app lifecycles do not guarantee standard JVM shutdown hooks, making this method unreliable for encrypted containers. |

Build Status
------------

[![Android CI](https://github.com/zoff99/iocipher_pack/workflows/build/badge.svg)](https://github.com/zoff99/iocipher_pack/actions?query=workflow%3A%22build%22)
[![Last release](https://img.shields.io/github/v/release/zoff99/iocipher_pack)](https://github.com/zoff99/iocipher_pack/releases/latest)
[![Release](https://jitpack.io/v/zoff99/pkgs_guardianprojectIOCipher.svg)](https://jitpack.io/#zoff99/pkgs_guardianprojectIOCipher)
[![Liberapay](https://img.shields.io/liberapay/goal/zoff.svg?logo=liberapay)](https://liberapay.com/zoff/donate)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/zoff99/iocipher_pack)

the Android lib is published on jitpack.io:<br>
https://jitpack.io/#zoff99/pkgs_guardianprojectIOCipher

Building for Android
--------------------

This app relies on OpenSSL libcrypto, sqlcipher, and libsqlfs, which
are all "native" C code that need to be built.

```
# you need docker and jdk 11 or higher installed
./compile_docker_all.sh
```

Usage in an Android App:
------------------------

<img src="https://github.com/zoff99/iocipher_pack/releases/download/nightly/android_screen01_21.png" height="300"></a><img src="https://github.com/zoff99/iocipher_pack/releases/download/nightly/android_screen01_29.png" height="300"></a><img src="https://github.com/zoff99/iocipher_pack/releases/download/nightly/android_screen01_33.png" height="300"></a><img src="https://github.com/zoff99/iocipher_pack/releases/download/nightly/android_screen01_35.png" height="300"></a>
<br>

add to your project `build.gradle`<br>
```
allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url "https://jitpack.io"
        }
    }
}
```

add to your module `build.gradle`<br>
```
implementation 'com.github.zoff99:pkgs_guardianprojectIOCipher:1.0.33'
```

then see
https://github.com/zoff99/iocipher_pack/tree/master/007_example_android
for a usage example.

Building for Linux
------------------

<img src="https://github.com/zoff99/iocipher_pack/releases/download/nightly/package-screenshot-linux-deb.png" width="90%"></a>

This app relies on OpenSSL libcrypto, sqlcipher, and libsqlfs, which
are all "native" C code that need to be built.

```
# you need docker and jdk 11 or higher installed
./compile_docker_all.sh
cd ./003_src_iocipher/linux_jni/c_src
./compile_linux_c.sh
```

Building for Windows (on Linux)
-------------------------------

This app relies on OpenSSL libcrypto, sqlcipher, and libsqlfs, which
are all "native" C code that need to be built.

```
# you need docker and jdk 11 or higher installed
./compile_docker_all.sh
cd ./003_src_iocipher/linux_jni/c_src
./compile_linux_c.sh

# you need a working x86_64-w64-mingw32-gcc installed and in your path
apt-get install -y --no-install-recommends \
          wine libwine-dev libwine wine64 wine64-tools \
          make wget git coreutils autoconf \
          libtool pkg-config gcc-mingw-w64-x86-64 \
          gcc-mingw-w64-x86-64 g++-mingw-w64-x86-64 binutils-mingw-w64-x86-64 \
          mingw-w64-tools pkg-config-mingw-w64-x86-64 \
          mingw-w64-x86-64-dev

./compile_win_c.sh
```

Python Example
--------------

<img src="https://github.com/zoff99/iocipher_pack/releases/download/nightly/python_screen-0.png" width="30%"><img src="https://github.com/zoff99/iocipher_pack/releases/download/nightly/python_screen-1.png" width="30%">

Unit Tests
----------

<img src="https://github.com/zoff99/iocipher_pack/releases/download/nightly/unit_test_c_screen.png" width="30%"><img src="https://github.com/zoff99/iocipher_pack/releases/download/nightly/unit_test_java_screen.png" width="30%">

License
-------

When taken as a whole, this project is under the the LGPLv3 license
since it is the only license that is compatible with the licenses of
all the components.  The source code for this comes from a few
different places, so there are a number of licenses for different
chunks.

* Apache 2.0 (Android Internals): Much of the code here is taken from
  the Android internals, so it has an Apache 2.0 license.

* OpenSSL has an Apache 2.0 license.

* LGPL 2.1 (libsqlfs)

* BSD-style (sqlcipher)

We believe the LGPLv3 is compatible with all reasonable uses, including
proprietary software, but let us know if it provides difficulties for you.
For more info on how that works with Java, see:

https://www.gnu.org/licenses/lgpl-java.en.html

Current bugs
------------

There are still some "int" values used for sizes.
mtime and atime is not fully working the same as posix files.


Based upon
----------

This project is a continuation of:

https://github.com/guardianproject/IOCipher

and based on:

https://github.com/guardianproject/libsqlfs<br>
https://github.com/openssl/openssl/<br>
https://github.com/sqlite/sqlite<br>
https://github.com/sqlcipher/sqlcipher<br>
https://github.com/sqlcipher/sqlcipher-android<br>



<br>
Any use of this project's code by GitHub Copilot, past or present, is done
without our permission.  We do not consent to GitHub's use of this project's
code in Copilot.
<br>
No part of this work may be used or reproduced in any manner for the purpose of training artificial intelligence technologies or systems.


