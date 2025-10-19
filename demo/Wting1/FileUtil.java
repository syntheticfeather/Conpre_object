package Wting1;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    // 保存对象列表到文件
    public static <T> void saveList(List<T> list, String filePath) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(list);
        }
    }
    
    // 从文件读取对象列表
    @SuppressWarnings("unchecked")
    public static <T> List<T> readList(String filePath) throws IOException, ClassNotFoundException {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (List<T>) ois.readObject();
        }
    }
    
    // 保存字符串到文件
    public static void saveString(String content, String filePath) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write(content);
        }
    }
    
    // 从文件读取字符串
    public static String readString(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
    
    // 检查文件是否存在
    public static boolean fileExists(String filePath) {
        return new File(filePath).exists();
    }
    
    // 创建文件（包括父目录）
    public static boolean createFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file.createNewFile();
    }
}

