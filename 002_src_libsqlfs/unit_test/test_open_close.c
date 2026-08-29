/*
 * test_open_close.c — sqlfs_open / sqlfs_close / instance count
 */
#include <unistd.h>
#include "test_framework.h"
#include "../sqlfs.h"

#define DB "_test_open_close.db"

/* ── helpers ──────────────────────────────────────────────────── */
static void cleanup(void) {
    unlink(DB);
    unlink(DB "-wal");
    unlink(DB "-shm");
    unlink(DB "-journal");
}

/* ── tests ────────────────────────────────────────────────────── */

static bool t_open_new_db(void) {
    cleanup();
    sqlfs_t *fs = NULL;
    int rc = sqlfs_open(DB, &fs);
    T_ASSERT_INT_EQ(rc, 1, "sqlfs_open returns 1");
    T_ASSERT_PTR_NOT_NULL(fs, "sqlfs pointer non-NULL");
    sqlfs_close(fs);
    cleanup();
    return true;
}

static bool t_open_close_cycle(void) {
    cleanup();
    sqlfs_t *fs = NULL;
    T_ASSERT_INT_EQ(sqlfs_open(DB, &fs), 1, "open");
    T_ASSERT_INT_EQ(sqlfs_close(fs), 1, "close returns 1");
    cleanup();
    return true;
}

static bool t_reopen_after_close(void) {
    cleanup();
    sqlfs_t *fs = NULL;

    /* first open/close */
    T_ASSERT_INT_EQ(sqlfs_open(DB, &fs), 1, "first open");
    T_ASSERT_INT_EQ(sqlfs_close(fs), 1, "first close");

    /* second open/close on same file */
    fs = NULL;
    T_ASSERT_INT_EQ(sqlfs_open(DB, &fs), 1, "second open");
    T_ASSERT_PTR_NOT_NULL(fs, "second pointer non-NULL");
    T_ASSERT_INT_EQ(sqlfs_close(fs), 1, "second close");

    cleanup();
    return true;
}

static bool t_instance_count_zero_after_close(void) {
    cleanup();
    sqlfs_t *fs = NULL;
    sqlfs_open(DB, &fs);
    sqlfs_close(fs);
    T_ASSERT_INT_EQ(sqlfs_instance_count(), 0, "instance count == 0");
    cleanup();
    return true;
}

static bool t_open_creates_root_dir(void) {
    cleanup();
    sqlfs_t *fs = NULL;
    sqlfs_open(DB, &fs);

    /* root "/" must exist and be a directory after open */
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/"), 1, "root is a dir");

    sqlfs_close(fs);
    cleanup();
    return true;
}

/* ── main ─────────────────────────────────────────────────────── */
int main(void)
{
    printf(C_BOLD "\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf("   SQLFS  ·  Open / Close / Lifecycle Tests\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf(C_RESET "\n");

    TEST_SUITE("sqlfs_open / sqlfs_close");
    RUN_TEST(t_open_new_db);
    RUN_TEST(t_open_close_cycle);
    RUN_TEST(t_reopen_after_close);
    SUITE_END();

    TEST_SUITE("instance tracking");
    RUN_TEST(t_instance_count_zero_after_close);
    SUITE_END();

    TEST_SUITE("root directory");
    RUN_TEST(t_open_creates_root_dir);
    SUITE_END();

    return test_summary("Open / Close");
}
