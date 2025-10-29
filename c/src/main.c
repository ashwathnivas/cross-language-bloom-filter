#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "bloomfilter.h"

int main(int argc, char* argv[]) {
    if (argc < 3) {
        fprintf(stderr, "Usage: %s <bloom_file> <string_to_check>\n", argv[0]);
        return 1;
    }

    const char* path = argv[1];
    const char* str  = argv[2];

    BloomFilter* bf = bloomFilterLoad(path);
    if (!bf) return 1;

    bool present = bloomFilterContains(bf, (const uint8_t*)str, strlen(str));
    printf("Contains: %s\n", present ? "YES" : "NO");

    bloomFilterFree(bf);
    return 0;
}