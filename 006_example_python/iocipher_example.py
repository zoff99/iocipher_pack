# ctypes_test.py
import ctypes
from ctypes import *
import pathlib


if __name__ == "__main__":
    print("trying to load C library")
    # Load the shared library into ctypes
    libname = pathlib.Path().absolute() / "libiocipher2_python.so"
    c_lib = ctypes.CDLL(libname)
    print("C library loaded")

    sqlfs = ctypes.POINTER(ctypes.c_void_p)

    HANDLE = ctypes.c_void_p
    sqlfs = HANDLE(0)
    # print(repr(sqlfs))
    database_filename = "./vfs.db"
    database_password = "" ## empty string means "no password set" !!

    print("Creating VFS %s...", database_filename);
    c_lib.sqlfs_open_password.argtypes = (ctypes.c_char_p, ctypes.c_char_p, ctypes.POINTER(ctypes.c_void_p))

    rc = c_lib.sqlfs_open_password(
                ctypes.c_char_p(database_filename.encode('utf-8')),
                ctypes.c_char_p(database_password.encode('utf-8')),
                sqlfs)

    assert rc == 1
    print(repr(sqlfs))
    assert sqlfs != HANDLE(0)
    print("VFS: OPEN");



    rc = 0
    print("closing VFS");
    rc = c_lib.sqlfs_close(sqlfs);
    assert(rc);
    print("VFS: CLOSED");


