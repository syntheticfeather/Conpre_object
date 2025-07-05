package JavaStudy.clas;
public class L {

    public void test() {
        man me = new man(19, "gcc", "Male", 1000);
        // me.ID = 5;// 这句将报错，虽然是public 但是 final
        // me.age = 5;// 这句将报错，age为private，不能直接修改
        me.show();
    }

    public static void main(String[] args) {
        var ins = new L();
        ins.test();
    }

}


class man {
    // 未标明访问权限，默认为public
    // me.age = 26; // 可直接修改成员变量的值，很显然不太好
    // 所以全部设置为private
    private int age;
    // final关键词 == c语言的 const 关键字，表示常量，不可修改
    final int ID;
    private String name;
    private String gender;
    private int[] score;

    public man(int age, String name, String gender, int ID) {
        // 函数名与类名相同，构造函数。
        this.age = age;
        this.name = name;
        this.gender = gender;    
        this.ID = ID;// 对于常量变量，该次赋值，将再也改变不了
    }

    // 采用权限为 public 的函数来访问 private 的 变量
    // 保护内部数据，防止被外部修改
    public void show() {
        System.out.println("ID: " + ID);
        System.out.println("年龄: " + age);
        System.out.println("姓名: " + name);
        System.out.println("性别: " + gender);
        // 该局的函数是类中的辅助函数，不向外界暴露。
        System.out.println("出生年份" + GetBirthYear());
        for(int s: this.score)
        {
            System.out.println(s);
        }
    }

    // 对必要的修改情况，仍是书写 public 函数
    public void setAge(int age) {
        this.age = age;
    }

    // 同样也有private的函数
    private int GetBirthYear()
    {
        return 2025 - age;
    }

    // 可变变量的传入
    // 与传入数组的差异
    // 两者不算方法的重载，int... 是一种更方便的书写方式
    public void RecordScore(int... score) {
        this.score = score;
    }

    // public void RecordScore(int[] score) {
    //     this.score = score;
    // }
}