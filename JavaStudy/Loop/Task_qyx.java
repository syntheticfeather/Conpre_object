package JavaStudy.Loop;

import java.util.ArrayList;
import java.util.List;

public class Task_qyx {

    // 任务一：打印1到100之间的偶数(不使用条件判断)
    public static void printEvenNumbers() {
        for (int i = 2; i <= 100; i += 2) {
            System.out.println(i);
        }
    }

    // 任务二：找到所有的水仙花数
    public static List<Integer> findNarcissisticNumbers() {
        List<Integer> narcissisticNumbers = new ArrayList<>();
        for (int num = 100; num <= 999; num++) {
            int sumOfCubes = 0;
            int temp = num;
            while (temp > 0) {
                int digit = temp % 10;
                sumOfCubes += Math.pow(digit, 3);
                temp /= 10;
            }
            if (sumOfCubes == num) {
                narcissisticNumbers.add(num);
            }
        }
        return narcissisticNumbers;
    }

    // 任务三：判断是否是索菲亚数列
    public static boolean isSophiaSequence(int[] sequence) {
        if (sequence.length < 2) {
            return false;
        }
        for (int i = 0; i < sequence.length - 1; i++) {
            if ((sequence[i] % 2 == sequence[i + 1] % 2)) {
                return false;
            }
        }
        return true;
    }

    // 任务四：输出1 2 3 4的所有排列
    public static List<List<Integer>> generatePermutations() {
        List<List<Integer>> permutations = new ArrayList<>();
        permute(new ArrayList<>(), new int[]{1, 2, 3, 4}, permutations);
        return permutations;
    }

    private static void permute(List<Integer> current, int[] nums, List<List<Integer>> result) {
        if (current.size() == nums.length) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int num : nums) {
            if (!current.contains(num)) {
                current.add(num);
                permute(current, nums, result);
                current.remove(current.size() - 1);
            }
        }
    }
}




