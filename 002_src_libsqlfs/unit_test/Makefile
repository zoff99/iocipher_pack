#
# unit_test/Makefile
# ==================
# Builds unit tests directly against ../sqlfs.c (the real code).
# No fragile extraction — tests always run against current source.
#
# When the source changes, simply re-run 'make test_all' and all
# tests automatically exercise the updated code.
#
# Build errors stop immediately. Test failures do NOT stop other tests.
#
# Usage:
#   make test_all              - build and run all tests
#   make test_open_close       - open/close lifecycle tests
#   make test_directory        - directory operation tests
#   make test_file_io          - file read/write tests
#   make test_transaction      - transaction tests
#   make test_attributes       - attribute/metadata tests
#   make test_rename           - rename/move tests
#   make test_readdir          - directory listing tests
#   make test_symlink          - symlink/readlink tests
#   make test_metadata         - chmod/chown/utime metadata tests
#   make test_open_flags       - advanced open flag tests
#   make test_large_files      - large file and block boundary tests
#   make test_path_resolution  - path resolution and edge case tests
#   make test_threading        - threading/concurrency tests
#   make asan_all              - all tests with AddressSanitizer
#   make tsan_all              - all tests with ThreadSanitizer
#   make ubsan_all             - all tests with UBSanitizer
#   make clean                 - remove generated files
#

SHELL      := /bin/bash
BUILD_DIR  := ./_build
BIN_DIR    := ./_bin

CC     ?= gcc
AR     ?= ar

# ── Paths ─────────────────────────────────────────────────────────
# SQLFS_C    : the code under test
# SQLITE_C   : the SQLite amalgamation (plain or sqlcipher)
SQLFS_C    := ../sqlfs.c
SQLFS_H    := ../sqlfs.h
SQLITE_C   := ../sqlite/sqlite3.c

# ── Compiler flags ────────────────────────────────────────────────
# -I..   resolves #include "sqlfs.h" / "sqlfs_internal.h"
# -I../sqlite resolves #include "sqlite3.h"
CFLAGS := -std=c99 -Wall -Wextra -Wno-unused-function -Wno-unused-parameter \
          -Wno-unused-variable -g -O0 -pthread \
          -I.. -I../sqlite -I. -D_GNU_SOURCE=1

# Extra flags injected by sanitizer builds (empty by default)
CFLAGS_EXTRA ?=

# Link flags: sqlfs needs pthreads and math.
# If you compiled with SQLCipher (HAVE_LIBSQLCIPHER), add OpenSSL:
#   LDLIBS += ../openssl_libs/libcrypto.a ../openssl_libs/libssl.a
LDLIBS := -lm -lpthread

# ── SQLite compile flags ─────────────────────────────────────────
SQLITE_CFLAGS := -DSQLITE_THREADSAFE=1 -DSQLITE_TEMP_STORE=2 \
                 -Wno-unused-parameter -Wno-implicit-fallthrough \
                 -Wno-pointer-arith -Wno-sign-compare -Wno-double-promotion

# ── All test binaries ─────────────────────────────────────────────
ALL_BINS := $(BIN_DIR)/test_open_close \
            $(BIN_DIR)/test_directory \
            $(BIN_DIR)/test_file_io \
            $(BIN_DIR)/test_transaction \
            $(BIN_DIR)/test_attributes \
            $(BIN_DIR)/test_rename \
            $(BIN_DIR)/test_readdir \
            $(BIN_DIR)/test_symlink \
            $(BIN_DIR)/test_metadata \
            $(BIN_DIR)/test_open_flags \
            $(BIN_DIR)/test_large_files \
            $(BIN_DIR)/test_path_resolution \
            $(BIN_DIR)/test_threading

.PHONY: all test_all test_open_close test_directory test_file_io \
        test_transaction test_attributes test_rename test_readdir \
        test_symlink test_metadata test_open_flags test_large_files \
        test_path_resolution test_threading \
        asan_all tsan_all ubsan_all clean build_all objs

all: test_all

# ============================================================
#  Directory creation
# ============================================================
$(BUILD_DIR):
	mkdir -p $(BUILD_DIR)

$(BIN_DIR):
	mkdir -p $(BIN_DIR)

# ============================================================
#  Compile shared objects (done once, reused by all tests)
# ============================================================
objs: $(BUILD_DIR) $(BUILD_DIR)/sqlite3.o $(BUILD_DIR)/sqlfs.o

