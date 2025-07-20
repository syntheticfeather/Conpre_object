package JavaStudy.Operation;

public class Task_Wting {

    //任务一：两数求和
    public static int getSum(int a, int b) {
        return a + b;
    }

    //任务二：单位转换
    public static double change(double f) {
        double c = (f - 32) * 5 / 9;
        return c;
    }

    //任务三：计算欧氏距离平方
    public static double calculate(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) + Math.pow(z1 - z2, 2);
    }

    //任务四：位移
    public static double move(double t, int v0) {
        final double GRAVITY = 9.8;
        double m = v0 * t - 0.5 * GRAVITY * Math.pow(t, 2);
        return m;
    }

    //任务五：交换值
    public static int[] exchange(int x, int y) {
        x = x ^ y;
        y = x ^ y;
        x = x ^ y;
        int[] result = new int[] {x, y};
        return result;
    }

    //任务六：奇偶性判断
    public static boolean isEven(int num) {
        boolean isEven = (num & 1) == 0;
        return isEven;
    }

    //任务七：数据压缩编码
    public static int compression(short s1, short s2) {
        return (s1 << 16) | (s2 & 0xFFFF);
    }

    public static void main(String[] args) {
        int a = 1;
        int b = 3;
        System.out.println("before exchange: a= " + a + ", b= " + b);
        exchange(a, b);
        System.out.println("after exchange: a= " + a + ", b= " + b);
    }
}
