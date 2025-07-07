package JavaStudy.FileOI;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class L {

    public void test() throws IOException {
        // 写入文件
        // 绝对路径，每个人电脑上都不同
        // try( FileWriter fw = new
        // FileWriter("d:\\Study\\Conpre_object\\JavaStudy\\FileOI\\test.txt"))
        // 相对路径
        try (FileWriter fw = new FileWriter("JavaStudy\\FileOI\\FileOITest.txt", StandardCharsets.UTF_8)) {
            fw.write("2577\n");
            fw.write("Hello World\n");
            fw.write("This is a first OI test.\nTake some notes.");
            fw.write("Take some notes.");
        }
        /*
         * 设置成utf_8编码
         * try( FileWriter fw = new FileWriter("JavaStudy\\FileOI\\FileOITest.txt",
         * StandardCharsets.UTF_8))
         * 变为append追加模式
         * 那两个都要怎么办呢？
         * try (BufferedWriter bw = new BufferedWriter(
         * new OutputStreamWriter(
         * new FileOutputStream("JavaStudy/FileOI/FileOITest.txt", true), // true表示追加模式
         * StandardCharsets.UTF_8)))
         */
        // 读数字？
        // 注意读出来的全是字符，
        try (Reader reader = new InputStreamReader(new FileInputStream("JavaStudy/FileOI/FileOITest.txt"),
                StandardCharsets.UTF_8)) {
            int c;
            while ((c = reader.read()) != -1 && c != '\n') {
                // 此处的 c 读出来是char的int值，需要转成int才能输出
                // 读取字符
                System.out.println(c - '0');
                System.out.println((char) c);
            }
        }
        // 读取字符串
        // type 1
        try (Reader isr = new InputStreamReader(new FileInputStream("JavaStudy/FileOI/FileOITest.txt"),
                StandardCharsets.UTF_8)) {
            char[] buffer = new char[1024];
            int c;
            // 调试可以发现是全部读完。
            // type 1
            // 直接新建string
            /*
             * while ((c = isr.read(buffer)) != -1)
             * {
             * System.out.println(new String(buffer, 0, c));
             * }
             */
            // type 2
            // 拿到StringBuilder拼接。
            StringBuilder sb = new StringBuilder();
            while ((c = isr.read(buffer)) != -1) {
                sb.append(buffer, 0, c);
            }
            String str = sb.toString();
            System.out.println(str);
        }
        // type 2
        // bufferedReader
        // 自带缓存，可以按行读取
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream("JavaStudy/FileOI/FileOITest.txt"),
                        StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) { // 正确读取行内容
                System.out.println(line);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        L l = new L();
        l.test();
    }
}

// Com_pre
// {
// JavaStudy
// {
// FIleOI
// {
// L.java
// }
// }

// }