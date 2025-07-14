package JavaStudy.DataType;
public class Task_Wting {

    //任务一:合适数据类型
    public static void main(String[] args){
    System.out.println("long");

    //任务二：返回int float double long最大数值和占用字节
    System.out.println("The Max Short:" + Short.MAX_VALUE);
    System.out.println("The size of Short in bytes:"+Short.BYTES);

    System.out.println("The Max int:" + Integer.MAX_VALUE);
    System.out.println("The size of int in bytes: " +Integer.BYTES);

    System.out.println("The Max long: " + Long.MAX_VALUE);
    System.out.println("The size of long in bytes: " + Long.BYTES);
        
    System.out.println("The Max float:"+Float.MAX_VALUE);
    System.out.println("The size of float in bytes: " + Float.BYTES);
        
    System.out.println("The Max double:" + Double.MAX_VALUE);
    System.out.println("The size of double in bytes: " + Double.BYTES);
    
    //任务三：恰当表示
    int a=1_000_000_000;//10亿
    System.out.println(a);
    double PI=3.1415926535;
    System.out.println(PI);
    String s="Abondance";
    System.out.println(s);
    //int b=1111111修正
    Byte b=0b1111111;
    System.out.println(b);

    //任务四：溢出试验
    int x=Byte.MAX_VALUE + 1;  
    int y=Short.MIN_VALUE - 1;
    System.out.println("Byte.MAX_VALUE + 1="+x);
    System.out.println("Short.MIN_VALUE - 1="+y);

    //任务五：连类型自动提升
    double m=5/2;
    System.out.println(m);
    double n=5.0/2;
    System.out.println(n);
    System.out.println("'5/2'中'5'和'2'都是整数,'/'运算符只保留整数部分'2'，再转化为浮点型'2.0';'5.0/2'中'5.0'为浮点型，'2'自动变为浮点型，计算结果保留小数，得到'2.5'.");
    }
}
