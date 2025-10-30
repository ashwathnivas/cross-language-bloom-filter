#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "bloomfilter.h"

int main() {
    // Load the Bloom filter from a file
    const char* bloom_file = "filter.bf";
    BloomFilter* bf = bloomFilterLoad(bloom_file);
    if (!bf) {
        fprintf(stderr, "Failed to load Bloom filter from file: %s\n", bloom_file);
        return 1;
    }

    // Check if certain strings are in the Bloom filter
    const char* test_strings[] = {"hello", "world", "goodbye"};
    for (size_t i = 0; i < sizeof(test_strings) / sizeof(test_strings[0]); i++) {
        const char* str = test_strings[i];
        bool present = bloomFilterContains(bf, (const uint8_t*)str, strlen(str));
        printf("'%s' is %s in the Bloom filter\n", str, present ? "likely" : "not");
    }

    // Free the Bloom filter
    bloomFilterFree(bf);
    return 0;
}