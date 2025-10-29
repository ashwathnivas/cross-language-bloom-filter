package com.github.ashwathnivas.bloomfilter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;
import java.util.logging.Logger;

/**
 * Core Cross Platform Bloom Filter Implementation In Java
 */
public class BloomFilter {

    private static final Logger LOGGER = Logger.getLogger(BloomFilter.class.getName());

    private long entries;           // Expected number of entries
    private long bits;              // Number of bits in the filter
    private int hashes;             // Number of hash functions
    private BitSet bitArray;        // Bit array for the filter
    private boolean useMemoryMapping; // Whether to use memory mapping for large files

    private static final int HEADER_SIZE = Long.BYTES + Integer.BYTES + Long.BYTES; // 8+4+8=20

    // Constructor for creating a new Bloom filter
    public BloomFilter(long entries, double errorRate) {
        if (entries <= 0 || errorRate <= 0 || errorRate >= 1) {
            throw new IllegalArgumentException("Invalid parameters: entries must be > 0, error_rate must be in (0,1)"); //NO i18N
        }

        this.entries = entries;
        // m = -n * ln(p) / (ln(2))^2
        this.bits = (long) (-1.0 * entries * Math.log(errorRate) / (Math.log(2) * Math.log(2)) + 0.5);
        // k = (m/n) * ln(2)
        this.hashes = (int) ((double) this.bits / entries * Math.log(2) + 0.5);
        this.bitArray = new BitSet((int) this.bits);
        this.useMemoryMapping = false;

        LOGGER.info(String.format("Initialized: %.2f MB, k=%d, p=%.4f",
                this.bits / (8.0 * 1024.0 * 1024.0), this.hashes, errorRate));
    }

    // Private constructor for loading from file
    protected BloomFilter() {
        this.useMemoryMapping = false;
    }

    // Add an element to the Bloom filter
    public void add(String data) {
        add(data.getBytes());
    }

    public void add(byte[] data) {
        int hash1 = murmurHash(data, 42);
        int hash2 = murmurHash(data, hash1);

        for (int i = 0; i < this.hashes; i++) {
            long bit = Math.abs((long)(hash1 + i * hash2)) % this.bits;
            this.bitArray.set((int) bit);
        }
    }

    // Check if an element might be in the set
    public boolean contains(String data) {
        return contains(data.getBytes());
    }

    public boolean contains(byte[] data) {
        int hash1 = murmurHash(data, 42);
        int hash2 = murmurHash(data, hash1);

        for (int i = 0; i < this.hashes; i++) {
            long bit = Math.abs((long)(hash1 + i * hash2)) % this.bits;
            if (!this.bitArray.get((int) bit)) {
                return false;
            }
        }
        return true;
    }



    // Save the Bloom filter to a file
    public void save(String filename) throws IOException {
        Path path = Paths.get(filename);

        try (FileChannel channel = FileChannel.open(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            // Calculate the size needed for the bit array
            long byteArraySize = (this.bits + 7) / 8;

            // Create a ByteBuffer for the header
            ByteBuffer header = ByteBuffer.allocate(8 + 4 + 8); // entries + hashes + bits
            header.putLong(this.entries);
            header.putInt(this.hashes);
            header.putLong(this.bits);
            header.flip();

            // Write header
            channel.write(header);

            // Convert BitSet to byte array and write
            byte[] bitData = new byte[(int) byteArraySize];
            for (int i = 0; i < this.bits; i++) {
                if (this.bitArray.get(i)) {
                    bitData[i / 8] |= (1 << (i % 8));
                }
            }

            ByteBuffer bitBuffer = ByteBuffer.wrap(bitData);
            channel.write(bitBuffer);
        }

        LOGGER.info(String.format("Saved to %s: %.2f MB%n", filename,
                (this.bits / 8.0) / (1024.0 * 1024.0)));
    }

    // Load a Bloom filter from a file
    public static BloomFilter load(String filename) throws IOException {
        Path path = Paths.get(filename);

        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + filename); //NO I18N
        }

        BloomFilter bf = new BloomFilter();

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            // Read header
            ByteBuffer header = ByteBuffer.allocate(8 + 4 + 8);
            channel.read(header);
            header.flip();

            bf.entries = header.getLong();
            bf.hashes = header.getInt();
            bf.bits = header.getLong();

            // Calculate expected file size
            long byteArraySize = (bf.bits + 7) / 8;
            long expectedSize = 8 + 4 + 8 + byteArraySize; // header + bit array

            if (channel.size() < expectedSize) {
                throw new IOException("File too small: " + filename); //NO I18N
            }

