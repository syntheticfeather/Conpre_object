import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Random;
import java.util.Scanner;

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

    public static void main(String[] args) {
    }
}

class Task {

    public static void main(String[] args) {
        Task t = new Task();
        t.input();
        t.changeGrade();
        t.encrypt();
    }

    final int MAX_SIZE = 2048;

    public void input() {
        Scanner sc = new Scanner(System.in);
        try (FileWriter fw = new FileWriter(".\\JavaStudy\\FileOI\\Input.txt")) {
            while (true) {
                String str = sc.nextLine();
                if (str.isEmpty())
                    break;
                fw.write(str.replace(" ", "_") + "\n");
            }
        } catch (Exception e) {
        }
        sc.close();
    }

    public void changeGrade() {
        // try(BufferedReader br= new BufferedReader(
        // new InputStreamReader(new FileInputStream("Input.txt"),
        // StandardCharsets.UTF_8)))
        try (BufferedReader br = new BufferedReader(
                new FileReader(".\\JavaStudy\\FileOI\\Input.txt"))) {
            // 拿出姓名栏
            String[] name = new String[MAX_SIZE];
            int size = 0;
            String comLine;
            while ((comLine = br.readLine()) != null) {
                name[size++] = comLine.split("_")[1];
            }
            // 读成绩表姓名
            String[][] grade = new String[MAX_SIZE][3];
            int s = 0;
            try (BufferedReader br2 = new BufferedReader(
                    new FileReader(".\\JavaStudy\\FileOI\\grade.txt"))) {
                String line;
                while ((line = br2.readLine()) != null) {
                    String[] larr = line.split("_");
                    grade[s][0] = larr[0]; // 学号
                    grade[s][1] = larr[1]; // 姓名
                    grade[s][2] = larr[2]; // 总分
                    s++;
                }
            }
            // 修改数据
            for (int i = 0; i < s; i++) {
                for (int j = 0; j < size; j++) {
                    if (name[j].equals(grade[i][1])) {
                        int score = Integer.parseInt(grade[i][2]) + 3; // 成绩 + 3
                        grade[i][2] = score + "*"; // 在总分后面加一个 "*"`
                    }
                }
            }
            try (FileWriter fw = new FileWriter(".\\JavaStudy\\FileOI\\grade.txt")) {
                for (int i = 0; i < s; i++) {
                    fw.write(grade[i][0] + "_" + grade[i][1] + "_" + grade[i][2] + "\n");
                }
            }
        } catch (Exception e) {
        }
    }

    public void encrypt() {
        try (BufferedReader br = new BufferedReader(new FileReader(".\\JavaStudy\\FileOI\\grade.txt"))) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(".\\JavaStudy\\FileOI\\encrypted.txt"))) {
                {
                    String line;
                    while ((line = br.readLine()) != null) {
                        StringBuilder sb = new StringBuilder(line);
                        for (int i = 0; i < sb.length(); i++) {
                            sb.setCharAt(i, (char) (sb.charAt(i) - 3));
                        }
                        bw.write(sb.toString());
                        bw.newLine();
                    }
                }
            } catch (Exception e) {
            }
        } catch (Exception e) {
        }
    }
}