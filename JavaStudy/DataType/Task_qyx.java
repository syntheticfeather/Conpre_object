package JavaStudy.DataType;

public class Task_qyx {
    public static void main(String[] args) {
        // 任务一：能容纳10^15的数据类型
        System.out.println("long");

        // 任务二：各数据类型的字节数和最大值
        printDataTypeInfo();

        // 任务三：字面量赋值
        long billion = 1_000_000_000L;
        double pi = 3.1415926535;
        String word = "Abondance";
        byte binary = 0b01111111;
        System.out.println("10亿: " + billion);
        System.out.println("10位圆周率: " + pi);
        System.out.println("Abondance: " + word);
        System.out.println("二进制的1111111: " + binary);

        // 任务四：溢出实验
        int byteOverflow = Byte.MAX_VALUE + 1;
        int shortUnderflow = Short.MIN_VALUE - 1;
        System.out.println("Byte.MAX_VALUE + 1: " + byteOverflow);
        System.out.println("Short.MIN_VALUE - 1: " + shortUnderflow);

        // 任务五：类型自动提升
        double intDivision = 5 / 2;
        double doubleDivision = 5.0 / 2;
        double result = intDivision + doubleDivision;
        
        System.out.println("5 / 2 = " + intDivision);
        System.out.println("5.0 / 2 = " + doubleDivision);
        System.out.println("5 / 2 + 5.0 / 2 = " + result);
        
        System.out.println("\n类型自动提升详细解释,");
        System.out.println("1. 5 / 2 中,两个操作数均为int类型,因此执行整数除法,结果为2(小数部分被截断)");
        System.out.println("2. 5.0 / 2 中,5.0是double类型,2是int类型");
        System.out.println("   - 根据Java类型提升规则,int类型的2会自动提升为double类型(2.0)");
        System.out.println("   - 执行double类型除法,结果为2.5");
        System.out.println("3. 最终表达式为 2 + 2.5,由于2被赋值给double变量,会转换为2.0");
        System.out.println("   - 2.0 + 2.5 = 4.5");
    }

    // 任务二：封装成函数
    public static void printDataTypeInfo() {
        System.out.println("The size of int: " + Integer.BYTES);
        System.out.println("The max int: " + Integer.MAX_VALUE);
        System.out.println("The size of float: " + Float.BYTES);
        System.out.println("The max float: " + Float.MAX_VALUE);
        System.out.println("The size of double: " + Double.BYTES);
        System.out.println("The max double: " + Double.MAX_VALUE);
        System.out.println("The size of long: " + Long.BYTES);
        System.out.println("The max long: " + Long.MAX_VALUE);
    }
}    