$(BUILD_DIR)/sqlite3.o: $(SQLITE_C) | $(BUILD_DIR)
	@echo "=== Compiling SQLite3 ==="
	$(CC) -c $(CFLAGS) $(CFLAGS_EXTRA) $(SQLITE_CFLAGS) $(SQLITE_C) -o $@

$(BUILD_DIR)/sqlfs.o: $(SQLFS_C) $(SQLFS_H) | $(BUILD_DIR)
	@echo "=== Compiling sqlfs.c ==="
	$(CC) -c $(CFLAGS) $(CFLAGS_EXTRA) $(SQLFS_C) -o $@

# ============================================================
#  Build test binaries
# ============================================================

$(BIN_DIR)/test_open_close: test_open_close.c objs | $(BIN_DIR)
	@echo "=== Building test_open_close ==="
	$(CC) $(CFLAGS) $(CFLAGS_EXTRA) $< \
		$(BUILD_DIR)/sqlfs.o $(BUILD_DIR)/sqlite3.o \
		-o $@ $(LDLIBS)
	@echo "  -> $@"

$(BIN_DIR)/test_directory: test_directory.c objs | $(BIN_DIR)
	@echo "=== Building test_directory ==="
	$(CC) $(CFLAGS) $(CFLAGS_EXTRA) $< \
		$(BUILD_DIR)/sqlfs.o $(BUILD_DIR)/sqlite3.o \
		-o $@ $(LDLIBS)
	@echo "  -> $@"

$(BIN_DIR)/test_file_io: test_file_io.c objs | $(BIN_DIR)
	@echo "=== Building test_file_io ==="
	$(CC) $(CFLAGS) $(CFLAGS_EXTRA) $< \
		$(BUILD_DIR)/sqlfs.o $(BUILD_DIR)/sqlite3.o \
		-o $@ $(LDLIBS)
	@echo "  -> $@"

$(BIN_DIR)/test_transaction: test_transaction.c objs | $(BIN_DIR)
	@echo "=== Building test_transaction ==="
	$(CC) $(CFLAGS) $(CFLAGS_EXTRA) $< \
		$(BUILD_DIR)/sqlfs.o $(BUILD_DIR)/sqlite3.o \
		-o $@ $(LDLIBS)
	@echo "  -> $@"

$(BIN_DIR)/test_attributes: test_attributes.c objs | $(BIN_DIR)
	@echo "=== Building test_attributes ==="
	$(CC) $(CFLAGS) $(CFLAGS_EXTRA) $< \
		$(BUILD_DIR)/sqlfs.o $(BUILD_DIR)/sqlite3.o \
		-o $@ $(LDLIBS)
	@echo "  -> $@"

$(BIN_DIR)/test_rename: test_rename.c objs | $(BIN_DIR)
	@echo "=== Building test_rename ==="
	$(CC) $(CFLAGS) $(CFLAGS_EXTRA) $< $(BUILD_DIR)/sqlfs.o $(BUILD_DIR)/sqlite3.o -o $@ $(LDLIBS)
	@echo "  -> $@"

$(BIN_DIR)/test_readdir: test_readdir.c objs | $(BIN_DIR)
	@echo "=== Building test_readdir ==="
	$(CC) $(CFLAGS) $(CFLAGS_EXTRA) $< $(BUILD_DIR)/sqlfs.o $(BUILD_DIR)/sqlite3.o -o $@ $(LDLIBS)
	@echo "  -> $@"

$(BIN_DIR)/test_symlink: test_symlink.c objs | $(BIN_DIR)
	@echo "=== Building test_symlink ==="
	$(CC) $(CFLAGS) $(CFLAGS_EXTRA) $< $(BUILD_DIR)/sqlfs.o $(BUILD_DIR)/sqlite3.o -o $@ $(LDLIBS)
	@echo "  -> $@"

$(BIN_DIR)/test_metadata: test_metadata.c objs | $(BIN_DIR)
	@echo "=== Building test_metadata ==="
	$(CC) $(CFLAGS) $(CFLAGS_EXTRA) $< $(BUILD_DIR)/sqlfs.o $(BUILD_DIR)/sqlite3.o -o $@ $(LDLIBS)
	@echo "  -> $@"

