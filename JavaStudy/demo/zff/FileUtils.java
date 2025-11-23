package zff;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class FileUtils {
    // 用户数据文件路径
    private static final String USER_FILE = "users.txt";
    // 学生数据文件路径
    private static final String STUDENT_FILE = "students.txt";

    // 加载用户数据
    public static ArrayList<User> loadUsers() {
        ArrayList<User> users = new ArrayList<>();
        File file = new File(USER_FILE);
        if (!file.exists()) {
            return users;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    users.add(new User(parts[0], parts[1], parts[2], parts[3]));
                }
            }
        } catch (IOException e) {
            System.out.println("用户数据加载失败：" + e.getMessage());
        }
        return users;
    }

    // 保存用户数据
    public static void saveUsers(ArrayList<User> users) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USER_FILE))) {
            for (User user : users) {
                String line = user.getUserName() + "," +
                        user.getPassword() + "," +
                        user.getPersonId() + "," +
                        user.getPhoneNumber();
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("用户数据保存失败：" + e.getMessage());
        }
    }

    // 加载学生数据
    public static ArrayList<Student> loadStudents() {
        ArrayList<Student> students = new ArrayList<>();
        File file = new File(STUDENT_FILE);
        if (!file.exists()) {
            return students;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    students.add(new Student(parts[0], parts[1], Integer.parseInt(parts[2]), parts[3]));
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("学生数据加载失败：" + e.getMessage());
        }
        return students;
    }

    // 保存学生数据
    public static void saveStudents(ArrayList<Student> students) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(STUDENT_FILE))) {
            for (Student student : students) {
                String line = student.getId() + "," +
                        student.getName() + "," +
                        student.getAge() + "," +
                        student.getAddress();
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("学生数据保存失败：" + e.getMessage());
        }
    }
}