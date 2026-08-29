/*
 * test_directory.c — mkdir / rmdir / is_dir / nested dirs
 */
#include <unistd.h>
#include <fcntl.h>
#include "test_framework.h"
#include "../sqlfs.h"

#define DB "_test_directory.db"
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

static bool t_mkdir_basic(void) {
    setup();
    int rc = sqlfs_proc_mkdir(fs, "/alpha", 0755);
    T_ASSERT_INT_EQ(rc, 0, "mkdir /alpha returns 0");
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/alpha"), 1, "is_dir /alpha");
    teardown();
    return true;
}

static bool t_rmdir_basic(void) {
    setup();
    sqlfs_proc_mkdir(fs, "/toremove", 0755);
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/toremove"), 1, "exists before rmdir");
    int rc = sqlfs_proc_rmdir(fs, "/toremove");
    T_ASSERT_INT_EQ(rc, 0, "rmdir returns 0");
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/toremove"), 0, "gone after rmdir");
    teardown();
    return true;
}

static bool t_rmdir_nonexistent_is_noop(void) {
    setup();
    /* sqlfs_proc_rmdir is idempotent: returns 0 even if path never existed */
    int rc = sqlfs_proc_rmdir(fs, "/no_such_dir");
    T_ASSERT_INT_EQ(rc, 0, "rmdir non-existent returns 0 (idempotent)");
    /* confirm it still doesn't exist */
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/no_such_dir"), 0, "still not a dir");
    teardown();
    return true;
}

static bool t_rmdir_then_recreate(void) {
    setup();
    sqlfs_proc_mkdir(fs, "/cycle", 0755);
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/cycle"), 1, "exists");
    sqlfs_proc_rmdir(fs, "/cycle");
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/cycle"), 0, "removed");
    /* recreate same name */
    T_ASSERT_INT_EQ(sqlfs_proc_mkdir(fs, "/cycle", 0755), 0, "recreate");
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/cycle"), 1, "exists again");
    teardown();
    return true;
}

static bool t_is_dir_on_file_returns_false(void) {
    setup();
    const char *data = "hello";
    sqlfs_proc_write(fs, "/afile", data, 5, 0, O_RDWR | O_CREAT | O_TRUNC);
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/afile"), 0, "file is not a dir");
    teardown();
    return true;
}

static bool t_nested_mkdir(void) {
    setup();
    T_ASSERT_INT_EQ(sqlfs_proc_mkdir(fs, "/n1", 0755), 0, "mkdir /n1");
    T_ASSERT_INT_EQ(sqlfs_proc_mkdir(fs, "/n1/n2", 0755), 0, "mkdir /n1/n2");
    T_ASSERT_INT_EQ(sqlfs_proc_mkdir(fs, "/n1/n2/n3", 0755), 0, "mkdir /n1/n2/n3");
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/n1/n2/n3"), 1, "deep dir exists");
    teardown();
    return true;
}

static bool t_mkdir_duplicate(void) {
    setup();
    T_ASSERT_INT_EQ(sqlfs_proc_mkdir(fs, "/dup", 0755), 0, "first mkdir");
    int rc = sqlfs_proc_mkdir(fs, "/dup", 0755);
    T_ASSERT_INT_NE(rc, 0, "duplicate mkdir fails");
    teardown();
    return true;
}

static bool t_mkdir_in_root(void) {
    setup();
    T_ASSERT_INT_EQ(sqlfs_proc_mkdir(fs, "/root_child", 0700), 0, "mkdir under /");
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/root_child"), 1, "child is dir");
    teardown();
    return true;
}

/* ── main ─────────────────────────────────────────────────────── */
int main(void)
{
    printf(C_BOLD "\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf("   SQLFS  ·  Directory Operation Tests\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf(C_RESET "\n");

    TEST_SUITE("mkdir");
    RUN_TEST(t_mkdir_basic);
    RUN_TEST(t_mkdir_in_root);
    RUN_TEST(t_mkdir_duplicate);
    RUN_TEST(t_nested_mkdir);
    SUITE_END();

    TEST_SUITE("rmdir");
    RUN_TEST(t_rmdir_basic);
    RUN_TEST(t_rmdir_nonexistent_is_noop);
    RUN_TEST(t_rmdir_then_recreate);
    SUITE_END();

    TEST_SUITE("is_dir");
    RUN_TEST(t_is_dir_on_file_returns_false);
    SUITE_END();

    return test_summary("Directory Ops");
}
