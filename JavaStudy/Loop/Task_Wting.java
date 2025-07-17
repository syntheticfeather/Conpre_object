package JavaStudy.Loop;
//import java.lang.reflect.Array;

public class Task_Wting{
    
    //任务一：打印偶数
    public static void getSum(){
        for(int i=0;i<100;i=i+2){
            System.out.println(i);
        }
    }
    //任务二：水仙花数
    public static void findNarcissus(){
        for(int i=100;i<1000;i++){
            if(i==Math.pow(i/100,3)+Math.pow((i/10)%10,3)+Math.pow(i%10, 3)){
                System.out.println(i);
            }
        }
    }
    //任务三：索菲亚数
    public static boolean isSofiya(int[] a){
        if(a.length==0) return true;
        if(a[0]%2==0){
            for(int i=0;i<a.length;i=i+2){
                if(a[i]%2==1){
                    return false;
                }
            }    
            for(int j=1;j<a.length;j=j+2){
                if(a[j]%2==0){
                    return false;
                }
            }
            return true;
        }else{
            for(int i=0;i<a.length;i=i+2){
                if(a[i]%2==0){
                    return false;
                }
            } 
            for(int j=1;j<a.length;j=j+2){
                if(a[j]%2==1){
                    return false;
                }
            }
            return true;
        }
    }
    //任务四：全排列
    public static void allRange(){
        for(int a=1;a<5;a++){
            for(int b=1;b<5;b++){
                if(b==a){
                    continue;
                }
                for(int c=1;c<5;c++){
                    if(c==a||c==b){
                        continue;
                    }
                    for(int d=1;d<5;d++){
                        if(d==a||d==b||d==c){
                            continue;
                        }
                        System.out.println(""+a+""+b+""+c+""+d);
                    }
                }
            }
        }

    }
}


