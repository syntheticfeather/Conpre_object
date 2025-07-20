package JavaStudy.Strin;

public class Task_qyx {

    // 任务一：将含有逗号分隔的字符串分割后重组为特定格式
    public static String formatCommaSeparatedString(String str) {
        String[] parts = str.split(",");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            result.append("[").append(part.trim()).append("]");
        }
        return result.toString();
    }

    // 任务二：反转字符串中的每个单词，并拼接结果
    public static String reverseWords(String s) {
        String[] words = s.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append(new StringBuilder(word).reverse().toString()).append(" ");
        }
        return result.toString().trim();
    }

    // 任务三：将多个路径片段用 / 拼接，避免重复分隔符
    public static String concatenatePaths(String... paths) {
        StringBuilder result = new StringBuilder();
        for (String path : paths) {
            if (path.isEmpty()) {
                continue;
            }
            if (result.length() > 0 && !result.toString().endsWith("/") && !path.startsWith("/")) {
                result.append("/");
            }
            result.append(path.replaceFirst("^/", "").replaceAll("/$", ""));
        }
        return result.toString();
    }

    // 任务四：替换模板中的占位符 {{key}} 替换为实际值
    public static String replacePlaceholders(String template, String key) {
        return template.replace("{{key}}", key);
    }

    // 任务五：按照你电的要求格式化字符串
    public static String formatString(String input) {
        StringBuilder digits = new StringBuilder();
        StringBuilder letters = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                if (digits.length() > 0) {
                    digits.append("#");
                }
                digits.append(c);
            } else if (Character.isLetter(c)) {
                if (letters.length() > 0) {
                    letters.append("@");
                }
                letters.append(c);
            }
        }

        StringBuilder result = new StringBuilder();
        result.append("<");
        if (digits.length() > 0) {
            result.append("(").append(digits).append(")");
        }
        if (digits.length() > 0 && letters.length() > 0) {
            result.append("||");
        }
        if (letters.length() > 0) {
            result.append("[").append(letters).append("]");
        }
        result.append(">");

        return result.toString();
    }
}




