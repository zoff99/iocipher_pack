/*
 * test_rename.c — renaming and moving files and directories
 */
#include <unistd.h>
#include <fcntl.h>
#include "test_framework.h"
#include "../sqlfs.h"

#define DB "_test_rename.db"
static sqlfs_t *fs;

static void setup(void) {
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
    sqlfs_open(DB, &fs);
}
static void teardown(void) {
    sqlfs_close(fs);
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
}

static bool t_rename_file(void) {
    setup();
    sqlfs_proc_write(fs, "/old.txt", "data", 4, 0, O_RDWR | O_CREAT | O_TRUNC);
    int rc = sqlfs_proc_rename(fs, "/old.txt", "/new.txt");
    T_ASSERT_INT_EQ(rc, 0, "rename returns 0");
    
    sqlfs_stat st;
    T_ASSERT_INT_NE(sqlfs_proc_getattr(fs, "/old.txt", &st), 0, "old gone");
    T_ASSERT_INT_EQ(sqlfs_proc_getattr(fs, "/new.txt", &st), 0, "new exists");
    T_ASSERT_INT_EQ((int)st.st_size, 4, "size preserved");
    teardown();
    return true;
}

static bool t_rename_dir(void) {
    setup();
    sqlfs_proc_mkdir(fs, "/dirA", 0755);
    sqlfs_proc_write(fs, "/dirA/file.txt", "x", 1, 0, O_RDWR | O_CREAT | O_TRUNC);
    
    int rc = sqlfs_proc_rename(fs, "/dirA", "/dirB");
    T_ASSERT_INT_EQ(rc, 0, "rename dir returns 0");
    
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/dirA"), 0, "old dir gone");
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/dirB"), 1, "new dir exists");
    
    sqlfs_stat st;
    T_ASSERT_INT_EQ(sqlfs_proc_getattr(fs, "/dirB/file.txt", &st), 0, "child moved");
    teardown();
    return true;
}

static bool t_move_file_to_dir(void) {
    setup();
    sqlfs_proc_mkdir(fs, "/dest", 0755);
    sqlfs_proc_write(fs, "/root_file.txt", "hello", 5, 0, O_RDWR | O_CREAT | O_TRUNC);
    
    int rc = sqlfs_proc_rename(fs, "/root_file.txt", "/dest/root_file.txt");
    T_ASSERT_INT_EQ(rc, 0, "move to dir returns 0");
    
    sqlfs_stat st;
    T_ASSERT_INT_NE(sqlfs_proc_getattr(fs, "/root_file.txt", &st), 0, "old gone");
    T_ASSERT_INT_EQ(sqlfs_proc_getattr(fs, "/dest/root_file.txt", &st), 0, "new exists");
    teardown();
    return true;
}

static bool t_rename_nonexistent(void) {
    setup();
    int rc = sqlfs_proc_rename(fs, "/ghost", "/new_ghost");
    T_ASSERT_INT_NE(rc, 0, "rename non-existent fails");
    teardown();
    return true;
}

int main(void) {
    printf(C_BOLD "\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf("   SQLFS  ·  Rename / Move Tests\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf(C_RESET "\n");

    TEST_SUITE("rename");
    RUN_TEST(t_rename_file);
    RUN_TEST(t_rename_dir);
    RUN_TEST(t_move_file_to_dir);
    RUN_TEST(t_rename_nonexistent);
    SUITE_END();

    return test_summary("Rename / Move");
}
