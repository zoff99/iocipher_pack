/*
 * test_large_files.c — Large file I/O and SQLite block/chunk boundary tests
 * Verifies correct behavior when files exceed a single SQLite blob block.
 */
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <stdlib.h>
#include "test_framework.h"
#include "../sqlfs.h"

#define DB "_test_large_files.db"
#define BLOCK_SIZE 8192

static sqlfs_t *fs;

static void setup(void) {
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
    sqlfs_open(DB, &fs);
}
static void teardown(void) {
    sqlfs_close(fs);
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
}

/* ── helpers ──────────────────────────────────────────────────── */

/* Fill a buffer with a deterministic pattern based on index */
static void fill_pattern(unsigned char *buf, size_t size, unsigned char seed) {
    for (size_t i = 0; i < size; i++) {
        buf[i] = (unsigned char)((seed + i) & 0xFF);
    }
}

static bool verify_pattern(const unsigned char *buf, size_t size, unsigned char seed) {
    for (size_t i = 0; i < size; i++) {
        if (buf[i] != (unsigned char)((seed + i) & 0xFF)) {
            return false;
        }
    }
    return true;
}

/* ── tests ────────────────────────────────────────────────────── */

static bool t_write_read_100kb(void) {
    setup();
    const size_t SIZE = 100 * 1024; /* 100 KB */
    unsigned char *wbuf = malloc(SIZE);
    unsigned char *rbuf = malloc(SIZE);
    T_ASSERT_PTR_NOT_NULL(wbuf, "alloc write buffer");
    T_ASSERT_PTR_NOT_NULL(rbuf, "alloc read buffer");

    fill_pattern(wbuf, SIZE, 0x42);

    int wrc = sqlfs_proc_write(fs, "/large100k.bin", (const char *)wbuf, SIZE, 0,
                               O_RDWR | O_CREAT | O_TRUNC);
    T_ASSERT_INT_EQ(wrc, (int)SIZE, "write 100KB");

    memset(rbuf, 0, SIZE);
    struct fuse_file_info fi = {0};
    int rrc = sqlfs_proc_read(fs, "/large100k.bin", (char *)rbuf, SIZE, 0, &fi);
    T_ASSERT_INT_EQ(rrc, (int)SIZE, "read 100KB");
    T_ASSERT_TRUE(memcmp(wbuf, rbuf, SIZE) == 0, "100KB content matches");

    sqlfs_stat st;
    sqlfs_proc_getattr(fs, "/large100k.bin", &st);
    T_ASSERT_INT_EQ((int)st.st_size, (int)SIZE, "size is 100KB");

    free(wbuf);
    free(rbuf);
    teardown();
    return true;
}

static bool t_write_read_1mb(void) {
    setup();
    const size_t SIZE = 1024 * 1024; /* 1 MB */
    unsigned char *wbuf = malloc(SIZE);
    unsigned char *rbuf = malloc(SIZE);
    T_ASSERT_PTR_NOT_NULL(wbuf, "alloc write buffer");
    T_ASSERT_PTR_NOT_NULL(rbuf, "alloc read buffer");

    fill_pattern(wbuf, SIZE, 0xAB);

    int wrc = sqlfs_proc_write(fs, "/large1mb.bin", (const char *)wbuf, SIZE, 0,
                               O_RDWR | O_CREAT | O_TRUNC);
    T_ASSERT_INT_EQ(wrc, (int)SIZE, "write 1MB");

    memset(rbuf, 0, SIZE);
    struct fuse_file_info fi = {0};
    int rrc = sqlfs_proc_read(fs, "/large1mb.bin", (char *)rbuf, SIZE, 0, &fi);
    T_ASSERT_INT_EQ(rrc, (int)SIZE, "read 1MB");
    T_ASSERT_TRUE(memcmp(wbuf, rbuf, SIZE) == 0, "1MB content matches");

    free(wbuf);
    free(rbuf);
    teardown();
    return true;
}

static bool t_exact_block_boundary(void) {
    setup();
    /* Test writing exactly BLOCK_SIZE, BLOCK_SIZE-1, BLOCK_SIZE+1 */
    const char *names[] = {"/block_minus1", "/block_exact", "/block_plus1"};
    const size_t sizes[] = {BLOCK_SIZE - 1, BLOCK_SIZE, BLOCK_SIZE + 1};

    for (int t = 0; t < 3; t++) {
        size_t sz = sizes[t];
        unsigned char *wbuf = malloc(sz);
        unsigned char *rbuf = malloc(sz);
        fill_pattern(wbuf, sz, (unsigned char)(0x10 + t));

        int wrc = sqlfs_proc_write(fs, names[t], (const char *)wbuf, sz, 0,
                                   O_RDWR | O_CREAT | O_TRUNC);
        T_ASSERT_INT_EQ(wrc, (int)sz, "write block boundary");

        memset(rbuf, 0, sz);
        struct fuse_file_info fi = {0};
        int rrc = sqlfs_proc_read(fs, names[t], (char *)rbuf, sz, 0, &fi);
        T_ASSERT_INT_EQ(rrc, (int)sz, "read block boundary");
        T_ASSERT_TRUE(memcmp(wbuf, rbuf, sz) == 0, "block boundary content matches");

        free(wbuf);
        free(rbuf);
    }
    teardown();
    return true;
}