$(BIN_DIR)/test_open_flags: test_open_flags.c objs | $(BIN_DIR)
	@echo "=== Building test_open_flags ==="
	$(CC) $(CFLAGS) $(CFLAGS_EXTRA) $< $(BUILD_DIR)/sqlfs.o $(BUILD_DIR)/sqlite3.o -o $@ $(LDLIBS)
	@echo "  -> $@"

$(BIN_DIR)/test_large_files: test_large_files.c objs | $(BIN_DIR)
	@echo "=== Building test_large_files ==="
	$(CC) $(CFLAGS) $(CFLAGS_EXTRA) $< $(BUILD_DIR)/sqlfs.o $(BUILD_DIR)/sqlite3.o -o $@ $(LDLIBS)
	@echo "  -> $@"

$(BIN_DIR)/test_path_resolution: test_path_resolution.c objs | $(BIN_DIR)
	@echo "=== Building test_path_resolution ==="
	$(CC) $(CFLAGS) $(CFLAGS_EXTRA) $< $(BUILD_DIR)/sqlfs.o $(BUILD_DIR)/sqlite3.o -o $@ $(LDLIBS)
	@echo "  -> $@"

$(BIN_DIR)/test_threading: test_threading.c objs | $(BIN_DIR)
	@echo "=== Building test_threading ==="
	$(CC) $(CFLAGS) $(CFLAGS_EXTRA) $< $(BUILD_DIR)/sqlfs.o $(BUILD_DIR)/sqlite3.o -o $@ $(LDLIBS)
	@echo "  -> $@"

# ============================================================
#  Build ALL binaries (fails fast on compile errors)
# ============================================================
build_all: $(ALL_BINS)
	@echo ""
	@echo "=== All test binaries built successfully ==="

# ============================================================
#  Individual run targets
# ============================================================

test_open_close: $(BIN_DIR)/test_open_close
	@echo ""
	@echo "============================================================"
	@echo "  Running: Open / Close Tests"
	@echo "============================================================"
	./$(BIN_DIR)/test_open_close

test_directory: $(BIN_DIR)/test_directory
	@echo ""
	@echo "============================================================"
	@echo "  Running: Directory Tests"
	@echo "============================================================"
	./$(BIN_DIR)/test_directory

test_file_io: $(BIN_DIR)/test_file_io
	@echo ""
	@echo "============================================================"
	@echo "  Running: File I/O Tests"
	@echo "============================================================"
	./$(BIN_DIR)/test_file_io

test_transaction: $(BIN_DIR)/test_transaction
	@echo ""
	@echo "============================================================"
	@echo "  Running: Transaction Tests"
	@echo "============================================================"
	./$(BIN_DIR)/test_transaction

test_attributes: $(BIN_DIR)/test_attributes
	@echo ""
	@echo "============================================================"
	@echo "  Running: Attribute Tests"
	@echo "============================================================"
	./$(BIN_DIR)/test_attributes

test_rename: $(BIN_DIR)/test_rename
	@echo ""; echo "============================================================"
	@echo "  Running: Rename / Move Tests"
	@echo "============================================================"
	./$(BIN_DIR)/test_rename

test_readdir: $(BIN_DIR)/test_readdir
	@echo ""; echo "============================================================"
	@echo "  Running: Readdir Tests"
	@echo "============================================================"
	./$(BIN_DIR)/test_readdir

test_symlink: $(BIN_DIR)/test_symlink
	@echo ""; echo "============================================================"
	@echo "  Running: Symlink Tests"
	@echo "============================================================"
	./$(BIN_DIR)/test_symlink

test_metadata: $(BIN_DIR)/test_metadata
	@echo ""; echo "============================================================"
	@echo "  Running: Metadata Tests"
	@echo "============================================================"
	./$(BIN_DIR)/test_metadata

test_open_flags: $(BIN_DIR)/test_open_flags
	@echo ""; echo "============================================================"
	@echo "  Running: Open Flags Tests"
	@echo "============================================================"
	./$(BIN_DIR)/test_open_flags

test_large_files: $(BIN_DIR)/test_large_files
	@echo ""; echo "============================================================"
	@echo "  Running: Large File Tests"
	@echo "============================================================"
	./$(BIN_DIR)/test_large_files

test_path_resolution: $(BIN_DIR)/test_path_resolution
	@echo ""; echo "============================================================"
	@echo "  Running: Path Resolution Tests"
	@echo "============================================================"
	./$(BIN_DIR)/test_path_resolution

