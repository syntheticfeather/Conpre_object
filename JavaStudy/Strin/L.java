package JavaStudy.Strin;

import java.util.StringJoiner;

public class L {
    public void test() {
        {
            String str = "Hello World";
            System.out.println(str);

            System.out.println(str.contains("H"));// 是否包含
            System.out.println(str.contains("Wor"));// 是否包含

            System.out.println(str.startsWith("H"));// 开头
            System.out.println(str.endsWith("t"));// 结尾

            System.out.println(str.indexOf("l", 4)); // 从哪开始找

            System.out.println(str.length());

            str = str.trim(); // 去掉两端的空格

            str = str.replace("ll", "tt"); // 替换
            str = str.replace('W', 'X'); // 替换

            System.out.println(str);

            System.out.println(str.isEmpty());// 是否为空字符串

            System.out.println(str.isBlank());// 是否只有空白字符

            str = " Hello World , hello Java ";

            String[] ss = str.split(",");

            for (String s : ss)
                System.out.println(s.trim());

            System.out.println("XXXXXXXXXXXXXXXXX");
            // 字符串与其他类型的相互转换
            System.out.println(String.valueOf(125));
            System.out.println(String.valueOf(125) instanceof String);

            str = "1";
            double d = Double.parseDouble(str);
            Double dd = Double.valueOf(str);
            System.out.println(d);
            System.out.println(dd instanceof Double);
        }
        {
            var sb = new StringBuilder(1024);
            sb.append("Mr ")
                    .append("Bob")
                    .append("!")
                    .insert(0, "Hello, ")
                    .insert(0, 'e');
            sb.replace(0, 5, "H");// 段替换
            System.out.println(sb.toString());
            sb.reverse();
            System.out.println(sb.toString());
            sb.delete(0, 2);
            System.out.println(sb.toString());
            sb.setLength(0);
        }
        {
            String[] strs = { "Hello", "World", "Java" };
            StringJoiner sj = new StringJoiner(", ");
            sj.add("First");
            for (String str : strs) {
                sj.add(str);
            }
            sj.add("!");
            System.out.println(sj.toString());
            System.out.println(sj.toString());

            // 示例1：合并两个简单 StringJoiner
            StringJoiner sj1 = new StringJoiner(", ", "[", "]");
            sj1.add("A").add("B");

            StringJoiner sj2 = new StringJoiner("-", "{", "}");
            sj2.add("C").add("D");
            
            sj1.merge(sj2); // 合并 sj2 的内容到 sj1
            System.out.println(sj1.toString()); // 输出: [A, B, C, D]

            sj2.merge(sj1); // 合并 sj1 的内容到 sj2
            System.out.println(sj2.toString()); // 输出: {A, B, C, D}
        }
    }

    public static void main(String[] args) {
        new L().test();

    }

}
