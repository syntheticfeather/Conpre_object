package JavaStudy.Operation;

public class Task_qyx {
    // 两数之和
    public static int sum(int a, int b) {
      int c=a+b;
        return c;
    }

    // 温度单位转换
    public static double fahrenheitToCelsius(double f) {
      double g=(f - 32) * 5/9;
        return g;
    }

    // 三维空间两点欧氏距离平方
    public static double euclideanDistanceSquared(double x1, double y1, double z1, 
                                                   double x2, double y2, double z2) {
      double x=Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) + Math.pow(z1 - z2, 2);
        return x;
    }

    // 垂直上抛位移计算
    public static double verticalDisplacement(double v0, double t) {
        final double g = 9.8;
        double a=v0 * t - g * Math.pow(t, 2) / 2;
        return a;
    }

    // 位运算交换两个整数
    public static void swap(int[] nums) {
        nums[0] = nums[0] ^ nums[1];
        nums[1] = nums[0] ^ nums[1];
        nums[0] = nums[0] ^ nums[1];
    }

    // 位运算判断偶数
    public static boolean isEven(int num) {
        return (num & 1) == 0;
    }

    // 合并两个short为一个int
    public static int mergeShorts(short s1, short s2) {
        return ((s1 & 0xFFFF) << 16) | (s2 & 0xFFFF);
    }
}    
