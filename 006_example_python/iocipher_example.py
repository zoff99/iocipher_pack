#
#
#  IOCipher Linux python3 example
#  (C) Zoff in 2024
#
#

import ctypes
from ctypes import *
import pathlib
from dataclasses import dataclass

@dataclass
class vfs_file:
    pathname: str
    cur_pos: int = 0
    flags: int = 0

def setup_vfs():
    print("trying to load C library")
    # Load the shared library into ctypes
    libname = pathlib.Path().absolute() / "libiocipher2_python.so"
    global c_lib
    c_lib = ctypes.CDLL(libname)
    print("C library loaded")

    global sqlfs
    sqlfs = ctypes.POINTER(ctypes.c_void_p)

    HANDLE = ctypes.c_void_p
    sqlfs = HANDLE(0)
    # print(repr(sqlfs))
    global database_filename
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

def close_vfs():
    print("closing VFS");
    rc = c_lib.sqlfs_close(sqlfs);
    assert rc == 1
    print("VFS: CLOSED");

def vfs_open(pathname, flags):
    vfs_fd = vfs_file(pathname, 0, flags)
    return vfs_fd

def vfs_write(vfs_fd, instr):
    str_utf8 = instr.encode(encoding="utf-8")
    count = len(str_utf8)
    res = c_lib.sqlfs_proc_write(
            sqlfs,
            vfs_fd.pathname,
            ctypes.c_char_p(str_utf8),
            count,
            vfs_fd.cur_pos,
            vfs_fd.flags)

    print("bytes written: " + str(res))

    vfs_fd.cur_pos = vfs_fd.cur_pos + count

    if (res < 0):
        return -1
    elif (res == 0):
        return 0
    else:
        return count

def vfs_read(vfs_fd, readbuf, numbytes):
    # struct fuse_file_info fi = { 0 };
    # fi.flags |= ~0;
    HANDLE = ctypes.c_void_p
    fi = HANDLE(0)

    # int sqlfs_proc_read(sqlfs_t *sqlfs, const char *path, char *buf, size_t size, off_t offset, struct
    #                fuse_file_info *fi)

#    c_lib.sqlfs_proc_read.argtypes = (
#            ctypes.POINTER(ctypes.c_void_p),
#            ctypes.c_void_p,
#            ctypes.c_char_p,
#            ctypes.c_long,
#            ctypes.c_long,
#            ctypes.POINTER(ctypes.c_void_p))

    res = c_lib.sqlfs_proc_read(
            sqlfs,
            vfs_fd.pathname,
            byref(readbuf),
            numbytes,
            vfs_fd.cur_pos,
            fi)
    vfs_fd.cur_pos = vfs_fd.cur_pos + numbytes

    if (res < 0):
        return -1
    elif (res == 0):
        return 0
    else:
        return numbytes

def vfs_close(vfs_fd):
    # basically a dummy
    return 0;




if __name__ == "__main__":


    setup_vfs()


    example_filename_in_vfs = "MyFile.txt"


    file1 = vfs_open(example_filename_in_vfs, "w")
    print(file1)
    s = "Hello World öüß german chars !:3}"
    print("write to file:")
    vfs_write(file1, s)
    vfs_close(file1)


    file2 = vfs_open(example_filename_in_vfs, "r")
    buffer = create_string_buffer(300)
    max_countbytes = 40
    print("read from file:")
    vfs_read(file2, buffer, max_countbytes)
    print("=======================")
    print("Contents of File:")
    print("======== char* =======:")
    print(repr(buffer))
    buf2 = ctypes.cast(buffer, ctypes.c_char_p).value
    print("bytes len: " + str(len(buf2)))
    print("======= encoded ======:")
    print(str(buf2))
    print("======= as text ======:")
    print(buffer.raw.decode('utf-8').rstrip('\x00'))
    print("strlen: " + str(len(buffer.raw.decode('utf-8').rstrip('\x00'))))
    print("=======================")
    vfs_close(file2)


    close_vfs()


    print("== OK ==")

