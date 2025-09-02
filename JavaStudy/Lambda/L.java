package JavaStudy.Lambda;

import java.util.Arrays;

public class L {

    public static void main(String[] args) {
        String[] strs = {"a", "Apple", "banana", "Beat"};
        Arrays.sort(strs, (String s1, String s2) -> {
            System.out.println("a".compareTo("b"));
            return s1.compareTo(s2);
        });
        System.out.println(Arrays.toString(strs));
    }
}
