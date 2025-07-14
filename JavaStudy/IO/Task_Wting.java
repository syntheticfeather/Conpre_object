package JavaStudy.IO;
import java.util.Scanner;

public class Task_Wting {
    public static void main(String[] args) {
    Scanner scanner=new Scanner(System.in);
    System.out.println("输入整数");
    int num=scanner.nextInt();
    System.out.println("输入浮点数");
    double d=scanner.nextDouble();
    scanner.nextLine();
    System.out.println("输入字符串");
    String s=scanner.nextLine();  
    System.out.printf("整数：%d,浮点数：%f,字符串：%s",num,d,s);
    scanner.close();
    }
}
