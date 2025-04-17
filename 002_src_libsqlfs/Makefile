###################################################
#
# (C) 2024 Zoff
#
# IOCipher pack
#
###################################################

ismingw = 0
ccmachine = $(shell $(CC) -dumpmachine)
ifeq ($(findstring mingw, $(ccmachine)), mingw)
  ismingw = 1
  CFLAGS += --static
  SSL_LIB_DIR = openssl_win64_libs/
else
  CFLAGS += -Werror=maybe-uninitialized
  SSL_LIB_DIR = openssl_libs/
endif

# ------------------------------
# -- encryption is the default
ENCRYPT_CS = 1
# ------------------------------

ASANCFLAGS = -fsanitize=address -fno-omit-frame-pointer -static-libasan
TSANCFLAGS = -fsanitize=thread -fno-omit-frame-pointer -static-libasan
UBSANCFLAGS = -fno-sanitize-recover=all -fsanitize=undefined,float-divide-by-zero

CFLAGS += -g -O3 -fPIC -Wall -Wextra -pedantic -Wdouble-promotion -Wno-unknown-warning-option -std=c99 -pedantic -pthread -I./

ifdef ENCRYPT_CS
CFLAGS += -DSQLITE_HAS_CODEC -DHAVE_LIBSQLCIPHER -Isqlcipher/ -Iopenssl_includes
CFSQLCIPHER = -DHAVE_STDINT_H -DSQLITE_HAS_CODEC -DSQLCIPHER_CRYPTO_OPENSSL -DSQLITE_TEMP_STORE=2 \
        -DSQLITE_EXTRA_INIT=sqlcipher_extra_init -DSQLITE_EXTRA_SHUTDOWN=sqlcipher_extra_shutdown \
	-DSQLITE_THREADSAFE=1 -DSQLITE_ENABLE_COLUMN_METADATA -DSQLITE_ENABLE_FTS3_PARENTHESIS \
	-DSQLITE_ENABLE_FTS4 -DSQLITE_ENABLE_FTS4_UNICODE61 -DSQLITE_ENABLE_FTS5 \
	-DSQLITE_ENABLE_MEMORY_MANAGEMENT -DSQLITE_ENABLE_UNLOCK_NOTIFY -DSQLITE_ENABLE_RTREE \
	-DSQLITE_SOUNDEX -DHAVE_USLEEP -DSQLITE_ENABLE_LOAD_EXTENSION -DSQLITE_ENABLE_STAT3 \
	-DSQLITE_ENABLE_STAT4 -DSQLITE_ENABLE_JSON1 -DSQLITE_ENABLE_EXPLAIN_COMMENTS \
	-DSQLITE_DEFAULT_WAL_SYNCHRONOUS=1 -DSQLITE_MAX_VARIABLE_NUMBER=99999 \
	-DSQLITE_DEFAULT_JOURNAL_SIZE_LIMIT=10485760 -DSQLITE_ENABLE_SESSION \
	-DSQLITE_ENABLE_PREUPDATE_HOOK -DSQLITE_ENABLE_DBSTAT_VTAB

LBSQLCIPHER = $(SSL_LIB_DIR)/libcrypto.a $(SSL_LIB_DIR)/libssl.a -lm
SQLITE_DIR = sqlcipher
else
CFLAGS += -Isqlite/
CFSQLCIPHER = -DSQLITE_HAS_CODEC -DSQLITE_TEMP_STORE=2 \
	-DSQLITE_THREADSAFE=1 -DSQLITE_ENABLE_COLUMN_METADATA -DSQLITE_ENABLE_FTS3_PARENTHESIS \
	-DSQLITE_ENABLE_FTS4 -DSQLITE_ENABLE_FTS4_UNICODE61 -DSQLITE_ENABLE_FTS5 \
	-DSQLITE_ENABLE_MEMORY_MANAGEMENT -DSQLITE_ENABLE_UNLOCK_NOTIFY -DSQLITE_ENABLE_RTREE \
	-DSQLITE_SOUNDEX -DHAVE_USLEEP -DSQLITE_ENABLE_LOAD_EXTENSION -DSQLITE_ENABLE_STAT3 \
	-DSQLITE_ENABLE_STAT4 -DSQLITE_ENABLE_JSON1 -DSQLITE_ENABLE_EXPLAIN_COMMENTS \
	-DSQLITE_DEFAULT_WAL_SYNCHRONOUS=1 -DSQLITE_MAX_VARIABLE_NUMBER=99999 \
	-DSQLITE_DEFAULT_JOURNAL_SIZE_LIMIT=10485760 -DSQLITE_ENABLE_SESSION \
	-DSQLITE_ENABLE_PREUPDATE_HOOK -DSQLITE_ENABLE_DBSTAT_VTAB
LBSQLCIPHER = -lm
SQLITE_DIR = sqlite
endif

# ------------------------------
#  tests
# ------------------------------
all: libsqlfs.a

