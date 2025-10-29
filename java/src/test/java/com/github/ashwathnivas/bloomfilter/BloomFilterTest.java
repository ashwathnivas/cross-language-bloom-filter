package com.github.ashwathnivas.bloomfilter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BloomFilterTest {

    @Test
    void testRoundTrip(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("test.bf");

        BloomFilter bf = new BloomFilter(100, 0.01);
        bf.add("hello");
        bf.add("world");
        bf.save(file.toString());

        BloomFilter loaded = BloomFilter.load(file.toString());

        assertTrue(loaded.contains("hello"));
        assertTrue(loaded.contains("world"));
        assertFalse(loaded.contains("goodbye"));
    }

    @Test
    void testByteArrayRoundTrip() throws Exception {
        BloomFilter bf = new BloomFilter(100, 0.01);
        bf.add("test");

        byte[] data = bf.toByteArray();
        BloomFilter loaded = BloomFilter.loadFromByteArray(data);

        assertTrue(loaded.contains("test"));
    }
}