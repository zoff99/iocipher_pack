/*
 * test_readdir.c — directory listing via fuse_fill_dir_t
 */
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <stdlib.h>
#include "test_framework.h"
#include "../sqlfs.h"

#define DB "_test_readdir.db"
static sqlfs_t *fs;

static void setup(void) {
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
    sqlfs_open(DB, &fs);
}
static void teardown(void) {
    sqlfs_close(fs);
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
}

typedef struct {
    char **names;
    int count;
    int cap;
} dir_list_t;

static int dummy_filler(void *buf, const char *name, const struct stat *stbuf, off_t off) {
    (void)stbuf; (void)off;
    dir_list_t *list = (dir_list_t *)buf;
    if (list->count >= list->cap) {
        list->cap = list->cap == 0 ? 8 : list->cap * 2;
        list->names = realloc(list->names, list->cap * sizeof(char *));
    }
    list->names[list->count++] = strdup(name);
    return 0;
}

static bool contains(dir_list_t *list, const char *name) {
    for (int i = 0; i < list->count; i++) {
        if (strcmp(list->names[i], name) == 0) return true;
    }
    return false;
}

static void free_list(dir_list_t *list) {
    for (int i = 0; i < list->count; i++) free(list->names[i]);
    free(list->names);
    list->names = NULL;
    list->count = list->cap = 0;
}

static bool t_readdir_empty(void) {
    setup();
    sqlfs_proc_mkdir(fs, "/empty_dir", 0755);
    dir_list_t list = {0};
    struct fuse_file_info fi = {0};
    int rc = sqlfs_proc_readdir(fs, "/empty_dir", &list, dummy_filler, 0, &fi);
    T_ASSERT_INT_EQ(rc, 0, "readdir returns 0");
    
    T_ASSERT_TRUE(contains(&list, "."), "contains .");
    T_ASSERT_TRUE(contains(&list, ".."), "contains ..");
    
    int other_count = 0;
    for(int i=0; i<list.count; i++) {
        if (strcmp(list.names[i], ".") != 0 && strcmp(list.names[i], "..") != 0)
            other_count++;
    }
    T_ASSERT_INT_EQ(other_count, 0, "no other entries");
    
    free_list(&list);
    teardown();
    return true;
}

static bool t_readdir_multiple(void) {
    setup();
    sqlfs_proc_mkdir(fs, "/parent", 0755);
    sqlfs_proc_write(fs, "/parent/file1.txt", "a", 1, 0, O_RDWR | O_CREAT | O_TRUNC);
    sqlfs_proc_write(fs, "/parent/file2.txt", "b", 1, 0, O_RDWR | O_CREAT | O_TRUNC);
    sqlfs_proc_mkdir(fs, "/parent/subdir", 0755);
    
    dir_list_t list = {0};
    struct fuse_file_info fi = {0};
    sqlfs_proc_readdir(fs, "/parent", &list, dummy_filler, 0, &fi);
    
    T_ASSERT_TRUE(contains(&list, "file1.txt"), "contains file1");
    T_ASSERT_TRUE(contains(&list, "file2.txt"), "contains file2");
    T_ASSERT_TRUE(contains(&list, "subdir"), "contains subdir");
    
    free_list(&list);
    teardown();
    return true;
}

static bool t_readdir_root(void) {
    setup();
    sqlfs_proc_mkdir(fs, "/top1", 0755);
    sqlfs_proc_mkdir(fs, "/top2", 0755);
    
    dir_list_t list = {0};
    struct fuse_file_info fi = {0};
    sqlfs_proc_readdir(fs, "/", &list, dummy_filler, 0, &fi);
    
    T_ASSERT_TRUE(contains(&list, "top1"), "root contains top1");
    T_ASSERT_TRUE(contains(&list, "top2"), "root contains top2");
    
    free_list(&list);
    teardown();
    return true;
}

int main(void) {
    printf(C_BOLD "\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf("   SQLFS  ·  Readdir Tests\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf(C_RESET "\n");

    TEST_SUITE("readdir");
    RUN_TEST(t_readdir_empty);
    RUN_TEST(t_readdir_multiple);
    RUN_TEST(t_readdir_root);
    SUITE_END();

    return test_summary("Readdir");
}
