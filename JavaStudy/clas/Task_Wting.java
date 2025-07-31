package JavaStudy.Clas;

public class Task_Wting {

    public static void main(String[] args) {

    }
}

/*
 * 完善以下类，
 * 
 * woman <- human <- Animal <- Creature
 */

interface Creature{
    String LOCATION = "Earth";
    String NAME="***";
// TODO
// NAME的默认访问权限为--public static final
    void eat(int food);
    void sleep(int time);
    void breed(int Energy);
    void introduce();// 用该函数实现多态
    default void move(){
        System.out.println("This creature is moving.");
    }
}

abstract class Animal implements Creature{
    // 种族
    String species;
    String name=NAME;
    // 实现接口
    // TODO
    public Animal(String species){
        this.species=species;
    }
    @Override
    public void eat(int food){
        System.out.println("This animal eats "+food);
    }   
    @Override
    public void sleep(int time){
        System.out.println("This animal sleeps at"+time);
    }
    @Override
    public void breed(int Energy){
        System.out.println("This animal's breed is"+Energy);
    }
    @Override
    public void introduce(){
        System.out.println("This is a/an"+species+"and names"+name);
    }

}

class Human extends Animal{
    // 字段
    // 寿命，地域（默认为 80， Asia）
    int life=80;
    String location="Asia";
    // TODO
    // 构造方法
    // 既能传寿命，又能传地域，两者一起，或者两者都不传
    public Human(){
        super("Human");
    }
    public Human(int life){
        super("Human");
        this.life=life;
    } 
    public Human(String location){
        super("Human");
        this.location=location;
    } 
    public Human(int life,String location){
        super("Human");
        this.life=life;
        this.location=location;
    } 
    public Human(int life,String location,String name){
        super("Human");
        this.name=name;
        this.life=life;
        this.location=location;
    } 
    // EAT方法希望有一个传入float的重载方法
    // TODO
    public void eat(float food){
        System.out.println("This human eats "+food);
    }   
    @Override
    public void sleep(int time){
        System.out.println("This human sleeps at"+time);
    }
    @Override
    public void breed(int Energy){
        System.out.println("This human breeds with Energy level "+Energy);
    }
    @Override
    public void introduce(){
        System.out.println("This is a human.");
    }
}

class Woman extends Human{
    @Override
    public void eat(float food){
        System.out.println("This woman eats "+food);
    }   
    @Override
    public void sleep(int time){
        System.out.println("This woman sleeps at"+time);
    }
    @Override
    public void breed(int Energy){
        System.out.println("This woman breeds with Energy level "+Energy);
    }
    @Override
    public void introduce(){
        System.out.println("This is a woman.");
    }
}

class Man extends Human{
    @Override
    public void eat(float food){
        System.out.println("This man eats "+food);
    }   
    @Override
    public void sleep(int time){
        System.out.println("This man sleeps at"+time);
    }
    @Override
    public void breed(int Energy){
        System.out.println("This man breeds with Energy level "+Energy);
    }
    @Override
    public void introduce(){
        System.out.println("This is a man.");
    }
}
