IOCipher: Encrypted Virtual Disk (for Android)
----------------------------------------------

IOCipher is a virtual encrypted disk for apps without requiring the device to
be rooted. It uses a clone of the standard java.io API for working with
files. Just password handling & opening the virtual disk are what stand
between developers and fully encrypted file storage. It is based on libsqlfs
and SQLCipher.

If you are using this in your app, we'd love to hear about it!


Building
--------

This app relies on OpenSSL libcrypto, sqlcipher, and libsqlfs, which
are all "native" C code that needs to be built before working with the
Java.

```
./compile_docker_all.sh
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

Opening a file with<br>
`info.guardianproject.iocipher.FileOutputStream(f, true);`<br>
does currently **NOT** work.<br>
It will not append but instead open the file `f` for writing at the beginng of the file.


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



Any use of this project's code by GitHub Copilot, past or present, is done
without our permission.  We do not consent to GitHub's use of this project's
code in Copilot.

