IOCipher: Encrypted Virtual Disk (for Android and Linux JVM)
------------------------------------------------------------

IOCipher is a virtual encrypted disk for apps without requiring the device to
be rooted. It uses a clone of the standard java.io API for working with
files. Just password handling & opening the virtual disk are what stand
between developers and fully encrypted file storage. It is based on libsqlfs
and SQLCipher.

If you are using this in your app, we'd love to hear about it!

Build Status
------------

[![Android CI](https://github.com/zoff99/iocipher_pack/workflows/build/badge.svg)](https://github.com/zoff99/iocipher_pack/actions?query=workflow%3A%22build%22)
[![Last release](https://img.shields.io/github/v/release/zoff99/iocipher_pack)](https://github.com/zoff99/iocipher_pack/releases/latest)


Building for Android
--------------------

This app relies on OpenSSL libcrypto, sqlcipher, and libsqlfs, which
are all "native" C code that need to be built.

```
# you need docker and jdk 11 or higher installed
./compile_docker_all.sh
```

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

Building for Windows
--------------------

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


