package JavaStudy.Array;

import java.util.ArrayList;
import java.util.List;

public class Task_qyx {

    // 1.验证给定数列是否为等差数列
    public static boolean isArithmeticSequence(int[] arr) {
        if (arr.length < 2) {
            return true;
        }
        int difference = arr[1] - arr[0];
        for (int i = 2; i < arr.length; i++) {
            if (arr[i] - arr[i - 1] != difference) {
                return false;
            }
        }
        return true;
    }

    //2.计算斐波那契数列的第n项
    public static List<Integer> fibonacci(int n) {
        List<Integer> fibSequence = new ArrayList<>();
        if (n <= 0) {
            return fibSequence;
        }
        fibSequence.add(1);
        if (n == 1) {
            return fibSequence;
        }
        fibSequence.add(1);
        for (int i = 2; i < n; i++) {
            int nextFib = fibSequence.get(i - 1) + fibSequence.get(i - 2);
            fibSequence.add(nextFib);
        }
        return fibSequence;
    }

    //3.计算电击小子不去学校的天数
    public static int daysNotGoingToSchool(int[] rainfall, int m) {
        int count = 0;
        int consecutiveDays = 0;
        for (int rain : rainfall) {
            if (rain > m) {
                consecutiveDays++;
                if (consecutiveDays >= 3) {
                    count += 1;
                }
            } else {
                consecutiveDays = 0;
            }
        }
        return count;
    }

    // 4.验证数组是否可以通过循环移位成为严格递增序列
    public static boolean canBeStrictlyIncreasingByRotation(int[] arr) {
        int n = arr.length;
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (arr[i] >= arr[(i + 1) % n]) {
                count++;
            }
            if (count > 1) {
                return false;
            }
        }
        return true;
    }

    // 5.矩阵运算（加、减、乘、转置、逆）
    public static class MatrixOperations {
        // 矩阵加法
        public static double[][] addMatrices(double[][] A, double[][] B) {
            int rows = A.length;
            int cols = A[0].length;
            double[][] result = new double[rows][cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    result[i][j] = A[i][j] + B[i][j];
                }
            }
            return result;
        }

        // 矩阵减法
        public static double[][] subtractMatrices(double[][] A, double[][] B) {
            int rows = A.length;
            int cols = A[0].length;
            double[][] result = new double[rows][cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    result[i][j] = A[i][j] - B[i][j];
                }
            }
            return result;
        }

        // 矩阵乘法
        public static double[][] multiplyMatrices(double[][] A, double[][] B) {
            int rowsA = A.length;
            int colsA = A[0].length;
            int colsB = B[0].length;
            double[][] result = new double[rowsA][colsB];
            for (int i = 0; i < rowsA; i++) {
                for (int j = 0; j < colsB; j++) {
                    for (int k = 0; k < colsA; k++) {
                        result[i][j] += A[i][k] * B[k][j];
                    }
                }
            }
            return result;
        }

        // 矩阵转置
        public static double[][] transposeMatrix(double[][] A) {
            int rows = A.length;
            int cols = A[0].length;
            double[][] result = new double[cols][rows];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    result[j][i] = A[i][j];
                }
            }
            return result;
        }

        // 矩阵求逆
        public static double[][] invertMatrix(double[][] A) {
            int n = A.length;
            double[][] inverse = new double[n][n];
            double[][] augmented = augmentMatrix(A);

            for (int col = 0; col < n; col++) {
                pivot(augmented, col);
                eliminateForward(augmented, col);
                eliminateBackward(augmented, col);
            }

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    inverse[i][j] = augmented[i][n + j];
                }
            }

            return inverse;
        }

        private static double[][] augmentMatrix(double[][] A) {
            int n = A.length;
            double[][] augmented = new double[n][2 * n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    augmented[i][j] = A[i][j];
                }
                augmented[i][n + i] = 1;
            }
            return augmented;
        }

        private static void pivot(double[][] A, int col) {
            int n = A.length;
            int maxRow = col;
            for (int i = col + 1; i < n; i++) {
                if (Math.abs(A[i][col]) > Math.abs(A[maxRow][col])) {
                    maxRow = i;
                }
            }
            swapRows(A, col, maxRow);
        }

        private static void swapRows(double[][] A, int row1, int row2) {
            double[] temp = A[row1];
            A[row1] = A[row2];
            A[row2] = temp;
        }

        private static void eliminateForward(double[][] A, int col) {
            int n = A.length;
            for (int i = col + 1; i < n; i++) {
                double factor = A[i][col] / A[col][col];
                for (int j = col; j < 2 * n; j++) {
                    A[i][j] -= factor * A[col][j];
                }
            }
        }

        private static void eliminateBackward(double[][] A, int col) {
            int n = A.length;
            for (int i = col - 1; i >= 0; i--) {
                double factor = A[i][col] / A[col][col];
                for (int j = col; j < 2 * n; j++) {
                    A[i][j] -= factor * A[col][j];
                }
            }
            normalizeRow(A, col);
        }

        private static void normalizeRow(double[][] A, int col) {
            double divisor = A[col][col];
            for (int j = col; j < 2 * A.length; j++) {
                A[col][j] /= divisor;
            }
        }
    }
}