test_threading: $(BIN_DIR)/test_threading
	@echo ""; echo "============================================================"
	@echo "  Running: Threading Tests"
	@echo "============================================================"
	./$(BIN_DIR)/test_threading

# ============================================================
#  Run ALL tests
#
#  PHASE 1 (build_all): Builds every test binary. If ANY
#  compilation or link error occurs, make STOPS immediately.
#  Build errors are never swallowed.
#
#  PHASE 2 (run): Runs every test binary. Test FAILURES
#  (exit code 1) do NOT stop other tests — all suites run
#  and a summary is printed at the end.
#
#  PHASE 3 (aggregate): Parses the per-binary test_summary()
#  output and prints a GRAND TOTAL of individual tests across
#  all suites. Each binary already prints its own summary;
#  this adds a combined total at the very end.
# ============================================================
test_all: build_all
	@echo ""
	@echo "============================================================"
	@echo "  RUNNING ALL TEST SUITES"
	@echo "============================================================"
	@OUTFILE=$$(mktemp); \
	TOTAL_TESTS=0; \
	TOTAL_PASSED=0; \
	TOTAL_FAILED=0; \
	SUITES_RUN=0; \
	SUITES_FAILED=0; \
	for BIN in $(ALL_BINS); do \
		./$$BIN > $$OUTFILE 2>&1; \
		RC=$$?; \
		cat $$OUTFILE; \
		SUITES_RUN=$$((SUITES_RUN+1)); \
		if [ $$RC -ne 0 ]; then \
			SUITES_FAILED=$$((SUITES_FAILED+1)); \
		fi; \
		CLEAN=$$(sed "s/$$'\033'\\[[0-9;]*[a-zA-Z]//g" $$OUTFILE); \
		T=$$(echo "$$CLEAN" | grep 'Total:' | tail -n1 | awk '{print $$NF}'); \
		P=$$(echo "$$CLEAN" | grep 'Passed:' | tail -n1 | awk '{print $$NF}'); \
		F=$$(echo "$$CLEAN" | grep 'Failed:' | tail -n1 | awk '{print $$NF}'); \
		T=$${T:-0}; P=$${P:-0}; F=$${F:-0}; \
		TOTAL_TESTS=$$((TOTAL_TESTS + T)); \
		TOTAL_PASSED=$$((TOTAL_PASSED + P)); \
		TOTAL_FAILED=$$((TOTAL_FAILED + F)); \
	done; \
	rm -f $$OUTFILE; \
	echo ""; \
	echo "============================================================"; \
	echo "  GRAND TOTAL (all individual tests across all suites)"; \
	echo "============================================================"; \
	echo "    Total:   $$TOTAL_TESTS"; \
	echo "    Passed:  $$TOTAL_PASSED"; \
	echo "    Failed:  $$TOTAL_FAILED"; \
	echo "    Suites:  $$SUITES_RUN run, $$SUITES_FAILED with failures"; \
	echo "============================================================"; \
	if [ $$TOTAL_FAILED -eq 0 ]; then \
		echo "    >>> ALL $$TOTAL_TESTS TESTS PASSED <<<"; \
	else \
		echo "    >>> $$TOTAL_FAILED TEST(S) FAILED <<<"; \
	fi; \
	echo "============================================================"; \
	echo ""; \
	exit $$TOTAL_FAILED

# ============================================================
#  Sanitizer builds
# ============================================================
asan_all: clean
	@echo "=== ASAN BUILD ==="
	$(MAKE) CFLAGS_EXTRA="-fsanitize=address -fno-omit-frame-pointer" test_all

tsan_all: clean
	@echo "=== TSAN BUILD ==="
	$(MAKE) CFLAGS_EXTRA="-fsanitize=thread -fno-omit-frame-pointer" test_all

ubsan_all: clean
	@echo "=== UBSAN BUILD ==="
	$(MAKE) CFLAGS_EXTRA="-fsanitize=undefined,float-divide-by-zero -fno-sanitize-recover=all" test_all

# ============================================================
#  Clean
# ============================================================
clean:
	rm -rf $(BUILD_DIR) $(BIN_DIR)
	rm -f *.db *.db-journal *.db-wal *.db-shm _test_*.db*
	@echo "Cleaned."
