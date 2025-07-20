package JavaStudy.Operation;

public class Task {

    boolean isEven(int num) {
        return (num & 1) == 0;
    }

    public static int compression(short s1, short s2) {
        return (s1 << 16) | (s2 & 0xFFFF);
    }

    public static void main(String[] args) {
        // 两数之和
        int a = 10;
        int b = 20;
        Task t = new Task();
        System.out.println(t.isEven(b));
        System.out.println(t.isEven(a));
        System.out.println(t.isEven(a - 1));

    }
}
// 以下任务均写函数
/*
描述:
    两数之和，梦开始的算法
任务:
    写一个函数，返回两个整数数的和
 */


 /*
描述:
    温度单位转换
任务:
    用一行表达式将华氏度f转换为摄氏度c
    公式：c = (f - 32) * 5/9
 */
 /*
描述:
    几何计算，是的你电懒得不行
任务:
    计算三维空间两点(x1,y1,z1)和(x2,y2,z2)的欧氏距离平方
    公式：(x1-x2)² + (y1-y2)² + (z1-z2)²
 */
 /*
描述:
    物理运动计算
任务:
    计算初速度v0的物体垂直上抛t秒(t可以是小数)后的位移（公式：v0*t - g*t²/2，g=9.8）
 */
 /*
描述:
    数字位操作
任务:
    用位运算交换整数x和y的值（不用临时变量）
 */
 /*
描述:
    奇偶校验
任务:
    用位运算判断整数num是否为偶数（返回boolean）
 */

 /*
描述:
    数据压缩编码
任务:
    将两个short值s1,s2合并为一个int（s1在高16位，s2在低16位）
 */
