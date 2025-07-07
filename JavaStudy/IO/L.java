package JavaStudy.IO;

import java.util.Scanner;

public class L {

    public void test() {
        try (Scanner sc = new Scanner(System.in)) {
            // System.out.print("Enter a number: " + sc.nextInt() + "\n");
            // String s = sc.nextLine();// 不跳空格
            // 不定数量的输入            
            while (true)
            {
                String str = sc.nextLine().trim();
                if (str.isEmpty()) {
                    break; // 如果输入为空，则退出循环
                } 
                System.out.println(str);               
            }
        } catch (Exception e) {
            System.out.println("Invalid input. Please enter a valid number.");
        }
    }

    public static void main(String[] args) {
        L l = new L();
        l.test();
    }
}
