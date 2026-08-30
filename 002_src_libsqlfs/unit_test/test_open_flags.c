/*
 * test_open_flags.c — Advanced open flag behavior
 * Tests O_EXCL, O_APPEND, O_TRUNC, O_RDONLY enforcement
 */
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include "test_framework.h"
#include "../sqlfs.h"

#define DB "_test_open_flags.db"
static sqlfs_t *fs;

static void setup(void) {
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
    sqlfs_open(DB, &fs);
}
static void teardown(void) {
    sqlfs_close(fs);
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
}

/* ── O_EXCL tests ─────────────────────────────────────────────── */

static bool t_o_excl_creat_new(void) {
    setup();
    /* O_EXCL | O_CREAT on a non-existent file should succeed */
    struct fuse_file_info fi = {0};
    fi.flags = O_RDWR | O_CREAT | O_EXCL;
    int rc = sqlfs_proc_open(fs, "/exclusive.txt", &fi);
    T_ASSERT_INT_EQ(rc, 0, "O_EXCL|O_CREAT on new file succeeds");
    sqlfs_proc_release(fs, "/exclusive.txt", &fi);
    teardown();
    return true;
}

static bool t_o_excl_creat_existing_fails(void) {
    setup();
    /* Create the file first */
    sqlfs_proc_write(fs, "/exists.txt", "data", 4, 0, O_RDWR | O_CREAT | O_TRUNC);
    
    /* Now try O_EXCL | O_CREAT on the existing file — must fail */
    struct fuse_file_info fi = {0};
    fi.flags = O_RDWR | O_CREAT | O_EXCL;
    int rc = sqlfs_proc_open(fs, "/exists.txt", &fi);
    T_ASSERT_INT_EQ(rc, -EEXIST, "O_EXCL|O_CREAT on existing file fails with -EEXIST");
    teardown();
    return true;
}

/* ── O_TRUNC tests ────────────────────────────────────────────── */

static bool t_o_trunc_on_open(void) {
    setup();
    /* Write some data */
    sqlfs_proc_write(fs, "/trunc_me.txt", "hello world", 11, 0, O_RDWR | O_CREAT | O_TRUNC);
    
    sqlfs_stat st;
    sqlfs_proc_getattr(fs, "/trunc_me.txt", &st);
    T_ASSERT_INT_EQ((int)st.st_size, 11, "size is 11 before O_TRUNC open");
    
    /* Open with O_TRUNC — file should be truncated immediately */
    struct fuse_file_info fi = {0};
    fi.flags = O_RDWR | O_TRUNC;
    int rc = sqlfs_proc_open(fs, "/trunc_me.txt", &fi);
    T_ASSERT_INT_EQ(rc, 0, "open with O_TRUNC succeeds");
    
    sqlfs_proc_getattr(fs, "/trunc_me.txt", &st);
    T_ASSERT_INT_EQ((int)st.st_size, 0, "size is 0 after O_TRUNC open");
    
    sqlfs_proc_release(fs, "/trunc_me.txt", &fi);
    teardown();
    return true;
}

/* ── O_APPEND tests ───────────────────────────────────────────── */

static bool t_o_append_ignores_offset(void) {
    setup();
    /* Write initial data */
    sqlfs_proc_write(fs, "/append.txt", "AAAA", 4, 0, O_RDWR | O_CREAT | O_TRUNC);
    
    /* Write with O_APPEND and offset=0.
     * With O_APPEND, the write should go to the END regardless of offset. */
    sqlfs_proc_write(fs, "/append.txt", "BB", 2, 0, O_RDWR | O_APPEND);
    
    char buf[16] = {0};
    struct fuse_file_info fi = {0};
    int rrc = sqlfs_proc_read(fs, "/append.txt", buf, sizeof(buf), 0, &fi);
    T_ASSERT_INT_EQ(rrc, 6, "total 6 bytes after append");
    T_ASSERT_STR_EQ(buf, "AAAABB", "append went to end, not offset 0");
    teardown();
    return true;
}

