package JavaStudy.IO;
import java.util.Scanner;

public class Task_qyx {
    public static void main(String[] args) {
        Scanner scanner=new Scanner(System.in);
        int integerValue = scanner.nextInt();
        double doubleValue = scanner.nextDouble();
        scanner.nextLine(); 
        String stringValue = scanner.nextLine();
        System.out.println(integerValue + " " + doubleValue + " " + stringValue);
        scanner.close();
    }
}