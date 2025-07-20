package JavaStudy.Operation;

public class Task_zff {

    //计算两数之和
    public static int getSum(int x, int y) {
        return x + y;
    }

    //温度单位转换
    public static double fToC(double f) {
        return (f - 32) * 5 / 9;
    }

    //几何计算
    public static double distanceSquared(double x1, double y1, double z1,
            double x2, double y2, double z2) {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2);
    }

    //物理运动计算
    public static double getDistance(double v0, double t) {
        return v0 * t - 9.8 * t * t / 2;
    }

    //数字位操作
    public static int[] numberExchange(int a, int b) {
        a = a ^ b;
        b = a ^ b;
        a = a ^ b;
        return new int[]{a, b};
    }

    //奇偶校验
    public static boolean parity(int a) {
        return (a & 1) == 1;
        // 1 & 0 = 0
        // 0 & 0 = 0
    }
    

    //数据压缩编码
    public static int numberMerge(short s1, short s2) {
        int a = s2 & 0XFFFF;
        int b = s1 << 16;
        return a | b;
    }

    public static void main(String[] args) {
        int sum = getSum(19, 24);
        System.out.println("19+24=" + sum);

        double c = fToC(100.0);
        System.out.println("100华氏度转为摄氏度:" + c);

        double s = distanceSquared(1, 2, 3, 4, 5, 6);
        System.out.println("两点距离平方：" + s);

        double x = getDistance(10, 2);
        System.out.println("v0=10,after 2s:x=" + x);

        int[] n = numberExchange(2, 3);
        System.out.println("after change a=" + n[0] + " b=" + n[1]);

        int t = 5;
        boolean a = parity(t);
        System.out.println(t + " is even? " + a);

        short s1 = 0X1234;
        short s2 = 0X5678;
        int s12 = numberMerge(s1, s2);
        System.out.printf("after merge:0X%08x", s12);
    }
}
