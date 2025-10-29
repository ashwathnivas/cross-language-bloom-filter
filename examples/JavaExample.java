import bloomfilter.BloomFilter;

public class JavaExample {
    public static void main(String[] args) throws Exception {
        BloomFilter bf = new BloomFilter(1000, 0.01);
        bf.add("hello");
        bf.add("world");
        bf.save("filter.bf");

        BloomFilter loaded = BloomFilter.load("filter.bf");
        System.out.println("hello: " + loaded.contains("hello"));
        System.out.println("goodbye: " + loaded.contains("goodbye"));
    }
}