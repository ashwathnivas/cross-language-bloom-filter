cmake_minimum_required(VERSION 3.10)
project(BloomFilter C)

set(CMAKE_C_STANDARD 11)

# Library
add_library(bloomfilter STATIC src/bloomfilter.c)
target_include_directories(bloomfilter PUBLIC include)

# Demo
add_executable(bloomfilter_demo src/main.c)
target_link_libraries(bloomfilter_demo bloomfilter)

# Tests
enable_testing()
add_executable(test_roundtrip tests/c/test_roundtrip.c)
target_link_libraries(test_roundtrip bloomfilter)
add_test(NAME RoundTrip COMMAND test_roundtrip)