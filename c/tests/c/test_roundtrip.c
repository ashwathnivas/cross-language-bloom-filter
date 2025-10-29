#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "bloomfilter.h"

int main() {
    BloomFilter* bf = bloomFilterLoad("java/target/test.bf");
    if (!bf) {
        printf("Failed to load filter\n");
        return 1;
    }

    if (!bloomFilterContains(bf, "hello", 5)) return 1;
    if (!bloomFilterContains(bf, "world", 5)) return 1;
    if (bloomFilterContains(bf, "goodbye", 7)) return 1;

    bloomFilterFree(bf);
    printf("C test passed!\n");
    return 0;
}