/*
 * test_symlink.c — symlinks and readlink
 */
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include "test_framework.h"
#include "../sqlfs.h"

#define DB "_test_symlink.db"
static sqlfs_t *fs;

static void setup(void) {
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
    sqlfs_open(DB, &fs);
}
static void teardown(void) {
    sqlfs_close(fs);
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
}

static bool t_symlink_and_readlink(void) {
    setup();
    sqlfs_proc_write(fs, "/target.txt", "hello", 5, 0, O_RDWR | O_CREAT | O_TRUNC);
    
    /* FUSE symlink signature: (target, linkpath) */
    int rc = sqlfs_proc_symlink(fs, "/target.txt", "/link.txt");
    T_ASSERT_INT_EQ(rc, 0, "symlink returns 0");
    
    char buf[256] = {0};
    rc = sqlfs_proc_readlink(fs, "/link.txt", buf, sizeof(buf));
    T_ASSERT_INT_EQ(rc, 0, "readlink returns 0");
    T_ASSERT_STR_EQ(buf, "/target.txt", "readlink matches target");
    
    teardown();
    return true;
}

static bool t_symlink_getattr(void) {
    setup();
    sqlfs_proc_write(fs, "/real.txt", "data", 4, 0, O_RDWR | O_CREAT | O_TRUNC);
    sqlfs_proc_symlink(fs, "/real.txt", "/sym.txt");
    
    sqlfs_stat st;
    int rc = sqlfs_proc_getattr(fs, "/sym.txt", &st);
    T_ASSERT_INT_EQ(rc, 0, "getattr on symlink");
    T_ASSERT_TRUE(S_ISLNK(st.st_mode), "mode is symlink");
    
    teardown();
    return true;
}

static bool t_symlink_unlink(void) {
    setup();
    sqlfs_proc_write(fs, "/keep.txt", "keep", 4, 0, O_RDWR | O_CREAT | O_TRUNC);
    sqlfs_proc_symlink(fs, "/keep.txt", "/deleteme.lnk");
    
    T_ASSERT_INT_EQ(sqlfs_proc_unlink(fs, "/deleteme.lnk"), 0, "unlink symlink");
    
    sqlfs_stat st;
    T_ASSERT_INT_NE(sqlfs_proc_getattr(fs, "/deleteme.lnk", &st), 0, "symlink gone");
    T_ASSERT_INT_EQ(sqlfs_proc_getattr(fs, "/keep.txt", &st), 0, "target still exists");
    
    teardown();
    return true;
}

int main(void) {
    printf(C_BOLD "\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf("   SQLFS  ·  Symlink Tests\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf(C_RESET "\n");

    TEST_SUITE("symlinks");
    RUN_TEST(t_symlink_and_readlink);
    RUN_TEST(t_symlink_getattr);
    RUN_TEST(t_symlink_unlink);
    SUITE_END();

    return test_summary("Symlinks");
}
