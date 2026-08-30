/*
 * test_threading.c — Concurrent access and thread safety
 * Each thread opens its own sqlfs_t connection to the same database file.
 * This mirrors how FUSE handles multi-threaded access.
 */
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <stdlib.h>
#include <pthread.h>
#include "test_framework.h"
#include "../sqlfs.h"

#define DB "_test_threading.db"

/* ── Thread argument struct ───────────────────────────────────── */
typedef struct {
    int thread_id;
    int success;
} thread_arg_t;

/* ── Test 1: Concurrent mkdir (each thread has own connection) ── */

static void *thread_mkdir(void *arg) {
    thread_arg_t *ta = (thread_arg_t *)arg;
    sqlfs_t *fs = NULL;

    /* Each thread opens its own connection */
    if (!sqlfs_open(DB, &fs)) {
        ta->success = 0;
        return NULL;
    }

    char path[64];
    snprintf(path, sizeof(path), "/thread_dir_%d", ta->thread_id);
    int rc = sqlfs_proc_mkdir(fs, path, 0755);
    ta->success = (rc == 0);

    sqlfs_close(fs);
    return NULL;
}

static bool t_concurrent_mkdir(void) {
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");

    const int NUM_THREADS = 8;
    pthread_t threads[NUM_THREADS];
    thread_arg_t args[NUM_THREADS];

    for (int i = 0; i < NUM_THREADS; i++) {
        args[i].thread_id = i;
        args[i].success = 0;
        pthread_create(&threads[i], NULL, thread_mkdir, &args[i]);
    }
    for (int i = 0; i < NUM_THREADS; i++) {
        pthread_join(threads[i], NULL);
    }

    /* Verify all dirs were created (open a single connection to check) */
    sqlfs_t *fs = NULL;
    sqlfs_open(DB, &fs);
    for (int i = 0; i < NUM_THREADS; i++) {
        T_ASSERT_TRUE(args[i].success, "thread mkdir succeeded");
        char path[64];
        snprintf(path, sizeof(path), "/thread_dir_%d", i);
        T_ASSERT_INT_EQ(sqlfs_is_dir(fs, path), 1, "dir exists");
    }
    sqlfs_close(fs);

    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
    return true;
}

/* ── Test 2: Concurrent mkdir same path (race) ────────────────── */

static void *thread_mkdir_same(void *arg) {
    thread_arg_t *ta = (thread_arg_t *)arg;
    sqlfs_t *fs = NULL;

    if (!sqlfs_open(DB, &fs)) {
        ta->success = 0;
        return NULL;
    }

    int rc = sqlfs_proc_mkdir(fs, "/race_dir", 0755);
    /* Exactly one thread should get rc==0, others get -EEXIST */
    ta->success = (rc == 0);

    sqlfs_close(fs);
    return NULL;
}

static bool t_concurrent_mkdir_same_path(void) {
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");

    const int NUM_THREADS = 8;
    pthread_t threads[NUM_THREADS];
    thread_arg_t args[NUM_THREADS];

    for (int i = 0; i < NUM_THREADS; i++) {
        args[i].thread_id = i;
        args[i].success = 0;
        pthread_create(&threads[i], NULL, thread_mkdir_same, &args[i]);
    }
    for (int i = 0; i < NUM_THREADS; i++) {
        pthread_join(threads[i], NULL);
    }

    /* Exactly one thread should have succeeded.
     * SQLite's UNIQUE constraint on the key column ensures only one
     * INSERT succeeds; the rest fail with a constraint violation
     * which sqlfs translates to -EEXIST. */
    int success_count = 0;
    for (int i = 0; i < NUM_THREADS; i++) {
        if (args[i].success) success_count++;
    }
    T_ASSERT_INT_EQ(success_count, 1, "exactly one thread wins the mkdir race");

    sqlfs_t *fs = NULL;
    sqlfs_open(DB, &fs);
    T_ASSERT_INT_EQ(sqlfs_is_dir(fs, "/race_dir"), 1, "dir was created");
    sqlfs_close(fs);

    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
    return true;
}

/* ── Test 3: Concurrent writes to different files ─────────────── */

static void *thread_write_own_file(void *arg) {
    thread_arg_t *ta = (thread_arg_t *)arg;
    sqlfs_t *fs = NULL;

    if (!sqlfs_open(DB, &fs)) {
        ta->success = 0;
        return NULL;
    }

    char path[64];
    snprintf(path, sizeof(path), "/thread_file_%d.txt", ta->thread_id);

    char data[128];
    int len = snprintf(data, sizeof(data), "data_from_thread_%d", ta->thread_id);

    int rc = sqlfs_proc_write(fs, path, data, len, 0, O_RDWR | O_CREAT | O_TRUNC);
    ta->success = (rc == len);

    sqlfs_close(fs);
    return NULL;
}

static bool t_concurrent_write_different_files(void) {
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");

    const int NUM_THREADS = 8;
    pthread_t threads[NUM_THREADS];
    thread_arg_t args[NUM_THREADS];

    for (int i = 0; i < NUM_THREADS; i++) {
        args[i].thread_id = i;
        args[i].success = 0;
        pthread_create(&threads[i], NULL, thread_write_own_file, &args[i]);
    }
    for (int i = 0; i < NUM_THREADS; i++) {
        pthread_join(threads[i], NULL);
    }

    /* Verify all files were written correctly */
    sqlfs_t *fs = NULL;
    sqlfs_open(DB, &fs);
    for (int i = 0; i < NUM_THREADS; i++) {
        T_ASSERT_TRUE(args[i].success, "thread write succeeded");

        char path[64];
        snprintf(path, sizeof(path), "/thread_file_%d.txt", i);
        char buf[128] = {0};
        struct fuse_file_info fi = {0};
        int rrc = sqlfs_proc_read(fs, path, buf, sizeof(buf), 0, &fi);
        T_ASSERT_TRUE(rrc > 0, "file readable");

        char expected[128];
        snprintf(expected, sizeof(expected), "data_from_thread_%d", i);
        T_ASSERT_STR_EQ(buf, expected, "content matches");
    }
    sqlfs_close(fs);

    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
    return true;
}

