package JavaStudy.FileOI;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Task_Wting {

    public static void main(String[] args) {
        Task_Wting task = new Task_Wting();
        //task.input();
        //task.changeGrade();
        task.encrypt();
    }
    final int MAX_SIZE = 2048; 
    //在文件Input_W.txt中输入(格式为:学号_姓名_投诉时间_投诉内容）投诉信息
    public void input() {
        // TODO
        Scanner scanner=new Scanner(System.in);
        System.out.println("请输入投诉信息（格式示例）：");
        System.out.println("2021001 Alice 2023 12 01 Professor_late_to_class");
        System.out.println("请输入总行数 n:");

        try (BufferedWriter writer=new BufferedWriter(new FileWriter("D:\\testpan\\GitHub\\Conpre_object\\JavaStudy\\FileOI\\Input_W.txt"))){
            int n=scanner.nextInt();
            scanner.nextLine(); // 消耗换行符
            for (int i=0;i<n;i++) {
                String line=scanner.nextLine();
                String[] parts=line.split(" ");
                String studentId=parts[0];
                String name=parts[1];
                String year=parts[2];
                String month=parts[3];
                String day=parts[4];
                String complaint=String.join(" ",Arrays.copyOfRange(parts,5,parts.length));
                String formattedLine = String.format("%s_%s_%s_%s_%s_%s", 
                        studentId, name, year, month, day, complaint);
                writer.write(formattedLine);
                writer.newLine();//格式化写入
            }
        }catch(IOException e) {
            e.printStackTrace();//
        }
        scanner.close();
        // MEDIUM
    }

    //根据Input_W.txt中投诉信息，在grade.txt中的对应同学的成绩+3并在总分后加“*”
    public void changeGrade() {
        // TODO
        //读取投诉学生学号
        Map<String, Integer> complaintCounts=new HashMap<>();
        try (BufferedReader br=new BufferedReader(new FileReader("D:\\testpan\\GitHub\\Conpre_object\\JavaStudy\\FileOI\\Input_W.txt"))){
            String line;
            while ((line=br.readLine())!= null){
                String studentId=line.split("_")[0];
                complaintCounts.put(studentId,complaintCounts.getOrDefault(studentId,0)+1);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        List<String> gradeLines=new ArrayList<>();
        try (BufferedReader br=new BufferedReader(new FileReader("D:\\testpan\\GitHub\\Conpre_object\\JavaStudy\\FileOI\\grade_W.txt"))){
            String line;
            while ((line=br.readLine())!=null){
                gradeLines.add(line);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        List<String> updatedLines=new ArrayList<>();
        for (String line:gradeLines){
            String[] parts=line.split("_");
            String studentId=parts[0];
            String name=parts[1];
            int originalScore=Integer.parseInt(parts[2]);
            if (complaintCounts.containsKey(studentId)){
                int count=complaintCounts.get(studentId);
                int newScore=originalScore+3*count;
                String stars="*".repeat(count);
                String updatedLine=String.format("%s_%s_%d%s",studentId,name,newScore,stars);
                updatedLines.add(updatedLine);
            }else{
                updatedLines.add(line);
            }
        }
        try (FileWriter fw=new FileWriter("D:\\testpan\\GitHub\\Conpre_object\\JavaStudy\\FileOI\\grade_W.txt")) {
            for (String line:updatedLines){
                fw.write(line+"\n");
            }
        } catch(IOException e){
            e.printStackTrace();
        }
        // MEDIUM
    }

    //输出对ASCII码加3后的字符
    public void encrypt() {
        // TODO
        try (BufferedReader reader=new BufferedReader(new FileReader("D:\\testpan\\GitHub\\Conpre_object\\JavaStudy\\FileOI\\grade_W.txt"));
             BufferedWriter writer=new BufferedWriter(new FileWriter("D:\\testpan\\GitHub\\Conpre_object\\JavaStudy\\FileOI\\encrypt_W.txt"))){
            String line;
            while((line=reader.readLine())!=null){
                StringBuilder encrypted=new StringBuilder();
                for(char c:line.toCharArray()){
                    encrypted.append((char)(c-3));
                }
                writer.write(encrypted.toString());
                writer.newLine();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        // HARD
    }

}

//try-catch-finally结构 用于

// 测试输出案例
// 2
// 2021001 Alice 2023 12 01 Professor_late_to_class
// 2021005 Eva 2023 12 02 No_TA_session

// Input.txt 预期结果
// 2021001_Alice_2023_12_01_Professor_late_to_class
// 2021005_Eva_2023_12_02_No_TA_session

// grade.txt 原数据
// 2021001_Alice_85
// 2021002_Bob_78
// 2021003_Charlie_92
// 2021004_David_88
// 2021005_Eva_76
// 2021006_Frank_91
// 2021007_Grace_80
// 2021008_Henry_87
// 2021009_Ivy_73
// 2021010_Jack_89

// grade.txt 预期结果
// 2021001_Alice_88*
// 2021002_Bob_78
// 2021003_Charlie_92
// 2021004_David_88
// 2021005_Eva_79*
// 2021006_Frank_91
// 2021007_Grace_80
// 2021008_Henry_87
// 2021009_Ivy_73
// 2021010_Jack_89

// encrypted.txt 预期结果
// /-/.--.\>if`b\55'
// /-/.--/\?l_\45
// /-/.--0\@e^oifb\6/
// /-/.--1\A^sfa\55
// /-/.--2\Bs^\46'
// /-/.--3\Co^kh\6.
// /-/.--4\Do^`b\5-
// /-/.--5\Ebkov\54
// /-/.--6\Fsv\40
// /-/.-.-\G^`h\56
