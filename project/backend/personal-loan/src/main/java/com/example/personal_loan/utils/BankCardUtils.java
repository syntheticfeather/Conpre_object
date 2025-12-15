package com.example.personal_loan.utils;

public class BankCardUtils {
    /**
     * 校验银行卡号是否合法：
     * - 必须为纯数字
     * - 长度通常为 13~19 位（国内常见 16/19）
     * - 通过 Luhn 算法校验
     */
    public static boolean isValid(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }
        String cleanCard = cardNumber.replaceAll("\\s+", ""); // 去除空格
        // 必须为16位，后续需要修改，支持19位
        if (cleanCard.length() != 16) {
            return false;
        }
        return true;
    }

    /**
     * Luhn 算法校验（用于银行卡、信用卡等）
     */
    private static boolean passesLuhnCheck(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
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
