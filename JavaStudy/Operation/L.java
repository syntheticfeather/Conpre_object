package JavaStudy.Operation;
public class L {

    public void test() {
        int a = 2;
        int b = 10;
        System.err.println(a + b);
        System.err.println(a - b);
        System.err.println(a * b);        
        // 不运行程序先想想除出来是多少呢？
        System.err.println(a / b);
        // 哈这不是次方
        System.err.println(a ^ b); 

    }

    public static void main(String[] args) {
        L l = new L();
        l.test();
    }
}
