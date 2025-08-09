package gcc.pra;

import org.junit.jupiter.api.Test;

public class ThreadLocalTest {

    @Test
    public void test() {
        ThreadLocal tl = new ThreadLocal();

        new Thread(() -> {
            tl.set("Hello");
            System.out.println(tl.get() + " from thread 1");
            System.out.println(tl.get() + " from thread 1");
            System.out.println(tl.get() + " from thread 1");
        }, "Thread 1").start();

        new Thread(() -> {
            tl.set("World");
            System.out.println(tl.get() + " from thread 2");
            System.out.println(tl.get() + " from thread 2");
            System.out.println(tl.get() + " from thread 2");
        }, "Thread 2").start();
    }
}
