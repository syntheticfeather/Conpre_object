package JavaStudy.Strin;

public class Task_Wting {
    //任务一：
    public static void rebuild1(String str){
        String[] ss=str.split("\\,");
        str=String.join("***",ss); 
    }
    //任务二：
     public static String rebuild2(String str){
        String[] ss=str.split("\\s");
        for(int i=0;i<ss.length;i++){
            ss[i]=new StringBuilder(ss[i]).reverse().toString();
        }
        return String.join(" ",ss);
    }
    //任务三：规范分隔符
    public static String rebuild3(String str){
        String[] ss=str.split("\\s");
        for(int i=0;i<ss.length;i++){
            ss[i]=ss[i].replace("[\\/\\s]+","");
        }
        return String.join("/",ss);
    }
    //任务四：
    public static String rebuild4(String str,String[] keys,String[] values){
        if(str==null||keys==null||values==null){
            return str;
        }
        for(int i=0;i<keys.length;i++){
            String s="{{"+keys[i]+"}}";
            str=str.replace(s,values[i]);
        }
        return str;
    }
    //任务五：分隔字符
    public static String spilt(String str){
        char[] cs=str.toCharArray();
        char[] c1=new char[str.length()];
        char[] c2=new char[str.length()];
        int i=0,j=0;
        for(int k=0;k<cs.length;k++){
            if(Character.isDigit(cs[k])==true){
                c1[i]=cs[k];
                i++;
            }else if(Character.isLetter(cs[k])==true){
                c2[j]=cs[k];
                j++;
            }else{
                continue;
            }
        }
        String s1=c1.toString();
        String[] ss1=s1.split("\\s");
        s1=String.join("#", ss1);
        String s2=c2.toString();
        String[] ss2=s2.split("\\s");
        s1=String.join("@", ss2);
        str="<"+"("+s1+")"+"||"+"["+s2+"]"+">";
        return str;
    }
    public static void main(String[] args) {

    }
}