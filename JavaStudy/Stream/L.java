package JavaStudy.Stream;

import java.util.stream.Stream;

public class L {

    public static void main(String[] args) {
        // 创建一个自然数stream
        Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5); // 创建Stream
        // 用generate
        Stream<Integer> stream2 = Stream.generate(() -> (int) (Math.random() * 100)).limit(10);
        
        System.out.println(stream.count());
        // System.out.println(stream.sum());
        stream2.forEach(n -> System.out.println(n));
        // System.out.println(stream.reduce(0, (acc, op) -> acc + op));
        // stream.collect(Collectors.toList());
    }
}
