package JavaStudy.If;
import java.util.Scanner;

public class Task_Zff {
    public static int task1(int x){
        if(x<0){
            return -1;
        }else if(x>=0&&x<10){
            return x*x;
        }else if(x>=10&&x<20){
            return x-10;
        }else{
            return x/10;
        }
    }

    //计算电费
    public static double getElectricBill(double n){
        if(n<15){
            return n*2.8;
        }else if(Math.abs(n-15)<0.0000001){
            return 15*2.8;
        }else if(n>15&&n<25){
            return 2.8*15+(n-15)*3.5;
        }else if(Math.abs(n-25)<0.0000001){
            return 2.8*15+10*3.5;
        }else{
            return 2.8*15+10*3.5+(n-25)*4.6;
        }
    }

    //求分数对应的等级
    public static String getGrade(double score){
        int a=(int)score/10;
        String grade=switch(a){
            case 9,10 -> "A";
            case 8 -> "B";
            case 7 -> "C";
            case 6 -> "D";
            case 5,4,3,2,1,0 -> "E";
            default -> "-1";
        };
        return grade;
    }

    //判断年月日合法性
    public static boolean judgeDate(int year,int month,int day){
        if(year<=0){
            return false;
        }
        if(month<1||month>12){
            return  false;
        }
        int maxDay=switch(month){
            case 1,3,5,7,8,10,12 -> 31;
            case 4,6,9,11 -> 30;
            case 2 ->{
                if((year%4==0&&year%100!=0)||year%400==0){
                    yield 29;
                }else{
                    yield 28;
                }
            }
            default -> -1;
        };
        if(day<1||day>maxDay){
            return false;
        }
        return true;
    }

    //人工智能回复
    public static String aiReply(String s){
        StringBuilder amountStr=new StringBuilder();
        for(char c:s.toCharArray()){
            if(Character.isDigit(c)||c=='.'){
                amountStr.append(c);
            }
        }
        double amount=0;
        if(amountStr.length()>0){
            amount=Double.parseDouble(amountStr.toString());
        }
        if(s.contains("退货")&&amount>500){
            return "请联系VIP客服专线400-xxxx";
        }
        if(s.contains("你好")||(s.contains("hello"))){
            return "您好，请问有什么可以帮助您？";
        }
        if(s.contains("价格")&&s.contains("优惠")){
            return "当前周年庆全场8折";
        }
        if(s.contains("?")&&s.length()>10){
            return "您的问题已记录，稍后回复";
        }
        return "请详细描述您的问题";
    }

    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        System.out.println("inout score:");
        double score=sc.nextDouble();
        System.out.println("The grade is:"+getGrade(score));
        sc.nextLine();
        System.out.println("input year-month-day:");
        String date=sc.nextLine();
        String[] parts=date.split("-");
        int year=Integer.parseInt(parts[0]);
        int month=Integer.parseInt(parts[1]);
        int day=Integer.parseInt(parts[2]);
        System.out.println("is valid?"+judgeDate(year, month, day));
        System.out.println("人工智能回复");
        String s=sc.nextLine();
        System.out.println(aiReply(s));
        sc.close();
    }
}
