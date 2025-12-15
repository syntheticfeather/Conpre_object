package com.example.personal_loan.utils;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class IdCardUtils {
    private static final String[] AREA_CODES = {
        "11", "12", "13", "14", "15", "21", "22", "23",
        "31", "32", "33", "34", "35", "36", "37", "41",
        "42", "43", "44", "45", "46", "50", "51", "52",
        "53", "54", "61", "62", "63", "64", "65", "71",
        "81", "82", "91"
    };

    private static final int[] WEIGHT = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    /**
     * 校验身份证号是否合法（格式 + 地区 + 出生日期 + 校验码）
     */
    public static boolean isValid(String idCard) {
        if (idCard == null || idCard.length() != 18) {
            return false;
        }

        String upperId = idCard.toUpperCase();
        String areaCode = upperId.substring(0, 2);
        String birthStr = upperId.substring(6, 14);
        String checkBit = upperId.substring(17);

        // 1. 校验地区码（前两位）
        boolean validArea = false;
        for (String code : AREA_CODES) {
            if (areaCode.equals(code)) {
                validArea = true;
                break;
            }
        }
        if (!validArea) {
            return false;
        }

        // 2. 校验出生日期（YYYYMMDD）
        try {
            LocalDate birthDate = LocalDate.parse(birthStr, DateTimeFormatter.BASIC_ISO_DATE);
            LocalDate now = LocalDate.now();
            if (birthDate.isAfter(now)) {
                return false; // 出生日期不能在未来
            }
            // 可选：限制年龄范围（如 0~150 岁）
            if (Period.between(birthDate, now).getYears() > 150) {
                return false;
            }
        } catch (DateTimeParseException e) {
            return false;
        }

        // 3. 校验前17位是否全为数字
        for (int i = 0; i < 17; i++) {
            if (!Character.isDigit(upperId.charAt(i))) {
                return false;
            }
        }

        // 4. 校验最后一位（校验码）
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (upperId.charAt(i) - '0') * WEIGHT[i];
        }
        char expectedCheck = CHECK_CODES[sum % 11];
        return checkBit.charAt(0) == expectedCheck;
    }

    /**
     * 提取出生日期（格式：yyyy-MM-dd）
     */
    public static String getBirthDate(String idCard) {
        if (!isValid(idCard)) {
            return null;
        }
        String birthStr = idCard.substring(6, 14);
        return birthStr.substring(0, 4) + "-" + birthStr.substring(4, 6) + "-" + birthStr.substring(6, 8);
    }

    /**
     * 获取性别（true: 男, false: 女）
     */
    public static boolean isMale(String idCard) {
        if (!isValid(idCard)) {
            return false;
        }
        int genderBit = Character.getNumericValue(idCard.charAt(16));
        return genderBit % 2 == 1;
    }
}
