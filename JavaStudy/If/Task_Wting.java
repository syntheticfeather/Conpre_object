package JavaStudy.If;

public class Task_Wting {
    //任务一：x
    public static double calculateX(double x) {
        if(x<0){
            return -1;
        }else if(0<=x&&x<10){
            return Math.pow(x, 2);
        }else if(10<=x&&x<20){
            return x-10;
        }else{
            return x/10;
        }   
    }
    //任务二:电费
    public static double calculateEnergy(double n) {
        if(n<0){
            return -1;
        }else if(n<15){
            return n*2.8;
        }else if(15<n&&n<=25){
            return 15*2.8+(n-15)*3.5;
        }else{
            return 15*2.8+10*3.5+(n-25)*4.6;
        }
        
    }
    //任务三：换算等级
    public static String calculateGrade(int n) {
        if(n<0||n>100){
            return "-1";
        }else if(0<=n&&n<59){
            return "E";
        }else if(60<=n&&n<69){
            return "D";
        }else if(70<=n&&n<79){
            return "C";
        }else if(80<=n&&n<89){
            return "B";
        }else{
            return "A";
        }
    }
    //任务四：合法性判断
    public static boolean judge(int year,int month,int day ){
        if(year>0){
            if(month==1||month==3||month==5||month==7||month==8||month==10||month==12){
                if(day==31){
                    return true;
                }else{
                    return false;
                }
            }else if(month==4||month==6||month==9||month==11){
                if(day==30){
                    return true;
                }else{
                    return false;
                }
            }else if(month==2){
                if((year%4==0&&year%100!=0)||(year%400==0)){
                    if(day==28){
                        return true;
                    }else{
                        return false;
                    }
                }else{
                    if(day==29){
                        return true;
                    }else{
                        return false;
                    }
                }
            }else{
                return false;
            }
        }
        else{
            return false;
        }
        
    }
    //任务五：
/*
题目描述：
    小明要当智能人工，想让人工智能帮忙
    根据用户输入匹配预设回复规则：
    1. 输入"你好"/"hello" → "您好，请问有什么可以帮助您？"
    2. 输入"价格"且包含"优惠" → "当前周年庆全场8折"
    3. 输入"退货"且金额＞500 → "请联系VIP客服专线400-xxxx"
    4. 输入"?"且长度＞10 → "您的问题已记录，稍后回复"
    5. 其他情况 → "请详细描述您的问题"
    要求实现复合条件判断
*/
   public static String answer(String input,double amount) {
       if("你好".equals(input) || "hello".equals(input)){
            return "您好，请问有什么可以帮助您？";
       }
       if(input.contains("价格")&&input.contains("优惠")){
            return "当前周年庆全场8折";
       }
       if(input.contains("退货")&&amount>500){
            return "请联系VIP客服专线400-xxxx";
       }
       if(input.contains("退货")&&input.length()>10){
            return "您的问题已记录，稍后回复";
       }
        return "请详细描述您的问题。";
   }
    
}
