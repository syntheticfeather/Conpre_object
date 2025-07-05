package JavaStudy.HelloWorld;
public class L {

    public void test() {
        System.out.println("Hello World");// 自带\n
        System.out.print("Hello World\n");
        System.out.printf("%d\n", 5);// 格式化输出
        // 没有\n不会刷新缓冲区
        // System.out.print("Hello World");
    }

    public static void main(String[] args) {
        var ins = new L();
        ins.test();
    }

}