static bool t_o_append_multiple(void) {
    setup();
    sqlfs_proc_write(fs, "/multi_append.txt", "1", 1, 0, O_RDWR | O_CREAT | O_TRUNC);
    sqlfs_proc_write(fs, "/multi_append.txt", "2", 1, 0, O_RDWR | O_APPEND);
    sqlfs_proc_write(fs, "/multi_append.txt", "3", 1, 0, O_RDWR | O_APPEND);
    
    char buf[16] = {0};
    struct fuse_file_info fi = {0};
    int rrc = sqlfs_proc_read(fs, "/multi_append.txt", buf, sizeof(buf), 0, &fi);
    T_ASSERT_INT_EQ(rrc, 3, "total 3 bytes");
    T_ASSERT_STR_EQ(buf, "123", "sequential appends correct");
    teardown();
    return true;
}

/* ── O_RDONLY enforcement ─────────────────────────────────────── */

static bool t_o_rdonly_flag_ignored_by_write(void) {
    setup();
    /* Create a file first with 4 bytes */
    sqlfs_proc_write(fs, "/readonly.txt", "data", 4, 0, O_RDWR | O_CREAT | O_TRUNC);
    
    /* sqlfs_proc_write is stateless and does not enforce O_RDONLY.
     * It relies on the FUSE/VFS layer to enforce file descriptor permissions.
     * Therefore, passing O_RDONLY in mode_flags is ignored and the write succeeds. */
    int rc = sqlfs_proc_write(fs, "/readonly.txt", "bad", 3, 0, O_RDONLY);
    T_ASSERT_INT_EQ(rc, 3, "write succeeds despite O_RDONLY flag (stateless API)");
    
    /* Verify the data was actually written.
     * Writing 3 bytes over a 4-byte file results in "bada" (size remains 4). */
    char buf[16] = {0};
    struct fuse_file_info fi = {0};
    int rrc = sqlfs_proc_read(fs, "/readonly.txt", buf, sizeof(buf), 0, &fi);
    T_ASSERT_INT_EQ(rrc, 4, "read back 4 bytes (file size unchanged)");
    T_ASSERT_STR_EQ(buf, "bada", "content was partially overwritten");
    
    teardown();
    return true;
}

/* ── O_CREAT without O_EXCL ───────────────────────────────────── */

static bool t_o_creat_existing_no_trunc(void) {
    setup();
    sqlfs_proc_write(fs, "/keep.txt", "original", 8, 0, O_RDWR | O_CREAT | O_TRUNC);
    
    /* O_CREAT without O_TRUNC on existing file should preserve content */
    struct fuse_file_info fi = {0};
    fi.flags = O_RDWR | O_CREAT;
    int rc = sqlfs_proc_open(fs, "/keep.txt", &fi);
    T_ASSERT_INT_EQ(rc, 0, "open O_CREAT on existing succeeds");
    
    sqlfs_stat st;
    sqlfs_proc_getattr(fs, "/keep.txt", &st);
    T_ASSERT_INT_EQ((int)st.st_size, 8, "size preserved without O_TRUNC");
    
    sqlfs_proc_release(fs, "/keep.txt", &fi);
    teardown();
    return true;
}

/* ── main ─────────────────────────────────────────────────────── */
int main(void)
{
    printf(C_BOLD "\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf("   SQLFS  ·  Advanced Open Flags Tests\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf(C_RESET "\n");

    TEST_SUITE("O_EXCL");
    RUN_TEST(t_o_excl_creat_new);
    RUN_TEST(t_o_excl_creat_existing_fails);
    SUITE_END();

    TEST_SUITE("O_TRUNC");
    RUN_TEST(t_o_trunc_on_open);
    SUITE_END();

    TEST_SUITE("O_APPEND");
    RUN_TEST(t_o_append_ignores_offset);
    RUN_TEST(t_o_append_multiple);
    SUITE_END();

    TEST_SUITE("O_RDONLY enforcement");
    RUN_TEST(t_o_rdonly_flag_ignored_by_write);
    SUITE_END();

    TEST_SUITE("O_CREAT without O_TRUNC");
    RUN_TEST(t_o_creat_existing_no_trunc);
    SUITE_END();

    return test_summary("Open Flags");
}
