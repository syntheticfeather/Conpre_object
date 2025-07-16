package JavaStudy.Loop;

import java.util.Scanner;

public class Task_Zff {
    //索菲亚数列
    public static boolean judgeSofia(String s){
        String[] parts=s.split(" ");
        if(Integer.parseInt(parts[0])%2==0){
            for(int i=2;i<parts.length;i+=2){
                if(Integer.parseInt(parts[i])%2!=0){
                    return false;
                }
            }
            for(int i=1;i<parts.length;i+=2){
                if(Integer.parseInt(parts[i])%2==0){
                    return false;
                }
            }
        }else{
            for(int i=2;i<parts.length;i+=2){
                if(Integer.parseInt(parts[i])%2==0){
                    return false;
                }
            }
            for(int i=1;i<parts.length;i+=2){
                if(Integer.parseInt(parts[i])%2!=0){
                    return false;
                }
            }
        }
        return true;
    }

    public static void main(String[] args){
        //打印1到100的偶数
        for(int i=2;i<=100;i+=2){
            System.out.print(i);
            System.out.print(" ");
        }
        System.out.println("");

        //找水仙花数
        for(int i=100;i<1000;i++){
            int a=i%100;
            int b=i/10%10;
            int c=i%10;
            if(a*a*a+b*b*b+c*c*c==i){
                System.out.println(i);
            }
        }

        System.out.println("input a sequence:");
        Scanner sc=new Scanner(System.in);
        String s=sc.nextLine();
        if(judgeSofia(s)==true){
            System.out.println("it is sofia sequence");
        }else{
            System.out.println("it is not sofia sequence");
        }

        //1 2 3 4的所有排列
        for(int i=1;i<5;i++){
            for(int j=1;j<5;j++){
                if(i==j){
                    continue;
                }else{
                    for(int k=1;k<5;k++){
                        if(k==i||k==j){
                            continue;
                        }else{
                            for(int n=1;n<5;n++){
                                if(n==i||n==j||n==k){
                                    continue;
                                }else{
                                    System.out.println(i+" "+j+" "+k+" "+n);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
