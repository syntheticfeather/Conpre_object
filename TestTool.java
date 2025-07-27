
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
        int n = sc.nextInt();
        try (FileWriter fw = new FileWriter(".\\JavaStudy\\FileOI\\Input.txt")) {
            for (int i = 0; i < n; i++) {
                String str = sc.nextLine();
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

// * {
//     /* 外部边框和内边距重置 */
//     margin: 0;
//     padding: 0;
//     /* 元素内容区域包括边框和内边距 */
//     box-sizing: border-box;
// }
// /* 根元素变量 */
// :root {
//     --primary-color: #3498db;
//     --secondary-color: #2c3e50;
//     --accent-color: #e74c3c;
//     --light-color: #ecf0f1;
//     --dark-color: #34495e;
//     --text-color: #333;
//     --border-color: #ddd;
//     --success-color: #2ecc71;
// }
// body {
//     font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
//     /* 行高 */
//     line-height: 1.6;
//     /* 字体颜色 */
//     color: var(--text-color);
//     /* 背景颜色 */
//     background-color: #f9f9f9;
//     display: flex;
//     flex-direction: column;
//     min-height: 100vh;
// }
// .container {
//     /* 占整体的60% */
//     max-width: 80%;
//     margin: 0 auto;
//     /* 居中 */
//     display: flex;
//     justify-content: center;
//     display: block;
//     /* 保持默认布局 */
//     gap: 0;
//     /* 重置间隙 */
//     /* 加一个边框 */
//     border: 1px solid var(--border-color);
//     /* 内边框 */
//     padding: 20px;
// }
// /* 超链接 */
// a {
//     /* 去除下划线 */
//     text-decoration: none;
//     /* 设置颜色 */
//     color: var(--primary-color);
//     /* 鼠标悬停 */
//     transition: all 0.3s ease;
// }
// /* 超链接悬停颜色 */
// a:hover {
//     color: var(--accent-color);
// }
// /* 头部样式 */
// .header {
//     background-color: white;
//     /* 阴影 */
//     box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
//     /* 位置固定 */
//     position: sticky;
//     top: 0;
//     z-index: 1000;
//     padding: 15px 0;
// }
// .header .container {
//     display: flex;
//     /* 水平均匀分布 */
//     justify-content: space-between;
//     /* 垂直居中 */
//     align-items: center;
// }
// .logo {
//     display: flex;
//     align-items: center;
//     font-size: 1.5rem;
//     font-weight: bold;
//     color: var(--secondary-color);
// }
// /* logo中的图标 */
// .logo i {
//     /* 与右侧元素相距 10px */
//     margin-right: 10px;
//     color: var(--primary-color);
// }
// .navbar ul {
//     display: flex;
//     /* 分布方向 (默认为 row )*/
//     flex-direction: row;
//     /* 去除默认的列表样式 */
//     list-style: none;
// }
// .navbar ul li {
//     /* 左右间距为 10px */
//     margin: 0 10px;
// }
// /* 导航链接 */
// .navbar ul li a {
//     color: var(--dark-color);
//     font-weight: 500;
//     display: flex;
//     align-items: center;
// }
// /* 链接内的图标 */
// .navbar ul li a i {
//     margin-right: 5px;
//     font-size: 0.9rem;
// }
// /* 目前所在的网页 */
// .navbar ul li a.active {
//     color: var(--primary-color);
// }
// .search-bar {
//     display: flex;
//     /* 垂直居中 */
//     align-items: center;
// }
// .search-bar input {
//     padding: 8px 15px;
//     /* 边框 */
//     border: 1px solid var(--border-color);
//     border-radius: 4px 0 0 4px;
//     outline: none;
//     width: 200px;
// }
// .search-bar button {
//     background: var(--primary-color);
//     color: white;
//     border: none;
//     padding: 9px 15px;
//     border-radius: 0 4px 4px 0;
//     cursor: pointer;
//     transition: background 0.3s ease;
// }
// /* 搜索按钮悬停样式 */
// .search-bar button:hover {
//     background: #2980b9;
// }
// /* 现在没东西 */
// .mobile-menu-btn {
//     display: none;
//     font-size: 1.5rem;
//     cursor: pointer;
//     color: var(--dark-color);
// }
// /* 主要内容布局 */
// .main-content {
//     /* 左右内边距 */
//     padding: 20px 0;
//     flex: 1;
//     /* 确保内容区占据剩余空间 */
// }
// .container {
//     display: flex;
//     gap: 30px;
// }
// .blog-posts {
//     flex: 1;
//     /* 占据更多空间 */
//     max-width: 900px;
//     /* 限制最大宽度 */
// }
// .sidebar {
//     flex: 1;
//     /* 使用flex比例 */
//     max-width: 300px;
//     /* 限制最大宽度 */
//     display: flex;
//     flex-direction: column;
//     gap: 2%;
// }
// /* 文章样式 */
// .post {
//     background: white;
//     border-radius: 8px;
//     overflow: hidden;
//     box-shadow: 0 3px 10px rgba(0, 0, 0, 0.08);
//     margin-bottom: 30px;
//     transition: transform 0.3s ease;
// }
// .post:last-child {
//     margin-bottom: 0;
// }
// /* 文章悬停样式 */
// .post:hover {
//     transform: translateY(-5px);
// }
// /* 文章头部 */
// .post-header {
//     padding: 20px;
// }
// .post-title {
//     font-size: 1.8rem;
//     margin-bottom: 10px;
//     color: var(--secondary-color);
// }
// /* 元数据 */
// .post-meta {
//     display: flex;
//     color: #777;
//     font-size: 0.9rem;
//     margin-bottom: 15px;
// }
// .post-meta span {
//     margin-right: 15px;
//     display: flex;
//     align-items: center;
// }
// .post-meta i {
//     margin-right: 5px;
// }
// .post-thumbnail img {
//     width: 100%;
//     height: 300px;
//     object-fit: cover;
//     display: block;
// }
// .post-content {
//     padding: 20px;
//     line-height: 1.8;
// }
// .post-content p {
//     margin-bottom: 15px;
// }
// .post-footer {
//     padding: 20px;
//     display: flex;
//     justify-content: space-between;
//     align-items: center;
//     border-top: 1px solid var(--border-color);
// }
// .read-more {
//     background: var(--primary-color);
//     color: white;
//     padding: 8px 15px;
//     border-radius: 4px;
//     font-weight: 500;
//     display: inline-flex;
//     align-items: center;
// }
// .read-more:hover {
//     background: #2980b9;
//     color: white;
// }
// .read-more i {
//     margin-left: 5px;
//     font-size: 0.8rem;
// }
// .post-tags {
//     display: flex;
// }
// .post-tags span {
//     background: #f1f1f1;
//     color: #666;
//     padding: 5px 10px;
//     border-radius: 10px;
//     font-size: 0.85rem;
//     margin-left: 5px;
// }
// /* 侧边栏组件 */
// .widget {
//     background: white;
//     border-radius: 8px;
//     overflow: hidden;
//     box-shadow: 0 3px 10px rgba(0, 0, 0, 0.08);
//     margin-bottom: 30px;
// }
// .widget-title {
//     background: var(--secondary-color);
//     color: white;
//     padding: 15px 20px;
// }
// .widget-title h3 {
//     display: flex;
//     align-items: center;
//     font-size: 1.2rem;
// }
// .widget-title i {
//     margin-right: 10px;
// }
// .widget-content {
//     padding: 20px;
// }
// .avatar {
//     height: 120px;
//     margin: 0 auto 15px;
// }
// .avatar img {
//     /* 居中 */
//     margin: 0 auto;
//     margin-top: 5px;
//     display: block;
//     width: 100px;
//     height: 100px;
//     border-radius: 50%;
//     object-fit: cover;
// }
// .social-links {
//     width: 100%;
//     display: flex;
//     justify-content: center;
//     margin-top: 15px;
// }
// .social-links a {
//     display: flex;
//     align-items: center;
//     justify-content: center;
//     width: 36px;
//     height: 36px;
//     border-radius: 50%;
//     background: #f1f1f1;
//     color: var(--dark-color);
//     margin: 0 5px;
//     transition: all 0.3s ease;
// }
// .social-links a:hover {
//     background: var(--primary-color);
//     color: white;
// }
// .popular-post-thumb {
//     width: 60px;
//     height: 60px;
//     border-radius: 4px;
//     overflow: hidden;
// }
// .popular-post-thumb img {
//     width: 100%;
//     height: 100%;
//     object-fit: cover;
// }
// .popular-posts ul {
//     list-style: none;
// }
// .popular-posts li {
//     margin-bottom: 15px;
//     padding-bottom: 15px;
//     border-bottom: 1px solid var(--border-color);
// }
// .popular-posts li:last-child {
//     margin-bottom: 0;
//     padding-bottom: 0;
//     border-bottom: none;
// }
// .popular-posts li a {
//     display: flex;
//     color: var(--text-color);
// }
// .popular-posts li a:hover {
//     color: var(--primary-color);
// }
// .popular-post-content {
//     margin-left: 15px;
// }
// .popular-post-content h4 {
//     font-size: 1rem;
//     margin-bottom: 5px;
// }
// .popular-post-content span {
//     color: #777;
//     font-size: 0.85rem;
// }
// .categories ul {
//     list-style: none;
// }
// .categories li {
//     margin-bottom: 10px;
//     padding-bottom: 10px;
//     border-bottom: 1px dashed var(--border-color);
// }
// .categories li:last-child {
//     margin-bottom: 0;
//     padding-bottom: 0;
//     border-bottom: none;
// }
// .categories li a {
//     display: flex;
//     justify-content: space-between;
//     color: var(--text-color);
// }
// .categories li a:hover {
//     color: var(--primary-color);
// }
// .categories li a span {
//     background: var(--primary-color);
//     color: white;
//     font-size: 0.8rem;
//     padding: 2px 8px;
//     border-radius: 10px;
// }
// /* 页脚样式 */
// .footer {
//     background: var(--secondary-color);
//     color: white;
//     padding: 50px 0 0;
// }
// .footer-container {
//     display: flex;
//     flex-wrap: wrap;
//     justify-content: center;
//     padding: 0 20px;
// }
// .footer-content {
//     display: flex;
//     flex-wrap: wrap;
//     justify-content: space-between;
//     margin-bottom: 30px;
// }
// .footer-widget {
//     min-width: 250px;
//     margin-right: 20px;
//     margin-bottom: 30px;
// }
// .footer-widget:last-child {
//     margin-right: 0;
// }
// .footer-widget h3 {
//     font-size: 1.3rem;
//     margin-bottom: 20px;
//     position: relative;
//     padding-bottom: 10px;
// }
// .footer-widget h3::after {
//     content: '';
//     position: absolute;
//     bottom: 0;
//     left: 0;
//     width: 40px;
//     height: 2px;
//     background: var(--primary-color);
// }
// .footer-widget p {
//     margin-bottom: 20px;
//     opacity: 0.8;
// }
// .footer-widget ul {
//     list-style: none;
// }
// .footer-widget ul li {
//     margin-bottom: 10px;
// }
// .footer-widget ul li a {
//     color: #ddd;
//     display: block;
//     transition: all 0.3s ease;
// }
// .footer-widget ul li a:hover {
//     color: var(--primary-color);
//     padding-left: 5px;
// }
// .subscribe-form {
//     display: flex;
//     margin-top: 15px;
//     max-width: 300px;
// }
// .subscribe-form input {
//     flex: 1;
//     padding: 10px 15px;
//     border: none;
//     border-radius: 4px 0 0 4px;
//     outline: none;
// }
// .subscribe-form button {
//     background: var(--primary-color);
//     color: white;
//     border: none;
//     padding: 0 20px;
//     border-radius: 0 4px 4px 0;
//     cursor: pointer;
//     transition: background 0.3s ease;
// }
// .subscribe-form button:hover {
//     background: #2980b9;
// }
// .copyright {
//     text-align: center;
//     padding: 20px 0;
//     border-top: 1px solid rgba(255, 255, 255, 0.1);
//     font-size: 0.9rem;
//     opacity: 0.7;
// }
// .mobile-search-container {
//     padding: 15px 20px;
//     border-bottom: 1px solid rgba(255, 255, 255, 0.1);
//     background: rgba(0, 0, 0, 0.1);
// }
// .mobile-search-bar {
//     display: flex;
//     background: white;
//     border-radius: 30px;
//     overflow: hidden;
//     padding: 5px 10px;
//     box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
//     transition: all 0.3s ease;
// }
// .mobile-search-bar:focus-within {
//     box-shadow: 0 2px 10px rgba(52, 152, 219, 0.5);
// }
// .mobile-search-bar input {
//     flex: 1;
//     border: none;
//     padding: 8px 12px;
//     outline: none;
//     background: transparent;
//     font-size: 0.95rem;
// }
// .mobile-search-bar button {
//     background: transparent;
//     border: none;
//     color: var(--primary-color);
//     cursor: pointer;
//     padding: 0 8px;
//     font-size: 1.1rem;
//     transition: transform 0.3s ease;
// }
// .mobile-search-bar button:hover {
//     transform: scale(1.1);
// }
// /* 添加的移动菜单样式 */
// .mobile-menu-container {
//     position: fixed;
//     top: 0;
//     right: -300px;
//     width: 280px;
//     height: 100%;
//     background: var(--secondary-color);
//     z-index: 2000;
//     transition: all 0.4s ease;
//     box-shadow: -5px 0 15px rgba(0, 0, 0, 0.2);
//     overflow-y: auto;
// }
// .mobile-menu-container.active {
//     right: 0;
// }
// .mobile-menu-header {
//     display: flex;
//     justify-content: space-between;
//     align-items: center;
//     padding: 20px;
//     border-bottom: 1px solid rgba(255, 255, 255, 0.1);
// }
// .mobile-menu-title {
//     color: white;
//     font-size: 1.2rem;
//     font-weight: bold;
// }
// .mobile-close-btn {
//     background: none;
//     border: none;
//     color: white;
//     font-size: 1.5rem;
//     cursor: pointer;
//     transition: transform 0.3s ease;
// }
// .mobile-close-btn:hover {
//     transform: rotate(90deg);
// }
// .mobile-navbar {
//     padding: 20px;
// }
// .mobile-navbar ul {
//     list-style: none;
// }
// .mobile-navbar ul li {
//     margin-bottom: 15px;
// }
// .mobile-navbar ul li a {
//     display: flex;
//     align-items: center;
//     color: white;
//     padding: 10px;
//     border-radius: 4px;
//     transition: all 0.3s ease;
// }
// .mobile-navbar ul li a:hover,
// .mobile-navbar ul li a.active {
//     background: var(--primary-color);
//     color: white;
//     padding-left: 15px;
// }
// .mobile-navbar ul li a i {
//     margin-right: 10px;
//     width: 20px;
//     text-align: center;
// }
// .overlay {
//     position: fixed;
//     top: 0;
//     left: 0;
//     width: 100%;
//     height: 100%;
//     background: rgba(0, 0, 0, 0.5);
//     z-index: 1500;
//     display: none;
// }
// .overlay.active {
//     display: block;
// }
// /* 响应式调整 */
// @media(max - width: 992px) {
//     .container-inner {
//         flex-direction: column;
//     }
//     .sidebar {
//         max-width: 100%;
//         width: 100%;
//     }
// }
// @media(max - width: 768px) {
//     .navbar,
//     .search-bar {
//         display: none;
//     }
//     .mobile-menu-btn {
//         display: block;
//         position: relative;
//         z-index: 2100;
//     }
//     .container {
//         max-width: 95%;
//     }
//     .footer-widget {
//         flex: 0 0 100%;
//         margin-right: 0;
//     }
//     .header .container-outer {
//         flex-wrap: wrap;
//     }
//     .search-bar {
//         order: 3;
//         width: 100%;
//         margin-top: 15px;
//     }
// }
