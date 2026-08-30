/*
 * test_path_resolution.c — Path edge cases: slashes, dots, root protection
 * Documents that sqlfs.c treats paths as literal keys and does not
 * perform POSIX path normalization (//, ., .., trailing slashes).
 */
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <stdio.h>
#include "test_framework.h"
#include "../sqlfs.h"

#define DB "_test_path_resolution.db"
static sqlfs_t *fs;

static void setup(void) {
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
    sqlfs_open(DB, &fs);
}
static void teardown(void) {
    sqlfs_close(fs);
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
}

/* ── Trailing slash tests ─────────────────────────────────────── */

static bool t_trailing_slash_dir(void) {
    setup();
    /* sqlfs treats paths as literal keys. mkdir with trailing slash
     * creates a key that includes the slash. */
    int rc = sqlfs_proc_mkdir(fs, "/trail_dir/", 0755);
    T_ASSERT_INT_EQ(rc, 0, "mkdir with trailing slash");

    /* The directory exists under the literal key "/trail_dir/" */
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/trail_dir/"), 1, "dir exists with slash");

    /* Accessing without slash fails because it's a different key.
     * This documents that sqlfs does not strip trailing slashes. */
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/trail_dir"), 0, "dir missing without slash (no normalization)");
    teardown();
    return true;
}

static bool t_trailing_slash_on_file_fails(void) {
    setup();
    sqlfs_proc_write(fs, "/afile.txt", "x", 1, 0, O_RDWR | O_CREAT | O_TRUNC);

    /* getattr on a file path with trailing slash should fail
     * because "/afile.txt/" is a different key than "/afile.txt" */
    sqlfs_stat st;
    int rc = sqlfs_proc_getattr(fs, "/afile.txt/", &st);
    T_ASSERT_TRUE(rc != 0, "getattr file with trailing slash fails");
    teardown();
    return true;
}

/* ── Double slash tests ───────────────────────────────────────── */

static bool t_double_slash_resolution(void) {
    setup();
    sqlfs_proc_mkdir(fs, "/parent", 0755);
    sqlfs_proc_write(fs, "/parent/child.txt", "data", 4, 0, O_RDWR | O_CREAT | O_TRUNC);

    /* sqlfs does not normalize double slashes.
     * "//parent//child.txt" is a different key than "/parent/child.txt". */
    sqlfs_stat st;
    int rc = sqlfs_proc_getattr(fs, "//parent//child.txt", &st);
    T_ASSERT_INT_EQ(rc, -2, "double slash path fails (no normalization)");
    teardown();
    return true;
}

/* ── Dot and dot-dot tests ────────────────────────────────────── */

static bool t_dot_resolution(void) {
    setup();
    sqlfs_proc_mkdir(fs, "/dotdir", 0755);
    sqlfs_proc_write(fs, "/dotdir/file.txt", "dot", 3, 0, O_RDWR | O_CREAT | O_TRUNC);

    /* sqlfs does not resolve "." components */
    sqlfs_stat st;
    int rc = sqlfs_proc_getattr(fs, "/dotdir/./file.txt", &st);
    T_ASSERT_INT_EQ(rc, -2, "dot path fails (no normalization)");
    teardown();
    return true;
}

static bool t_dotdot_resolution(void) {
    setup();
    sqlfs_proc_mkdir(fs, "/aaa", 0755);
    sqlfs_proc_mkdir(fs, "/aaa/bbb", 0755);
    sqlfs_proc_write(fs, "/aaa/target.txt", "found", 5, 0, O_RDWR | O_CREAT | O_TRUNC);

    /* sqlfs does not resolve ".." components */
    sqlfs_stat st;
    int rc = sqlfs_proc_getattr(fs, "/aaa/bbb/../target.txt", &st);
    T_ASSERT_INT_EQ(rc, -2, "dot-dot path fails (no normalization)");
    teardown();
    return true;
}

/* ── Root protection tests ────────────────────────────────────── */

