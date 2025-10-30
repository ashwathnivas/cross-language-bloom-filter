# Bloom Filter Binary Format (v1)

**All multi-byte integers: Big-endian**

| Offset | Size | Type   | Description |
|-------|------|--------|-----------|
| 0     | 8    | uint64 | `entries` (expected insertions) |
| 8     | 4    | uint32 | `hashes` (k) |
| 12    | 8    | uint64 | `bits` (m) |
| 20    | `ceil(bits/8)` | byte[] | Bit array (LSB per byte) |

**Bit indexing**: bit `i` → `byte[i/8]`, bit `(i % 8)` (0 = LSB)

**Hashing**: MurmurHash32 (seed 42), then `h1 + i*h2` in **signed 32-bit**, take abs, mod `bits`