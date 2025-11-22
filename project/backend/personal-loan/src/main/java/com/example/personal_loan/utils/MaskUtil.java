package com.example.personal_loan.utils;


public final class MaskUtil {
    private MaskUtil() {
        
    }

    /**
     * 手机号脱敏：13812345678 → 138****5678
     *
     */
    public static String maskPhone(String phone) {
        // 取前3位 + **** + 后4位
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 身份证脱敏：110101199003072316 → 1101011990********16
     */
    public static String maskIdCard(String idCard) {
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }
}
