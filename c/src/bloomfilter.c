// c
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdbool.h>
#include <string.h>

/*
 Interop notes with Java implementation:
 - Header order (big-endian): [entries:8][hashes:4][bits:8]
 - Bit array packed LSB-first per byte: bit i -> byte[i/8], bit (i%8)
 - Hashing: MurmurHash32 (0x5bd1e995) with Java int semantics
 - Indexing: compute h1 + i*h2 in 32-bit signed int with wrap, then cast to long
*/

typedef struct {
    uint64_t entries;     // Expected number of entries (from file)
    uint64_t bits;        // Number of bits in the filter
    uint32_t hashes;      // Number of hash functions
    uint8_t*  bit_array;  // Bit array
} BloomFilter;

/* --- Bit operations (LSB-first within a byte) --- */
static inline bool get_bit(const uint8_t* array, uint64_t index) {
    return (array[index >> 3] >> (index & 7)) & 1U;
}

/* --- Big-endian readers --- */
static bool read_be_u32(FILE* f, uint32_t* out) {
    uint8_t b[4];
    if (fread(b, 1, 4, f) != 4) return false;
    *out = ((uint32_t)b[0] << 24) |
           ((uint32_t)b[1] << 16) |
           ((uint32_t)b[2] <<  8) |
           ((uint32_t)b[3] <<  0);
    return true;
}

static bool read_be_u64(FILE* f, uint64_t* out) {
    uint8_t b[8];
    if (fread(b, 1, 8, f) != 8) return false;
    *out = ((uint64_t)b[0] << 56) |
           ((uint64_t)b[1] << 48) |
           ((uint64_t)b[2] << 40) |
           ((uint64_t)b[3] << 32) |
           ((uint64_t)b[4] << 24) |
           ((uint64_t)b[5] << 16) |
           ((uint64_t)b[6] <<  8) |
           ((uint64_t)b[7] <<  0);
    return true;
}

/* --- MurmurHash32 (matches Java) --- */
static int32_t murmur_hash32(const uint8_t* data, size_t length, int32_t seed) {
    const uint32_t m = 0x5bd1e995U;
    const uint32_t r = 24U;

    uint32_t h = (uint32_t)seed ^ (uint32_t)length;
    size_t len = length;
    size_t offset = 0;

    while (len >= 4) {
        uint32_t k = (uint32_t)data[offset] |
                     ((uint32_t)data[offset + 1] << 8) |
                     ((uint32_t)data[offset + 2] << 16) |
                     ((uint32_t)data[offset + 3] << 24);

        k *= m;
        k ^= k >> r;
        k *= m;

        h *= m;
        h ^= k;

        offset += 4;
        len -= 4;
    }

    switch (len) {
        case 3: h ^= (uint32_t)data[offset + 2] << 16;
        case 2: h ^= (uint32_t)data[offset + 1] << 8;
        case 1: h ^= (uint32_t)data[offset + 0];
                h *= m;
    }

    h ^= h >> 13;
    h *= m;
    h ^= h >> 15;

    return (int32_t)h; // signed 32-bit like Java
}

/* --- Loader --- */
BloomFilter* bloomFilterLoad(const char* filename) {
    FILE* f = fopen(filename, "rb");
    if (!f) {
        fprintf(stderr, "Failed to open file: %s\n", filename);
        return NULL;
    }

    BloomFilter* bf = (BloomFilter*)calloc(1, sizeof(BloomFilter));
    if (!bf) {
        fclose(f);
        fprintf(stderr, "Out of memory\n");
        return NULL;
    }

    // Header: entries(8), hashes(4), bits(8) in big-endian
    if (!read_be_u64(f, &bf->entries) ||
        !read_be_u32(f, &bf->hashes)  ||
        !read_be_u64(f, &bf->bits)) {
        fprintf(stderr, "Failed to read header (big-endian)\n");
        free(bf);
        fclose(f);
        return NULL;
    }

    uint64_t byte_count = (bf->bits + 7) / 8;
    if (byte_count == 0) {
        fprintf(stderr, "Invalid bit array size\n");
        free(bf);
        fclose(f);
        return NULL;
    }

    bf->bit_array = (uint8_t*)malloc((size_t)byte_count);
    if (!bf->bit_array) {
        fprintf(stderr, "Out of memory for bit array (%llu bytes)\n",
                (unsigned long long)byte_count);
        free(bf);
        fclose(f);
        return NULL;
    }

    size_t nread = fread(bf->bit_array, 1, (size_t)byte_count, f);
    if (nread != (size_t)byte_count) {
        fprintf(stderr, "Failed to read bit array (read %zu of %llu)\n",
                nread, (unsigned long long)byte_count);
        free(bf->bit_array);
        free(bf);
        fclose(f);
        return NULL;
    }

    fclose(f);
    return bf;
}

/* --- Query --- */
bool bloomFilterContains(const BloomFilter* bf, const uint8_t* data, size_t length) {
    if (!bf || !bf->bit_array || bf->bits == 0 || bf->hashes == 0) return false;

    int32_t h1 = murmur_hash32(data, length, 42);
    int32_t h2 = murmur_hash32(data, length, h1);

    for (uint32_t i = 0; i < bf->hashes; ++i) {
        // Java does 32-bit signed arithmetic with wrap for (h1 + i*h2)
        uint32_t ucombined = (uint32_t)h1 + ((uint32_t)i * (uint32_t)h2);
        int32_t combined32 = (int32_t)ucombined;

        int64_t combined64 = (int64_t)combined32;
        uint64_t bit = (uint64_t)(combined64 < 0 ? -combined64 : combined64) % bf->bits;

        if (!get_bit(bf->bit_array, bit)) {
            return false;
        }
    }
    return true;
}

/* --- Cleanup --- */
void bloomFilterFree(BloomFilter* bf) {
    if (!bf) return;
    free(bf->bit_array);
    free(bf);
}