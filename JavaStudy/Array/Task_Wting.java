package JavaStudy.Array;

public class Task_Wting {
    //任务一：等差数列
    public static boolean isArithmetic(int[] arr){
        int q=arr[1]-arr[0];
        for(int i=0;i<arr.length-1;i++){
            if(q!=arr[i+1]-arr[i]){
                return false;
            }
        }
        return true;
    }
    //任务二：斐波那契数列
    public static int getFibonacci(int n){
        int[] arr=new int[n+1];
        arr[1]=1;
        arr[2]=1;
        for(int i=3;i<=n;i++){
            arr[i]=arr[i-1]+arr[i-2];
        }
        return arr[n];
    }
    //任务三：
    public static int getDays(int[] arr,int m){
        int n=0,j=0;
        for(int i=0;i<arr.length;i++){      
            n++;
            j++;
            if(j==3){
                return -1;
            }else{
                j=0;
            }
        }
        return n;
    }
    //任务四：<循环位移>
    public static boolean isLoopDisplacement(int[] arr){
        int m=0,n=0;
        if(arr.length==0){
            return true;
        }
        for(int i=0;i<arr.length-1;i++){
            if(arr[i]<arr[i+1]){
                m++;
            }else if(arr[i]>arr[i+1]){
                n++;
            }else{
                return false;
            }
        }
        if(m<=0||n<=0){
            return true;
        }else{
            return false;
        }
    }
    //任务五：矩阵
    private  static int[][] givenMatrix;
    private static int m;
    private static int n;
    public static void set(int m,int n){
        givenMatrix=new int[n][m];
    }
    //加
    public static int[][] add(int[][] arr){
        if(arr==null||arr.length!=n||arr[0].length!=m){
            throw new IllegalArgumentException("矩阵不匹配");
        }
        int[][] result=new int[n][m];
        for(int i=0;i<n;i++){
            for(int j=0;j<m;j++){
                result[i][j]=arr[i][j]+givenMatrix[i][j];
            }
        }
        return result;
    } 
    //减
    public static int[][] subtract(int[][] arr){
        if(arr==null||arr.length!=n||arr[0].length!=m){
            throw new IllegalArgumentException("矩阵不匹配");
        }
        int[][] result=new int[n][m];
        for(int i=0;i<n;i++){
            for(int j=0;j<m;j++){
                result[i][j]=arr[i][j]-givenMatrix[i][j];
            }
        }
        return result;
    } 
    //乘
    public static int[][] multiply(int[][] arr){
        if(arr==null||arr.length!=m||arr[0].length!=n){
            throw new IllegalArgumentException("矩阵不匹配");
        }
        int[][] result=new int[n][m];
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                for(int k=0;k<n;k++){
                    result[i][j]=arr[i][k]*givenMatrix[k][j];
                }
            }
        }
        return result;
    }
    //转置
    public static int[][] transpose(int[][] arr0){
        if(arr0==null) return arr0;
        int[][] arr=new int[n][m];
        for(int i=0;i<n;i++){
            for(int j=0;j<m;j++){
                arr[i][j]=arr0[j][i];
            }
        }
        return arr;
    }
    //逆运算
    public static double[][] inverse(double[][] matrix){
        if(matrix==null) throw new IllegalArgumentException("矩阵不能为空");
        int n=matrix.length;
        for(int i=0;i<n;i++){
            if(n!=matrix[i].length){
                throw new IllegalArgumentException("矩阵必须为方阵");
            }
        }
        double det=determinant(matrix);
        if(Math.abs(det)<1e-10) {
            throw new IllegalArgumentException("矩阵的行列式为零，无法求逆");
        }
        double[][] adjugate=new double[n][n];
        for(int i=0;i<n;i++){
            for(int j=0;j<n;j++){
                double minor=determinant(minor(matrix,i,j));
                double cofactor=(i+j)%2==0?minor:-minor;
                adjugate[j][i]=cofactor;
            }
        }
        double[][] inverse=new double[n][n];
        for(int i=0;i<n;i++){
            for(int j=0;j<n;j++){
                inverse[i][j]=adjugate[i][j]/det;
            }
        }
        return inverse;
    }
    //矩阵行列式
    private static double determinant(double[][] matrix){
        int n=matrix.length;
        if(n==1){
            return matrix[0][0];
        }
        if(n==2){
            return matrix[0][0]*matrix[1][1]-matrix[0][1]*matrix[1][0];
        }
        double det=0;
        for (int i=0;i<n;i++){
            double[][] submatrix=minor(matrix,0,i);
            double sign=(i%2==0)?1:-1;
            det+=sign*matrix[0][i]*determinant(submatrix);
        }
        return det;
    }
    //余子式矩阵
    private static double[][] minor(double[][] matrix,int row,int col) {
        int n=matrix.length;
        double[][] minor=new double[n-1][n-1];
        int minorRow=0;
        for(int i=0;i<n;i++) {
            if(i==row) continue;
            int minorCol=0;
            for(int j=0;j<n;j++){
                if(j==col) continue;
                minor[minorRow][minorCol]=matrix[i][j];
                minorCol++;
            }
            minorRow++;
        }
        return minor;
    } 

}

