/*
 *
 * IOCipher Linux C example
 * (C) Zoff in 2024
 *
 */

#include <assert.h>
#include <bits/time.h>
#include <errno.h>
#include <linux/limits.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include "sqlfs.h"

static struct sqlfs_t *sqlfs = NULL;


// ======= VFS helper functions =======
// ======= VFS helper functions =======
// ======= VFS helper functions =======
// ======= VFS helper functions =======
struct vfs_file {
    char pathname[PATH_MAX];
    size_t cur_pos;
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

ssize_t vfs_write(struct vfs_file *fd, const void *buf, size_t count)
{
    int written_bytes_or_error = sqlfs_proc_write(sqlfs, fd->pathname, buf, count, fd->cur_pos, fd->flags);

    if (written_bytes_or_error > 0)
    {
        fd->cur_pos = fd->cur_pos + written_bytes_or_error;
    }
    return (ssize_t)written_bytes_or_error;
}

ssize_t vfs_read(struct vfs_file *fd, void *buf, size_t count)
{
    struct fuse_file_info fi = { 0 };
    fi.flags |= ~0;
    int read_bytes_or_error = sqlfs_proc_read(sqlfs, fd->pathname, buf, count, fd->cur_pos, &fi);
    if (read_bytes_or_error > 0)
    {
        fd->cur_pos = fd->cur_pos + read_bytes_or_error;
    }
    return (ssize_t)read_bytes_or_error;
}

static size_t vfs_get_file_size(struct vfs_file *fd)
{
    struct stat stbuf;
    int res = sqlfs_proc_getattr(sqlfs, fd->pathname, &stbuf);
    printf("fsize=%ld\n", (long)stbuf.st_size);
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


// gives a counter value that increaes every millisecond
static uint64_t current_time_monotonic_default()
{
    uint64_t time = 0;
    struct timespec clock_mono;
    clock_gettime(CLOCK_MONOTONIC, &clock_mono);
    time = 1000ULL * clock_mono.tv_sec + (clock_mono.tv_nsec / 1000000ULL);
    return time;
}

int main(int argc, char *argv[])
{
    setup_vfs();

#define FILENAME "example.txt"
#define LARGEFILENAME "large_data.dat"
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
    ssize_t written_bytes = vfs_write(fd, write_buf, strlen(write_buf));
    printf("written_bytes=%ld\n", (long)written_bytes);

    size_t size1 = vfs_get_file_size(fd);
    printf("check file size:%ld %ld\n", (long)size1, (long)strlen(write_buf));
    assert((size_t)size1 == (size_t)strlen(write_buf));

    printf("close file\n");
    vfs_close(fd);

    // Read from file
    printf("open file\n");
    fd = vfs_open(FILENAME, O_RDONLY);
    printf("read from file\n");
    ssize_t read_bytes = vfs_read(fd, read_buf, BUFFER_SIZE);
    printf("read_bytes=%ld\n", (long)read_bytes);
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
    written_bytes = vfs_write(fd, "C programming", 13);
    printf("written_bytes=%ld\n", (long)written_bytes);

    size_t size2 = vfs_get_file_size(fd);
    printf("check file size:%ld %ld\n", (long)size2, (long)strlen(write_buf));
    assert((size_t)size2 == (size_t)strlen(write_buf));

    printf("seek again\n");
    vfs_lseek(fd, 0, SEEK_SET); // Seek back to the start
    printf("read\n");
    read_bytes = vfs_read(fd, read_buf, BUFFER_SIZE);
    printf("read_bytes=%ld\n", (long)read_bytes);
    printf("File content after modification: %s\n", read_buf);
    printf("close file\n");
    vfs_close(fd);


    // Random access to a file
    printf("open random access file\n");
    fd = vfs_open(FILENAME, O_RDWR);
    printf("seek\n");
    const int seek_bytes_end = 11;
    vfs_lseek(fd, seek_bytes_end, SEEK_END); // Seek "seek_bytes_end" bytes after the current end of file

    size_t size3 = vfs_get_file_size(fd);
    printf("check file size:%ld %ld\n", (long)size3, (long)strlen(write_buf));
    assert((size_t)size3 == (size_t)strlen(write_buf));

    printf("write\n");
    const int write_bytes = 8;
    written_bytes = vfs_write(fd, "mdifjw398ur893u3u98woier", write_bytes);
    printf("written_bytes=%ld\n", (long)written_bytes);

    size_t size4 = vfs_get_file_size(fd);
    printf("check file size:%ld %ld\n", (long)size4, (long)(strlen(write_buf) + seek_bytes_end + write_bytes));
    assert((size_t)size4 == (size_t)(strlen(write_buf) + seek_bytes_end + write_bytes));

    // to silence warnings
    current_time_monotonic_default();

#if RUN_LARGE_FILETEST
    // Open very large file and write ---------------------
    printf("open large file\n");
    fd = vfs_open(LARGEFILENAME, O_WRONLY | O_CREAT | O_TRUNC);
    assert(fd);
    const int kbuf_size = 8192 * 1000;
    const long wanted_file_size = 16L * 1024 * 1024 * 1024; // 16 GBytes
    const long loops = wanted_file_size / kbuf_size;
    uint8_t kbuf[kbuf_size];
    memset(kbuf, 16, kbuf_size);
    printf("write to large file\n");
    uint64_t t1 = current_time_monotonic_default();
    for(long i=0;i<loops;i++) {
        written_bytes = vfs_write(fd, kbuf, kbuf_size);
        // printf("written_bytes=%ld\n", (long)written_bytes);
        const long cur_size = i * kbuf_size;
        const long cur_size_mb = cur_size / (1024*1024);
        if ((cur_size % (10*1024*1024)) == 0)
        {
            const uint64_t t2 = current_time_monotonic_default();
            float delta_t = (float)((t2 - t1)) / 1000.0f;
            float mbs = ((float)cur_size / (1024*1024)) / delta_t;
            printf("WRITE: size:%.2f MiB/sec %ld MiBytes\n", (float)mbs, cur_size_mb);
        }
    }

    size_t size_l1 = vfs_get_file_size(fd);
    printf("check file size:%ld %ld\n", (long)size_l1, (long)(loops * kbuf_size));
    assert((size_t)size_l1 == (size_t)(loops * kbuf_size));

    printf("close file\n");
    vfs_close(fd);
    // -----------------------------------------------------


    // Read from very large file ---------------------
    printf("open file\n");
    fd = vfs_open(LARGEFILENAME, O_RDONLY);
    printf("read from file\n");

    memset(kbuf, 16, kbuf_size);
    t1 = current_time_monotonic_default();
    for(long i=0;i<loops;i++) {
        read_bytes = vfs_read(fd, kbuf, kbuf_size);
        // printf("read_bytes=%ld\n", (long)read_bytes);
        const long cur_size = i * kbuf_size;
        const long cur_size_mb = cur_size / (1024*1024);
        if ((cur_size % (10*1024*1024)) == 0)
        {
            const uint64_t t2 = current_time_monotonic_default();
            float delta_t = (float)((t2 - t1)) / 1000.0f;
            float mbs = ((float)cur_size / (1024*1024)) / delta_t;
            printf("READ : size:%.2f MiB/sec %ld MiBytes\n", (float)mbs, cur_size_mb);
        }
    }

    size_l1 = vfs_get_file_size(fd);
    printf("check file size:%ld %ld\n", (long)size_l1, (long)(loops * kbuf_size));
    assert((size_t)size_l1 == (size_t)(loops * kbuf_size));

    off_t offset1 = size_l1 - 1000;
    off_t got_offset = vfs_lseek(fd, offset1, SEEK_SET);
    printf("seek pos 1:%ld\n", (long)got_offset);

    offset1 = size_l1 - 1000;
    got_offset = vfs_lseek(fd, offset1, SEEK_CUR);
    printf("seek pos 2:%ld\n", (long)got_offset);

    offset1 = size_l1 - 1000;
    got_offset = vfs_lseek(fd, offset1, SEEK_END);
    printf("seek pos 3:%ld\n", (long)got_offset);

    const int bsize = 1000;
    uint8_t lbuf[bsize];
    printf("trying to add to file\n");
    written_bytes = vfs_write(fd, lbuf, bsize);
    printf("written_bytes=%ld\n", (long)written_bytes);

    size_l1 = vfs_get_file_size(fd);
    printf("check file size:%ld %ld\n", (long)size_l1, (long)(got_offset + bsize));
    assert((size_t)size_l1 == (size_t)(got_offset + bsize));

    printf("close file\n");
    vfs_close(fd);
    // -----------------------------------------------------
#endif

    close_vfs();

    printf("== OK ==\n");
}
