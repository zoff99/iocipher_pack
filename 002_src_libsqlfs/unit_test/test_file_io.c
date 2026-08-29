/*
 * test_file_io.c — write / read / truncate / offset I/O
 */
#include <unistd.h>
#include <fcntl.h>
#include "test_framework.h"
#include "../sqlfs.h"

#define DB "_test_file_io.db"
static sqlfs_t *fs;

static void setup(void) {
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
    sqlfs_open(DB, &fs);
}
static void teardown(void) {
    sqlfs_close(fs);
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
}

/* ── tests ────────────────────────────────────────────────────── */

static bool t_write_read_small(void) {
    setup();
    const char *msg = "Hello, sqlfs!";
    int wrc = sqlfs_proc_write(fs, "/small.txt", msg, strlen(msg), 0,
                               O_RDWR | O_CREAT | O_TRUNC);
    T_ASSERT_INT_EQ(wrc, (int)strlen(msg), "write returns byte count");

    char buf[128] = {0};
    struct fuse_file_info fi = {0};
    int rrc = sqlfs_proc_read(fs, "/small.txt", buf, sizeof(buf), 0, &fi);
    T_ASSERT_INT_EQ(rrc, (int)strlen(msg), "read returns byte count");
    T_ASSERT_STR_EQ(buf, msg, "content matches");
    teardown();
    return true;
}

static bool t_write_read_binary(void) {
    setup();
    unsigned char data[256];
    for (int i = 0; i < 256; i++) data[i] = (unsigned char)i;

    int wrc = sqlfs_proc_write(fs, "/bin.dat", (const char *)data, 256, 0,
                               O_RDWR | O_CREAT | O_TRUNC);
    T_ASSERT_INT_EQ(wrc, 256, "write 256 bytes");

    unsigned char buf[256] = {0};
    struct fuse_file_info fi = {0};
    int rrc = sqlfs_proc_read(fs, "/bin.dat", (char *)buf, 256, 0, &fi);
    T_ASSERT_INT_EQ(rrc, 256, "read 256 bytes");
    T_ASSERT_TRUE(memcmp(data, buf, 256) == 0, "binary content matches");
    teardown();
    return true;
}

static bool t_write_at_offset(void) {
    setup();
    sqlfs_proc_write(fs, "/off.txt", "AAAA", 4, 0, O_RDWR | O_CREAT | O_TRUNC);
    sqlfs_proc_write(fs, "/off.txt", "BB", 2, 4, O_RDWR);

    char buf[16] = {0};
    struct fuse_file_info fi = {0};
    int rrc = sqlfs_proc_read(fs, "/off.txt", buf, sizeof(buf), 0, &fi);
    T_ASSERT_INT_EQ(rrc, 6, "total 6 bytes");
    T_ASSERT_STR_EQ(buf, "AAAABB", "content with offset write");
    teardown();
    return true;
}

static bool t_read_with_offset(void) {
    setup();
    sqlfs_proc_write(fs, "/offr.txt", "0123456789", 10, 0,
                     O_RDWR | O_CREAT | O_TRUNC);

    char buf[8] = {0};
    struct fuse_file_info fi = {0};
    int rrc = sqlfs_proc_read(fs, "/offr.txt", buf, 4, 3, &fi);
    T_ASSERT_INT_EQ(rrc, 4, "read 4 bytes from offset 3");
    T_ASSERT_TRUE(memcmp(buf, "3456", 4) == 0, "offset content");
    teardown();
    return true;
}

static bool t_truncate_to_zero(void) {
    setup();
    sqlfs_proc_write(fs, "/trunc.txt", "some data", 9, 0,
                     O_RDWR | O_CREAT | O_TRUNC);

    sqlfs_stat st;
    T_ASSERT_INT_EQ(sqlfs_proc_getattr(fs, "/trunc.txt", &st), 0, "getattr before");
    T_ASSERT_INT_EQ((int)st.st_size, 9, "size is 9");

    T_ASSERT_INT_EQ(sqlfs_proc_truncate(fs, "/trunc.txt", 0), 0, "truncate to 0");
    T_ASSERT_INT_EQ(sqlfs_proc_getattr(fs, "/trunc.txt", &st), 0, "getattr after");
    T_ASSERT_INT_EQ((int)st.st_size, 0, "size is 0");
    teardown();
    return true;
}

static bool t_truncate_extend(void) {
    setup();
    sqlfs_proc_write(fs, "/ext.txt", "AB", 2, 0, O_RDWR | O_CREAT | O_TRUNC);
    sqlfs_proc_truncate(fs, "/ext.txt", 10);

    sqlfs_stat st;
    sqlfs_proc_getattr(fs, "/ext.txt", &st);
    T_ASSERT_INT_EQ((int)st.st_size, 10, "extended to 10");
    teardown();
    return true;
}

static bool t_unlink_file(void) {
    setup();
    sqlfs_proc_write(fs, "/del.txt", "bye", 3, 0, O_RDWR | O_CREAT | O_TRUNC);
    T_ASSERT_INT_EQ(sqlfs_proc_unlink(fs, "/del.txt"), 0, "unlink returns 0");

    sqlfs_stat st;
    int rc = sqlfs_proc_getattr(fs, "/del.txt", &st);
    T_ASSERT_INT_EQ(rc, -2, "getattr returns -ENOENT after unlink");
    teardown();
    return true;
}

static bool t_overwrite_file(void) {
    setup();
    sqlfs_proc_write(fs, "/ow.txt", "first", 5, 0, O_RDWR | O_CREAT | O_TRUNC);
    sqlfs_proc_write(fs, "/ow.txt", "second!", 7, 0, O_RDWR | O_CREAT | O_TRUNC);

    char buf[32] = {0};
    struct fuse_file_info fi = {0};
    int rrc = sqlfs_proc_read(fs, "/ow.txt", buf, sizeof(buf), 0, &fi);
    T_ASSERT_INT_EQ(rrc, 7, "overwritten size");
    T_ASSERT_STR_EQ(buf, "second!", "overwritten content");
    teardown();
    return true;
}

/* ── main ─────────────────────────────────────────────────────── */
int main(void)
{
    printf(C_BOLD "\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf("   SQLFS  ·  File I/O Tests\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf(C_RESET "\n");

    TEST_SUITE("write / read");
    RUN_TEST(t_write_read_small);
    RUN_TEST(t_write_read_binary);
    RUN_TEST(t_overwrite_file);
    SUITE_END();

    TEST_SUITE("offset I/O");
    RUN_TEST(t_write_at_offset);
    RUN_TEST(t_read_with_offset);
    SUITE_END();

    TEST_SUITE("truncate");
    RUN_TEST(t_truncate_to_zero);
    RUN_TEST(t_truncate_extend);
    SUITE_END();

    TEST_SUITE("unlink");
    RUN_TEST(t_unlink_file);
    SUITE_END();

    return test_summary("File I/O");
}
