/*
 * test_transaction.c — begin / complete / break transaction
 */
#include <unistd.h>
#include <fcntl.h>
#include "test_framework.h"
#include "../sqlfs.h"

#define DB "_test_transaction.db"
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

static bool t_begin_complete(void) {
    setup();
    int rc = sqlfs_begin_transaction(fs);
    T_ASSERT_INT_EQ(rc, 1, "begin returns 1");

    sqlfs_proc_mkdir(fs, "/tx_dir", 0755);

    rc = sqlfs_complete_transaction(fs, 1);
    T_ASSERT_INT_EQ(rc, 1, "complete returns 1");
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/tx_dir"), 1, "dir persists");
    teardown();
    return true;
}

static bool t_begin_break_rollback(void) {
    setup();
    sqlfs_proc_mkdir(fs, "/keeper", 0755);

    int rc = sqlfs_begin_transaction(fs);
    T_ASSERT_INT_EQ(rc, 1, "begin");

    sqlfs_proc_mkdir(fs, "/will_vanish", 0755);

    rc = sqlfs_break_transaction(fs);
    T_ASSERT_INT_EQ(rc, 1, "break returns 1");

    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/keeper"), 1, "pre-existing dir still there");
    teardown();
    return true;
}

static bool t_write_inside_transaction(void) {
    setup();
    sqlfs_begin_transaction(fs);
    sqlfs_proc_write(fs, "/tx_file.txt", "data", 4, 0,
                     O_RDWR | O_CREAT | O_TRUNC);
    sqlfs_complete_transaction(fs, 1);

    char buf[16] = {0};
    struct fuse_file_info fi = {0};
    int rrc = sqlfs_proc_read(fs, "/tx_file.txt", buf, sizeof(buf), 0, &fi);
    T_ASSERT_INT_EQ(rrc, 4, "file written in tx is readable");
    T_ASSERT_STR_EQ(buf, "data", "content matches");
    teardown();
    return true;
}

static bool t_multiple_operations_in_tx(void) {
    setup();
    sqlfs_begin_transaction(fs);
    sqlfs_proc_mkdir(fs, "/multi", 0755);
    sqlfs_proc_mkdir(fs, "/multi/sub", 0755);
    sqlfs_proc_write(fs, "/multi/file.txt", "hello", 5, 0,
                     O_RDWR | O_CREAT | O_TRUNC);
    sqlfs_complete_transaction(fs, 1);

    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/multi"), 1, "/multi exists");
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/multi/sub"), 1, "/multi/sub exists");

    char buf[16] = {0};
    struct fuse_file_info fi = {0};
    sqlfs_proc_read(fs, "/multi/file.txt", buf, sizeof(buf), 0, &fi);
    T_ASSERT_STR_EQ(buf, "hello", "file content");
    teardown();
    return true;
}

/* ── main ─────────────────────────────────────────────────────── */
int main(void)
{
    printf(C_BOLD "\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf("   SQLFS  ·  Transaction Tests\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf(C_RESET "\n");

    TEST_SUITE("begin / complete");
    RUN_TEST(t_begin_complete);
    RUN_TEST(t_write_inside_transaction);
    RUN_TEST(t_multiple_operations_in_tx);
    SUITE_END();

    TEST_SUITE("begin / break (rollback)");
    RUN_TEST(t_begin_break_rollback);
    SUITE_END();

    return test_summary("Transactions");
}
