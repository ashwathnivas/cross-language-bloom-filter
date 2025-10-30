# cross-language-bloom-filter

[![CI](https://github.com/ashwathnivas/cross-language-bloom-filter/actions/workflows/ci.yml/badge.svg)](https://github.com/ashwathnivas/cross-language-bloom-filter/actions)

**One Bloom filter. Many languages.**

Write in **Java** → Read in **C** → Extend to **Python, Go, Rust**.

A **language-agnostic Bloom filter** with **byte-for-byte identical serialization** between **Java** and **C**.

- Add elements in Java → query in C
- Add in C → query in Java (soon)
- Extendable to Python, Go, Rust, etc.

## Quick Start

### Java
```java
BloomFilter bf = new BloomFilter(100000, 0.01);
bf.add("hello");
bf.save("filter.bf");
```

### C
```C
BloomFilter* bf = bloomFilterLoad("filter.bf");
printf("Contains: %s\n", bloomFilterContains(bf, "hello", 5) ? "YES" : "NO");
bloomFilterFree(bf);
```

## Features
- Zero dependencies
- MIT Licensed
- Binary compatible (big-endian header)
- MurmurHash32 with Java/C int semantics
- Memory efficient
- Byte-for-byte identical files

## Works Everywhere

| OS | Java | C |
|----|------|---|
| Linux | Yes | Yes |
| macOS | Yes | Yes |
| Windows | Yes | Yes (MSVC/MinGW) |

---
