/*
 * test_attributes.c — getattr / access / stat / metadata
 */
#include <unistd.h>
#include <fcntl.h>
#include "test_framework.h"
#include "../sqlfs.h"

#define DB "_test_attributes.db"
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

static bool t_getattr_root(void) {
    setup();
    sqlfs_stat st;
    int rc = sqlfs_proc_getattr(fs, "/", &st);
    T_ASSERT_INT_EQ(rc, 0, "getattr / returns 0");
    T_ASSERT_TRUE(S_ISDIR(st.st_mode), "root is a directory");
    teardown();
    return true;
}

static bool t_getattr_directory(void) {
    setup();
    sqlfs_proc_mkdir(fs, "/attrdir", 0755);
    sqlfs_stat st;
    int rc = sqlfs_proc_getattr(fs, "/attrdir", &st);
    T_ASSERT_INT_EQ(rc, 0, "getattr dir");
    T_ASSERT_TRUE(S_ISDIR(st.st_mode), "is directory");
    teardown();
    return true;
}

static bool t_getattr_file_size(void) {
    setup();
    const char *content = "1234567890";
    sqlfs_proc_write(fs, "/sized.txt", content, 10, 0,
                     O_RDWR | O_CREAT | O_TRUNC);

    sqlfs_stat st;
    int rc = sqlfs_proc_getattr(fs, "/sized.txt", &st);
    T_ASSERT_INT_EQ(rc, 0, "getattr file");
    T_ASSERT_TRUE(S_ISREG(st.st_mode), "is regular file");
    T_ASSERT_INT_EQ((int)st.st_size, 10, "size == 10");
    teardown();
    return true;
}

static bool t_getattr_nonexistent(void) {
    setup();
    sqlfs_stat st;
    int rc = sqlfs_proc_getattr(fs, "/ghost", &st);
    T_ASSERT_INT_EQ(rc, -2, "getattr missing returns -ENOENT");
    teardown();
    return true;
}

static bool t_access_root(void) {
    setup();
    int rc = sqlfs_proc_access(fs, "/", F_OK);
    T_ASSERT_INT_EQ(rc, 0, "root is accessible");
    teardown();
    return true;
}

static bool t_access_nonexistent(void) {
    setup();
    int rc = sqlfs_proc_access(fs, "/nope", F_OK);
    T_ASSERT_INT_EQ(rc, -2, "missing path returns -ENOENT");
    teardown();
    return true;
}

static bool t_file_size_grows_with_write(void) {
    setup();
    sqlfs_proc_write(fs, "/grow.txt", "abc", 3, 0, O_RDWR | O_CREAT | O_TRUNC);
    sqlfs_stat st;
    sqlfs_proc_getattr(fs, "/grow.txt", &st);
    T_ASSERT_INT_EQ((int)st.st_size, 3, "size 3 after first write");

    sqlfs_proc_write(fs, "/grow.txt", "de", 2, 3, O_RDWR);
    sqlfs_proc_getattr(fs, "/grow.txt", &st);
    T_ASSERT_INT_EQ((int)st.st_size, 5, "size 5 after append");
    teardown();
    return true;
}

/* ── main ─────────────────────────────────────────────────────── */
int main(void)
{
    printf(C_BOLD "\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf("   SQLFS  ·  Attribute / Metadata Tests\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf(C_RESET "\n");

    TEST_SUITE("getattr");
    RUN_TEST(t_getattr_root);
    RUN_TEST(t_getattr_directory);
    RUN_TEST(t_getattr_file_size);
    RUN_TEST(t_getattr_nonexistent);
    SUITE_END();

    TEST_SUITE("access");
    RUN_TEST(t_access_root);
    RUN_TEST(t_access_nonexistent);
    SUITE_END();

    TEST_SUITE("size tracking");
    RUN_TEST(t_file_size_grows_with_write);
    SUITE_END();

    return test_summary("Attributes");
}
