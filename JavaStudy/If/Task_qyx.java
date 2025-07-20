package JavaStudy.If;

public class Task_qyx {

    // 1：计算x的值
    public static double computeX(double x) {
        if (x < 0) {
            return -1;
        } else if (x >= 0 && x < 10) {
            return Math.pow(x, 2);
        } else if (x >= 10 && x < 20) {
            return x - 10;
        } else {
            return x / 10;
        }
    }

    // 2.计算电费
    public static double calculateElectricityCost(double n) {
        if (n < 0) {
            return -1;
        } else if (n <= 15) {
            return n * 2.8;
        } else if (n <= 25) {
            return 15 * 2.8 + (n - 15) * 3.5;
        } else {
            return 15 * 2.8 + 10 * 3.5 + (n - 25) * 4.6;
        }
    }

    // 3.求分数对应的等级
    public static String determineGrade(int score) {
        if (score < 0 || score > 100) {
            return "-1";
        } else if (score < 60) {
            return "E";
        } else if (score < 70) {
            return "D";
        } else if (score < 80) {
            return "C";
        } else if (score < 90) {
            return "B";
        } else {
            return "A";
        }
    }

    // 4.判断年月日合法性
    public static boolean isValidDate(int year, int month, int day) {
        if (year <= 0) {
            return false;
        }
        if (month < 1 || month > 12) {
            return false;
        }
        int maxDaysInMonth;
        switch (month) {
            case 1: case 3: case 5: case 7: case 8: case 10: case 12:
                maxDaysInMonth = 31;
                break;
            case 4: case 6: case 9: case 11:
                maxDaysInMonth = 30;
                break;
            case 2:
                if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
                    maxDaysInMonth = 29; // 闰年
                } else {
                    maxDaysInMonth = 28; // 平年
                }
                break;
            default:
                return false;
        }
        return day >= 1 && day <= maxDaysInMonth;
    }

    // 5.人工智能回复
    public static String getAIResponse(String input, double amount) {
        if ("你好".equals(input) || "hello".equals(input)) {
            return "您好，请问有什么可以帮助您？";
        }
        if (input.contains("价格") && input.contains("优惠")) {
            return "当前周年庆全场8折";
        }
        if (input.contains("退货") && amount > 500) {
            return "请联系VIP客服专线400-xxxx";
        }
        if (input.contains("?") && input.length() > 10) {
            return "您的问题已记录，稍后回复";
        }
        return "请详细描述您的问题。";
    }
}