/* ── Test 4: Concurrent appends to same file ──────────────────── */

#define APPEND_THREADS 4
#define APPENDS_PER_THREAD 10

static void *thread_append_same_file(void *arg) {
    thread_arg_t *ta = (thread_arg_t *)arg;
    sqlfs_t *fs = NULL;

    if (!sqlfs_open(DB, &fs)) {
        ta->success = 0;
        return NULL;
    }

    char line[64];
    int len = snprintf(line, sizeof(line), "T%d ", ta->thread_id);

    for (int i = 0; i < APPENDS_PER_THREAD; i++) {
        sqlfs_proc_write(fs, "/shared_append.txt", line, len, 0, O_RDWR | O_APPEND);
    }
    ta->success = 1;

    sqlfs_close(fs);
    return NULL;
}

static bool t_concurrent_append_same_file(void) {
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");

    /* Create the file first so all threads can append */
    sqlfs_t *fs_init = NULL;
    sqlfs_open(DB, &fs_init);
    sqlfs_proc_write(fs_init, "/shared_append.txt", "", 0, 0, O_RDWR | O_CREAT | O_TRUNC);
    sqlfs_close(fs_init);

    const int NUM_THREADS = APPEND_THREADS;
    pthread_t threads[NUM_THREADS];
    thread_arg_t args[NUM_THREADS];

    for (int i = 0; i < NUM_THREADS; i++) {
        args[i].thread_id = i;
        args[i].success = 0;
        pthread_create(&threads[i], NULL, thread_append_same_file, &args[i]);
    }
    for (int i = 0; i < NUM_THREADS; i++) {
        pthread_join(threads[i], NULL);
    }

    /* Verify final file size: each append is "TN " (3 chars),
     * 4 threads * 10 appends * 3 chars = 120 bytes */
    sqlfs_t *fs = NULL;
    sqlfs_open(DB, &fs);
    sqlfs_stat st;
    sqlfs_proc_getattr(fs, "/shared_append.txt", &st);
    int expected_size = NUM_THREADS * APPENDS_PER_THREAD * 3;
    T_ASSERT_INT_EQ((int)st.st_size, expected_size, "all appends accounted for");
    sqlfs_close(fs);

    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
    return true;
}

/* ── Test 5: Read during write ────────────────────────────────── */

static volatile int writer_done = 0;

static void *thread_writer(void *arg) {
    (void)arg;
    sqlfs_t *fs = NULL;
    if (!sqlfs_open(DB, &fs)) return NULL;

    for (int i = 0; i < 50; i++) {
        char data[32];
        int len = snprintf(data, sizeof(data), "iter_%04d", i);
        sqlfs_proc_write(fs, "/rw_file.txt", data, len, 0, O_RDWR | O_CREAT | O_TRUNC);
    }
    writer_done = 1;
    sqlfs_close(fs);
    return NULL;
}

static void *thread_reader(void *arg) {
    thread_arg_t *ta = (thread_arg_t *)arg;
    sqlfs_t *fs = NULL;
    if (!sqlfs_open(DB, &fs)) {
        ta->success = 0;
        return NULL;
    }

    char buf[64] = {0};
    struct fuse_file_info fi = {0};
    int reads = 0;

    while (!writer_done && reads < 200) {
        int rc = sqlfs_proc_read(fs, "/rw_file.txt", buf, sizeof(buf), 0, &fi);
        if (rc > 0) reads++;
    }
    ta->success = (reads > 0);
    sqlfs_close(fs);
    return NULL;
}

static bool t_concurrent_read_write(void) {
    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
    writer_done = 0;

    /* Create file first so reader doesn't fail on missing file */
    sqlfs_t *fs_init = NULL;
    sqlfs_open(DB, &fs_init);
    sqlfs_proc_write(fs_init, "/rw_file.txt", "init", 4, 0, O_RDWR | O_CREAT | O_TRUNC);
    sqlfs_close(fs_init);

    pthread_t writer, reader;
    thread_arg_t rarg = {0, 0};

    pthread_create(&writer, NULL, thread_writer, NULL);
    pthread_create(&reader, NULL, thread_reader, &rarg);

    pthread_join(writer, NULL);
    pthread_join(reader, NULL);

    T_ASSERT_TRUE(rarg.success, "reader got data during concurrent writes");

    unlink(DB); unlink(DB "-wal"); unlink(DB "-shm"); unlink(DB "-journal");
    return true;
}

/* ── main ─────────────────────────────────────────────────────── */
int main(void)
{
    printf(C_BOLD "\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf("   SQLFS  ·  Threading / Concurrency Tests\n");
    printf("  ════════════════════════════════════════════════════════════\n");
    printf(C_RESET "\n");

    TEST_SUITE("concurrent mkdir");
    RUN_TEST(t_concurrent_mkdir);
    RUN_TEST(t_concurrent_mkdir_same_path);
    SUITE_END();

    TEST_SUITE("concurrent writes");
    RUN_TEST(t_concurrent_write_different_files);
    SUITE_END();

    TEST_SUITE("concurrent appends");
    RUN_TEST(t_concurrent_append_same_file);
    SUITE_END();

    TEST_SUITE("read/write race");
    RUN_TEST(t_concurrent_read_write);
    SUITE_END();

    return test_summary("Threading");
}
