package com.github.ashwathnivas.bloomfilter;

public class JavaExample {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "generate".equals(args[0])) {
            BloomFilter bf = new BloomFilter(1000, 0.01);
            bf.add("hello");
            bf.add("world");
            bf.save("filter.bf");
            System.out.println("Generated filter.bf");
            return;
        }

        BloomFilter bf = new BloomFilter(1000, 0.01);
        bf.add("test");
        bf.save("filter.bf");
        System.out.println("Saved filter.bf");
    }
}