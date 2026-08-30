/*
 * test_framework.h — Minimal C unit-test harness for sqlfs.
 * No external libraries. Just include and go.
 */
#ifndef TEST_FRAMEWORK_H
#define TEST_FRAMEWORK_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>

/* ── ANSI colours ─────────────────────────────────────────────── */
#define C_RESET   "\033[0m"
#define C_BOLD    "\033[1m"
#define C_DIM     "\033[2m"
#define C_RED     "\033[31m"
#define C_GREEN   "\033[32m"
#define C_YELLOW  "\033[33m"
#define C_CYAN    "\033[36m"
#define C_WHITE   "\033[37m"

/* ── Global counters (one per translation unit) ───────────────── */
static int  _tests_run    = 0;
static int  _tests_passed = 0;
static int  _tests_failed = 0;

/* ── Assertion macros ─────────────────────────────────────────── */

#define T_ASSERT_TRUE(expr, msg) \
    do { \
        if (!(expr)) { \
            printf(C_RED "    FAIL  %s:%d  %s\n" C_RESET, \
                   __func__, __LINE__, (msg)); \
            return false; \
        } \
    } while (0)

#define T_ASSERT_FALSE(expr, msg) \
    T_ASSERT_TRUE(!(expr), msg)

#define T_ASSERT_INT_EQ(a, b, msg) \
    do { \
        long long _va = (long long)(a), _vb = (long long)(b); \
        if (_va != _vb) { \
            printf(C_RED "    FAIL  %s:%d  %s  (got %lld, want %lld)\n" C_RESET, \
                   __func__, __LINE__, (msg), _va, _vb); \
            return false; \
        } \
    } while (0)

#define T_ASSERT_INT_NE(a, b, msg) \
    do { \
        long long _va = (long long)(a), _vb = (long long)(b); \
        if (_va == _vb) { \
            printf(C_RED "    FAIL  %s:%d  %s  (both %lld)\n" C_RESET, \
                   __func__, __LINE__, (msg), _va); \
            return false; \
        } \
    } while (0)

#define T_ASSERT_INT_GE(a, b, msg) \
    do { \
        long long _va = (long long)(a), _vb = (long long)(b); \
        if (_va < _vb) { \
            printf(C_RED "    FAIL  %s:%d  %s  (got %lld, want >= %lld)\n" C_RESET, \
                   __func__, __LINE__, (msg), _va, _vb); \
            return false; \
        } \
    } while (0)

#define T_ASSERT_STR_EQ(a, b, msg) \
    do { \
        const char *_sa = (a), *_sb = (b); \
        if (_sa == NULL || _sb == NULL || strcmp(_sa, _sb) != 0) { \
            printf(C_RED "    FAIL  %s:%d  %s  (got \"%s\", want \"%s\")\n" C_RESET, \
                   __func__, __LINE__, (msg), \
                   _sa ? _sa : "(null)", _sb ? _sb : "(null)"); \
            return false; \
        } \
    } while (0)

#define T_ASSERT_PTR_NOT_NULL(p, msg) \
    do { \
        if ((p) == NULL) { \
            printf(C_RED "    FAIL  %s:%d  %s  (NULL)\n" C_RESET, \
                   __func__, __LINE__, (msg)); \
            return false; \
        } \
    } while (0)

#define T_ASSERT_PTR_NULL(p, msg) \
    do { \
        if ((p) != NULL) { \
            printf(C_RED "    FAIL  %s:%d  %s  (not NULL)\n" C_RESET, \
                   __func__, __LINE__, (msg)); \
            return false; \
        } \
    } while (0)

/* ── Suite / runner macros ────────────────────────────────────── */

#define TEST_SUITE(name) \
    printf(C_CYAN "  [%s]\n" C_RESET, (name))

#define RUN_TEST(fn) \
    do { \
        _tests_run++; \
        printf("    %-52s", #fn); \
        fflush(stdout); \
        if (fn()) { \
            _tests_passed++; \
            printf(C_GREEN "PASS\n" C_RESET); \
        } else { \
            _tests_failed++; \
        } \
    } while (0)

#define SUITE_END() printf("\n")

/* ── Summary ──────────────────────────────────────────────────── */

static inline int test_summary(const char *label)
{
    printf(C_BOLD "  ────────────────────────────────────────────────────────────\n" C_RESET);
    if (_tests_failed == 0) {
        printf(C_GREEN C_BOLD "  ✓  %s:  %d / %d passed\n" C_RESET,
               label, _tests_passed, _tests_run);
    } else {
        printf(C_RED C_BOLD "  ✗  %s:  %d / %d passed,  %d FAILED\n" C_RESET,
               label, _tests_passed, _tests_run, _tests_failed);
    }
    printf(C_BOLD "  ────────────────────────────────────────────────────────────\n" C_RESET);
    printf("\n");

    /* Print machine-parseable summary for Makefile aggregation */
    printf("  Total:   %d\n", _tests_run);
    printf("  Passed:  %d\n", _tests_passed);
    printf("  Failed:  %d\n", _tests_failed);

    return (_tests_failed > 0) ? 1 : 0;
}

#endif /* TEST_FRAMEWORK_H */

