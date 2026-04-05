package com.example.personal_loan.utils;

public class BankCardUtils {

    /**
     * 校验银行卡号是否合法：
     * - 必须为纯数字（允许包含空格）
     * - 长度通常为 16~19 位
     * - 通过 Luhn 算法校验
     */
    public static boolean isValid(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }
        // 1. 去除空格
        String cleanCard = cardNumber.replaceAll("\\s+", "");

        // 2. 校验是否为纯数字且长度在合理范围内 (16-19位是常见标准)
        if (!cleanCard.matches("\\d{16,19}")) {
            return false;
        }

        // 3. 执行Luhn算法校验
        return passesLuhnCheck(cleanCard);
    }

    /**
     * Luhn 算法校验（用于银行卡、信用卡等）
     */
    private static boolean passesLuhnCheck(String cardNumber) {
        int sum = 0;
        boolean alternate = false; // 用于标记是否需要加倍
        // 从右向左遍历
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            if (alternate) {
                digit *= 2;
                // 如果加倍后大于9，则减去9 (等同于将两位数的个位与十位相加)
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        // 如果总和能被10整除，则校验通过
        return sum % 10 == 0;
    }

    /**
     * 获取银行卡号后四位（用于展示）
     */
    public static String maskCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String clean = cardNumber.replaceAll("\\s+", "");
        if (clean.length() < 4) return "****";
        return "**** **** **** " + clean.substring(clean.length() - 4);
    }
}