static bool t_overwrite_middle_of_large_file(void) {
    setup();
    const size_t SIZE = 64 * 1024; /* 64 KB */
    unsigned char *buf = malloc(SIZE);
    T_ASSERT_PTR_NOT_NULL(buf, "alloc buffer");

    /* Fill with pattern A */
    fill_pattern(buf, SIZE, 0x00);
    sqlfs_proc_write(fs, "/middle.bin", (const char *)buf, SIZE, 0,
                     O_RDWR | O_CREAT | O_TRUNC);

    /* Overwrite 100 bytes in the middle with pattern B */
    const size_t OFFSET = SIZE / 2;
    const size_t OVERWRITE_LEN = 100;
    unsigned char patch[OVERWRITE_LEN];
    fill_pattern(patch, OVERWRITE_LEN, 0xFF);
    sqlfs_proc_write(fs, "/middle.bin", (const char *)patch, OVERWRITE_LEN, OFFSET, O_RDWR);

    /* Read back and verify */
    memset(buf, 0, SIZE);
    struct fuse_file_info fi = {0};
    int rrc = sqlfs_proc_read(fs, "/middle.bin", (char *)buf, SIZE, 0, &fi);
    T_ASSERT_INT_EQ(rrc, (int)SIZE, "read back full file");

    /* Verify beginning is untouched */
    T_ASSERT_TRUE(verify_pattern(buf, OFFSET, 0x00), "beginning untouched");

    /* Verify middle is patched */
    T_ASSERT_TRUE(memcmp(buf + OFFSET, patch, OVERWRITE_LEN) == 0, "middle patched");

    /* Verify end is untouched */
    T_ASSERT_TRUE(verify_pattern(buf + OFFSET + OVERWRITE_LEN,
                                  SIZE - OFFSET - OVERWRITE_LEN, 
                                  (unsigned char)(0x00 + OFFSET + OVERWRITE_LEN)),
                  "end untouched");

    free(buf);
    teardown();
    return true;
}

static bool t_multiple_block_writes(void) {
    setup();
    /* Write a large file in multiple small chunks */
    const size_t CHUNK = BLOCK_SIZE;
    const int NUM_CHUNKS = 4;
    const size_t TOTAL = CHUNK * NUM_CHUNKS;

    for (int i = 0; i < NUM_CHUNKS; i++) {
        unsigned char chunk[CHUNK];
        fill_pattern(chunk, CHUNK, (unsigned char)(i * 10));
        sqlfs_proc_write(fs, "/chunked.bin", (const char *)chunk, CHUNK,
                         i * CHUNK, O_RDWR | O_CREAT);
    }

    /* Verify total size */
    sqlfs_stat st;
    sqlfs_proc_getattr(fs, "/chunked.bin", &st);
    T_ASSERT_INT_EQ((int)st.st_size, (int)TOTAL, "total size correct");

    /* Read back and verify each chunk */
    for (int i = 0; i < NUM_CHUNKS; i++) {
        unsigned char rbuf[CHUNK];
        struct fuse_file_info fi = {0};
        int rrc = sqlfs_proc_read(fs, "/chunked.bin", (char *)rbuf, CHUNK,
                                  i * CHUNK, &fi);
        T_ASSERT_INT_EQ(rrc, (int)CHUNK, "read chunk");
        T_ASSERT_TRUE(verify_pattern(rbuf, CHUNK, (unsigned char)(i * 10)),
                      "chunk content matches");
    }

    teardown();
    return true;
}

/* ── main ─────────────────────────────────────────────────────── */
int main(void)
{
    printf(C_BOLD "\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf("   SQLFS  ·  Large File / Block Boundary Tests\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf(C_RESET "\n");

    TEST_SUITE("large file write/read");
    RUN_TEST(t_write_read_100kb);
    RUN_TEST(t_write_read_1mb);
    SUITE_END();

    TEST_SUITE("block boundaries");
    RUN_TEST(t_exact_block_boundary);
    RUN_TEST(t_multiple_block_writes);
    SUITE_END();

    TEST_SUITE("partial overwrite");
    RUN_TEST(t_overwrite_middle_of_large_file);
    SUITE_END();

    return test_summary("Large Files");
}
