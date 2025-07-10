package JavaStudy.DataType;
public class L {

    public void test() {
        System.out.println("for int");
        int a = 5;
        int b = Integer.SIZE;
        int c = Integer.BYTES;
        System.out.println("value of a: " + a);
        System.out.println("The size of int in bits: " + b);
        System.out.println("The number of bytes used by int: " + c);
        System.out.println("The Max int:" + Integer.MAX_VALUE);
        System.out.println("The min int:" + Integer.MIN_VALUE);
        System.out.println("------");

        System.out.println("for double");
        double d = 5.5;
        int e = Double.SIZE;
        int f = Double.BYTES;
        System.out.println("value of d: " + d);
        System.out.println("The size of double in bits: " + e);
        System.out.println("The number of bytes used by double: " + f);
        System.out.println("The Max double:" + Double.MAX_VALUE);
        System.out.println("The min double:" + Double.MIN_VALUE);
        System.out.println("------");
    }

    public static void main(String[] args) {
        L ins = new L(); //创建对象
        ins.test();

        int n=3300;
        int y=100 + (++n);
        int x=100 + (n++);
        System.out.println(y);//3401
        System.out.println(x);//3401

        double a = 1.0 / 10;
        double b = 1 - 9.0 / 10;
        System.out.println(a);
        System.out.println(b);//a!=b
    }

}

