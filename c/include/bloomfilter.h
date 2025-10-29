#ifndef BLOOMFILTER_H
#define BLOOMFILTER_H

#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>

typedef struct BloomFilter BloomFilter;

BloomFilter* bloomFilterLoad(const char* filename);
bool bloomFilterContains(const BloomFilter* bf, const uint8_t* data, size_t length);
void bloomFilterFree(BloomFilter* bf);

#endif