            // Read bit array
            ByteBuffer bitBuffer = ByteBuffer.allocate((int) byteArraySize);
            channel.read(bitBuffer);
            bitBuffer.flip();

            bf.bitArray = new BitSet((int) bf.bits);
            byte[] bitData = bitBuffer.array();

            // Convert byte array back to BitSet
            for (int i = 0; i < bf.bits; i++) {
                if ((bitData[i / 8] & (1 << (i % 8))) != 0) {
                    bf.bitArray.set(i);
                }
            }
        }

        LOGGER.info(String.format("Loaded from %s: %.2f MB%n", filename,
                (bf.bits / 8.0) / (1024.0 * 1024.0)));

        return bf;
    }

    // Load a Bloom filter from a byte array (header + bit array)
        public static BloomFilter loadFromByteArray(byte[] data) throws IOException {
            if (data == null || data.length < HEADER_SIZE) {
                throw new IOException("Invalid Bloom filter byte array"); //NO I18N
            }
            ByteBuffer buffer = ByteBuffer.wrap(data);

            BloomFilter bf = new BloomFilter();
            bf.entries = buffer.getLong();
            bf.hashes = buffer.getInt();
            bf.bits = buffer.getLong();

            long byteArraySize = (bf.bits + 7) / 8;
            if (data.length < HEADER_SIZE + byteArraySize) {
                throw new IOException("Byte array too small for Bloom filter"); //NO I18N
            }

            if (bf.entries <= 0 || bf.bits <= 0 || bf.hashes <= 0) {
                throw new IOException("Corrupt Bloom filter header"); //NO I18N
            }

            if (bf.bits > Integer.MAX_VALUE) {
                throw new IOException("Bloom filter too large to load into BitSet"); //NO I18N
            }

            bf.bitArray = new BitSet((int) bf.bits);
            byte[] bitData = new byte[(int) byteArraySize];
            buffer.get(bitData);

            for (int i = 0; i < bf.bits; i++) {
                if ((bitData[i / 8] & (1 << (i % 8))) != 0) {
                    bf.bitArray.set(i);
                }
            }
            return bf;
        }

    // Get the Bloom filter as a byte array (header + bit array)
    public byte[] toByteArray() {
        long byteArraySize = (this.bits + 7) / 8;

        // Prepare header: entries (8 bytes), hashes (4 bytes), bits (8 bytes)
        ByteBuffer header = ByteBuffer.allocate(8 + 4 + 8);
        header.putLong(this.entries);
        header.putInt(this.hashes);
        header.putLong(this.bits);
        header.flip();

        // Prepare bit array
        byte[] bitData = new byte[(int) byteArraySize];
        for (int i = 0; i < this.bits; i++) {
            if (this.bitArray.get(i)) {
                bitData[i / 8] |= (1 << (i % 8));
            }
        }

        // Combine header and bit array
        byte[] result = new byte[header.remaining() + bitData.length];
        header.get(result, 0, header.remaining());
        System.arraycopy(bitData, 0, result, header.capacity(), bitData.length);

        return result;
    }

    // Print statistics about the Bloom filter
    public void printStats() {
        double sizeMB = (this.bits / 8.0) / (1024.0 * 1024.0);
        double falsePositiveRate = Math.exp(-((double) this.bits / this.entries) *
                Math.log(2) * Math.log(2) / this.hashes);

        LOGGER.info(String.format(
            "Bloom Filter Stats:\n  Entries: %d\n  Bits: %d\n  Hashes (k): %d\n  Size: %.2f MB\n  Theoretical False Positive Rate: %.4f%%", this.entries, this.bits, this.hashes, sizeMB, falsePositiveRate * 100)); //NO i18N
    }

    // MurmurHash implementation
    private static int murmurHash(byte[] data, int seed) {
        final int m = 0x5bd1e995;
        final int r = 24;

        int h = seed ^ data.length;
        int len = data.length;
        int offset = 0;

        while (len >= 4) {
            int k = (data[offset] & 0xff) |
                    ((data[offset + 1] & 0xff) << 8) |
                    ((data[offset + 2] & 0xff) << 16) |
                    ((data[offset + 3] & 0xff) << 24);

            k *= m;
            k ^= k >>> r;
            k *= m;

            h *= m;
            h ^= k;

            offset += 4;
            len -= 4;
        }

        switch (len) {
            case 3:
                h ^= (data[offset + 2] & 0xff) << 16;
            case 2:
                h ^= (data[offset + 1] & 0xff) << 8;
            case 1:
                h ^= (data[offset] & 0xff);
                h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }

}
