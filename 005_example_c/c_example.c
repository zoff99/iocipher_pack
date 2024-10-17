
#include <assert.h>
#include <errno.h>
#include <linux/limits.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "sqlfs.h"

static struct sqlfs_t *sqlfs = NULL;


// ======= VFS helper functions =======
// ======= VFS helper functions =======
// ======= VFS helper functions =======
// ======= VFS helper functions =======
struct vfs_file {
    char pathname[PATH_MAX];
    long cur_pos;
    int flags;
};

static void setup_vfs()
{
    char *database_filename = "vfs.db";
    char *database_password = "";
    int rc;
    int i;

    printf("Creating VFS %s...\n", database_filename);
    rc = sqlfs_open_password(database_filename, database_password, &sqlfs);
    assert(rc);
    assert(sqlfs != 0);
    printf("VFS: OPEN\n");
}

static void close_vfs()
{
    int rc;
    printf("closing VFS\n");
    rc = sqlfs_close(sqlfs);
    assert(rc);
    printf("VFS: CLOSED\n");
}

struct vfs_file *vfs_open(const char *pathname, int flags)
{
    if (pathname == NULL) {
        return NULL;
    }
    if (strlen(pathname) < 1) {
        return NULL;
    }
    if (strlen(pathname) > PATH_MAX) {
        return NULL;
    }

    struct vfs_file* vfs_fd = calloc(1, sizeof(struct vfs_file));
    if (vfs_fd == NULL) {
        return NULL;
    }

    memcpy(vfs_fd->pathname, pathname, strlen(pathname));
    vfs_fd->cur_pos = 0;
    vfs_fd->flags = flags;
    return vfs_fd;
}

int vfs_write(struct vfs_file *fd, const void *buf, size_t count)
{
    int res = sqlfs_proc_write(sqlfs, fd->pathname, buf, count, fd->cur_pos, fd->flags);
    fd->cur_pos = fd->cur_pos + count;

    if (res < 0)
    {
        return -1;
    }
    else if (res == 0)
    {
        return 0;
    }
    else
    {
        return count;
    }
}

ssize_t vfs_read(struct vfs_file *fd, void *buf, size_t count)
{
    struct fuse_file_info fi = { 0 };
    fi.flags |= ~0;
    int res = sqlfs_proc_read(sqlfs, fd->pathname, buf, count, fd->cur_pos, &fi);
    fd->cur_pos = fd->cur_pos + count;

    if (res < 0)
    {
        return -1;
    }
    else if (res == 0)
    {
        return 0;
    }
    else
    {
        return count;
    }
}

static long vfs_get_file_size(struct vfs_file *fd)
{
    struct stat stbuf;
    int res = sqlfs_proc_getattr(sqlfs, fd->pathname, &stbuf);
    // printf("fsize=%ld\n", (long)stbuf.st_size);
    return stbuf.st_size;
}

off_t vfs_lseek(struct vfs_file *fd, off_t offset, int whence)
{
    if (whence == SEEK_SET)
    {
        fd->cur_pos = offset;
    }
    else if (whence == SEEK_CUR)
    {
        fd->cur_pos = fd->cur_pos + offset;
    }
    else if (whence == SEEK_END)
    {
        fd->cur_pos = vfs_get_file_size(fd) + offset;
    }
    return (off_t)fd->cur_pos;
}

int vfs_mkdir(const char *pathname, mode_t mode)
{
    int res = sqlfs_proc_mkdir(sqlfs, pathname, mode);
    if (res != 0)
    {
        return -1;
    }
    else
    {
        return 0;
    }
}

int vfs_rmdir(const char *pathname)
{
    int res = sqlfs_proc_rmdir(sqlfs, pathname);
    if (res != 0)
    {
        return -1;
    }
    else
    {
        return 0;
    }
}

int vfs_close(struct vfs_file *fd)
{
    if (fd == NULL)
    {
        errno = -EBADF;
        return -1;
    }
    free(fd);
    return 0;
}
// ======= VFS helper functions =======
// ======= VFS helper functions =======
// ======= VFS helper functions =======
// ======= VFS helper functions =======




int main(int argc, char *argv[])
{
    setup_vfs();

#define FILENAME "example.txt"
#define DIRNAME "example_dir"
#define BUFFER_SIZE 100

    // File operations
    struct vfs_file *fd = NULL;
    char write_buf[BUFFER_SIZE] = "Hello, this is a test.";
    char read_buf[BUFFER_SIZE];
    memset(read_buf, 0, BUFFER_SIZE);

    // Open file and write
    printf("open file\n");
    fd = vfs_open(FILENAME, O_WRONLY | O_CREAT | O_TRUNC);
    assert(fd);
    printf("write to file\n");
    vfs_write(fd, write_buf, strlen(write_buf));

    long size1 = vfs_get_file_size(fd);
    printf("check file size:%ld %ld\n", size1, (long)strlen(write_buf));
    assert((size_t)size1 == (size_t)strlen(write_buf));

    printf("close file\n");
    vfs_close(fd);

    // Read from file
    printf("open file\n");
    fd = vfs_open(FILENAME, O_RDONLY);
    printf("read from file\n");
    vfs_read(fd, read_buf, BUFFER_SIZE);
    printf("close file\n");
    vfs_close(fd);

    printf("compare read and write buffers\n");
    // Compare bytes
    if (strcmp(write_buf, read_buf) == 0) {
        printf("Bytes are the same.\n");
    } else {
        printf("Bytes are different.\n");
        exit(1);
    }

    // Create directory
    printf("create directory\n");
    vfs_mkdir(DIRNAME, 0755);
    assert(sqlfs_is_dir(sqlfs, DIRNAME));

    // Delete directory
    printf("delete directory\n");
    vfs_rmdir(DIRNAME);
    assert(!sqlfs_is_dir(sqlfs, DIRNAME));

    // Random access to a file
    printf("open random access file\n");
    fd = vfs_open(FILENAME, O_RDWR);
    printf("seek\n");
    vfs_lseek(fd, 7, SEEK_SET); // Seek to the 8th byte
    printf("write\n");
    vfs_write(fd, "C programming", 13);

    long size2 = vfs_get_file_size(fd);
    printf("check file size:%ld %ld\n", size2, (long)strlen(write_buf));
    assert((size_t)size2 == (size_t)strlen(write_buf));

    printf("seek again\n");
    vfs_lseek(fd, 0, SEEK_SET); // Seek back to the start
    printf("read\n");
    vfs_read(fd, read_buf, BUFFER_SIZE);
    printf("File content after modification: %s\n", read_buf);
    printf("close file\n");
    vfs_close(fd);


    // Random access to a file
    printf("open random access file\n");
    fd = vfs_open(FILENAME, O_RDWR);
    printf("seek\n");
    const int seek_bytes_end = 11;
    vfs_lseek(fd, seek_bytes_end, SEEK_END); // Seek "seek_bytes_end" bytes after the current end of file

    long size3 = vfs_get_file_size(fd);
    printf("check file size:%ld %ld\n", size3, (long)strlen(write_buf));
    assert((size_t)size3 == (size_t)strlen(write_buf));

    printf("write\n");
    const int write_bytes = 8;
    vfs_write(fd, "mdifjw398ur893u3u98woier", write_bytes);

    long size4 = vfs_get_file_size(fd);
    printf("check file size:%ld %ld\n", size4, (long)(strlen(write_buf) + seek_bytes_end + write_bytes));
    assert((size_t)size4 == (size_t)(strlen(write_buf) + seek_bytes_end + write_bytes));

    close_vfs();

    printf("== OK ==\n");
}
