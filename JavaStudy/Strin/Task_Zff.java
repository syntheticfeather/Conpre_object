package JavaStudy.Strin;
import java.util.Scanner;
import java.util.StringJoiner;

public class Task_Zff {
    //分隔字符串后重组
    public static String task1(String s){
        String[] ss=s.split("//,");
        var sj=new StringJoiner("][","[","]");
        for(String s1:ss){
            sj.add(s1);
        }
        return sj.toString();
    }
    //反转每个单词并拼接
    public static String task2(String s){
        String[] ss=s.split(" ");
        var sb=new StringBuilder();
        for(int i=0;i<ss.length;i++){
            var s1=new StringBuilder();
            s1.append(ss[i]);
            s1.reverse();
            sb.append(s1.toString());
            sb.append(" ");
        }
        sb.delete(sb.length()-1, sb.length());
        return sb.toString();
    }
    //拼接路径
    public static String connectPath(String[] path){
        if(path[0].endsWith("/")){
            path[0]=path[0].replaceAll("/+$", "");
        }
        if(path[path.length-1].startsWith("/")){
            path[path.length-1]=path[path.length-1].replaceAll("^/+", "");
        }
        for(int i=1;i<path.length-1;i++){
            path[i]=path[i].replaceAll("^/+|/+$", "");
        }
        var sj=new StringJoiner("/");
        for(String s:path){
            sj.add(s);
        }
        return sj.toString();
    }
    //替换占位符
    public static String replace(String s,String[] a){
        String s1=a[0];
        return s.replace("{{key}}",s1);
    }
    //分隔数字和字母
    public static String splitNumAndLetter(String s){
        var num=new StringJoiner("#","(",")");
        var letter=new StringJoiner("@","[","]");
        char[] s1=s.toCharArray();
        for(char c:s1){
            if(c>='a'&&c<='z'||c>='A'&&c<='Z'){
                letter.add(Character.toString(c));
            }else if(c>='0'&&c<='9'){
                num.add(Character.toString(c));
            }
        }
        var sj=new StringJoiner("||","<",">");
        sj.add(num.toString());
        sj.add(letter.toString());
        return sj.toString();
    }
    
    public static void main(String[] args){
        System.out.println("输入用逗号分隔的字符串：");
        Scanner sc=new Scanner(System.in);
        String s1=sc.nextLine();
        String result1=task1(s1);
        System.out.println(result1);
        
        System.out.println("输入一句英文：");
        String s2=sc.nextLine();
        String result2=task2(s2);
        System.out.println(result2);
        sc.close();
        
        String[] path=new String[]{"/usr", "bin", "/java"};
        System.out.println(connectPath(path));

        String s3="Hello, {{key}}!";
        String[] key = {"Alice"};
        System.out.println(replace(s3, key));

        String s4="123abc?";
        System.out.println(splitNumAndLetter(s4));
    }
}
