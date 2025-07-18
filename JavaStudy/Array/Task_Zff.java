package JavaStudy.Array;
import java.util.Arrays;
import java.util.Scanner;

public class Task_Zff {
    //验证等差数列
    public static boolean arithmeticSq(int[] a){
        int d=a[1]-a[0];
        for(int i=2;i<a.length;i++){
            if(a[i]!=a[0]+i*d){
                return false;
            }
        }
        return true;
    }
    //斐波那契数列
    public static int fabonacci(int n){
        if(n==1||n==2){
            return 1;
        }else{
            int a=1,b=1;
            int temp=0;
            for(int i=3;i<=n;i++){
                temp=a+b;
                a=b;
                b=temp;
            }
            return temp;
        }
    }
    //不去学校的天数
    public static int notGoToSchool(int[] a,int n,int m){
        int day=0;
        for(int i=0;i<n;i++){
            if(a[i]<=m){
                day++;
            }else if(i>1&&a[i-1]>m&&a[i-2]>m){
                break;
            }
        }
        return n-day;
    }
    //循环移位成为递增序列
    public static boolean circularMove(int[] a){
        int k=0;
        for(int i=0;i<a.length-1;i++){
            if(a[i]>=a[i+1]){
                k=i+1;
                break;
            }
        }
        if(k==0){
            return true;
        }
        for(int i=k;i<a.length-1;i++){
            if(a[i]>=a[i+1]){
                return false;
            }
        }
        if(a[a.length-1]<a[0]){
            return true;
        }else{
            return false;
        }
    }
    //矩阵加法
    public static double[][] matrixPlus(double[][] a,double[][] b){
        int row=a.length;
        int col=a[0].length;
        double[][] c=new double[row][col];
        for(int i=0;i<row;i++){
            for(int j=0;j<col;j++){
                c[i][j]=a[i][j]+b[i][j];
            }
        }
        return c;
    }
    //矩阵减法
    public static double[][] matrixMinus(double[][] a,double[][] b){
        int row=a.length;
        int col=a[0].length;
        double[][] c=new double[row][col];
        for(int i=0;i<row;i++){
            for(int j=0;j<col;j++){
                c[i][j]=a[i][j]-b[i][j];
            }
        }
        return c;
    }
    //矩阵乘法
    public static double[][] matrixMultiplicate(double[][] a,double[][] b){
        int rowA=a.length;
        int colA=a[0].length;
        int colB=b[0].length;
        double[][] c=new double[rowA][colB];
        for(int i=0;i<rowA;i++) {
            for(int j=0;j<colB;j++) {
                for(int k=0;k<colA;k++) {
                    c[i][j]+=a[i][k]*b[k][j];
                }
            }
        }
        return c;
    }
    //矩阵转置
    public static double[][] matrixTranspose(double[][] a){
        int row=a.length;
        int col=a[0].length;
        double[][] c=new double[row][col];
        for(int i=0;i<row;i++){
            for(int j=0;j<col;j++){
                c[j][i]=a[i][j];
            }
        }
        return c;
    }
    //矩阵求逆
    public static double[][] matirxInverse(double[][] a){
        int n=a.length;
        double[][] augmented=new double[n][2*n];
        for(int i=0;i<n;i++){
            System.arraycopy(a[i],0,augmented[i],0,n);
            augmented[i][i+n]=1.0;
        }
        for(int i=0;i<n;i++){
        int pivot=i;
        for(int j=i+1;j<n;j++){
            if (Math.abs(augmented[j][i])>Math.abs(augmented[pivot][i])) {
                pivot=j;
            }
        }
        double[] temp=augmented[i];
        augmented[i]=augmented[pivot];
        augmented[pivot]=temp;
        double divisor=augmented[i][i];
        for(int j=0; j<2*n; j++){
            augmented[i][j]/=divisor;
        }
        for(int j=0;j<n;j++){
            if(j!=i){
                double factor=augmented[j][i];
                for(int k=0;k<2*n;k++){
                    augmented[j][k]-=factor*augmented[i][k];
                }
            }
        }
    }
    double[][] result=new double[n][n];
    for(int i=0;i<n;i++){
        System.arraycopy(augmented[i], n, result[i], 0, n);
    }
    return result;
    }
    public static void main(String[] args) {
        int[] sq1=new int[]{3,5,7,9};
        int[] sq2=new int[]{10,7,4,2};
        System.out.println("sq1 is arithhmetic ? "+arithmeticSq(sq1));
        System.out.println("sq2 is arithhmetic ? "+arithmeticSq(sq2));
        Scanner sc=new Scanner(System.in);
        System.out.println("input term in fabonacci sequence:");
        int n=sc.nextInt();
        System.out.println("第"+n+"项是"+fabonacci(n));
        int[] rainfall=new int[]{20,35,46,39,0};
        int a=rainfall.length;
        int max=40;
        System.out.println("The day not go to school:"+notGoToSchool(rainfall, a, max));
        int[] a1=new int[]{4,7,6,1,2};
        int[] a2=new int[]{3,4,5,1,2};
        if(circularMove(a1)==true){
            System.out.println("a1 can be moved to be an increasing sequence");
        }else{
            System.out.println("a1 can not be moved to be an increasing sequence");
        }
        if(circularMove(a2)==true){
            System.out.println("a2 can be moved to be an increasing sequence");
        }else{
            System.out.println("a2 can not be moved to be an increasing sequence");
        }
        double[][] matrix1={
            {1,1,1},
            {1,2,3},
            {1,4,9}
        };
        double[][] matrix2={
            {1,2,3},
            {1,1,1},
            {2,3,2}
        };
        System.out.println("After plus:");
        System.out.println(Arrays.deepToString(matrixPlus(matrix1,matrix2)));
        System.out.println("After minus:");
        System.out.println(Arrays.deepToString(matrixMinus(matrix1, matrix2)));
        System.out.println("After multiplication:");
        System.out.println(Arrays.deepToString(matrixMultiplicate(matrix1, matrix2)));
        System.out.println("After inverse:");
        System.out.println(Arrays.deepToString(matirxInverse(matrix1)));
        System.out.println("After transpose:");
        System.out.println(Arrays.deepToString(matrixTranspose(matrix1)));
        sc.close();
    }
}
