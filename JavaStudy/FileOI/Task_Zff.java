package JavaStudy.FileOI;
import java.io.*;
import java.util.*;

public class Task_Zff {
    public static void main(String[] args) throws IOException {
        Task_Zff test=new Task_Zff();
        test.input();
        test.changeGrade();
        test.encrypt();

    }

    final int MAX_SIZE = 2048; 

    public void input() throws IOException{
        try(BufferedWriter writer=new BufferedWriter(new FileWriter("Input.txt",true))){
            Scanner sc=new Scanner(System.in);
            int n=sc.nextInt();
            sc.nextLine();
            for(int i=0;i<n;i++){
                String s=sc.nextLine();
                String[] ss=s.split(" ");
                StringJoiner sj=new StringJoiner("_");
                for(String a:ss){
                    sj.add(a);
                }
                writer.write(sj.toString());
                writer.newLine();
            }
            sc.close();
        }
    }

    public void changeGrade() throws IOException{
        Set<String> complainIds = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader("Input.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("_");
                complainIds.add(parts[0]);
            }
        } 
        try (BufferedReader br = new BufferedReader(new FileReader("D:\\Conpre_Object\\JavaStudy\\FileOI\\grade.txt"));
             BufferedWriter bw = new BufferedWriter(new FileWriter("new_grade.txt"))) {
            
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("_");
                String id = parts[0];
                String name = parts[1];
                int score = Integer.parseInt(parts[2]);
                if (complainIds.contains(id)) {
                    bw.write(id + "_" + name + "_" + (score + 3) + "*");
                } else {
                    bw.write(line); 
                }
                bw.newLine();
            }
        }
        new File("grade.txt").delete();
        new File("new_grade.txt").renameTo(new File("grade.txt"));
    }
    public void encrypt() throws IOException{
        try (BufferedReader br = new BufferedReader(new FileReader("grade.txt"));
             BufferedWriter bw = new BufferedWriter(new FileWriter("encrypted.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                StringBuilder encryptedLine = new StringBuilder();
                for (char c : line.toCharArray()) {
                    int shifted = c + 3;
                    encryptedLine.append((char) shifted);
                } 
            bw.write(encryptedLine.toString());
            bw.newLine(); 
            }
        } 
    }
}