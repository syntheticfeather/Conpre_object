package JavaStudy.DataType;

public class Task_zff {
    //任务二 int float double long占用的字节数和能容纳的最大值
    public static void findSize(){
        // int
        System.out.println("The size of int:"+Integer.BYTES);
        System.out.println("The max int:"+Integer.MAX_VALUE);
        // float
        System.out.println("The size of float:"+Float.BYTES);
        System.out.println("The max float:"+Float.MAX_VALUE);
        // double
        System.out.println("The size of double:"+Double.BYTES);
        System.out.println("The max double:"+Double.MAX_VALUE);
        System.out.println("The size of long:"+Long.BYTES);
        // long
        System.out.println("The max long:"+Long.MAX_VALUE);
    }

    public static void main(String[] args){
        //任务一 能容纳10^15的数据类型
        System.out.println("long");

        //任务二
        findSize();

        //任务三 字面量赋值
        int oneBillon=1000000000;
        double pi=3.1415926535;
        String word="Abondance";
        byte x=0b01111111;

        //任务四 溢出实验
        int b=Byte.MAX_VALUE+1;
        int s=Short.MIN_VALUE-1;
        System.out.println("The max byte add 1:"+b);
        System.out.println("The min short minus 1:"+s);

        //任务五
        int a1=5/2; //整数运算结果是整数，5/2=2
        double a2=5.0/2; //不同类型运算，范围小的会提升成范围大的再运算，2提升成double类型，5.0/2=2.5
        double y=5/2+5.0/2; // 2+2.5=4.5  2提升为double类型
        System.out.println("5/2="+a1);
        System.out.println("5.0/2="+a2);
        System.out.println("5/2+5.0/2 = "+y);
    }
}