static bool t_cannot_unlink_root(void) {
    setup();
    int rc = sqlfs_proc_unlink(fs, "/");
    T_ASSERT_TRUE(rc != 0, "unlink / fails");
    teardown();
    return true;
}

static bool t_cannot_rmdir_root(void) {
    setup();
    int rc = sqlfs_proc_rmdir(fs, "/");
    T_ASSERT_TRUE(rc != 0, "rmdir / fails");
    teardown();
    return true;
}

static bool t_cannot_rename_root(void) {
    setup();
    /* Requires the sqlfs.c fix:
     *   if (strcmp(from, "/") == 0) return -EINVAL;
     * at the top of sqlfs_proc_rename. */
    int rc = sqlfs_proc_rename(fs, "/", "/new_root");
    T_ASSERT_TRUE(rc != 0, "rename / fails");
    teardown();
    return true;
}

static bool t_root_always_exists(void) {
    setup();
    sqlfs_stat st;
    int rc = sqlfs_proc_getattr(fs, "/", &st);
    T_ASSERT_INT_EQ(rc, 0, "root always accessible");
    T_ASSERT_TRUE(S_ISDIR(st.st_mode), "root is a directory");
    teardown();
    return true;
}

/* ── Deep nesting tests ───────────────────────────────────────── */

static bool t_moderately_deep_nesting(void) {
    setup();
    /* Create a 10-level deep path */
    char path[256] = "";
    for (int i = 0; i < 10; i++) {
        char segment[32];
        snprintf(segment, sizeof(segment), "/level%d", i);
        strcat(path, segment);
        int rc = sqlfs_proc_mkdir(fs, path, 0755);
        T_ASSERT_INT_EQ(rc, 0, "create nested level");
    }

    /* Write a file at the bottom.
     * filepath must be large enough for path + "/deepfile.txt" + NUL. */
    char filepath[512];
    int written = snprintf(filepath, sizeof(filepath), "%s/deepfile.txt", path);
    T_ASSERT_TRUE(written > 0 && written < (int)sizeof(filepath), "path fits in buffer");

    sqlfs_proc_write(fs, filepath, "deep", 4, 0, O_RDWR | O_CREAT | O_TRUNC);

    sqlfs_stat st;
    int rc = sqlfs_proc_getattr(fs, filepath, &st);
    T_ASSERT_INT_EQ(rc, 0, "deep file accessible");
    T_ASSERT_INT_EQ((int)st.st_size, 4, "deep file has content");
    teardown();
    return true;
}

/* ── Empty path tests ─────────────────────────────────────────── */

static bool t_empty_path_fails(void) {
    setup();
    sqlfs_stat st;
    int rc = sqlfs_proc_getattr(fs, "", &st);
    T_ASSERT_TRUE(rc != 0, "empty path fails");
    teardown();
    return true;
}

/* ── main ─────────────────────────────────────────────────────── */
int main(void)
{
    printf(C_BOLD "\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf("   SQLFS  ·  Path Resolution & Edge Case Tests\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf(C_RESET "\n");

    TEST_SUITE("trailing slashes");
    RUN_TEST(t_trailing_slash_dir);
    RUN_TEST(t_trailing_slash_on_file_fails);
    SUITE_END();

    TEST_SUITE("slash normalization");
    RUN_TEST(t_double_slash_resolution);
    SUITE_END();

    TEST_SUITE("dot / dot-dot");
    RUN_TEST(t_dot_resolution);
    RUN_TEST(t_dotdot_resolution);
    SUITE_END();

    TEST_SUITE("root protection");
    RUN_TEST(t_cannot_unlink_root);
    RUN_TEST(t_cannot_rmdir_root);
    RUN_TEST(t_cannot_rename_root);
    RUN_TEST(t_root_always_exists);
    SUITE_END();

    TEST_SUITE("deep nesting");
    RUN_TEST(t_moderately_deep_nesting);
    SUITE_END();

    TEST_SUITE("empty path");
    RUN_TEST(t_empty_path_fails);
    SUITE_END();

    return test_summary("Path Resolution");
}
