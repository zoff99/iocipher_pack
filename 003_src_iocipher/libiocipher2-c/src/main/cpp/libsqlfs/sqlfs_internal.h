/******************************************************************************
Copyright 2006 Palmsource, Inc (an ACCESS company).

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

*****************************************************************************/


#ifndef __SQLFS_INTERNAL_H__
#define __SQLFS_INTERNAL_H__

#ifdef __MINGW32__
# ifndef _LARGEFILE64_SOURCE
#  define _LARGEFILE64_SOURCE
# endif
#endif

#include <sys/types.h>
#include <sys/stat.h>
#ifdef __ANDROID__
// Android uses a statvfs-like statfs struct and call.
# include <sys/vfs.h>
# define statvfs statfs
# define fstatvfs fstatfs
#else
# ifndef __MINGW32__
#  include <sys/statvfs.h>
# else
// HINT: this is just a hack for MingW
    typedef uint32_t uid_t;
    typedef uint32_t gid_t;

    typedef uint32_t fsblkcnt_t;
    typedef uint32_t fsfilcnt_t;

    #define S_IFLNK    0120000
    #define S_IFSOCK   0140000
    #define ST_NOSUID  2

    struct statvfs {
        unsigned long  f_bsize;    /* Filesystem block size */
        unsigned long  f_frsize;   /* Fragment size */
        fsblkcnt_t     f_blocks;   /* Size of fs in f_frsize units */
        fsblkcnt_t     f_bfree;    /* Number of free blocks */
        fsblkcnt_t     f_bavail;   /* Number of free blocks for
                                        unprivileged users */
        fsfilcnt_t     f_files;    /* Number of inodes */
        fsfilcnt_t     f_ffree;    /* Number of free inodes */
        fsfilcnt_t     f_favail;   /* Number of free inodes for
                                        unprivileged users */
        unsigned long  f_fsid;     /* Filesystem ID */
        unsigned long  f_flag;     /* Mount flags */
        unsigned long  f_namemax;  /* Maximum filename length */
    };


# endif
#endif
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <utime.h>

#ifdef __MINGW32__
typedef off64_t sqlfs_off_t;
typedef struct _stat64 sqlfs_stat;
#else
typedef off_t sqlfs_off_t;
typedef struct stat sqlfs_stat;
#endif

#define TYPE_NULL "null"
#define TYPE_DIR "dir"
#define TYPE_INT "int"
#define TYPE_DOUBLE "double"
#define TYPE_STRING "string"
#define TYPE_SYM_LINK "sym link"
#define TYPE_BOOL  "bool"
#define TYPE_LIST "list"
#define TYPE_BLOB "blob"

typedef struct sqlfs_t sqlfs_t;

typedef struct
{
    char *path;
    char *type;
    int32_t inode;
    int32_t uid;
    int32_t gid;
    int32_t mode;
    size_t size;
    /*
    char *acl;
    char *attributes;
    */
    time_t atime; /* last access time */
    time_t mtime; /* last modify time */
    time_t ctime; /* last status change time */
} key_attr;


typedef struct
{
    char *data;
    size_t size;
} key_value;


void clean_attr(key_attr *attr);

void clean_value(key_value *value);

int sqlfs_del_tree(sqlfs_t *sqlfs, const char *key);
int sqlfs_del_tree_with_exclusion(sqlfs_t *sqlfs, const char *key, const char *exclusion_pattern);

int sqlfs_get_value(sqlfs_t *sqlfs, const char *key, key_value *value,
                    size_t begin, size_t end);

int sqlfs_set_value(sqlfs_t *sqlfs, const char *key, const key_value *value,
                    size_t begin,  size_t end);

int sqlfs_get_attr(sqlfs_t *sqlfs, const char *key, key_attr *attr);

int sqlfs_set_attr(sqlfs_t *sqlfs, const char *key, const key_attr *attr);

int sqlfs_is_dir(sqlfs_t *sqlfs, const char *key);



int sqlfs_set_type(sqlfs_t *sqlfs, const char *key, const char *type);
int sqlfs_list_keys(sqlfs_t *, const char *pattern, void *buf, fuse_fill_dir_t filler);

int sqlfs_begin_transaction(sqlfs_t *sqlfs);
int sqlfs_complete_transaction(sqlfs_t *sqlfs, int i);
int sqlfs_break_transaction(sqlfs_t *sqlfs);

int sqlfs_proc_getattr(sqlfs_t *, const char *path, sqlfs_stat *stbuf);
int sqlfs_proc_access(sqlfs_t *, const char *path, int mask);
int sqlfs_proc_create(sqlfs_t *sqlfs, const char *path, mode_t mode, struct fuse_file_info *fi);
int sqlfs_proc_readlink(sqlfs_t *, const char *path, char *buf, size_t size);
int sqlfs_proc_readdir(sqlfs_t *, const char *path, void *buf, fuse_fill_dir_t filler,
                       sqlfs_off_t offset, struct fuse_file_info *fi);
int sqlfs_proc_mknod(sqlfs_t *, const char *path, mode_t mode, dev_t rdev);
int sqlfs_proc_mkdir(sqlfs_t *, const char *path, mode_t mode);
int sqlfs_proc_unlink(sqlfs_t *, const char *path);
int sqlfs_proc_rmdir(sqlfs_t *, const char *path);
int sqlfs_proc_symlink(sqlfs_t *, const char *path, const char *to);
int sqlfs_proc_rename(sqlfs_t *, const char *from, const char *to);
int sqlfs_proc_link(sqlfs_t *, const char *from, const char *to);
int sqlfs_proc_chmod(sqlfs_t *, const char *path, mode_t mode);
int sqlfs_proc_chown(sqlfs_t *, const char *path, uid_t uid, gid_t gid);
int sqlfs_proc_truncate(sqlfs_t *, const char *path, sqlfs_off_t size);
int sqlfs_proc_utime(sqlfs_t *, const char *path, struct utimbuf *buf);
int sqlfs_proc_open(sqlfs_t *, const char *path, struct fuse_file_info *fi);
int sqlfs_proc_read(sqlfs_t *, const char *path, char *buf, size_t size, sqlfs_off_t offset, struct
                    fuse_file_info *fi);
int sqlfs_proc_write(sqlfs_t *, const char *path, const char *buf, size_t size, sqlfs_off_t offset,
                     int mode_flags);
int sqlfs_proc_statfs(sqlfs_t *, const char *path, struct statvfs *stbuf);
int sqlfs_proc_release(sqlfs_t *, const char *path, struct fuse_file_info *fi);
int sqlfs_proc_fsync(sqlfs_t *, const char *path, int isfdatasync, struct fuse_file_info *fi);
int sqlfs_proc_setxattr(sqlfs_t *, const char *path, const char *name, const char *value,
                        size_t size, int flags);
int sqlfs_proc_getxattr(sqlfs_t *, const char path, const char *name, char *value, size_t size);
int sqlfs_proc_listxattr(sqlfs_t *, const char *path, char *list, size_t size);
int sqlfs_proc_removexattr(sqlfs_t *, const char *path, const char *name);

#endif /* NOT __SQLFS_INTERNAL_H__ */


/* -*- mode: c; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4; c-file-style: "bsd"; -*- */
