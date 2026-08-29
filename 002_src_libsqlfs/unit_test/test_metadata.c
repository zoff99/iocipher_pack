/*
 * test_metadata.c — chmod / chown / utime
 */
#include <unistd.h>
#include <fcntl.h>
#include <utime.h>
#include "test_framework.h"
#include "../sqlfs.h"

#define DB "_test_metadata.db"
static sqlfs_t *fs;

static void setup(void) {
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
    sqlfs_open(DB, &fs);
}
static void teardown(void) {
    sqlfs_close(fs);
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
}

static bool t_chmod(void) {
    setup();
    sqlfs_proc_write(fs, "/chmod.txt", "x", 1, 0, O_RDWR | O_CREAT | O_TRUNC);
    
    int rc = sqlfs_proc_chmod(fs, "/chmod.txt", 0644);
    T_ASSERT_INT_EQ(rc, 0, "chmod returns 0");
    
    sqlfs_stat st;
    sqlfs_proc_getattr(fs, "/chmod.txt", &st);
    /* Mask with 0777 to ignore file type bits */
    T_ASSERT_INT_EQ((int)(st.st_mode & 0777), 0644, "mode is 0644");
    
    teardown();
    return true;
}

static bool t_chown(void) {
    setup();
    sqlfs_proc_mkdir(fs, "/chown_dir", 0755);
    
    int rc = sqlfs_proc_chown(fs, "/chown_dir", 1000, 1000);
    T_ASSERT_INT_EQ(rc, 0, "chown returns 0");
    
    sqlfs_stat st;
    sqlfs_proc_getattr(fs, "/chown_dir", &st);
    T_ASSERT_INT_EQ((int)st.st_uid, 1000, "uid is 1000");
    T_ASSERT_INT_EQ((int)st.st_gid, 1000, "gid is 1000");
    
    teardown();
    return true;
}

static bool t_utime(void) {
    setup();
    sqlfs_proc_write(fs, "/utime.txt", "y", 1, 0, O_RDWR | O_CREAT | O_TRUNC);
    
    struct utimbuf tb;
    tb.actime = 1600000000;
    tb.modtime = 1600000000;
    
    int rc = sqlfs_proc_utime(fs, "/utime.txt", &tb);
    T_ASSERT_INT_EQ(rc, 0, "utime returns 0");
    
    sqlfs_stat st;
    sqlfs_proc_getattr(fs, "/utime.txt", &st);
    T_ASSERT_INT_EQ((int)st.st_atime, 1600000000, "atime updated");
    T_ASSERT_INT_EQ((int)st.st_mtime, 1600000000, "mtime updated");
    
    teardown();
    return true;
}

static bool t_metadata_nonexistent(void) {
    setup();
    int rc = sqlfs_proc_chmod(fs, "/ghost", 0644);
    T_ASSERT_INT_NE(rc, 0, "chmod non-existent fails");
    
    rc = sqlfs_proc_chown(fs, "/ghost", 1000, 1000);
    T_ASSERT_INT_NE(rc, 0, "chown non-existent fails");
    
    struct utimbuf tb = {0};
    rc = sqlfs_proc_utime(fs, "/ghost", &tb);
    T_ASSERT_INT_NE(rc, 0, "utime non-existent fails");
    
    teardown();
    return true;
}

int main(void) {
    printf(C_BOLD "\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf("   SQLFS  ·  Metadata (chmod/chown/utime) Tests\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf(C_RESET "\n");

    TEST_SUITE("metadata modification");
    RUN_TEST(t_chmod);
    RUN_TEST(t_chown);
    RUN_TEST(t_utime);
    SUITE_END();

    TEST_SUITE("metadata edge cases");
    RUN_TEST(t_metadata_nonexistent);
    SUITE_END();

    return test_summary("Metadata");
}
