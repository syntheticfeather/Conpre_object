import java.util.Random;

public class TestTool {

    Random ran = new Random();

    // 随机整数生成，lower下界，upper上界
    public int ranInt(int lower, int upper) {
        return ran.nextInt(upper - lower + 1) + lower;
    }

    // 随机浮点数生成，lower下界，upper上界
    public double ranDouble(double lower, double upper) {
        return ran.nextDouble() * (upper - lower) + lower;
    }

    // 随机整数数组生成，size数组大小，lower下界，upper上界
    public int[] ranIntArray(int size, int lower, int upper) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = ran.nextInt(upper - lower + 1) + lower;
        }
        return arr;
    }

    // 随机浮点数数组生成，size数组大小，lower下界，upper上界
    public double[] ranDoubleArray(int size, double lower, double upper) {
        double[] arr = new double[size];
        for (int i = 0; i < size; i++) {
            arr[i] = ran.nextDouble() * (upper - lower) + lower;
        }
        return arr;
    }
}
