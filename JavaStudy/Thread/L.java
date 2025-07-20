package JavaStudy.Thread;

import java.time.LocalTime;

public class L {

    public void test() throws Exception {
        {
            // 线程的创建
            // System.out.println("This could be a little bit confusing");

            // Thread t = new T();
            // t.start();// 没有join 所以不会等 t 结束再往下进行
            // System.out.println("This is the end of t1");
            // // 结果先 end 后 hello world

            // Thread t2 = new T("not bad", 3000);
            // t2.start();
            // t2.join(); // main线程等待t2线程执行完毕，才会往下进行
            // // 结果先 not bad 后 end

            // System.out.println("This is the end of t2");
        }
        {
            // 中断进程
            // Thread t1 = new T();
            // t1.start();
            // Thread.sleep(5000);
            // t1.interrupt(); // 中断t1线程
            // t1.join();
            // System.out.println("This is the end of t1");
        }
        {
            // 创建守护线程
            Thread daemonThread = new Thread(() -> {
                while (true) {
                    System.out.println("守护线程正在运行...");
                    try {
                        Thread.sleep(1000); // 每秒执行一次
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            // daemonThread.setDaemon(true); // 设置为守护线程
            daemonThread.start();

            // 主线程工作
            System.out.println("主线程开始工作");
            for (int i = 1; i <= 3; i++) {
                System.out.println("主线程处理任务 " + i);
                try {
                    Thread.sleep(1500); // 模拟工作耗时
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("主线程工作完成，即将退出");
        }
    }

    public static void main(String[] args) throws Exception {
        L l = new L();
        l.test();
    }

}

class T extends Thread {
    String s = "Hello World";
    int time = 1000;

    public T() {
    }

    public T(int i) {
        this.time = i;
    }

    @Override
    public void run() {
        while (true) {
            System.out.println(LocalTime.now());
            try {
                Thread.sleep(time);
            } catch (InterruptedException ex) {
            }
        }
    }
}

class T2 extends Thread {
    String s = "Hello World from T2";
    int time = 1000;

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
            System.out.println(s);
        } catch (Exception e) {
        }
    }

}