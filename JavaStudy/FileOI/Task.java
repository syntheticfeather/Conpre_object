package JavaStudy.FileOI;

/*
 * 现在，你电的电路系统崩了，没有服务器，没有网络，你只能把文件保存到本地磁盘上。
 * 不过你还可以投诉你电
 * 对于甲同学:
 *      你需要记录你班同学的投诉信息，存入到Input.txt
 *      格式为:
 *          学号_姓名_投诉时间(YYYY_MM_DD)_投诉内容
 *      同学们通过控制台输入。
 *      输入格式为:
 *          第一行 n 输入的行数
 * z        接下来 n 行,每行输入一个投诉信息
 *          学号 姓名(只有英文名) 投诉时间(YYYY MM DD) 投诉内容 (投诉内容只有英文)
 *          (中间都以一个空格区分,并且前后不会有多余的空格)
 *
 * 
 * 对于乙同学:
 *      你是大物的助教，手上有一份成绩单，里面记录了每一名同学的成绩。grade.txt
 *          格式为:
 *              学号_姓名_总分
 *      你需要从Input.txt中读取信息，
 *      然后为每一名投诉同学期末成绩 + 3，以次表扬，不过还需要在总分后面加一个 "*"。
 * 
 * 
 * 对于丙同学:
 *      你不希望所有人都可以直接查看你的这份grade.txt，
 *      所以你要把这份文件加密。
 *      加密规则:
 *          对于每一个字符，将其ASCII码加3，然后输出。
 *      例如:
 *          'a'的ASCII码为97,减3后为64,输出'@'
 *          'b'的ASCII码为98,减3后为65,输出'A'
 *      不过我们是在实验可行性，所以你不能覆写文件，而是新生成文件名为 encrypted.txt。
 * 
 * 保证:
 *      输入的总字符数量不会超过2k
 *      每次输入格式严格遵循要求
 *      保证字符不好因为凯撒加密溢出
 *      输入文件名为Input.txt
 *      输出文件名为grade.txt
 *      加密文件名为encrypted.txt
 */

class Task {

    public static void main(String[] args) {

    }

    final int MAX_SIZE = 2048; 

    public void input() {
        // TODO
        // MEDIUM
    }

    public void changeGrade() {
        // TODO
        // HARD
    }

    public void encrypt() {
        // TODO
        // MEDIUM
    }
}

// 测试输出案例
// 2
// 2021001 Alice 2023 12 01 Professor_late_to_class
// 2021005 Eva 2023 12 02 No_TA_session

// Input.txt 预期结果
// 2021001_Alice_2023_12_01_Professor_late_to_class
// 2021005_Eva_2023_12_02_No_TA_session

// grade.txt 原数据
// 2021001_Alice_85
// 2021002_Bob_78
// 2021003_Charlie_92
// 2021004_David_88
// 2021005_Eva_76
// 2021006_Frank_91
// 2021007_Grace_80
// 2021008_Henry_87
// 2021009_Ivy_73
// 2021010_Jack_89

// grade.txt 预期结果
// 2021001_Alice_88*
// 2021002_Bob_78
// 2021003_Charlie_92
// 2021004_David_88
// 2021005_Eva_79*
// 2021006_Frank_91
// 2021007_Grace_80
// 2021008_Henry_87
// 2021009_Ivy_73
// 2021010_Jack_89

// encrypted.txt 预期结果
// /-/.--.\>if`b\55'
// /-/.--/\?l_\45
// /-/.--0\@e^oifb\6/
// /-/.--1\A^sfa\55
// /-/.--2\Bs^\46'
// /-/.--3\Co^kh\6.
// /-/.--4\Do^`b\5-
// /-/.--5\Ebkov\54
// /-/.--6\Fsv\40
// /-/.-.-\G^`h\56
