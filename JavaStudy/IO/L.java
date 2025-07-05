package JavaStudy.IO;

import java.util.Scanner;


public class L {

    public void test() {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.print("Enter a number: " + sc.nextInt() + "\n");
            // String s = sc.nextLine();// 不跳空格
            String s = sc.next();// 跳过空格
            System.out.println("tt" + s);
        }
        catch (Exception e) {
            System.out.println("Invalid input. Please enter a valid number.");
        }
    }

    public static void main(String[] args) {
        L l = new L();
        l.test();
    }
}
