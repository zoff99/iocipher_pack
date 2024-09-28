cmake_minimum_required(VERSION 3.4.1)
project(libiocipher2-c)

include_directories(libsqlfs)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DHAVE_LIBSQLCIPHER -fPIC -g -fstack-protector-all")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DHAVE_LIBSQLCIPHER -std=c99 -fPIC -g -fstack-protector-all")

# JniException.cpp

add_library(iocipher2 SHARED
        JniConstants.cpp
        JNIHelp.cpp
        JNI_OnLoad.cpp
        readlink.cpp
        realpath.cpp
        toStringArray.cpp
        info_guardianproject_iocipher_File.cpp
        info_guardianproject_libcore_io_Memory.cpp
        info_guardianproject_libcore_io_OsConstants.cpp
        info_guardianproject_libcore_io_Posix.cpp
        info_guardianproject_iocipher_VirtualFileSystem.cpp
        )

add_library(
        libcrypto
        STATIC
        IMPORTED
)
add_library(
        libssl
        STATIC
        IMPORTED
)
add_library(
        libsqlfs
        STATIC
        IMPORTED
)
add_library(
        sqlite3
        STATIC
        IMPORTED
)
set_target_properties(libcrypto PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libcrypto.a)
set_target_properties(libssl PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libssl.a)
set_target_properties(libsqlfs PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libsqlfs.a)
set_target_properties(sqlite3 PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/sqlite3.a)

target_link_libraries(iocipher2
        libsqlfs
        sqlite3
        libssl
        libcrypto
        android
        log)