test: libsqlfs.a sqlite3.a tests/c_*.c
	$(CC) -Itests/ tests/c_api.c -D_GNU_SOURCE=1 $(ASANCFLAGS) $(CFLAGS) $(LIBS) libsqlfs.a sqlite3.a $(LBSQLCIPHER) -o c_api
	$(CC) -Itests/ tests/c_api_key.c -D_GNU_SOURCE=1 $(ASANCFLAGS) $(CFLAGS) $(LIBS) libsqlfs.a sqlite3.a $(LBSQLCIPHER) -o c_api_key
	$(CC) -Itests/ tests/c_thread_api.c -D_GNU_SOURCE=1 $(ASANCFLAGS) $(CFLAGS) $(LIBS) libsqlfs.a sqlite3.a $(LBSQLCIPHER) -o c_thread_api
	$(CC) -Itests/ tests/c_thread_api_key.c -D_GNU_SOURCE=1 $(ASANCFLAGS) $(CFLAGS) $(LIBS) libsqlfs.a sqlite3.a $(LBSQLCIPHER) -o c_thread_api_key
	$(CC) -Itests/ tests/c_api_password.c -D_GNU_SOURCE=1 $(ASANCFLAGS) $(CFLAGS) $(LIBS) libsqlfs.a sqlite3.a $(LBSQLCIPHER) -o c_api_password
	$(CC) -Itests/ tests/c_perf.c -D_GNU_SOURCE=1 $(CFLAGS) $(ASANCFLAGS) $(LIBS) libsqlfs.a sqlite3.a $(LBSQLCIPHER) -o c_perf
	$(CC) -Itests/ tests/c_perf_key.c -D_GNU_SOURCE=1 $(ASANCFLAGS) $(CFLAGS) $(LIBS) libsqlfs.a sqlite3.a $(LBSQLCIPHER) -o c_perf_key

test_tsan: libsqlfs.a sqlite3.a tests/c_*.c
	$(CC) -Itests/ tests/c_api.c -D_GNU_SOURCE=1 $(TSANCFLAGS) $(CFLAGS) $(LIBS) libsqlfs.a sqlite3.a $(LBSQLCIPHER) -o c_api
	$(CC) -Itests/ tests/c_api_key.c -D_GNU_SOURCE=1 $(TSANCFLAGS) $(CFLAGS) $(LIBS) libsqlfs.a sqlite3.a $(LBSQLCIPHER) -o c_api_key
	$(CC) -Itests/ tests/c_thread_api.c -D_GNU_SOURCE=1 $(TSANCFLAGS) $(CFLAGS) $(LIBS) libsqlfs.a sqlite3.a $(LBSQLCIPHER) -o c_thread_api
	$(CC) -Itests/ tests/c_thread_api_key.c -D_GNU_SOURCE=1 $(TSANCFLAGS) $(CFLAGS) $(LIBS) libsqlfs.a sqlite3.a $(LBSQLCIPHER) -o c_thread_api_key
	$(CC) -Itests/ tests/c_api_password.c -D_GNU_SOURCE=1 $(TSANCFLAGS) $(CFLAGS) $(LIBS) libsqlfs.a sqlite3.a $(LBSQLCIPHER) -o c_api_password
	$(CC) -Itests/ tests/c_perf.c -D_GNU_SOURCE=1 $(CFLAGS) $(TSANCFLAGS) $(LIBS) libsqlfs.a sqlite3.a $(LBSQLCIPHER) -o c_perf
	$(CC) -Itests/ tests/c_perf_key.c -D_GNU_SOURCE=1 $(TSANCFLAGS) $(CFLAGS) $(LIBS) libsqlfs.a sqlite3.a $(LBSQLCIPHER) -o c_perf_key

runtest: testdbclean test
	./c_api
	./c_api_key
	./c_thread_api
	./c_thread_api_key
	./c_api_password
	./c_perf
	./c_perf_key

runtest_tsan: testdbclean test_tsan
	./c_api
	./c_api_key
	./c_thread_api
	./c_thread_api_key
	./c_api_password
	./c_perf
	./c_perf_key
# ------------------------------
#  sqlfs lib
# ------------------------------
libsqlfs.a: sqlfs.o
	$(AR) rcs libsqlfs.a sqlfs.o

sqlfs.o: sqlfs.c sqlfs.h sqlite3.a
	$(CC) -c -D_GNU_SOURCE=1 $(CFLAGS) $(LIBS) -Wno-unused-parameter -Wno-unused-variable -Wno-gnu-statement-expression $< -o $@

# ------------------------------
#  SQLITE3
# ------------------------------
sqlite3.a: sqlite3.o
	$(AR) rcs sqlite3.a sqlite3.o

sqlite3.o: $(SQLITE_DIR)/sqlite3.c
	$(CC) -c $(CFLAGS) $(CFSQLCIPHER) -Wno-unused-but-set-variable -Wno-unused-parameter -Wno-implicit-fallthrough -Wno-pointer-arith -Wno-unused-variable -Wno-sign-compare -Wno-double-promotion $(LIBS) $< -o $@

# ------------------------------
#  cleanup
# ------------------------------
clean:
	rm -f sqlite3.o sqlite3.a
	rm -f sqlfs.o libsqlfs.a c_*
	rm -f *.db* .db-*

clean2:
	rm -f sqlfs.o libsqlfs.a c_*
	rm -f *.db* .db-*

testclean:
	rm -f c_*
	rm -f *.db* .db-*

testdbclean:
	rm -f *.db* .db-*
