public class BloomFilterTest 
{
    @Test
    void testRoundTrip() throws Exception {
    BloomFilter bf = new BloomFilter(100, 0.01);
    bf.add("hello");
    bf.add("world");

    bf.save("test.bf");
    BloomFilter loaded = BloomFilter.load("test.bf");

    assertTrue(loaded.contains("hello"));
    assertFalse(loaded.contains("goodbye"));
    }
}
